/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;

import java.util.Map;

/**
 * Modular Database Manager using delegation pattern
 * 
 * Orchestrates database operations across specialized modules:
 * - DatabaseConnectionManager: HikariCP setup, session management, health checks
 * - DatabaseTableOperations: All batch insert methods, transaction management
 * - DatabaseUtilities: JSON conversion, ItemManager integration, helper methods
 * - DatabasePerformanceMonitor: Performance tracking, health monitoring, statistics
 * 
 * This class maintains the exact same public API as the original monolithic version
 * while delegating all work to specialized modules for better maintainability.
 * 
 * Features maintained:
 * - Connection pooling with HikariCP for optimal performance
 * - Batch operations with optimized processing
 * - Async database operations with transaction management
 * - Session-based data organization
 * - Real-time performance monitoring and health checks
 * - 34+ optimized database tables for comprehensive data storage
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0 - Modular Architecture
 */
@Slf4j
public class DatabaseManager
{
    // Core dependencies
    private final Client client;
    private final ItemManager itemManager;
    
    // Specialized database modules
    private final DatabaseConnectionManager connectionManager;
    private final DatabaseTableOperations tableOperations;
    private final DatabaseUtilities utilities;
    private final DatabasePerformanceMonitor performanceMonitor;
    
    /**
     * Constructor - initializes all modular database components
     */
    public DatabaseManager(Client client, ItemManager itemManager)
    {
        this.client = client;
        this.itemManager = itemManager;
        
        // Initialize all specialized database modules
        this.connectionManager = new DatabaseConnectionManager(client);
        this.utilities = new DatabaseUtilities(client, itemManager);
        this.performanceMonitor = new DatabasePerformanceMonitor();
        
        // Table operations needs connection manager and utilities
        this.tableOperations = new DatabaseTableOperations(client, itemManager, connectionManager, utilities);
        
        log.debug("DatabaseManager initialized with modular architecture - 4 modules ready");
    }
    
    /**
     * Initialize a new session
     * 
     * This method maintains the same public API as the original version.
     * 
     * @return Session ID
     */
    public Integer initializeSession()
    {
        log.debug("DatabaseManager.initializeSession() - delegating to connection manager");
        
        try {
            Integer sessionId = connectionManager.initializeSession();
            performanceMonitor.recordSuccess();
            return sessionId;
        } catch (Exception e) {
            performanceMonitor.recordError("Session initialization failed", e);
            throw e;
        }
    }
    
    /**
     * Store tick data (adds to batch for processing)
     * 
     * This method maintains the exact same signature and behavior as the original
     * monolithic version, but delegates all work to table operations.
     * 
     * @param tickData Data to store
     */
    public void storeTickData(TickDataCollection tickData)
    {
        log.debug("DatabaseManager.storeTickData() - delegating to table operations");
        
        try {
            // Update player name if needed before storing
            utilities.updatePlayerNameIfNeeded(tickData);
            
            // Delegate to table operations
            tableOperations.storeTickData(tickData);
            
        } catch (Exception e) {
            performanceMonitor.recordError("Tick data storage failed", e);
            log.error("Error storing tick data", e);
        }
    }
    
    /**
     * Force immediate batch processing (for debugging)
     * 
     * This method maintains compatibility with the original API.
     * CRITICAL FIX: Now properly delegates to table operations force flush
     */
    public void forceFlushBatch()
    {
        log.debug("DatabaseManager.forceFlushBatch() - delegating to table operations");
        
        if (tableOperations != null) {
            tableOperations.forceFlushBatch();
            log.debug("DatabaseManager.forceFlushBatch() completed - delegation successful");
        } else {
            log.error("DatabaseManager.forceFlushBatch() - tableOperations is null, cannot flush");
        }
    }
    
    /**
     * Check if database is healthy
     * 
     * This method maintains the same public API as the original version.
     */
    public boolean isHealthy()
    {
        return connectionManager.isHealthy() && performanceMonitor.isHealthy();
    }
    
