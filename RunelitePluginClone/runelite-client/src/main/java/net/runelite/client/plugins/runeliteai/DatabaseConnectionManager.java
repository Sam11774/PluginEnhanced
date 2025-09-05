/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dedicated manager for database connections and sessions
 * 
 * Responsible for:
 * - HikariCP connection pool setup and management
 * - Database session lifecycle management
 * - Connection health monitoring and validation
 * - Database schema verification and setup
 * - Session tracking with active session management
 * 
 * Migrated from DatabaseManager connection and session management logic
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
public class DatabaseConnectionManager
{
    // Core dependencies
    private final Client client;
    
    // Connection management
    private HikariDataSource dataSource;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    
    // Session management
    private final Map<Integer, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    
    // Database configuration
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/runelite_ai";
    private static final String DB_USERNAME = "postgres";
    private static final String DB_PASSWORD = "sam11773";
    private static final int MAX_POOL_SIZE = 10;
    private static final int MIN_IDLE = 2;
    private static final long CONNECTION_TIMEOUT = 30000; // 30 seconds
    private static final long IDLE_TIMEOUT = 600000; // 10 minutes
    private static final long MAX_LIFETIME = 1800000; // 30 minutes
    
    public DatabaseConnectionManager(Client client)
    {
        this.client = client;
        log.debug("DatabaseConnectionManager initialized");
        
        // Initialize connection pool
        initializeConnectionPool();
    }
    
    /**
     * Initialize HikariCP connection pool
     */
    private void initializeConnectionPool()
    {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USERNAME);
            config.setPassword(DB_PASSWORD);
            
            // Connection pool settings
            config.setMaximumPoolSize(MAX_POOL_SIZE);
            config.setMinimumIdle(MIN_IDLE);
            config.setConnectionTimeout(CONNECTION_TIMEOUT);
            config.setIdleTimeout(IDLE_TIMEOUT);
            config.setMaxLifetime(MAX_LIFETIME);
            
            // Performance and reliability settings
            config.setLeakDetectionThreshold(60000); // 1 minute
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(3000); // 3 seconds
            
            // Connection pool name for monitoring
            config.setPoolName("RuneLiteAI-CP");
            
            // Additional PostgreSQL optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("reWriteBatchedInserts", "true");
            
            this.dataSource = new HikariDataSource(config);
            
            // Test the connection
            testConnection();
            
