/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Level;
import ch.qos.logback.core.Appender;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logging Configuration Manager
 * 
 * Provides dynamic control over logging configuration including:
 * - Production mode toggle (disable file logging, console only)
 * - Debug logging level control
 * - Appender management for performance optimization
 * - Runtime logging configuration updates
 * 
 * This manager allows the plugin to adapt logging behavior based on
 * configuration changes without requiring restart.
 * 
 * Production Mode:
 * - Disables all file appenders to prevent disk I/O overhead
 * - Reduces log levels to WARN and above for performance
 * - Maintains console logging for critical information
 * - Preserves error logging for troubleshooting
 * 
 * @author RuneLiteAI Team
 */
@Slf4j
public class LoggingConfigurationManager {

    private final RuneliteAIConfig config;
    private final LoggerContext loggerContext;
    private final Map<String, Map<String, Appender<?>>> originalAppenders = new ConcurrentHashMap<>();
    private boolean productionModeActive = false;
    private boolean initialized = false;
    
    // RuneLiteAI logger names for targeted configuration
    private static final Set<String> RUNELITEAI_LOGGERS = Set.of(
        "net.runelite.client.plugins.runeliteai.RuneliteAIPlugin",
        "net.runelite.client.plugins.runeliteai.DataCollectionManager", 
        "net.runelite.client.plugins.runeliteai.DataCollectionOrchestrator",
        "net.runelite.client.plugins.runeliteai.DatabaseManager",
        "net.runelite.client.plugins.runeliteai.SecurityAnalyticsManager",
        "net.runelite.client.plugins.runeliteai.PerformanceMonitor",
        "net.runelite.client.plugins.runeliteai.PlayerDataCollector",
        "net.runelite.client.plugins.runeliteai.WorldDataCollector", 
        "net.runelite.client.plugins.runeliteai.InputDataCollector",
        "net.runelite.client.plugins.runeliteai.CombatDataCollector",
        "net.runelite.client.plugins.runeliteai.SocialDataCollector",
        "net.runelite.client.plugins.runeliteai.InterfaceDataCollector",
        "net.runelite.client.plugins.runeliteai.SystemMetricsCollector",
        "net.runelite.client.plugins.runeliteai.DatabaseConnectionManager",
        "net.runelite.client.plugins.runeliteai.DatabaseTableOperations",
        "net.runelite.client.plugins.runeliteai.DatabaseUtilities",
        "net.runelite.client.plugins.runeliteai.DatabasePerformanceMonitor",
        "net.runelite.client.plugins.runeliteai.ClickIntelligenceService",
        "net.runelite.client.plugins.runeliteai.OSRSWikiService",
        "net.runelite.client.plugins.runeliteai.QualityValidator",
        "net.runelite.client.plugins.runeliteai.BehavioralAnalysisManager"
    );

    public LoggingConfigurationManager(RuneliteAIConfig config) {
        this.config = config;
        this.loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        log.info("LoggingConfigurationManager initialized");
    }

    /**
     * Initialize logging configuration based on current config settings
     */
    public void initializeLogging() {
        if (initialized) {
            return;
        }

        try {
            // Store original appenders for potential restoration
            storeOriginalAppenders();
            
            // Apply initial configuration
            updateLoggingConfiguration();
            
            initialized = true;
            log.info("Logging configuration initialized successfully - Production Mode: {}", 
                config.enableProductionMode());
                
        } catch (Exception e) {
            log.error("Failed to initialize logging configuration", e);
        }
    }

    /**
     * Update logging configuration based on current config settings
     * Called when configuration changes
     */
    public void updateLoggingConfiguration() {
        try {
            boolean newProductionMode = config.enableProductionMode();
            boolean debugLogging = config.enableDebugLogging();
            
            if (newProductionMode != productionModeActive) {
                if (newProductionMode) {
                    enableProductionMode();
                } else {
                    disableProductionMode();
                }
                productionModeActive = newProductionMode;
            }
            
            // Update debug logging level
            updateDebugLogging(debugLogging);
            
            log.info("Logging configuration updated - Production Mode: {}, Debug Logging: {}", 
                newProductionMode, debugLogging);
                
        } catch (Exception e) {
            log.error("Failed to update logging configuration", e);
        }
    }