    /**
     * Get database performance statistics
     * 
     * This method maintains the same public API as the original version.
     */
    public Map<String, Object> getDatabaseStats()
    {
        Map<String, Object> stats = performanceMonitor.getDatabaseStats();
        
        // Add connection manager stats
        stats.put("connected", connectionManager.isConnected());
        stats.put("activeSessions", connectionManager.getActiveSessions().size());
        
        // Add table operations stats
        Map<String, Object> tableStats = tableOperations.getPerformanceStats();
        stats.putAll(tableStats);
        
        return stats;
    }
    
    /**
     * Get active sessions
     * 
     * This method maintains compatibility with the original API.
     */
    public Map<Integer, SessionInfo> getActiveSessions()
    {
        return connectionManager.getActiveSessions();
    }
    
    /**
     * Shutdown the database manager
     * 
     * This method maintains the same public API as the original version.
     */
    public void shutdown()
    {
        log.debug("DatabaseManager.shutdown() - shutting down all modules");
        
        try {
            // Shutdown in reverse order of dependency
            tableOperations.shutdown();
            performanceMonitor.shutdown();
            connectionManager.shutdown();
            
            log.debug("DatabaseManager shutdown completed");
            
        } catch (Exception e) {
            log.error("Error during database shutdown", e);
        }
    }
    
    // ===============================================================
    // GETTER METHODS FOR MODULE ACCESS
    // These allow external code to access specific modules if needed
    // ===============================================================
    
    public DatabaseConnectionManager getConnectionManager() { return connectionManager; }
    public DatabaseTableOperations getTableOperations() { return tableOperations; }
    public DatabaseUtilities getUtilities() { return utilities; }
    public DatabasePerformanceMonitor getPerformanceMonitor() { return performanceMonitor; }
    
    // ===============================================================
    // UTILITY METHODS FOR BACKWARD COMPATIBILITY
    // These maintain compatibility with any external code that may
    // directly access functionality from the original monolithic version
    // ===============================================================
    
    /**
     * Get current player name
     */
    public String getCurrentPlayerName()
    {
        return utilities.getCurrentPlayerName();
    }
    
    /**
     * Check if database is connected
     */
    public boolean isConnected()
    {
        return connectionManager.isConnected();
    }
    
    /**
     * Convert inventory items to JSON (for external access)
     */
    public String convertInventoryItemsToJson(net.runelite.api.Item[] inventoryItems)
    {
        return utilities.convertInventoryItemsToJson(inventoryItems);
    }
    
    /**
     * Record database call timing
     */
    public void recordDatabaseCall(long timeNanos)
    {
        performanceMonitor.recordDatabaseCall(timeNanos);
    }
    
    /**
     * Get database performance metrics
     * This method maintains the exact same signature and functionality as the original version.
     * 
     * @return Performance summary string
     */
    public String getPerformanceMetrics()
    {
        long totalCalls = performanceMonitor.getDatabaseStats().containsKey("totalCalls") 
            ? (Long) performanceMonitor.getDatabaseStats().get("totalCalls") : 0;
        long totalRecords = performanceMonitor.getDatabaseStats().containsKey("totalRecords") 
            ? (Long) performanceMonitor.getDatabaseStats().get("totalRecords") : 0;
        long totalBatches = performanceMonitor.getDatabaseStats().containsKey("totalBatches") 
            ? (Long) performanceMonitor.getDatabaseStats().get("totalBatches") : 0;
        double avgTimeMs = performanceMonitor.getAverageProcessingTimeMs();
        
        return String.format(
            "Database Performance: %d calls, avg %.0fms, %d records, %d batches, pool: %s",
            totalCalls, avgTimeMs, totalRecords, totalBatches,
            connectionManager.isConnected() ? "connected" : "disconnected"
        );
    }
    
    // ===============================================================
    // SESSION MANAGEMENT METHODS FOR BACKWARD COMPATIBILITY
    // These maintain compatibility with existing session handling code
    // ===============================================================
    
    /**
     * Finalize session (backward compatibility)
     */
    public void finalizeSession(Integer sessionId)
    {
        log.debug("DatabaseManager.finalizeSession({}) - delegating to connection manager", sessionId);
        
        try {
            connectionManager.finalizeSession(sessionId);
            performanceMonitor.recordSuccess();
        } catch (Exception e) {
            performanceMonitor.recordError("Session finalization failed", e);
            throw e;
        }
    }
}