            connected.set(true);
            log.info("Database connection pool initialized successfully - URL: {}, Pool size: {}", 
                DB_URL, MAX_POOL_SIZE);
            
        } catch (Exception e) {
            log.error("Failed to initialize database connection pool", e);
            connected.set(false);
            throw new RuntimeException("Database connection initialization failed", e);
        }
    }
    
    /**
     * Test database connection and verify schema
     */
    private void testConnection()
    {
        try (Connection conn = dataSource.getConnection()) {
            log.debug("Testing database connection...");
            
            // Test basic connectivity
            try (PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        log.debug("Database connectivity test passed");
                    }
                }
            }
            
            // Verify core tables exist
            verifyDatabaseSchema(conn);
            
            log.debug("Database connection and schema verification completed");
            
        } catch (SQLException e) {
            log.error("Database connection test failed", e);
            throw new RuntimeException("Database connection test failed", e);
        }
    }
    
    /**
     * Verify that essential database tables exist
     */
    private void verifyDatabaseSchema(Connection conn) throws SQLException
    {
        String[] requiredTables = {
            "sessions", "game_ticks", "player_location", "player_vitals",
            "player_equipment", "player_inventory", "combat_data", "click_context"
        };
        
        for (String tableName : requiredTables) {
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT 1 FROM information_schema.tables WHERE table_name = ?")) {
                stmt.setString(1, tableName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        log.warn("Required table '{}' not found in database schema", tableName);
                        // Could auto-create schema here if needed
                    }
                }
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
     * Get current player name from the client
     */
    private String getCurrentPlayerName()
    {
        if (client != null && client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null) {
            String playerName = client.getLocalPlayer().getName().trim();
            if (!playerName.isEmpty()) {
                log.debug("Retrieved player name from client: {}", playerName);
                return playerName;
            }
        }
        
        // Try to get username from client if LocalPlayer is null
        if (client != null && client.getUsername() != null && !client.getUsername().trim().isEmpty()) {
            String username = client.getUsername().trim();
            log.debug("Retrieved username from client as fallback: {}", username);
            return username;
        }
        
        log.warn("Unable to retrieve player name - client or player not available, using fallback");
        return "UnknownPlayer";
    }
    
    /**
     * Get a database connection from the pool
     */
    public Connection getConnection() throws SQLException
    {
        if (!connected.get() || dataSource == null) {
            throw new SQLException("Database not connected");
        }
        
        return dataSource.getConnection();
    }
    
    /**
     * Check if database is connected
     */
    public boolean isConnected()
    {
        return connected.get() && dataSource != null && !dataSource.isClosed();
    }
    
    /**
     * Check database health
     */
    public boolean isHealthy()
    {
        if (!isConnected()) {
            return false;
        }
        
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            log.warn("Database health check failed", e);
            return false;
        }
    }
    
    /**
     * Get active sessions
     */
    public Map<Integer, SessionInfo> getActiveSessions()
    {
        return new ConcurrentHashMap<>(activeSessions);
    }
    
    /**
     * Finalize a session with proper total_ticks calculation
     */
    public void finalizeSession(Integer sessionId)
    {
        if (sessionId == null) {
            return;
        }
        
        try (Connection conn = getConnection()) {
            // First, calculate the total_ticks from game_ticks table
            int totalTicks = 0;
            String countTicksQuery = "SELECT COUNT(*) FROM game_ticks WHERE session_id = ?";
            
            try (PreparedStatement countStmt = conn.prepareStatement(countTicksQuery)) {
                countStmt.setInt(1, sessionId);
                
                try (ResultSet rs = countStmt.executeQuery()) {
                    if (rs.next()) {
                        totalTicks = rs.getInt(1);
                        log.debug("Calculated total_ticks for session {}: {}", sessionId, totalTicks);
                    }
                }
            }
            
            // Update the session with completion status, end time, and total_ticks
            String updateSession = "UPDATE sessions SET status = 'COMPLETED', end_time = ?, total_ticks = ? WHERE session_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(updateSession)) {
                stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                stmt.setInt(2, totalTicks);
                stmt.setInt(3, sessionId);
                
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    activeSessions.remove(sessionId);
                    log.info("Session {} finalized successfully with {} total ticks", sessionId, totalTicks);
                } else {
                    log.warn("No session found with ID {} to finalize", sessionId);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to finalize session {}", sessionId, e);
        }
    }
    
    /**
     * Get connection pool statistics
     */
    public Map<String, Object> getConnectionStats()
    {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        if (dataSource != null) {
            stats.put("totalConnections", dataSource.getHikariPoolMXBean().getTotalConnections());
            stats.put("activeConnections", dataSource.getHikariPoolMXBean().getActiveConnections());
            stats.put("idleConnections", dataSource.getHikariPoolMXBean().getIdleConnections());
            stats.put("threadsAwaitingConnection", dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
            // HikariCP pool name (some versions don't have getPoolName method)
            try {
                stats.put("poolName", "RuneLiteAI-CP");
            } catch (Exception e) {
                stats.put("poolName", "RuneLiteAI-CP");
            }
        }
        
        stats.put("connected", connected.get());
        stats.put("activeSessions", activeSessions.size());
        stats.put("shutdown", shutdown.get());
        
        return stats;
    }
    
    /**
     * Shutdown the connection manager
     */
    public void shutdown()
    {
        log.debug("DatabaseConnectionManager shutdown initiated");
        
        shutdown.set(true);
        
        // Finalize all active sessions
        for (Integer sessionId : activeSessions.keySet()) {
            try {
                finalizeSession(sessionId);
            } catch (Exception e) {
                log.warn("Error finalizing session {} during shutdown", sessionId, e);
            }
        }
        
        // Close connection pool
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
                log.debug("Database connection pool closed");
            } catch (Exception e) {
                log.error("Error closing database connection pool", e);
            }
        }
        
        connected.set(false);
        log.debug("DatabaseConnectionManager shutdown completed");
    }
}