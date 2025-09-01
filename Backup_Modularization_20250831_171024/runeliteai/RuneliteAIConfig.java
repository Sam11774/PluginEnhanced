/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

/**
 * Configuration interface for RuneLiteAI Plugin
 * 
 * Provides comprehensive configuration options for:
 * - Database logging and connection settings
 * - Performance monitoring and optimization
 * - Security analytics and automation detection
 * - Quality validation and data integrity
 * - Behavioral analysis and pattern recognition
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@ConfigGroup("runeliteai")
public interface RuneliteAIConfig extends Config
{
    // ===== CORE SETTINGS SECTION =====
    
    @ConfigSection(
        name = "Core Settings",
        description = "Core plugin functionality settings",
        position = 1
    )
    String coreSection = "core";
    
    @ConfigItem(
        keyName = "enableDatabaseLogging",
        name = "Enable Database Logging",
        description = "Enable PostgreSQL database logging for collected data",
        section = coreSection,
        position = 1
    )
    default boolean enableDatabaseLogging()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "requireDatabaseConnection",
        name = "Require Database Connection",
        description = "Require database connection for plugin to function (fail startup if database unavailable)",
        section = coreSection,
        position = 2
    )
    default boolean requireDatabaseConnection()
    {
        return false;
    }
    
    @ConfigItem(
        keyName = "enablePerformanceMonitoring",
        name = "Enable Performance Monitoring",
        description = "Enable real-time performance monitoring and optimization",
        section = coreSection,
        position = 3
    )
    default boolean enablePerformanceMonitoring()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "enableDebugLogging",
        name = "Enable Debug Logging",
        description = "Enable detailed debug logging (may impact performance)",
        section = coreSection,
        position = 4
    )
    default boolean enableDebugLogging()
    {
        return false;
    }
    
    @ConfigItem(
        keyName = "enableProductionMode",
        name = "Enable Production Mode",
        description = "Disable all file logging for production deployment (logs only to console). WARNING: This will disable all structured logging to D:\\RuneliteAI\\Logs",
        section = coreSection,
        position = 5
    )
    default boolean enableProductionMode()
    {
        return false;
    }
    
    // ===== DATA COLLECTION SECTION =====
    
    @ConfigSection(
        name = "Data Collection",
        description = "Data collection and quality settings",
        position = 2
    )
    String dataSection = "data";
    
    @ConfigItem(
        keyName = "enableQualityValidation",
        name = "Enable Quality Validation",
        description = "Enable real-time data quality validation and scoring",
        section = dataSection,
        position = 1
    )
    default boolean enableQualityValidation()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "minimumQualityThreshold",
        name = "Minimum Quality Threshold",
        description = "Minimum data quality score (0.0-1.0) - data below this threshold will be flagged",
        section = dataSection,
        position = 2
    )
    default double minimumQualityThreshold()
    {
        return 0.7;
    }
    
    @ConfigItem(
        keyName = "enableEnvironmentDataCollection",
        name = "Enable Environment Data",
        description = "Collect environmental data (NPCs, objects, terrain)",
        section = dataSection,
        position = 3
    )
    default boolean enableEnvironmentDataCollection()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "enableInputDataCollection",
        name = "Enable Input Data",
        description = "Collect input data (mouse, keyboard, camera movements)",
        section = dataSection,
        position = 4
    )
    default boolean enableInputDataCollection()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "enableCombatDataCollection",
        name = "Enable Combat Data",
        description = "Collect combat data (damage, prayers, spells)",
        section = dataSection,
        position = 5
    )
    default boolean enableCombatDataCollection()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "enableSocialDataCollection",
        name = "Enable Social Data",
        description = "Collect social data (chat, friends, clan interactions)",
        section = dataSection,
        position = 6
    )
    default boolean enableSocialDataCollection()
    {
        return true;
    }
    
    // ===== SECURITY ANALYTICS SECTION =====
    
    @ConfigSection(
        name = "Security Analytics",
        description = "Automation detection and security monitoring",
        position = 3
    )
    String securitySection = "security";
    
    @ConfigItem(
        keyName = "enableSecurityAnalytics",
        name = "Enable Security Analytics",
        description = "Enable automation detection and behavioral security analysis",
        section = securitySection,
        position = 1
    )
    default boolean enableSecurityAnalytics()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "automationDetectionThreshold",
        name = "Automation Detection Threshold",
        description = "Automation detection sensitivity (0.0-1.0) - higher values are more sensitive",
        section = securitySection,
        position = 2
    )
    default double automationDetectionThreshold()
    {
        return 0.75;
    }
    
    @ConfigItem(
        keyName = "enableBehavioralAnalysis",
        name = "Enable Behavioral Analysis",
        description = "Enable advanced behavioral pattern analysis",
        section = securitySection,
        position = 3
    )
    default boolean enableBehavioralAnalysis()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "behavioralAnalysisWindowSize",
        name = "Behavioral Analysis Window",
        description = "Number of ticks to analyze for behavioral patterns",
        section = securitySection,
        position = 4
    )
    default int behavioralAnalysisWindowSize()
    {
        return 100;
    }
    
    // ===== PERFORMANCE SECTION =====
    
    @ConfigSection(
        name = "Performance",
        description = "Performance optimization and monitoring settings",
        position = 4
    )
    String performanceSection = "performance";
    
    @ConfigItem(
        keyName = "maxProcessingTimeMs",
        name = "Max Processing Time (ms)",
        description = "Maximum allowed processing time per tick (triggers warnings)",
        section = performanceSection,
        position = 1
    )
    default int maxProcessingTimeMs()
    {
        return 2;
    }
    
    @ConfigItem(
        keyName = "enableParallelProcessing",
        name = "Enable Parallel Processing",
        description = "Enable multi-threaded parallel data collection (recommended)",
        section = performanceSection,
        position = 2
    )
    default boolean enableParallelProcessing()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "parallelProcessingThreads",
        name = "Parallel Processing Threads",
        description = "Number of threads for parallel processing (0 = auto-detect)",
        section = performanceSection,
        position = 3
    )
    default int parallelProcessingThreads()
    {
        return 4;
    }
    
    @ConfigItem(
        keyName = "enableMemoryOptimization",
        name = "Enable Memory Optimization",
        description = "Enable memory optimization features (LRU caches, bounded queues)",
        section = performanceSection,
        position = 4
    )
    default boolean enableMemoryOptimization()
    {
        return true;
    }
    
    // ===== DATABASE SECTION =====
    
    @ConfigSection(
        name = "Database",
        description = "Database connection and storage settings",
        position = 5
    )
    String databaseSection = "database";
    
    @ConfigItem(
        keyName = "databaseBatchSize",
        name = "Database Batch Size",
        description = "Number of records to batch for database operations (higher = better performance)",
        section = databaseSection,
        position = 1
    )
    default int databaseBatchSize()
    {
        return 500;
    }
    
    @ConfigItem(
        keyName = "databaseConnectionTimeout",
        name = "Database Connection Timeout (ms)",
        description = "Timeout for database connection attempts",
        section = databaseSection,
        position = 2
    )
    default int databaseConnectionTimeout()
    {
        return 5000;
    }
    
    @ConfigItem(
        keyName = "databaseMaxConnections",
        name = "Database Max Connections",
        description = "Maximum number of database connections in pool",
        section = databaseSection,
        position = 3
    )
    default int databaseMaxConnections()
    {
        return 10;
    }
    
    @ConfigItem(
        keyName = "enableDatabaseCompression",
        name = "Enable Database Compression",
        description = "Enable data compression for database storage (saves space, may impact performance)",
        section = databaseSection,
        position = 4
    )
    default boolean enableDatabaseCompression()
    {
        return false;
    }
    
    // ===== REPORTING SECTION =====
    
    @ConfigSection(
        name = "Reporting",
        description = "Data reporting and export settings",
        position = 6
    )
    String reportingSection = "reporting";
    
    @ConfigItem(
        keyName = "enablePerformanceReporting",
        name = "Enable Performance Reporting",
        description = "Enable periodic performance reporting to logs",
        section = reportingSection,
        position = 1
    )
    default boolean enablePerformanceReporting()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "performanceReportingInterval",
        name = "Performance Reporting Interval (ticks)",
        description = "How often to generate performance reports",
        section = reportingSection,
        position = 2
    )
    default int performanceReportingInterval()
    {
        return 100;
    }
    
    @ConfigItem(
        keyName = "enableDataExport",
        name = "Enable Data Export",
        description = "Enable CSV/JSON data export functionality",
        section = reportingSection,
        position = 3
    )
    default boolean enableDataExport()
    {
        return false;
    }
    
    @ConfigItem(
        keyName = "exportFormat",
        name = "Export Format",
        description = "Data export format (CSV or JSON)",
        section = reportingSection,
        position = 4
    )
    default String exportFormat()
    {
        return "CSV";
    }
    
    // ===== OVERLAY SECTION =====
    
    @ConfigSection(
        name = "Overlay",
        description = "Status overlay display settings",
        position = 7
    )
    String overlaySection = "overlay";
    
    @ConfigItem(
        keyName = "showOverlay",
        name = "Show Status Overlay",
        description = "Display the RuneLite AI status overlay on screen",
        section = overlaySection,
        position = 1
    )
    default boolean showOverlay()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "overlayPosition",
        name = "Overlay Position",
        description = "Position of the status overlay on screen",
        section = overlaySection,
        position = 2
    )
    default String overlayPosition()
    {
        return "TOP_LEFT";
    }
    
    @ConfigItem(
        keyName = "showDetailedInfo",
        name = "Show Detailed Information",
        description = "Show additional details like session ID and data points per tick",
        section = overlaySection,
        position = 3
    )
    default boolean showDetailedInfo()
    {
        return true;
    }
    
    // ===== ADVANCED SECTION =====
    
    @ConfigSection(
        name = "Advanced",
        description = "Advanced settings for expert users",
        position = 8
    )
    String advancedSection = "advanced";
    
    @ConfigItem(
        keyName = "enableExperimentalFeatures",
        name = "Enable Experimental Features",
        description = "Enable experimental features (may be unstable)",
        section = advancedSection,
        position = 1
    )
    default boolean enableExperimentalFeatures()
    {
        return false;
    }
    
    @ConfigItem(
        keyName = "customSamplingRate",
        name = "Custom Sampling Rate",
        description = "Custom data sampling rate (1.0 = every tick, 0.5 = every other tick, etc.)",
        section = advancedSection,
        position = 2
    )
    default double customSamplingRate()
    {
        return 1.0;
    }
    
    @ConfigItem(
        keyName = "enableDetailedTimingBreakdown",
        name = "Enable Detailed Timing Breakdown",
        description = "Enable component-level timing breakdown (for performance analysis)",
        section = advancedSection,
        position = 3
    )
    default boolean enableDetailedTimingBreakdown()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "maxMemoryUsageMB",
        name = "Max Memory Usage (MB)",
        description = "Maximum memory usage for plugin (0 = unlimited)",
        section = advancedSection,
        position = 4
    )
    default int maxMemoryUsageMB()
    {
        return 512;
    }
    
    // ===== COLLECTION FILTERING SECTION =====
    
    @ConfigSection(
        name = "Collection Filtering",
        description = "Filter what data is collected to reduce overhead",
        position = 9
    )
    String filteringSection = "filtering";
    
    @ConfigItem(
        keyName = "collectPlayerData",
        name = "Collect Player Data",
        description = "Collect data about other players",
        section = filteringSection,
        position = 1
    )
    default boolean collectPlayerData()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "collectNPCData",
        name = "Collect NPC Data",
        description = "Collect data about NPCs",
        section = filteringSection,
        position = 2
    )
    default boolean collectNPCData()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "collectObjectData",
        name = "Collect Object Data",
        description = "Collect data about game objects and ground items",
        section = filteringSection,
        position = 3
    )
    default boolean collectObjectData()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "collectProjectileData",
        name = "Collect Projectile Data",
        description = "Collect data about projectiles and spell effects",
        section = filteringSection,
        position = 4
    )
    default boolean collectProjectileData()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "maxPlayersToTrack",
        name = "Max Players to Track",
        description = "Maximum number of nearby players to track (0 = unlimited)",
        section = filteringSection,
        position = 5
    )
    default int maxPlayersToTrack()
    {
        return 100;
    }
    
    @ConfigItem(
        keyName = "maxNPCsToTrack",
        name = "Max NPCs to Track",
        description = "Maximum number of nearby NPCs to track (0 = unlimited)",
        section = filteringSection,
        position = 6
    )
    default int maxNPCsToTrack()
    {
        return 200;
    }
}