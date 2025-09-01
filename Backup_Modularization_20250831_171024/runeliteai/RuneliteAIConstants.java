/*
 * Copyright (c) 2025, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

/**
 * Constants for RuneLiteAI plugin configuration
 * Centralizes all magic numbers and thresholds for better maintainability
 */
public class RuneliteAIConstants 
{
    // Database configuration
    public static final int DEFAULT_BATCH_SIZE = 1; // Real-time processing - immediate database insertion
    public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 5000;
    public static final int DEFAULT_POOL_SIZE = 10;
    public static final int QUEUE_SIZE = 10000;
    public static final int RETRY_ATTEMPTS = 3;
    public static final int RETRY_DELAY_MS = 1000;
    
    // Performance monitoring
    public static final int MAX_HISTORY_SIZE = 1000;
    public static final double WARNING_PROCESSING_TIME_MS = 5.0;
    public static final double CRITICAL_PROCESSING_TIME_MS = 10.0;
    public static final long WARNING_CLEANUP_INTERVAL = 300000; // 5 minutes
    public static final int MAX_PERFORMANCE_WARNINGS = 50;
    public static final int WARNING_RETENTION_MINUTES = 15;
    
    // Memory thresholds
    public static final int MEMORY_WARNING_THRESHOLD = 80; // 80% memory usage
    public static final int MEMORY_CRITICAL_THRESHOLD = 90; // 90% memory usage
    
    // Behavioral analysis
    public static final int MAX_ACTIVITY_HISTORY = 500;
    public static final int ANALYSIS_WINDOW_SIZE = 100;
    public static final int MIN_ANALYSIS_ACTIVITIES = 50;
    public static final int BEHAVIORAL_ANALYSIS_THRESHOLD = 50;
    
    // Security analytics
    public static final int MAX_RECENT_ACTIONS = 1000;
    public static final int MAX_CHAT_MESSAGES = 100;
    public static final int MIN_SECURITY_DATA_POINTS = 50;
    public static final int MEDIUM_SECURITY_THRESHOLD = 200;
    public static final double AUTOMATION_SCORE_THRESHOLD = 0.7;
    
    // Collection cache limits
    public static final int MAX_CHAT_MESSAGE_HISTORY = 100;
    public static final int MAX_ITEM_CHANGE_HISTORY = 50;
    public static final int MAX_STAT_CHANGE_HISTORY = 100;
    public static final int MAX_HITSPLAT_HISTORY = 200;
    public static final int MAX_ANIMATION_CHANGE_HISTORY = 100;
    public static final int MAX_INTERACTION_CHANGE_HISTORY = 100;
    
    // Threading and async processing
    public static final int EXECUTOR_THREAD_POOL_SIZE = 4;
    public static final int ASYNC_TIMEOUT_MS = 50;
    public static final String THREAD_NAME_PREFIX = "RuneliteAI-DataCollection";
    
    // Performance reporting
    public static final int PERFORMANCE_REPORT_INTERVAL_TICKS = 100;
    
    // Data validation
    public static final long MAX_PROCESSING_TIME_NANOS = 100_000_000_000L; // 100 seconds
    public static final int MAX_ANIMATION_ID = 10000;
    public static final int MIN_ENERGY_LEVEL = 0;
    public static final int MAX_ENERGY_LEVEL = 100;
    public static final long DATA_FRESHNESS_THRESHOLD_MS = 5000; // 5 seconds
    
    // Quality scoring
    public static final double EXCELLENT_TICK_TIME = 1.0; // 1ms
    public static final double GOOD_TICK_TIME = 2.0; // 2ms
    public static final double POOR_TICK_TIME = 10.0; // 10ms
    public static final double EXCELLENT_QUALITY_SCORE = 100.0;
    public static final double GOOD_QUALITY_SCORE = 75.0;
    public static final double POOR_QUALITY_SCORE = 50.0;
    
    // Ground object tracking
    public static final long GROUND_ITEM_OWNERSHIP_DURATION_MS = 60000; // 1 minute
    public static final long GROUND_ITEM_VISIBILITY_DURATION_MS = 180000; // 3 minutes
    public static final int MAX_GROUND_ITEM_HISTORY = 1000;
    public static final int GROUND_ITEM_CLEANUP_INTERVAL_MS = 30000; // 30 seconds
    
    // Widget and interface constants
    public static final int TRADE_SCREEN_WIDGET_ID = 335;
    public static final int BANK_INTERFACE_WIDGET_ID = 12;
    public static final int SHOP_INTERFACE_WIDGET_ID = 300;
    public static final int GE_INTERFACE_WIDGET_ID = 465;
    
    // Combat constants
    public static final int COMBAT_STYLE_ACCURATE = 0;
    public static final int COMBAT_STYLE_AGGRESSIVE = 1;
    public static final int COMBAT_STYLE_CONTROLLED = 2;
    public static final int COMBAT_STYLE_DEFENSIVE = 3;
    
    // Region boundaries (for location validation)
    public static final int MIN_WORLD_X = 1000;
    public static final int MAX_WORLD_X = 4000;
    public static final int MIN_WORLD_Y = 1000;
    public static final int MAX_WORLD_Y = 4000;
    public static final int MIN_PLANE = 0;
    public static final int MAX_PLANE = 3;
    