    /**
     * Enable production mode - disable file appenders, console only
     */
    private void enableProductionMode() {
        log.warn("ENABLING PRODUCTION MODE - File logging will be disabled");
        
        for (String loggerName : RUNELITEAI_LOGGERS) {
            Logger logger = loggerContext.getLogger(loggerName);
            if (logger != null) {
                // Remove all file-based appenders, keep only STDOUT
                Iterator<Appender<ch.qos.logback.classic.spi.ILoggingEvent>> appenders = 
                    logger.iteratorForAppenders();
                    
                while (appenders.hasNext()) {
                    Appender<ch.qos.logback.classic.spi.ILoggingEvent> appender = appenders.next();
                    String appenderName = appender.getName();
                    
                    // Keep only console appender
                    if (!"STDOUT".equals(appenderName)) {
                        logger.detachAppender(appender);
                        log.debug("Removed appender '{}' from logger '{}'", appenderName, loggerName);
                    }
                }
                
                // Set more conservative logging level for production
                if (logger.getLevel() == null || logger.getLevel().levelInt < Level.WARN.levelInt) {
                    logger.setLevel(Level.WARN);
                }
            }
        }
        
        log.warn("Production mode enabled - File logging disabled, console logging only");
    }

    /**
     * Disable production mode - restore file appenders
     */
    private void disableProductionMode() {
        log.info("DISABLING PRODUCTION MODE - Restoring file logging");
        
        // Restore original appenders
        for (Map.Entry<String, Map<String, Appender<?>>> entry : originalAppenders.entrySet()) {
            String loggerName = entry.getKey();
            Map<String, Appender<?>> appenders = entry.getValue();
            
            Logger logger = loggerContext.getLogger(loggerName);
            if (logger != null) {
                // Re-attach original appenders
                for (Map.Entry<String, Appender<?>> appenderEntry : appenders.entrySet()) {
                    String appenderName = appenderEntry.getKey();
                    Appender<?> appender = appenderEntry.getValue();
                    
                    if (!"STDOUT".equals(appenderName)) {
                        @SuppressWarnings("unchecked")
                        Appender<ch.qos.logback.classic.spi.ILoggingEvent> typedAppender = 
                            (Appender<ch.qos.logback.classic.spi.ILoggingEvent>) appender;
                        logger.addAppender(typedAppender);
                        log.debug("Restored appender '{}' to logger '{}'", appenderName, loggerName);
                    }
                }
                
                // Restore INFO level for regular operation
                logger.setLevel(Level.INFO);
            }
        }
        
        log.info("Production mode disabled - File logging restored");
    }

    /**
     * Update debug logging level for all RuneLiteAI loggers
     */
    private void updateDebugLogging(boolean enableDebug) {
        Level targetLevel = enableDebug ? Level.DEBUG : Level.INFO;
        
        // Don't change levels if production mode is active (it manages levels)
        if (productionModeActive) {
            return;
        }
        
        for (String loggerName : RUNELITEAI_LOGGERS) {
            Logger logger = loggerContext.getLogger(loggerName);
            if (logger != null) {
                logger.setLevel(targetLevel);
            }
        }
        
        log.info("Debug logging {}", enableDebug ? "enabled" : "disabled");
    }

    /**
     * Store original appenders for restoration when production mode is disabled
     */
    private void storeOriginalAppenders() {
        for (String loggerName : RUNELITEAI_LOGGERS) {
            Logger logger = loggerContext.getLogger(loggerName);
            if (logger != null) {
                Map<String, Appender<?>> loggerAppenders = new HashMap<>();
                
                Iterator<Appender<ch.qos.logback.classic.spi.ILoggingEvent>> appenders = 
                    logger.iteratorForAppenders();
                    
                while (appenders.hasNext()) {
                    Appender<ch.qos.logback.classic.spi.ILoggingEvent> appender = appenders.next();
                    loggerAppenders.put(appender.getName(), appender);
                }
                
                originalAppenders.put(loggerName, loggerAppenders);
            }
        }
        
        log.debug("Stored original appenders for {} loggers", originalAppenders.size());
    }

    /**
     * Get current logging status for monitoring
     */
    public Map<String, Object> getLoggingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("productionModeActive", productionModeActive);
        status.put("debugLoggingEnabled", config.enableDebugLogging());
        status.put("configuredLoggers", RUNELITEAI_LOGGERS.size());
        status.put("initialized", initialized);
        return status;
    }

    /**
     * Shutdown logging configuration manager
     */
    public void shutdown() {
        log.info("Shutting down LoggingConfigurationManager");
        
        if (productionModeActive) {
            // Restore file logging before shutdown
            disableProductionMode();
        }
        
        initialized = false;
    }
}