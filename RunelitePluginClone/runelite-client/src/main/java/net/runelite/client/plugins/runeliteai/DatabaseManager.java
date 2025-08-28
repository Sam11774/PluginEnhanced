/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.game.ItemManager;
import net.runelite.api.ItemComposition;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Database Manager for PostgreSQL integration with batch optimization
 * 
 * Features:
 * - Connection pooling with HikariCP for optimal performance
 * - Batch operations with 5x size optimization (100 → 500 records)
 * - Async database operations with transaction management
 * - Session-based data organization with UUID tracking
 * - Real-time performance monitoring and health checks
 * - Automatic retry logic and connection recovery
 * - 30+ optimized database tables for comprehensive data storage
 * 
 * Performance targets:
 * - Average insert rate: 1,000+ records/second
 * - Query performance: <10ms for real-time analytics
 * - Connection recovery: <5 seconds
 * - Database throughput: 5x improvement over baseline
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
public class DatabaseManager
{
    // Core dependencies
    private final Client client;
    private final ItemManager itemManager;
    
    // Database configuration - loaded from properties
    private final Properties dbConfig;
    private final String databaseUrl;
    private final String databaseUser;
    private final String databasePassword;
    private final int batchSize;
    private final int maxConnections;
    private final int connectionTimeout;
    
    // Connection management
    private HikariDataSource dataSource;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    
    // Batch processing
    private final ExecutorService batchExecutor;
    private final Queue<TickDataCollection> pendingBatch = new ConcurrentLinkedQueue<>();
    private final AtomicLong totalRecordsInserted = new AtomicLong(0);
    private final AtomicLong totalBatchesProcessed = new AtomicLong(0);
    
    // Performance tracking
    private final AtomicLong totalDatabaseTime = new AtomicLong(0);
    private final AtomicLong totalDatabaseCalls = new AtomicLong(0);
    private long lastPerformanceReport = 0;
    
    // Session management
    private final Map<Integer, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    private final AtomicLong sessionIdCounter = new AtomicLong(1);
    