    // World scanning and detection
    public static final int WORLD_SCAN_RADIUS = 15;
    public static final int KEYBOARD_SCAN_LIMIT = 256;
    public static final int PROJECTILE_HISTORY_LIMIT = 150;
    public static final int MEMORY_MB_CONVERSION = 1024 * 1024;
    
    // Animation ID ranges
    public static final int COMBAT_ANIMATION_MIN_1 = 400;
    public static final int COMBAT_ANIMATION_MAX_1 = 500;
    public static final int COMBAT_ANIMATION_MIN_2 = 1150;
    public static final int COMBAT_ANIMATION_MAX_2 = 1200;
    public static final int COMBAT_ANIMATION_MIN_3 = 7500;
    public static final int COMBAT_ANIMATION_MAX_3 = 7600;
    public static final int MOVEMENT_ANIMATION_MIN = 800;
    public static final int MOVEMENT_ANIMATION_MAX = 900;
    public static final int MAGIC_ANIMATION_MIN = 1200;
    public static final int MAGIC_ANIMATION_MAX = 1300;
    public static final int SKILLING_ANIMATION_MIN = 700;
    public static final int SKILLING_ANIMATION_MAX = 800;
    
    // Specific combat animation IDs
    public static final int UNARMED_PUNCH_ANIMATION = 422;
    public static final int UNARMED_KICK_ANIMATION = 423;
    public static final int SWORD_STAB_ANIMATION = 428;
    public static final int SWORD_SLASH_ANIMATION = 440;
    public static final int DAGGER_STAB_ANIMATION = 412;
    public static final int MACE_POUND_ANIMATION = 451;
    public static final int AXE_HACK_ANIMATION = 426;
    public static final int BOW_DRAW_ANIMATION = 1167;
    public static final int CROSSBOW_AIM_ANIMATION = 7552;
    public static final int WHIP_CRACK_ANIMATION = 1979;
    
    // Widget group IDs for interface detection
    public static final int INVENTORY_WIDGET_GROUP = 149;
    public static final int SKILLS_WIDGET_GROUP = 320;
    public static final int COMBAT_WIDGET_GROUP = 399;
    public static final int PRAYER_WIDGET_GROUP = 429;
    public static final int SPELLBOOK_WIDGET_GROUP = 541;
    public static final int QUEST_WIDGET_GROUP = 548;
    public static final int CHATBOX_WIDGET_GROUP = 161;
    public static final int MINIMAP_WIDGET_GROUP = 164;
    public static final int SETTINGS_WIDGET_GROUP = 593;
    public static final int DIALOGUE_WIDGET_GROUP = 231;
    public static final int DUELING_WIDGET_GROUP = 334;
    public static final int EQUIPMENT_WIDGET_GROUP = 206;
    
    // VarPlayer IDs
    public static final int AUTO_RETALIATE_VARP = 172;
    public static final int QUICK_PRAYER_VARP = 181;
    public static final int AUTOCAST_SPELL_VARP = 276;
    public static final int SPELLBOOK_VARP = 439;
    
    // City coordinate boundaries (Lumbridge)
    public static final int LUMBRIDGE_MIN_X = 3200;
    public static final int LUMBRIDGE_MAX_X = 3230;
    public static final int LUMBRIDGE_MIN_Y = 3200;
    public static final int LUMBRIDGE_MAX_Y = 3230;
    
    // Varrock boundaries
    public static final int VARROCK_MIN_X = 3090;
    public static final int VARROCK_MAX_X = 3125;
    public static final int VARROCK_MIN_Y = 3240;
    public static final int VARROCK_MAX_Y = 3270;
    
    // Falador boundaries
    public static final int FALADOR_MIN_X = 3250;
    public static final int FALADOR_MAX_X = 3280;
    public static final int FALADOR_MIN_Y = 3370;
    public static final int FALADOR_MAX_Y = 3400;
    
    // Performance monitoring intervals
    public static final long PERFORMANCE_REPORT_INTERVAL_MS = 60000; // 1 minute
    
    // Automation detection thresholds
    public static final double HIGH_AUTOMATION_SCORE = 0.8;
    public static final double MEDIUM_AUTOMATION_SCORE = 0.6;
    
    // Prayer drain rates
    public static final int PRAYER_DRAIN_RATE_LOW = 1;
    public static final int PRAYER_DRAIN_RATE_MEDIUM = 2;
    public static final int PRAYER_DRAIN_RATE_HIGH = 4;
    public static final int PRAYER_DRAIN_RATE_VERY_HIGH = 6;
    public static final int PRAYER_DRAIN_RATE_EXTREME = 8;
    public static final int PRAYER_DRAIN_RATE_MAXIMUM = 10;
    
    // Performance scoring thresholds  
    public static final double EXCELLENT_PERFORMANCE_MS = 1.0;
    public static final double GOOD_PERFORMANCE_MS = 2.0;
    public static final double FAIR_PERFORMANCE_MS = 5.0;
    public static final double POOR_PERFORMANCE_MS = 10.0;
    
    // Spellbook types
    public static final int STANDARD_SPELLBOOK = 0;
    public static final int ANCIENT_SPELLBOOK = 1;
    public static final int LUNAR_SPELLBOOK = 2;
    public static final int ARCEUUS_SPELLBOOK = 3;

    // Private constructor to prevent instantiation
    private RuneliteAIConstants() 
    {
        throw new UnsupportedOperationException("Constants class should not be instantiated");
    }
}