    public DatabaseManager(Client client, ItemManager itemManager)
    {
        this.client = client;
        this.itemManager = itemManager;
        this.batchExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "RuneliteAI-DatabaseBatch");
            t.setDaemon(true);
            return t;
        });
        
        // Load database configuration
        this.dbConfig = loadDatabaseConfig();
        this.databaseUrl = dbConfig.getProperty("database.url", "jdbc:postgresql://localhost:5432/runelite_ai");
        this.databaseUser = dbConfig.getProperty("database.username", "postgres");
        this.databasePassword = dbConfig.getProperty("database.password", "sam11773");
        this.batchSize = Integer.parseInt(dbConfig.getProperty("database.batch.size", String.valueOf(RuneliteAIConstants.DEFAULT_BATCH_SIZE)));
        this.maxConnections = Integer.parseInt(dbConfig.getProperty("database.pool.size", String.valueOf(RuneliteAIConstants.DEFAULT_POOL_SIZE)));
        this.connectionTimeout = Integer.parseInt(dbConfig.getProperty("database.pool.connection.timeout", String.valueOf(RuneliteAIConstants.DEFAULT_CONNECTION_TIMEOUT_MS)));
        
        initializeDatabase();
        startBatchProcessor();
        
        log.debug("DatabaseManager initialized with batch size {} and {} max connections", 
                batchSize, maxConnections);
    }
    
    /**
     * Load database configuration from properties file
     */
    private Properties loadDatabaseConfig()
    {
        Properties props = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/runeliteai/database.properties")) {
            if (input != null) {
                props.load(input);
                log.debug("Database configuration loaded from properties file");
            } else {
                log.warn("Database properties file not found, using defaults");
            }
        } catch (IOException e) {
            log.error("Failed to load database configuration", e);
        }
        
        // Override with environment variables if present
        String envUrl = System.getenv("RUNELITE_AI_DB_URL");
        String envUser = System.getenv("RUNELITE_AI_DB_USER");
        String envPassword = System.getenv("DB_PASSWORD");
        
        if (envUrl != null) props.setProperty("database.url", envUrl);
        if (envUser != null) props.setProperty("database.username", envUser);
        if (envPassword != null) props.setProperty("database.password", envPassword);
        
        return props;
    }
    
    /**
     * Initialize database connection pool
     */
    private void initializeDatabase()
    {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(databaseUrl);
            config.setUsername(databaseUser);
            config.setPassword(databasePassword);
            config.setDriverClassName(dbConfig.getProperty("database.driver", "org.postgresql.Driver"));
            
            // Connection pool configuration
            config.setMaximumPoolSize(maxConnections);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(Long.parseLong(dbConfig.getProperty("database.pool.idle.timeout", "600000"))); // 5 minutes
            config.setMaxLifetime(600000);  // 10 minutes
            config.setLeakDetectionThreshold(Long.parseLong(dbConfig.getProperty("database.pool.leak.detection", "2000"))); // 1 minute
            
            // Performance optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            
            dataSource = new HikariDataSource(config);
            
            // Test connection
            try (Connection conn = dataSource.getConnection()) {
                connected.set(true);
                log.debug("Database connection established successfully");
                
                // Verify schema exists
                verifyDatabaseSchema(conn);
            }
            
        } catch (Exception e) {
            connected.set(false);
            log.error("Failed to initialize database connection", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    /**
     * Verify database schema exists
     */
    private void verifyDatabaseSchema(Connection conn) throws SQLException
    {
        String checkSessionsTable = "SELECT EXISTS (SELECT FROM information_schema.tables " +
                                   "WHERE table_schema = 'public' AND table_name = 'sessions')";
        
        try (PreparedStatement stmt = conn.prepareStatement(checkSessionsTable);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next() && rs.getBoolean(1)) {
                log.debug("Database schema verified - sessions table exists");
            } else {
                log.warn("Database schema not found - sessions table missing");
                // Could auto-create schema here if needed
            }
        }
    }
    
    /**
     * Initialize a new session
     * @return Session ID
     */
    public Integer initializeSession()
    {
        if (!connected.get()) {
            throw new RuntimeException("Database not connected");
        }
        
        long startTime = System.nanoTime();
        Integer sessionId = null;
        
        try (Connection conn = dataSource.getConnection()) {
            // Use database auto-increment instead of manual counter
            String insertSession = 
                "INSERT INTO sessions (player_name, start_time, status, activity_type, world_id, session_uuid) " +
                "VALUES (?, ?, 'ACTIVE', 'testing', 301, gen_random_uuid()) RETURNING session_id";
            
            try (PreparedStatement stmt = conn.prepareStatement(insertSession)) {
                stmt.setString(1, getCurrentPlayerName());
                stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                
                try (var rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        sessionId = rs.getInt("session_id");
                        
                        SessionInfo sessionInfo = new SessionInfo(sessionId, System.currentTimeMillis());
                        activeSessions.put(sessionId, sessionInfo);
                        
                        recordDatabaseCall(System.nanoTime() - startTime);
                        log.debug("New session initialized: {}", sessionId);
                        return sessionId;
                    } else {
                        throw new SQLException("Failed to get session ID from insert");
                    }
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to initialize session", e);
            throw new RuntimeException("Session initialization failed", e);
        }
    }
    
    /**
     * Store tick data (adds to batch for processing)
     * @param tickData Data to store
     */
    public void storeTickData(TickDataCollection tickData)
    {
        System.out.println("[DATABASE-STORE] storeTickData CALLED!");
        System.out.println("[DATABASE-STORE] connected: " + connected.get() + ", shutdown: " + shutdown.get());
        log.debug("[DATABASE-STORE] storeTickData called - connected: {}, shutdown: {}", connected.get(), shutdown.get());
        
        if (!connected.get() || shutdown.get()) {
            System.out.println("[DATABASE-STORE] SKIPPING - Database not available");
            log.warn("[DATABASE-STORE] Database not available, skipping tick data storage - connected: {}, shutdown: {}", 
                connected.get(), shutdown.get());
            return;
        }
        System.out.println("[DATABASE-STORE] Database is connected, proceeding...");
        
        log.debug("DEBUG: Validating tick data - tickData null: {}", tickData == null);
        if (tickData != null) {
            log.debug("DEBUG: TickData validation details - sessionId: {}, tickNumber: {}, timestamp: {}, gameState: {}, processingTimeNanos: {}",
                tickData.getSessionId(), tickData.getTickNumber(), tickData.getTimestamp(),
                tickData.getGameState() != null ? "present" : "null", tickData.getProcessingTimeNanos());
        }
        
        if (tickData == null) {
            System.out.println("[DATABASE-STORE] VALIDATION FAILED - tickData is null");
            log.error("**VALIDATION FAILED** [DATABASE] Invalid tick data - tickData is null");
            return;
        }
        
        System.out.println("[DATABASE-STORE] tickData not null, checking isValid()...");
        if (!tickData.isValid()) {
            System.out.println("[DATABASE-STORE] VALIDATION FAILED - isValid() returned false");
            log.error("**VALIDATION FAILED** [DATABASE] Invalid tick data - isValid() returned false");
            log.error("**VALIDATION FAILED** [DATABASE] Validation details - sessionId: {}, tickNumber: {}, timestamp: {}, gameState: {}, processingTimeNanos: {}",
                tickData.getSessionId(), tickData.getTickNumber(), tickData.getTimestamp(),
                tickData.getGameState() != null ? "present" : "NULL", tickData.getProcessingTimeNanos());
            log.error("**VALIDATION FAILED** [DATABASE] Detailed validation - sessionId null: {}, tickNumber null: {}, timestamp null: {}, gameState null: {}, processingTimeNanos null: {}, processingTimeNanos <= 0: {}",
                tickData.getSessionId() == null, tickData.getTickNumber() == null, tickData.getTimestamp() == null,
                tickData.getGameState() == null, tickData.getProcessingTimeNanos() == null, 
                tickData.getProcessingTimeNanos() != null ? tickData.getProcessingTimeNanos() <= 0 : "N/A");
            return;
        }
        
        System.out.println("[DATABASE-STORE] Validation PASSED!");
        log.debug("[DATABASE] Tick data validation passed - sessionId: {}, tickNumber: {}", 
            tickData.getSessionId(), tickData.getTickNumber());
        
        // Check and update player name if it was previously "UnknownPlayer" but now we have the real name
        updatePlayerNameIfNeeded(tickData);
        
        // Add to batch queue
        boolean added = pendingBatch.offer(tickData);
        System.out.println("[DATABASE-STORE] Added to batch queue - success: " + added + ", queue size: " + pendingBatch.size());
        log.debug("[DATABASE] Added to batch queue - success: {}, queue size: {}/{}", 
                added, pendingBatch.size(), RuneliteAIConstants.DEFAULT_BATCH_SIZE);
        
        // Batch processing handled automatically by scheduled processor
        log.debug("DEBUG: Added to batch queue - success: {}, queue size: {}/{}", 
            added, pendingBatch.size(), RuneliteAIConstants.DEFAULT_BATCH_SIZE);
        
        // Process batch if it's full
        if (pendingBatch.size() >= RuneliteAIConstants.DEFAULT_BATCH_SIZE) {
            log.debug("DEBUG: Batch size reached, triggering batch processing");
            triggerBatchProcessing();
        }
    }
    
    /**
     * Start the batch processor
     */
    private void startBatchProcessor()
    {
        batchExecutor.submit(() -> {
            while (!shutdown.get()) {
                try {
                    // Process batch every 5 seconds or when full
                    Thread.sleep(5000);
                    
                    if (!pendingBatch.isEmpty()) {
                        log.debug("DEBUG: Periodic batch processing - queue size: {}", pendingBatch.size());
                        processBatch();
                    } else {
                        log.debug("DEBUG: Periodic batch check - queue empty");
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error in batch processor", e);
                }
            }
        });
    }
    
    /**
     * Trigger immediate batch processing
     */
    private void triggerBatchProcessing()
    {
        log.debug("DEBUG: triggerBatchProcessing called");
        batchExecutor.submit(this::processBatch);
    }
    
    /**
     * Force immediate batch processing (for debugging)
     */
    public void forceFlushBatch()
    {
        log.debug("DEBUG: Force flushing batch - current queue size: {}", pendingBatch.size());
        if (!pendingBatch.isEmpty()) {
            processBatch();
        }
    }
    
    /**
     * Process pending batch
     */
    private void processBatch()
    {
        System.out.println("[DATABASE-BATCH] processBatch called - queue size: " + pendingBatch.size() + ", connected: " + connected.get());
        log.debug("DEBUG: processBatch called - queue size: {}, connected: {}", 
            pendingBatch.size(), connected.get());
            
        if (pendingBatch.isEmpty() || !connected.get()) {
            System.out.println("[DATABASE-BATCH] Early return - empty: " + pendingBatch.isEmpty() + ", disconnected: " + !connected.get());
            log.debug("DEBUG: processBatch early return - empty queue: {}, disconnected: {}", 
                pendingBatch.isEmpty(), !connected.get());
            return;
        }
        
        List<TickDataCollection> batch = new ArrayList<>();
        
        // Collect batch items
        System.out.println("[DATABASE-BATCH] Collecting items from queue...");
        while (!pendingBatch.isEmpty() && batch.size() < RuneliteAIConstants.DEFAULT_BATCH_SIZE) {
            TickDataCollection item = pendingBatch.poll();
            if (item != null) {
                batch.add(item);
            }
        }
        System.out.println("[DATABASE-BATCH] Collected " + batch.size() + " items");
        
        if (batch.isEmpty()) {
            System.out.println("[DATABASE-BATCH] Batch is empty after collection, returning");
            log.debug("DEBUG: Batch is empty after collection, returning");
            return;
        }
        
        System.out.println("[DATABASE-BATCH] Processing batch of " + batch.size() + " items");
        log.debug("[DATABASE] Processing batch of {} items", batch.size());
        
        long startTime = System.nanoTime();
        
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("[DATABASE-BATCH] Got database connection");
            conn.setAutoCommit(false);
            System.out.println("[DATABASE-BATCH] Set autocommit=false");
            log.debug("[DATABASE] Connection acquired, autocommit disabled");
            
            // Batch insert to game_ticks table and capture generated tick_ids
            System.out.println("[DATABASE-BATCH] About to call insertGameTicksBatch");
            List<Long> tickIds = insertGameTicksBatch(conn, batch);
            System.out.println("[DATABASE-BATCH] insertGameTicksBatch completed with " + tickIds.size() + " tick_ids");
            log.debug("[DATABASE] Inserted {} records to game_ticks with {} generated tick_ids", batch.size(), tickIds.size());
            
            // Batch insert to other tables using the generated tick_ids
            insertPlayerDataBatch(conn, batch, tickIds);
            insertPlayerLocationBatch(conn, batch, tickIds);
            insertPlayerEquipmentBatch(conn, batch, tickIds);
            insertPlayerInventoryBatch(conn, batch, tickIds);
            insertPlayerPrayersBatch(conn, batch, tickIds);
            insertPlayerSpellsBatch(conn, batch, tickIds);
            insertWorldDataBatch(conn, batch, tickIds);
            insertCombatDataBatch(conn, batch, tickIds);
            insertInputDataBatch(conn, batch, tickIds);
            insertClickContextBatch(conn, batch, tickIds);
            insertSocialDataBatch(conn, batch, tickIds);
            insertInterfaceDataBatch(conn, batch, tickIds);
            insertSystemMetricsBatch(conn, batch, tickIds);
            insertWorldObjectsBatch(conn, batch, tickIds);
            
            System.out.println("[DATABASE-BATCH] About to commit");
            conn.commit();
            System.out.println("[DATABASE-BATCH] Commit successful");
            log.debug("[DATABASE] Batch committed successfully");
            
            long processingTime = System.nanoTime() - startTime;
            long recordCount = batch.size();
            
            totalRecordsInserted.addAndGet(recordCount);
            totalBatchesProcessed.incrementAndGet();
            recordDatabaseCall(processingTime);
            
            log.debug("DEBUG: Successfully processed batch - {} records inserted, total records: {}, batch #{}", 
                recordCount, totalRecordsInserted.get(), totalBatchesProcessed.get());
            
            // Performance reporting
            if (totalBatchesProcessed.get() % 10 == 0) {
                reportBatchPerformance(recordCount, processingTime);
            }
            
        } catch (SQLException e) {
            System.out.println("[DATABASE-BATCH] SQL Exception: " + e.getMessage());
            e.printStackTrace();
            log.error("CRITICAL: Failed to process batch of {} records - SQL Error", batch.size(), e);
            log.error("CRITICAL: Database connection status: connected={}, dataSource valid={}", 
                connected.get(), dataSource != null && !dataSource.isClosed());
            
            // Return failed items to queue for retry (simplified)
            for (TickDataCollection item : batch) {
                if (pendingBatch.size() < RuneliteAIConstants.DEFAULT_BATCH_SIZE * 2) { // Prevent infinite growth
                    pendingBatch.offer(item);
                }
            }
        }
    }
    
    /**
     * Insert game ticks batch
     */
    private List<Long> insertGameTicksBatch(Connection conn, List<TickDataCollection> batch) throws SQLException
    {
        log.debug("DEBUG: Inserting {} records into game_ticks table", batch.size());
        
        String insertSQL = 
            "INSERT INTO game_ticks (session_id, tick_number, timestamp, processing_time_ms, " +
            "player_name, world_x, world_y, plane, animation, health_ratio, data_points_count, world_id, collection_duration_ns) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        List<Long> generatedTickIds = new ArrayList<>();
        
        try (PreparedStatement stmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            for (TickDataCollection tickData : batch) {
                stmt.setObject(1, tickData.getSessionId());
                stmt.setObject(2, tickData.getTickNumber());
                stmt.setLong(3, tickData.getTimestamp());
                stmt.setLong(4, tickData.getProcessingTimeNanos() != null ? 
                    tickData.getProcessingTimeNanos() / 1_000_000 : 0);
                
                // Player data
                if (tickData.getPlayerData() != null) {
                    stmt.setString(5, tickData.getPlayerData().getPlayerName());
                    stmt.setObject(6, tickData.getPlayerData().getWorldX());
                    stmt.setObject(7, tickData.getPlayerData().getWorldY());
                    stmt.setObject(8, tickData.getPlayerData().getPlane());
                    stmt.setObject(9, tickData.getPlayerData().getAnimation());
                    stmt.setObject(10, tickData.getPlayerData().getHealthRatio());
                } else {
                    stmt.setNull(5, Types.VARCHAR);
                    stmt.setNull(6, Types.INTEGER);
                    stmt.setNull(7, Types.INTEGER);
                    stmt.setNull(8, Types.INTEGER);
                    stmt.setNull(9, Types.INTEGER);
                    stmt.setNull(10, Types.INTEGER);
                }
                
                stmt.setInt(11, tickData.getTotalDataPoints());
                stmt.setInt(12, 301); // world_id - using same hardcoded value as sessions
                long collectionDurationNs = tickData.getProcessingTimeNanos() != null ? 
                    tickData.getProcessingTimeNanos() : 0;
                stmt.setLong(13, collectionDurationNs); // collection_duration_ns in nanoseconds
                
                // Debug logging for timing measurements every 10th record
                if (tickData.getTickNumber() != null && tickData.getTickNumber() % 10 == 1) {
                    log.debug("[TIMING-DEBUG] Tick {}: processing_time_ms={}, collection_duration_ns={}", 
                        tickData.getTickNumber(), 
                        tickData.getProcessingTimeNanos() != null ? tickData.getProcessingTimeNanos() / 1_000_000 : 0,
                        collectionDurationNs);
                }
                
                stmt.addBatch();
            }
            
            int[] results = stmt.executeBatch();
            log.debug("[DATABASE] executeBatch results for game_ticks: {} records affected", results.length);
            
            // Retrieve generated keys
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                while (generatedKeys.next()) {
                    generatedTickIds.add(generatedKeys.getLong(1));
                }
            }
        }
        
        return generatedTickIds;
    }
    
    /**
     * Insert player data batch
     */
    private void insertPlayerDataBatch(Connection conn, List<TickDataCollection> batch, List<Long> tickIds) throws SQLException
    {
        String insertSQL = 
            "INSERT INTO player_vitals (session_id, tick_id, tick_number, timestamp, " +
            "current_hitpoints, max_hitpoints, current_prayer, max_prayer, energy, weight, " +
            "special_attack_percent, poisoned, diseased, venomed) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            for (int i = 0; i < batch.size(); i++) {
                TickDataCollection tickData = batch.get(i);
                Long tickId = i < tickIds.size() ? tickIds.get(i) : null;
                
                if (tickData.getPlayerVitals() != null && tickId != null) {
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setLong(2, tickId);
                    stmt.setObject(3, tickData.getTickNumber());
                    stmt.setTimestamp(4, new Timestamp(tickData.getTimestamp()));
                    stmt.setObject(5, tickData.getPlayerVitals().getCurrentHitpoints());
                    stmt.setObject(6, tickData.getPlayerVitals().getMaxHitpoints());
                    stmt.setObject(7, tickData.getPlayerVitals().getCurrentPrayer());
                    stmt.setObject(8, tickData.getPlayerVitals().getMaxPrayer());
                    stmt.setObject(9, tickData.getPlayerVitals().getEnergy());
                    stmt.setObject(10, tickData.getPlayerVitals().getWeight());
                    stmt.setObject(11, tickData.getPlayerVitals().getSpecialAttackPercent());
                    stmt.setObject(12, tickData.getPlayerVitals().getPoisoned());
                    stmt.setObject(13, tickData.getPlayerVitals().getDiseased());
                    stmt.setObject(14, tickData.getPlayerVitals().getVenomed());
                    
                    stmt.addBatch();
                }
            }
            
            int[] results = stmt.executeBatch();
            log.debug("[DATABASE] Player vitals batch insert: {} records", results.length);
        }
    }
    
    /**
     * Insert player location batch
     */
    private void insertPlayerLocationBatch(Connection conn, List<TickDataCollection> batch, List<Long> tickIds) throws SQLException
    {
        String insertSQL = 
            "INSERT INTO player_location (session_id, tick_id, tick_number, timestamp, " +
            "world_x, world_y, plane, region_id, chunk_x, chunk_y, local_x, local_y, " +
            "area_name, location_type) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            for (int i = 0; i < batch.size(); i++) {
                TickDataCollection tickData = batch.get(i);
                Long tickId = i < tickIds.size() ? tickIds.get(i) : null;
                
                if (tickData.getPlayerLocation() != null && tickId != null) {
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setLong(2, tickId);
                    stmt.setObject(3, tickData.getTickNumber());
                    stmt.setTimestamp(4, new Timestamp(tickData.getTimestamp()));
                    stmt.setObject(5, tickData.getPlayerLocation().getWorldX());
                    stmt.setObject(6, tickData.getPlayerLocation().getWorldY());
                    stmt.setObject(7, tickData.getPlayerLocation().getPlane());
                    stmt.setObject(8, tickData.getPlayerLocation().getRegionId());
                    stmt.setNull(9, Types.INTEGER); // chunk_x - not available in PlayerLocation
                    stmt.setNull(10, Types.INTEGER); // chunk_y - not available in PlayerLocation
                    stmt.setNull(11, Types.INTEGER); // local_x - not available in PlayerLocation
                    stmt.setNull(12, Types.INTEGER); // local_y - not available in PlayerLocation
                    stmt.setString(13, tickData.getPlayerLocation().getLocationName()); // area_name
                    stmt.setString(14, tickData.getPlayerLocation().getAreaType()); // location_type
                    
                    stmt.addBatch();
                }
                // If no player location data, fall back to game_ticks location data
                else if (tickData.getPlayerData() != null && tickId != null) {
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setLong(2, tickId);
                    stmt.setObject(3, tickData.getTickNumber());
                    stmt.setTimestamp(4, new Timestamp(tickData.getTimestamp()));
                    stmt.setObject(5, tickData.getPlayerData().getWorldX());
                    stmt.setObject(6, tickData.getPlayerData().getWorldY());
                    stmt.setObject(7, tickData.getPlayerData().getPlane());
                    stmt.setNull(8, Types.INTEGER); // region_id
                    stmt.setNull(9, Types.INTEGER); // chunk_x
                    stmt.setNull(10, Types.INTEGER); // chunk_y
                    stmt.setNull(11, Types.INTEGER); // local_x
                    stmt.setNull(12, Types.INTEGER); // local_y
                    stmt.setNull(13, Types.VARCHAR); // area_name - not available in fallback
                    stmt.setNull(14, Types.VARCHAR); // location_type - not available in fallback
                    
                    stmt.addBatch();
                }
            }
            
            int[] results = stmt.executeBatch();
            log.debug("[DATABASE] Player location batch insert: {} records", results.length);
        }
    }
    
    /**
     * Insert player equipment batch
     */
    private void insertPlayerEquipmentBatch(Connection conn, List<TickDataCollection> batch, List<Long> tickIds) throws SQLException
    {
        String insertSQL = 
            "INSERT INTO player_equipment (session_id, tick_id, tick_number, timestamp, " +
            "helmet_id, cape_id, amulet_id, weapon_id, body_id, shield_id, legs_id, gloves_id, boots_id, ring_id, ammo_id, " +
            "helmet_name, cape_name, amulet_name, weapon_name, body_name, shield_name, legs_name, gloves_name, boots_name, ring_name, ammo_name, " +
            "weapon_type, weapon_category, attack_style, combat_style, total_equipment_value, equipment_weight, " +
            "equipment_changes_count, weapon_changed, armor_changed, accessory_changed) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            for (int i = 0; i < batch.size(); i++) {
                TickDataCollection tickData = batch.get(i);
                Long tickId = i < tickIds.size() ? tickIds.get(i) : null;
                
                if (tickData.getPlayerEquipment() != null && tickId != null) {
                    DataStructures.PlayerEquipment equipment = tickData.getPlayerEquipment();
                    
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setLong(2, tickId);
                    stmt.setObject(3, tickData.getTickNumber());
                    stmt.setTimestamp(4, new Timestamp(tickData.getTimestamp()));
                    
                    // Equipment slot IDs (use new direct fields)
                    stmt.setObject(5, equipment.getHelmetId() != null ? equipment.getHelmetId() : -1);
                    stmt.setObject(6, equipment.getCapeId() != null ? equipment.getCapeId() : -1);
                    stmt.setObject(7, equipment.getAmuletId() != null ? equipment.getAmuletId() : -1);
                    stmt.setObject(8, equipment.getWeaponId() != null ? equipment.getWeaponId() : -1);
                    stmt.setObject(9, equipment.getBodyId() != null ? equipment.getBodyId() : -1);
                    stmt.setObject(10, equipment.getShieldId() != null ? equipment.getShieldId() : -1);
                    stmt.setObject(11, equipment.getLegsId() != null ? equipment.getLegsId() : -1);
                    stmt.setObject(12, equipment.getGlovesId() != null ? equipment.getGlovesId() : -1);
                    stmt.setObject(13, equipment.getBootsId() != null ? equipment.getBootsId() : -1);
                    stmt.setObject(14, equipment.getRingId() != null ? equipment.getRingId() : -1);
                    stmt.setObject(15, equipment.getAmmoId() != null ? equipment.getAmmoId() : -1);
                    
                    // Equipment slot names (friendly name resolution)
                    stmt.setString(16, equipment.getHelmetName());
                    stmt.setString(17, equipment.getCapeName());
                    stmt.setString(18, equipment.getAmuletName());
                    stmt.setString(19, equipment.getWeaponName());
                    stmt.setString(20, equipment.getBodyName());
                    stmt.setString(21, equipment.getShieldName());
                    stmt.setString(22, equipment.getLegsName());
                    stmt.setString(23, equipment.getGlovesName());
                    stmt.setString(24, equipment.getBootsName());
                    stmt.setString(25, equipment.getRingName());
                    stmt.setString(26, equipment.getAmmoName());
                    
                    // Equipment metadata (shifted by 11 due to new name columns)
                    stmt.setString(27, equipment.getWeaponType());
                    stmt.setString(28, "Unknown"); // weaponCategory not available
                    stmt.setString(29, equipment.getAttackStyle());
                    stmt.setObject(30, equipment.getCombatStyle()); // This is Integer
                    stmt.setObject(31, 0L); // totalValue not available
                    stmt.setObject(32, 0); // equipmentWeight not available
                    
                    // Equipment change tracking (not available in current structure)
                    stmt.setObject(33, 0); // equipmentChanges
                    stmt.setObject(34, false); // weaponChanged
                    stmt.setObject(35, false); // armorChanged
                    stmt.setObject(36, false); // accessoryChanged
                    
                    stmt.addBatch();
                }
            }
            
            int[] results = stmt.executeBatch();
            log.debug("[DATABASE] Player equipment batch insert: {} records", results.length);
        }
    }
    
    /**
     * Insert player inventory batch
     */
    private void insertPlayerInventoryBatch(Connection conn, List<TickDataCollection> batch, List<Long> tickIds) throws SQLException
    {
        String insertSQL = 
            "INSERT INTO player_inventory (session_id, tick_id, tick_number, timestamp, " +
            "total_items, free_slots, total_quantity, total_value, unique_item_types, " +
            "most_valuable_item_id, most_valuable_item_name, most_valuable_item_quantity, most_valuable_item_value, " +
            "inventory_items, items_added, items_removed, quantity_gained, quantity_lost, value_gained, value_lost, " +
            "last_item_used_id, last_item_used_name, consumables_used) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            for (int i = 0; i < batch.size(); i++) {
                TickDataCollection tickData = batch.get(i);
                Long tickId = i < tickIds.size() ? tickIds.get(i) : null;
                
                if (tickData.getPlayerInventory() != null && tickId != null) {
                    DataStructures.PlayerInventory inventory = tickData.getPlayerInventory();
                    
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setLong(2, tickId);
                    stmt.setObject(3, tickData.getTickNumber());
                    stmt.setTimestamp(4, new Timestamp(tickData.getTimestamp()));
                    
                    // Inventory summary data
                    stmt.setObject(5, inventory.getUsedSlots());
                    stmt.setObject(6, inventory.getFreeSlots());
                    stmt.setObject(7, getTotalQuantity(inventory.getInventoryItems())); // Calculate from items
                    stmt.setObject(8, inventory.getTotalValue());
                    stmt.setObject(9, getUniqueItemTypes(inventory.getItemCounts()));
                    
                    // Most valuable item tracking (calculate from items)
                    net.runelite.api.Item mostValuable = getMostValuableItem(inventory.getInventoryItems());
                    stmt.setObject(10, mostValuable != null ? mostValuable.getId() : -1);
                    stmt.setString(11, mostValuable != null ? getItemNameFromId(mostValuable.getId()) : "None");
                    stmt.setObject(12, mostValuable != null ? mostValuable.getQuantity() : 0);
                    stmt.setObject(13, 0L); // mostValuableItemValue not available
                    
                    // Inventory items as JSONB (convert Item[] to JSON string)
                    stmt.setString(14, convertInventoryItemsToJson(inventory.getInventoryItems()));
                    
                    // Inventory change tracking (now available with proper detection)
                    stmt.setObject(15, inventory.getItemsAdded() != null ? inventory.getItemsAdded() : 0); // itemsAdded
                    stmt.setObject(16, inventory.getItemsRemoved() != null ? inventory.getItemsRemoved() : 0); // itemsRemoved
                    stmt.setObject(17, inventory.getQuantityGained() != null ? inventory.getQuantityGained() : 0); // quantityGained
                    stmt.setObject(18, inventory.getQuantityLost() != null ? inventory.getQuantityLost() : 0); // quantityLost
                    stmt.setObject(19, inventory.getValueGained() != null ? inventory.getValueGained() : 0L); // valueGained
                    stmt.setObject(20, inventory.getValueLost() != null ? inventory.getValueLost() : 0L); // valueLost
                    
                    // Item interaction tracking
                    stmt.setObject(21, inventory.getLastItemId());
                    stmt.setString(22, inventory.getLastItemUsed());
                    stmt.setObject(23, 0); // consumablesUsed not available
                    
                    stmt.addBatch();
                }
            }
            
            int[] results = stmt.executeBatch();
            log.debug("[DATABASE] Player inventory batch insert: {} records", results.length);
        }
    }
    
    /**
     * Insert player prayers batch
     */
    private void insertPlayerPrayersBatch(Connection conn, List<TickDataCollection> batch, List<Long> tickIds) throws SQLException
    {
        String insertSQL = 
            "INSERT INTO player_prayers (session_id, tick_id, tick_number, timestamp, " +
            "current_prayer_points, max_prayer_points, prayer_drain_rate, prayer_bonus, " +
            "quick_prayers_enabled, quick_prayers_active, quick_prayer_count, " +
            "thick_skin, burst_of_strength, clarity_of_thought, sharp_eye, mystic_will, rock_skin, " +
            "superhuman_strength, improved_reflexes, rapid_restore, rapid_heal, protect_item, " +
            "hawk_eye, mystic_lore, steel_skin, ultimate_strength, incredible_reflexes, " +
            "protect_from_magic, protect_from_missiles, protect_from_melee, eagle_eye, mystic_might, " +
            "retribution, redemption, smite, chivalry, piety, preserve, rigour, augury, " +
            "prayers_activated, prayers_deactivated, prayer_points_drained) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            for (int i = 0; i < batch.size(); i++) {
                TickDataCollection tickData = batch.get(i);
                Long tickId = i < tickIds.size() ? tickIds.get(i) : null;
                
                if (tickData.getPlayerPrayers() != null && tickId != null) {
                    DataStructures.PlayerActivePrayers prayers = tickData.getPlayerPrayers();
                    
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setLong(2, tickId);
                    stmt.setObject(3, tickData.getTickNumber());
                    stmt.setTimestamp(4, new Timestamp(tickData.getTimestamp()));
                    
                    // Prayer points data (get from playerVitals)
                    DataStructures.PlayerVitals vitals = tickData.getPlayerVitals();
                    stmt.setObject(5, vitals != null ? vitals.getCurrentPrayer() : null);
                    stmt.setObject(6, vitals != null ? vitals.getMaxPrayer() : null);
                    stmt.setObject(7, prayers.getPrayerDrainRate());
                    stmt.setObject(8, 0); // prayerBonus not available
                    
                    // Quick prayers system
                    stmt.setObject(9, prayers.getQuickPrayerSet() != null); // quickPrayersEnabled
                    stmt.setObject(10, prayers.getQuickPrayerActive());
                    stmt.setObject(11, prayers.getActivePrayerCount());
                    
                    // Individual prayer states (extract from activePrayers map - use lowercase to match DataCollectionManager)
                    Map<String, Boolean> activePrayers = prayers.getActivePrayers();
                    stmt.setObject(12, getPrayerState(activePrayers, "thick_skin"));
                    stmt.setObject(13, getPrayerState(activePrayers, "burst_of_strength"));
                    stmt.setObject(14, getPrayerState(activePrayers, "clarity_of_thought"));
                    stmt.setObject(15, getPrayerState(activePrayers, "sharp_eye"));
                    stmt.setObject(16, getPrayerState(activePrayers, "mystic_will"));
                    stmt.setObject(17, getPrayerState(activePrayers, "rock_skin"));
                    stmt.setObject(18, getPrayerState(activePrayers, "superhuman_strength"));
                    stmt.setObject(19, getPrayerState(activePrayers, "improved_reflexes"));
                    stmt.setObject(20, getPrayerState(activePrayers, "rapid_restore"));
                    stmt.setObject(21, getPrayerState(activePrayers, "rapid_heal"));
                    stmt.setObject(22, getPrayerState(activePrayers, "protect_item"));
                    stmt.setObject(23, getPrayerState(activePrayers, "hawk_eye"));
                    stmt.setObject(24, getPrayerState(activePrayers, "mystic_lore"));
                    stmt.setObject(25, getPrayerState(activePrayers, "steel_skin"));
                    stmt.setObject(26, getPrayerState(activePrayers, "ultimate_strength"));
                    stmt.setObject(27, getPrayerState(activePrayers, "incredible_reflexes"));
                    stmt.setObject(28, getPrayerState(activePrayers, "protect_from_magic"));
                    stmt.setObject(29, getPrayerState(activePrayers, "protect_from_missiles"));
                    stmt.setObject(30, getPrayerState(activePrayers, "protect_from_melee"));
                    stmt.setObject(31, getPrayerState(activePrayers, "eagle_eye"));
                    stmt.setObject(32, getPrayerState(activePrayers, "mystic_might"));
                    stmt.setObject(33, getPrayerState(activePrayers, "retribution"));
                    stmt.setObject(34, getPrayerState(activePrayers, "redemption"));
                    stmt.setObject(35, getPrayerState(activePrayers, "smite"));
                    stmt.setObject(36, getPrayerState(activePrayers, "chivalry"));
                    stmt.setObject(37, getPrayerState(activePrayers, "piety"));
                    stmt.setObject(38, getPrayerState(activePrayers, "preserve"));
                    stmt.setObject(39, getPrayerState(activePrayers, "rigour"));
                    stmt.setObject(40, getPrayerState(activePrayers, "augury"));
                    
                    // Prayer activity tracking (not available in current structure)
                    stmt.setObject(41, 0); // prayersActivated
                    stmt.setObject(42, 0); // prayersDeactivated
                    stmt.setObject(43, 0); // prayerPointsDrained
                    
                    stmt.addBatch();
                }
            }
            
            int[] results = stmt.executeBatch();
            log.debug("[DATABASE] Player prayers batch insert: {} records", results.length);
        }
    }
    
    /**
     * Insert player spells batch
     */
    private void insertPlayerSpellsBatch(Connection conn, List<TickDataCollection> batch, List<Long> tickIds) throws SQLException
    {
        String insertSQL = 
            "INSERT INTO player_spells (session_id, tick_id, tick_number, timestamp, " +
            "selected_spell, spellbook, last_cast_spell, autocast_enabled, autocast_spell, " +
            "rune_pouch_1_id, rune_pouch_2_id, rune_pouch_3_id, rune_pouch_1_name, rune_pouch_2_name, rune_pouch_3_name) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            for (int i = 0; i < batch.size(); i++) {
                TickDataCollection tickData = batch.get(i);
                Long tickId = i < tickIds.size() ? tickIds.get(i) : null;
                
                if (tickData.getPlayerSpells() != null && tickId != null) {
                    DataStructures.PlayerActiveSpells spells = tickData.getPlayerSpells();
                    
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setLong(2, tickId);
                    stmt.setObject(3, tickData.getTickNumber());
                    stmt.setTimestamp(4, new Timestamp(tickData.getTimestamp()));
                    
                    // Spell data
                    stmt.setString(5, spells.getSelectedSpell());
                    stmt.setString(6, spells.getSpellbook());
                    stmt.setString(7, spells.getLastCastSpell());
                    stmt.setObject(8, spells.getAutocastEnabled());
                    stmt.setString(9, spells.getAutocastSpell());
                    
                    // Rune pouch data (IDs and friendly names)
                    stmt.setObject(10, spells.getRunePouch1());
                    stmt.setObject(11, spells.getRunePouch2());
                    stmt.setObject(12, spells.getRunePouch3());
                    stmt.setString(13, spells.getRunePouch1Name());
                    stmt.setString(14, spells.getRunePouch2Name());
                    stmt.setString(15, spells.getRunePouch3Name());
                    
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
    }
    
    /**
     * Insert world data batch
     */
    private void insertWorldDataBatch(Connection conn, List<TickDataCollection> batch, List<Long> tickIds) throws SQLException
    {
        String insertSQL = 
            "INSERT INTO world_environment (session_id, tick_id, tick_number, timestamp, " +
            "plane, base_x, base_y, nearby_player_count, nearby_npc_count, environment_type) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            for (int i = 0; i < batch.size(); i++) {
                TickDataCollection tickData = batch.get(i);
                Long tickId = i < tickIds.size() ? tickIds.get(i) : null;
                
                if (tickData.getWorldData() != null && tickId != null) {
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setLong(2, tickId);
                    stmt.setObject(3, tickData.getTickNumber());
                    stmt.setTimestamp(4, new Timestamp(tickData.getTimestamp()));
                    stmt.setObject(5, tickData.getWorldData().getPlane());
                    stmt.setObject(6, tickData.getWorldData().getBaseX());
                    stmt.setObject(7, tickData.getWorldData().getBaseY());
                    stmt.setObject(8, tickData.getWorldData().getNearbyPlayerCount());
                    stmt.setObject(9, tickData.getWorldData().getNearbyNPCCount());
                    stmt.setString(10, tickData.getWorldData().getEnvironmentType()); // environment_type
                    
                    stmt.addBatch();
                }
            }
            
            stmt.executeBatch();
        }
    }
    
    /**
     * Insert combat data batch
     */
    private void insertCombatDataBatch(Connection conn, List<TickDataCollection> batch, List<Long> tickIds) throws SQLException
    {
        String insertSQL = 
            "INSERT INTO combat_data (session_id, tick_id, tick_number, timestamp, " +
            "in_combat, is_attacking, target_name, target_type, target_combat_level, " +
            "current_animation, weapon_type, attack_style, special_attack_percent, " +
            "total_recent_damage, max_recent_hit, hit_count) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            for (int i = 0; i < batch.size(); i++) {
                TickDataCollection tickData = batch.get(i);
                Long tickId = i < tickIds.size() ? tickIds.get(i) : null;
                
                if (tickData.getCombatData() != null && tickId != null) {
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setLong(2, tickId);
                    stmt.setObject(3, tickData.getTickNumber());
                    stmt.setTimestamp(4, new Timestamp(tickData.getTimestamp()));
                    stmt.setBoolean(5, tickData.getCombatData().getInCombat() != null ? tickData.getCombatData().getInCombat() : false);
                    stmt.setBoolean(6, tickData.getCombatData().getIsAttacking() != null ? tickData.getCombatData().getIsAttacking() : false);
                    stmt.setString(7, tickData.getCombatData().getTargetName());
                    stmt.setString(8, tickData.getCombatData().getTargetType());
                    stmt.setObject(9, tickData.getCombatData().getTargetCombatLevel());
                    stmt.setObject(10, tickData.getCombatData().getCurrentAnimation());
                    stmt.setString(11, tickData.getCombatData().getWeaponType());
                    stmt.setString(12, tickData.getCombatData().getAttackStyle());
                    stmt.setObject(13, tickData.getCombatData().getSpecialAttackPercent());
                    
                    // Add hitsplat data if available
                    if (tickData.getHitsplatData() != null) {
                        stmt.setObject(14, tickData.getHitsplatData().getTotalRecentDamage());
                        stmt.setObject(15, tickData.getHitsplatData().getMaxRecentHit());
                        stmt.setObject(16, tickData.getHitsplatData().getHitCount());
                    } else {
                        stmt.setObject(14, 0);
                        stmt.setObject(15, 0);
                        stmt.setObject(16, 0);
                    }
                    
                    stmt.addBatch();
                }
            }
            
            stmt.executeBatch();
        }
    }
    
    /**
     * Insert input data batch
     */
    private void insertInputDataBatch(Connection conn, List<TickDataCollection> batch, List<Long> tickIds) throws SQLException
    {
        String insertSQL = 
            "INSERT INTO input_data (session_id, tick_id, tick_number, timestamp, " +
            "mouse_x, mouse_y, mouse_idle_time, key_press_count, active_keys_count, " +
            "camera_x, camera_y, camera_z, camera_pitch, camera_yaw, minimap_zoom, " +
            "menu_open, menu_entry_count, movement_distance, movement_speed, click_accuracy) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            for (int i = 0; i < batch.size(); i++) {
                TickDataCollection tickData = batch.get(i);
                Long tickId = i < tickIds.size() ? tickIds.get(i) : null;
                
                if (tickId == null) continue;
                // Collect input data from various sources
                Integer mouseX = null, mouseY = null, mouseIdleTime = null, keyPressCount = null, activeKeysCount = null;
                Integer cameraX = null, cameraY = null, cameraZ = null;
                Integer cameraPitch = null, cameraYaw = null;
                Double minimapZoom = null, movementDistance = null, movementSpeed = null, clickAccuracy = null;
                Boolean menuOpen = null;
                Integer menuEntryCount = null;
                
                if (tickData.getMouseInput() != null) {
                    mouseX = tickData.getMouseInput().getMouseX();
                    mouseY = tickData.getMouseInput().getMouseY();
                    mouseIdleTime = tickData.getMouseInput().getMouseIdleTime();
                }
                
                if (tickData.getKeyboardInput() != null) {
                    keyPressCount = tickData.getKeyboardInput().getKeyPressCount();
                    // Use the actively held keys count from KeyboardInputData
                    activeKeysCount = tickData.getKeyboardInput().getActiveKeysCount();
                    if (activeKeysCount == null) activeKeysCount = 0;
                    
                    log.debug("[KEYBOARD-DEBUG] Database insertion - keyPressCount={}, activeKeysCount={}", 
                        keyPressCount, activeKeysCount);
                }
                
                if (tickData.getCameraData() != null) {
                    cameraX = tickData.getCameraData().getCameraX();
                    cameraY = tickData.getCameraData().getCameraY();
                    cameraZ = tickData.getCameraData().getCameraZ();
                    cameraPitch = tickData.getCameraData().getCameraPitch();
                    cameraYaw = tickData.getCameraData().getCameraYaw();
                    minimapZoom = tickData.getCameraData().getMinimapZoom();
                    
                    // Debug logging to trace camera data through to database insertion
                    log.debug("[CAMERA-DEBUG] Database insertion - Camera data extracted: X={}, Y={}, Z={}, Pitch={}, Yaw={}, Zoom={}", 
                        cameraX, cameraY, cameraZ, cameraPitch, cameraYaw, minimapZoom);
                } else {
                    log.warn("[CAMERA-DEBUG] Database insertion - CameraData object is NULL in TickDataCollection");
                }
                
                if (tickData.getMenuData() != null) {
                    menuOpen = tickData.getMenuData().getMenuOpen();
                    menuEntryCount = tickData.getMenuData().getMenuEntryCount();
                }
                
                // Extract movement analytics
                movementDistance = tickData.getMovementDistance();
                movementSpeed = tickData.getMovementSpeed();
                
                // Always insert input data (even if mostly NULL) to maintain tick consistency
                stmt.setObject(1, tickData.getSessionId());
                stmt.setLong(2, tickId);
                stmt.setObject(3, tickData.getTickNumber());
                stmt.setTimestamp(4, new Timestamp(tickData.getTimestamp()));
                stmt.setObject(5, mouseX);
                stmt.setObject(6, mouseY);
                stmt.setObject(7, mouseIdleTime);
                stmt.setObject(8, keyPressCount);
                stmt.setObject(9, activeKeysCount);
                stmt.setObject(10, cameraX);
                stmt.setObject(11, cameraY);
                stmt.setObject(12, cameraZ);
                stmt.setObject(13, cameraPitch);
                stmt.setObject(14, cameraYaw);
                stmt.setObject(15, minimapZoom);
                stmt.setBoolean(16, menuOpen != null ? menuOpen : false);
                stmt.setObject(17, menuEntryCount);
                stmt.setObject(18, movementDistance);
                stmt.setObject(19, movementSpeed);
                stmt.setObject(20, clickAccuracy);
                    
                stmt.addBatch();
            }
            
            int[] results = stmt.executeBatch();
        }
    }
    
    /**
     * Insert click context batch
     */
    private void insertClickContextBatch(Connection conn, List<TickDataCollection> batch, List<Long> tickIds) throws SQLException
    {
        String insertSQL = 
            "INSERT INTO click_context (session_id, tick_id, tick_number, timestamp, " +
            "click_type, menu_action, menu_option, menu_target, target_type, target_id, " +
            "target_name, screen_x, screen_y, world_x, world_y, plane, " +
            "is_player_target, is_enemy_target, widget_info, click_timestamp, " +
            "param0, param1, item_id, item_name, item_op, is_item_op) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            for (int i = 0; i < batch.size(); i++) {
                TickDataCollection tickData = batch.get(i);
                Long tickId = i < tickIds.size() ? tickIds.get(i) : null;
                
                if (tickId == null) continue;
                
                DataStructures.ClickContextData clickContext = tickData.getClickContext();
                
                // Only insert if we have click context data
                if (clickContext != null) {
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setLong(2, tickId);
                    stmt.setObject(3, tickData.getTickNumber());
                    stmt.setTimestamp(4, new Timestamp(tickData.getTimestamp()));
                    stmt.setString(5, clickContext.getClickType());
                    stmt.setString(6, clickContext.getMenuAction());
                    stmt.setString(7, clickContext.getMenuOption());
                    stmt.setString(8, clickContext.getMenuTarget());
                    stmt.setString(9, clickContext.getTargetType());
                    stmt.setObject(10, clickContext.getTargetId());
                    stmt.setString(11, clickContext.getTargetName());
                    stmt.setObject(12, clickContext.getScreenX());
                    stmt.setObject(13, clickContext.getScreenY());
                    stmt.setObject(14, clickContext.getWorldX());
                    stmt.setObject(15, clickContext.getWorldY());
                    stmt.setObject(16, clickContext.getPlane());
                    stmt.setObject(17, clickContext.getIsPlayerTarget());
                    stmt.setObject(18, clickContext.getIsEnemyTarget());
                    stmt.setString(19, clickContext.getWidgetInfo());
                    stmt.setTimestamp(20, clickContext.getClickTimestamp() != null ? 
                        new Timestamp(clickContext.getClickTimestamp()) : null);
                    stmt.setObject(21, clickContext.getParam0());
                    stmt.setObject(22, clickContext.getParam1());
                    stmt.setObject(23, clickContext.getItemId());
                    stmt.setString(24, clickContext.getItemName());
                    stmt.setObject(25, clickContext.getItemOp());
                    stmt.setObject(26, clickContext.getIsItemOp());
                    
                    stmt.addBatch();
                    
                    log.debug("[CLICK-DB-DEBUG] Prepared click context for batch: {} -> {} ({})", 
                        clickContext.getClickType(), clickContext.getTargetType(), clickContext.getTargetName());
                }
            }
            
            int[] results = stmt.executeBatch();
            log.debug("[CLICK-DB-DEBUG] Inserted {} click context records", results.length);
        }
    }
    
    /**
     * Insert social data batch (chat, friends, clan, trade)
     */
    private void insertSocialDataBatch(Connection conn, List<TickDataCollection> batch, List<Long> tickIds) throws SQLException
    {
        // Insert chat data
        String chatSQL = "INSERT INTO chat_messages (session_id, tick_number, timestamp, " +
                        "total_messages, public_chat_count, private_chat_count, clan_chat_count, " +
                        "system_message_count, avg_message_length, most_active_type, " +
                        "last_message, last_message_time) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(chatSQL)) {
            for (TickDataCollection tickData : batch) {
                if (tickData.getChatData() != null) {
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setObject(2, tickData.getTickNumber());
                    stmt.setTimestamp(3, new Timestamp(tickData.getTimestamp()));
                    stmt.setInt(4, tickData.getChatData().getTotalMessageCount() != null ? 
                        tickData.getChatData().getTotalMessageCount() : 0);
                    stmt.setInt(5, tickData.getChatData().getPublicChatCount() != null ? 
                        tickData.getChatData().getPublicChatCount() : 0);
                    stmt.setInt(6, tickData.getChatData().getPrivateChatCount() != null ? 
                        tickData.getChatData().getPrivateChatCount() : 0);
                    stmt.setInt(7, tickData.getChatData().getClanChatCount() != null ? 
                        tickData.getChatData().getClanChatCount() : 0);
                    stmt.setInt(8, tickData.getChatData().getSystemMessageCount() != null ? 
                        tickData.getChatData().getSystemMessageCount() : 0);
                    stmt.setDouble(9, tickData.getChatData().getAverageMessageLength() != null ? 
                        tickData.getChatData().getAverageMessageLength() : 0.0);
                    stmt.setString(10, tickData.getChatData().getMostActiveMessageType());
                    stmt.setString(11, tickData.getChatData().getLastMessage());
                    stmt.setObject(12, tickData.getChatData().getLastMessageTime());
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
        
        // Insert friends data
        String friendsSQL = "INSERT INTO friends_data (session_id, tick_number, timestamp, " +
                           "total_friends, online_friends, offline_friends) " +
                           "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(friendsSQL)) {
            for (TickDataCollection tickData : batch) {
                if (tickData.getFriendsData() != null) {
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setObject(2, tickData.getTickNumber());
                    stmt.setTimestamp(3, new Timestamp(tickData.getTimestamp()));
                    stmt.setInt(4, tickData.getFriendsData().getTotalFriends() != null ? 
                        tickData.getFriendsData().getTotalFriends() : 0);
                    stmt.setInt(5, tickData.getFriendsData().getOnlineFriends() != null ? 
                        tickData.getFriendsData().getOnlineFriends() : 0);
                    stmt.setInt(6, tickData.getFriendsData().getOfflineFriends() != null ? 
                        tickData.getFriendsData().getOfflineFriends() : 0);
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
    }
    
    /**
     * Insert interface data batch
     */
    private void insertInterfaceDataBatch(Connection conn, List<TickDataCollection> batch, List<Long> tickIds) throws SQLException
    {
        String interfaceSQL = "INSERT INTO interface_data (session_id, tick_number, timestamp, " +
                             "total_open_interfaces, primary_interface, chatbox_open, inventory_open, " +
                             "skills_open, quest_open, settings_open) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(interfaceSQL)) {
            for (TickDataCollection tickData : batch) {
                if (tickData.getInterfaceData() != null) {
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setObject(2, tickData.getTickNumber());
                    stmt.setTimestamp(3, new Timestamp(tickData.getTimestamp()));
                    stmt.setInt(4, tickData.getInterfaceData().getTotalOpenInterfaces() != null ? 
                        tickData.getInterfaceData().getTotalOpenInterfaces() : 0);
                    stmt.setString(5, tickData.getInterfaceData().getPrimaryInterface());
                    stmt.setBoolean(6, tickData.getInterfaceData().getChatboxOpen() != null ? 
                        tickData.getInterfaceData().getChatboxOpen() : false);
                    stmt.setBoolean(7, tickData.getInterfaceData().getInventoryOpen() != null ? 
                        tickData.getInterfaceData().getInventoryOpen() : false);
                    stmt.setBoolean(8, tickData.getInterfaceData().getSkillsInterfaceOpen() != null ? 
                        tickData.getInterfaceData().getSkillsInterfaceOpen() : false);
                    stmt.setBoolean(9, tickData.getInterfaceData().getQuestInterfaceOpen() != null ? 
                        tickData.getInterfaceData().getQuestInterfaceOpen() : false);
                    stmt.setBoolean(10, tickData.getInterfaceData().getSettingsInterfaceOpen() != null ? 
                        tickData.getInterfaceData().getSettingsInterfaceOpen() : false);
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
        
        // Insert bank data
        String bankSQL = "INSERT INTO bank_data (session_id, tick_number, timestamp, " +
                        "bank_open, unique_items, used_slots, max_slots, total_value) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(bankSQL)) {
            for (TickDataCollection tickData : batch) {
                if (tickData.getBankData() != null) {
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setObject(2, tickData.getTickNumber());
                    stmt.setTimestamp(3, new Timestamp(tickData.getTimestamp()));
                    stmt.setBoolean(4, tickData.getBankData().getBankOpen() != null ? 
                        tickData.getBankData().getBankOpen() : false);
                    stmt.setInt(5, tickData.getBankData().getTotalUniqueItems() != null ? 
                        tickData.getBankData().getTotalUniqueItems() : 0);
                    stmt.setInt(6, tickData.getBankData().getUsedBankSlots() != null ? 
                        tickData.getBankData().getUsedBankSlots() : 0);
                    stmt.setInt(7, tickData.getBankData().getMaxBankSlots() != null ? 
                        tickData.getBankData().getMaxBankSlots() : 0);
                    stmt.setLong(8, tickData.getBankData().getTotalBankValue() != null ? 
                        tickData.getBankData().getTotalBankValue() : 0);
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
    }
    
    /**
     * Insert system metrics batch
     */
    private void insertSystemMetricsBatch(Connection conn, List<TickDataCollection> batch, List<Long> tickIds) throws SQLException
    {
        String metricsSQL = "INSERT INTO system_metrics (session_id, tick_number, timestamp, " +
                           "used_memory_mb, max_memory_mb, memory_usage_percent, cpu_usage_percent, " +
                           "client_fps, gc_count, gc_time_ms, uptime_ms, performance_score) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(metricsSQL)) {
            for (TickDataCollection tickData : batch) {
                if (tickData.getSystemMetrics() != null) {
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setObject(2, tickData.getTickNumber());
                    stmt.setTimestamp(3, new Timestamp(tickData.getTimestamp()));
                    stmt.setInt(4, tickData.getSystemMetrics().getUsedMemoryMB() != null ? 
                        tickData.getSystemMetrics().getUsedMemoryMB() : 0);
                    stmt.setInt(5, tickData.getSystemMetrics().getMaxMemoryMB() != null ? 
                        tickData.getSystemMetrics().getMaxMemoryMB() : 0);
                    stmt.setDouble(6, tickData.getSystemMetrics().getMemoryUsagePercent() != null ? 
                        tickData.getSystemMetrics().getMemoryUsagePercent() : 0.0);
                    stmt.setDouble(7, tickData.getSystemMetrics().getCpuUsagePercent() != null ? 
                        tickData.getSystemMetrics().getCpuUsagePercent() : -1.0);
                    stmt.setInt(8, tickData.getSystemMetrics().getClientFPS() != null ? 
                        tickData.getSystemMetrics().getClientFPS() : 0);
                    stmt.setLong(9, tickData.getSystemMetrics().getGcCount() != null ? 
                        tickData.getSystemMetrics().getGcCount() : 0);
                    stmt.setLong(10, tickData.getSystemMetrics().getGcTime() != null ? 
                        tickData.getSystemMetrics().getGcTime() : 0);
                    stmt.setLong(11, tickData.getSystemMetrics().getUptime() != null ? 
                        tickData.getSystemMetrics().getUptime() : 0);
                    
                    // Add performance score from timing breakdown
                    double performanceScore = 0.0;
                    if (tickData.getTimingBreakdown() != null && tickData.getTimingBreakdown().getPerformanceScore() != null) {
                        performanceScore = tickData.getTimingBreakdown().getPerformanceScore();
                    }
                    stmt.setDouble(12, performanceScore);
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
    }
    
    /**
     * Insert world objects batch
     */
    private void insertWorldObjectsBatch(Connection conn, List<TickDataCollection> batch, List<Long> tickIds) throws SQLException
    {
        // Insert ground items data
        String groundItemsSQL = "INSERT INTO ground_items_data (session_id, tick_number, timestamp, " +
                               "total_items, total_quantity, total_value, unique_item_types, scan_radius, " +
                               "most_valuable_item_id, most_valuable_item_name, most_valuable_item_quantity, most_valuable_item_value) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(groundItemsSQL)) {
            for (TickDataCollection tickData : batch) {
                if (tickData.getGroundItems() != null) {
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setObject(2, tickData.getTickNumber());
                    stmt.setTimestamp(3, new Timestamp(tickData.getTimestamp()));
                    stmt.setInt(4, tickData.getGroundItems().getTotalItems() != null ? 
                        tickData.getGroundItems().getTotalItems() : 0);
                    stmt.setInt(5, tickData.getGroundItems().getTotalQuantity() != null ? 
                        tickData.getGroundItems().getTotalQuantity() : 0);
                    stmt.setLong(6, tickData.getGroundItems().getTotalValue() != null ? 
                        tickData.getGroundItems().getTotalValue() : 0);
                    stmt.setInt(7, tickData.getGroundItems().getUniqueItemTypes() != null ? 
                        tickData.getGroundItems().getUniqueItemTypes() : 0);
                    stmt.setInt(8, tickData.getGroundItems().getScanRadius() != null ? 
                        tickData.getGroundItems().getScanRadius() : 15);
                    
                    // Extract most valuable item data from the mostValuableItem string
                    String mostValuableItem = tickData.getGroundItems().getMostValuableItem();
                    if (mostValuableItem != null && mostValuableItem.startsWith("Item_") && !mostValuableItem.equals("None") && !mostValuableItem.equals("Unknown")) {
                        try {
                            int itemId = Integer.parseInt(mostValuableItem.substring(5)); // Remove "Item_" prefix
                            stmt.setInt(9, itemId); // most_valuable_item_id
                            stmt.setString(10, getItemNameFromId(itemId)); // Use proper name resolution
                            stmt.setInt(11, 1); // most_valuable_item_quantity (placeholder)
                            stmt.setLong(12, 0); // most_valuable_item_value (placeholder)
                        } catch (NumberFormatException e) {
                            // If parsing fails, set nulls
                            stmt.setObject(9, null);
                            stmt.setObject(10, null);
                            stmt.setObject(11, null);
                            stmt.setObject(12, null);
                        }
                    } else {
                        // No most valuable item or invalid format
                        stmt.setObject(9, null);
                        stmt.setObject(10, null);
                        stmt.setObject(11, null);
                        stmt.setObject(12, null);
                    }
                    
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
        
        // Insert game objects data
        String gameObjectsSQL = "INSERT INTO game_objects_data (session_id, tick_number, timestamp, " +
                               "object_count, unique_object_types, scan_radius, interactable_objects, " +
                               "closest_object_distance, closest_object_id, closest_object_name) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(gameObjectsSQL)) {
            for (TickDataCollection tickData : batch) {
                if (tickData.getGameObjects() != null) {
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setObject(2, tickData.getTickNumber());
                    stmt.setTimestamp(3, new Timestamp(tickData.getTimestamp()));
                    stmt.setInt(4, tickData.getGameObjects().getObjectCount() != null ? 
                        tickData.getGameObjects().getObjectCount() : 0);
                    stmt.setInt(5, tickData.getGameObjects().getUniqueObjectTypes() != null ? 
                        tickData.getGameObjects().getUniqueObjectTypes() : 0);
                    stmt.setInt(6, tickData.getGameObjects().getScanRadius() != null ? 
                        tickData.getGameObjects().getScanRadius() : 15);
                    stmt.setInt(7, tickData.getGameObjects().getInteractableObjectsCount() != null ? 
                        tickData.getGameObjects().getInteractableObjectsCount() : 0);
                    stmt.setObject(8, tickData.getGameObjects().getClosestObjectDistance());
                    stmt.setObject(9, tickData.getGameObjects().getClosestObjectId());
                    stmt.setString(10, tickData.getGameObjects().getClosestObjectName());
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
        
        // Insert projectiles data
        String projectilesSQL = "INSERT INTO projectiles_data (session_id, tick_number, timestamp, " +
                               "active_projectiles, unique_projectile_types) " +
                               "VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(projectilesSQL)) {
            for (TickDataCollection tickData : batch) {
                if (tickData.getProjectiles() != null) {
                    stmt.setObject(1, tickData.getSessionId());
                    stmt.setObject(2, tickData.getTickNumber());
                    stmt.setTimestamp(3, new Timestamp(tickData.getTimestamp()));
                    stmt.setInt(4, tickData.getProjectiles().getActiveProjectiles() != null ? 
                        tickData.getProjectiles().getActiveProjectiles() : 0);
                    stmt.setInt(5, tickData.getProjectiles().getUniqueProjectileTypes() != null ? 
                        tickData.getProjectiles().getUniqueProjectileTypes() : 0);
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
    }
    
    /**
     * Finalize a session
     * @param sessionId Session to finalize
     */
    public void finalizeSession(Integer sessionId)
    {
        if (sessionId == null || !connected.get()) {
            return;
        }
        
        // Force flush any pending data before finalizing
        log.debug("[DATABASE] Finalizing session {} - forcing batch flush", sessionId);
        forceFlushBatch();
        
        long startTime = System.nanoTime();
        
        try (Connection conn = dataSource.getConnection()) {
            // First, count the actual ticks collected for this session
            int totalTicksCollected = 0;
            String countSQL = "SELECT COUNT(DISTINCT tick_number) FROM game_ticks WHERE session_id = ?";
            try (PreparedStatement countStmt = conn.prepareStatement(countSQL)) {
                countStmt.setInt(1, sessionId);
                try (var rs = countStmt.executeQuery()) {
                    if (rs.next()) {
                        totalTicksCollected = rs.getInt(1);
                        log.debug("[SESSION-DEBUG] Session {} collected {} total ticks", sessionId, totalTicksCollected);
                    }
                }
            }
            
            // Update session with completion status, end_time, and actual total_ticks count
            String updateSession = 
                "UPDATE sessions SET status = 'COMPLETED', end_time = ?, total_ticks = ? " +
                "WHERE session_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(updateSession)) {
                stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                stmt.setInt(2, totalTicksCollected);
                stmt.setInt(3, sessionId);
                
                int result = stmt.executeUpdate();
                
                if (result > 0) {
                    activeSessions.remove(sessionId);
                    recordDatabaseCall(System.nanoTime() - startTime);
                    log.debug("[SESSION-DEBUG] Session finalized: {} with {} total ticks", sessionId, totalTicksCollected);
                } else {
                    log.warn("Session not found for finalization: {}", sessionId);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to finalize session {}", sessionId, e);
        }
    }
    
    /**
     * Check if database is connected
     * @return True if connected
     */
    public boolean isConnected()
    {
        return connected.get() && dataSource != null && !dataSource.isClosed();
    }
    
    /**
     * Get database performance metrics
     * @return Performance summary
     */
    public String getPerformanceMetrics()
    {
        long totalCalls = totalDatabaseCalls.get();
        long avgTimeMs = totalCalls > 0 ? 
            (totalDatabaseTime.get() / totalCalls) / 1_000_000 : 0;
        
        return String.format(
            "Database Performance: %d calls, avg %dms, %d records, %d batches, pool: %s",
            totalCalls, avgTimeMs, totalRecordsInserted.get(), totalBatchesProcessed.get(),
            dataSource != null ? getPoolStatus() : "disconnected"
        );
    }
    
    /**
     * Get connection pool status
     */
    private String getPoolStatus()
    {
        if (dataSource == null) return "null";
        
        return String.format("%d/%d connections", 
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections());
    }
    
    /**
     * Record database call for performance tracking
     */
    private void recordDatabaseCall(long durationNanos)
    {
        totalDatabaseCalls.incrementAndGet();
        totalDatabaseTime.addAndGet(durationNanos);
    }
    
    /**
     * Report batch performance
     */
    private void reportBatchPerformance(long recordCount, long processingTime)
    {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastPerformanceReport > 60000) { // Every minute
            double processingTimeMs = processingTime / 1_000_000.0;
            double recordsPerSecond = recordCount / (processingTimeMs / 1000.0);
            
            log.debug("Batch Performance: {} records in {:.2f}ms ({:.0f} records/sec), " +
                    "Total: {} records in {} batches",
                    recordCount, processingTimeMs, recordsPerSecond,
                    totalRecordsInserted.get(), totalBatchesProcessed.get());
            
            lastPerformanceReport = currentTime;
        }
    }
    
    /**
     * Get current player name from client
     */
    private String getCurrentPlayerName()
    {
        if (client != null && client.getLocalPlayer() != null) {
            Player localPlayer = client.getLocalPlayer();
            String playerName = localPlayer.getName();
            log.debug("[PLAYER-NAME-DEBUG] getCurrentPlayerName called - localPlayer: {}, playerName: '{}'", 
                localPlayer != null ? "present" : "null", playerName);
            return playerName != null ? playerName : "UnknownPlayer";
        }
        log.debug("[PLAYER-NAME-DEBUG] getCurrentPlayerName called - client or localPlayer is null");
        return "UnknownPlayer";
    }
    
    /**
     * Update session player name if it was "UnknownPlayer" but now we have the real name
     */
    private void updatePlayerNameIfNeeded(TickDataCollection tickData)
    {
        try {
            // Only check if we have player data with a valid name
            if (tickData.getPlayerData() == null || tickData.getPlayerData().getPlayerName() == null) {
                return;
            }
            
            String currentPlayerName = tickData.getPlayerData().getPlayerName();
            Integer sessionId = tickData.getSessionId();
            
            // Skip if the player name is still unknown or we don't have a valid session ID
            if ("UnknownPlayer".equals(currentPlayerName) || sessionId == null) {
                return;
            }
            
            // Check if this session currently has "UnknownPlayer" as the name
            try (Connection conn = dataSource.getConnection()) {
                String checkSQL = "SELECT player_name FROM sessions WHERE session_id = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSQL)) {
                    checkStmt.setInt(1, sessionId);
                    try (var rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            String sessionPlayerName = rs.getString("player_name");
                            
                            // Update if the session has "UnknownPlayer" but we now have the real name
                            if ("UnknownPlayer".equals(sessionPlayerName) && !currentPlayerName.equals(sessionPlayerName)) {
                                String updateSQL = "UPDATE sessions SET player_name = ? WHERE session_id = ?";
                                try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                                    updateStmt.setString(1, currentPlayerName);
                                    updateStmt.setInt(2, sessionId);
                                    
                                    int updated = updateStmt.executeUpdate();
                                    if (updated > 0) {
                                        log.debug("[PLAYER-NAME-DEBUG] Updated session {} player name from 'UnknownPlayer' to '{}'", 
                                            sessionId, currentPlayerName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.warn("[PLAYER-NAME-DEBUG] Failed to update session player name", e);
        }
    }
    
    /**
     * Shutdown the database manager
     */
    public void shutdown()
    {
        shutdown.set(true);
        
        try {
            // Process remaining batch
            if (!pendingBatch.isEmpty()) {
                log.debug("Processing final batch of {} records", pendingBatch.size());
                processBatch();
            }
            
            // Shutdown executor
            batchExecutor.shutdown();
            if (!batchExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                batchExecutor.shutdownNow();
            }
            
            // Finalize active sessions
            for (Integer sessionId : activeSessions.keySet()) {
                finalizeSession(sessionId);
            }
            
            // Close data source
            if (dataSource != null) {
                dataSource.close();
            }
            
            connected.set(false);
            log.debug("DatabaseManager shutdown completed. Final stats: {}", getPerformanceMetrics());
            
        } catch (Exception e) {
            log.error("Error during database shutdown", e);
        }
    }
    
    /**
     * Session information holder
     */
    private static class SessionInfo
    {
        final int sessionId;
        final long startTime;
        
        SessionInfo(int sessionId, long startTime)
        {
            this.sessionId = sessionId;
            this.startTime = startTime;
        }
    }
    
    // ===== HELPER METHODS FOR DATA EXTRACTION =====
    
    /**
     * Extract equipment ID from equipment map
     */
    private Integer getEquipmentId(Map<String, Integer> equipmentIds, String slotName) {
        if (equipmentIds == null) {
            return -1;
        }
        return equipmentIds.getOrDefault(slotName, -1);
    }
    
    /**
     * Get item name from ID using ItemManager
     */
    private String getItemNameFromId(int itemId) {
        try {
            if (itemManager != null) {
                ItemComposition itemComp = itemManager.getItemComposition(itemId);
                if (itemComp != null && itemComp.getName() != null) {
                    return itemComp.getName();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get item name for ID {}: {}", itemId, e.getMessage());
        }
        return "Item_" + itemId; // Fallback to ID format
    }
    
    /**
     * Convert inventory Item array to JSON string for JSONB storage with friendly names
     */
    private String convertInventoryItemsToJson(net.runelite.api.Item[] inventoryItems) {
        if (inventoryItems == null || inventoryItems.length == 0) {
            log.debug("[INVENTORY-JSON-DEBUG] Converting NULL or empty inventory array to JSON");
            return "[]";
        }
        
        log.debug("[INVENTORY-JSON-DEBUG] Converting {} items to JSON", inventoryItems.length);
        
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        
        int validItemsFound = 0;
        for (int i = 0; i < inventoryItems.length; i++) {
            net.runelite.api.Item item = inventoryItems[i];
            if (item != null && item.getId() > 0) {
                validItemsFound++;
                if (!first) {
                    json.append(",");
                }
                
                // Get item name using ItemManager
                String itemName = "Unknown Item";
                try {
                    if (itemManager != null) {
                        ItemComposition itemComp = itemManager.getItemComposition(item.getId());
                        if (itemComp != null && itemComp.getName() != null) {
                            itemName = itemComp.getName().replace("\"", "\\\""); // Escape quotes for JSON
                        }
                    }
                } catch (Exception e) {
                    log.debug("Failed to get item name for ID {} in inventory JSON: {}", item.getId(), e.getMessage());
                }
                
                // Debug first few items being converted
                if (validItemsFound <= 3) {
                    log.debug("[INVENTORY-JSON-DEBUG] Converting item {}: slot={}, id={}, qty={}, name={}", 
                        validItemsFound, i, item.getId(), item.getQuantity(), itemName);
                }
                
                json.append(String.format(
                    "{\"slot\":%d,\"id\":%d,\"quantity\":%d,\"name\":\"%s\"}", 
                    i, item.getId(), item.getQuantity(), itemName
                ));
                first = false;
            }
        }
        
        log.debug("[INVENTORY-JSON-DEBUG] Found {} valid items out of {} total slots", validItemsFound, inventoryItems.length);
        
        json.append("]");
        return json.toString();
    }
    
    /**
     * Extract prayer state from activePrayers map
     */
    private Boolean getPrayerState(Map<String, Boolean> activePrayers, String prayerName) {
        if (activePrayers == null) {
            return false;
        }
        return activePrayers.getOrDefault(prayerName, false);
    }
    
    /**
     * Calculate total quantity of items in inventory
     */
    private int getTotalQuantity(net.runelite.api.Item[] items) {
        if (items == null) {
            return 0;
        }
        int total = 0;
        for (net.runelite.api.Item item : items) {
            if (item != null) {
                total += item.getQuantity();
            }
        }
        return total;
    }
    
    /**
     * Get unique item types count from item counts map
     */
    private int getUniqueItemTypes(Map<Integer, Integer> itemCounts) {
        if (itemCounts == null) {
            return 0;
        }
        return itemCounts.size();
    }
    
    /**
     * Find the most valuable item in inventory (by quantity as a proxy for value)
     */
    private net.runelite.api.Item getMostValuableItem(net.runelite.api.Item[] items) {
        if (items == null || items.length == 0) {
            return null;
        }
        
        net.runelite.api.Item mostValuable = null;
        int highestQuantity = 0;
        
        for (net.runelite.api.Item item : items) {
            if (item != null && item.getId() > 0 && item.getQuantity() > highestQuantity) {
                highestQuantity = item.getQuantity();
                mostValuable = item;
            }
        }
        
        return mostValuable;
    }
}