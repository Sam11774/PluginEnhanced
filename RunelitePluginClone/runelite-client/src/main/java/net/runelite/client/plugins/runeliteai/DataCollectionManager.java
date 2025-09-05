/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Modular Data Collection Manager using delegation pattern
 * 
 * Orchestrates data collection across specialized collector modules:
 * - PlayerDataCollector: Player vitals, stats, location, equipment
 * - WorldDataCollector: Environment, NPCs, objects, projectiles
 * - InputDataCollector: Mouse, keyboard, camera, movement analytics
 * - CombatDataCollector: Combat events, animations, damage tracking
 * - SocialDataCollector: Chat, clan, trade interactions
 * - InterfaceDataCollector: UI interactions, banking, widgets
 * - SystemMetricsCollector: Performance metrics, optimization
 * 
 * This class maintains the exact same public API as the original monolithic version
 * while delegating all work to specialized modules for better maintainability.
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0 - Modular Architecture
 */
@Slf4j
public class DataCollectionManager
{
    // Core dependencies
    private final Client client;
    private final ItemManager itemManager;
    private final ConfigManager configManager;
    private final RuneliteAIPlugin plugin;
    
    // Specialized collectors
    private final DataCollectionOrchestrator orchestrator;
    private final PlayerDataCollector playerCollector;
    private final WorldDataCollector worldCollector;
    private final InputDataCollector inputCollector;
    private final CombatDataCollector combatCollector;
    private final SocialDataCollector socialCollector;
    private final InterfaceDataCollector interfaceCollector;
    private final SystemMetricsCollector systemCollector;
    
    // Item container tracking for banking events
    private final Queue<ItemContainerChanged> recentItemChanges = new ConcurrentLinkedQueue<>();
    
    /**
     * Constructor - initializes all modular collectors
     */
    public DataCollectionManager(Client client, ItemManager itemManager, ConfigManager configManager, RuneliteAIPlugin plugin)
    {
        this.client = client;
        this.itemManager = itemManager;
        this.configManager = configManager;
        this.plugin = plugin;
        
        // Initialize support services
        GroundObjectTracker groundObjectTracker = new GroundObjectTracker();
        DistanceAnalyticsManager distanceAnalyticsManager = new DistanceAnalyticsManager();
        
        // Initialize all specialized collectors
        this.playerCollector = new PlayerDataCollector(client, itemManager);
        this.worldCollector = new WorldDataCollector(client, groundObjectTracker, distanceAnalyticsManager);
        this.inputCollector = new InputDataCollector(client, distanceAnalyticsManager, plugin);
        this.combatCollector = new CombatDataCollector(client);
        this.socialCollector = new SocialDataCollector(client);
        this.interfaceCollector = new InterfaceDataCollector(client, itemManager);
        this.systemCollector = new SystemMetricsCollector(client);
        
        // Set reference to DataCollectionManager for banking data collection
        this.interfaceCollector.setDataCollectionManager(this);
        
        // Initialize orchestrator with all collectors
        this.orchestrator = new DataCollectionOrchestrator(
            playerCollector, worldCollector, inputCollector, combatCollector,
            socialCollector, interfaceCollector, systemCollector
        );
        
        log.debug("DataCollectionManager initialized with modular architecture - 8 collectors ready");
    }
    
    /**
     * Collect all data for a single game tick - MAIN ENTRY POINT
     * 
     * This method maintains the exact same signature and behavior as the original
     * monolithic version, but delegates all work to the orchestrator.
     * 
     * @param sessionId Current session identifier
     * @param tickNumber Current tick number
     * @param gameStateSnapshot Current game state snapshot
     * @param gameStateDelta Changes since last tick
     * @return Complete tick data collection
     */
    public TickDataCollection collectAllData(Integer sessionId, int tickNumber, 
                                           GameStateSnapshot gameStateSnapshot, 
                                           GameStateDelta gameStateDelta)
    {
        log.debug("DataCollectionManager.collectAllData() - delegating to orchestrator");
        
        // Delegate to orchestrator - maintains all original functionality
        return orchestrator.collectAllData(sessionId, tickNumber, gameStateSnapshot, gameStateDelta);
    }
    
    /**
     * Shutdown the data collection manager
     * 
     * This method maintains the same public API as the original version.
     */
    public void shutdown()
    {
        log.debug("DataCollectionManager.shutdown() - shutting down orchestrator and all collectors");
        
        // Delegate to orchestrator
        orchestrator.shutdown();
        
        log.debug("DataCollectionManager shutdown completed");
    }
    
    // ===============================================================
    // EVENT FORWARDING METHODS
    // These methods forward events to the appropriate collectors
    // to maintain compatibility with the original event handling
    // ===============================================================
    
    /**
     * Forward hitsplat events to combat collector
     */
    public void onHitsplatApplied(HitsplatApplied hitsplatApplied)
    {
        combatCollector.onHitsplatApplied(hitsplatApplied);
    }
    
    /**
     * Forward animation events to combat collector
     */
    public void onAnimationChanged(AnimationChanged animationChanged)
    {
        combatCollector.onAnimationChanged(animationChanged);
    }
    
    /**
     * Forward interaction events to combat collector
     */
    public void onInteractingChanged(InteractingChanged interactingChanged)
    {
        combatCollector.onInteractingChanged(interactingChanged);
    }
    
    /**
     * Forward chat events to social collector
     */
    public void onChatMessage(ChatMessage chatMessage)
    {
        socialCollector.onChatMessage(chatMessage);
    }
    
    /**
     * Forward banking method detection to interface collector
     */
    public void detectBankingMethod(String menuOption, String menuTarget)
    {
        interfaceCollector.detectBankingMethod(menuOption, menuTarget);
    }
    
    /**
     * Get last banking method (for compatibility with original API)
     */
    public String getLastBankingMethod(String action)
    {
        return interfaceCollector.getLastBankingMethod(action);
    }
    
    /**
     * Update click context (for input collector)
     */
    public void updateClickContext(DataStructures.ClickContextData clickContext)
    {
        inputCollector.updateClickContext(clickContext);
    }
    
    // ===============================================================
    // GETTER METHODS FOR MODULE ACCESS
    // These allow external code to access specific collectors if needed
    // ===============================================================
    
    public PlayerDataCollector getPlayerCollector() { return playerCollector; }
    public WorldDataCollector getWorldCollector() { return worldCollector; }
    public InputDataCollector getInputCollector() { return inputCollector; }
    public CombatDataCollector getCombatCollector() { return combatCollector; }
    public SocialDataCollector getSocialCollector() { return socialCollector; }
    public InterfaceDataCollector getInterfaceCollector() { return interfaceCollector; }
    public SystemMetricsCollector getSystemCollector() { return systemCollector; }
    public DataCollectionOrchestrator getOrchestrator() { return orchestrator; }
    
    // ===============================================================
    // BACKWARD COMPATIBILITY METHODS
    // These maintain compatibility with any external code that may
    // directly access internal state from the original monolithic version
    // ===============================================================
    
    /**
     * Check if the data collection manager is shutdown
     */
    public boolean isShutdown()
    {
        return orchestrator.isShutdown();
    }
    
    /**
     * Get the last game state snapshot
     */
    public GameStateSnapshot getLastSnapshot()
    {
        return orchestrator.getLastSnapshot();
    }
    
    // ===============================================================
    // EVENT RECORDING METHODS FOR BACKWARD COMPATIBILITY
    // These maintain compatibility with existing event handling code
    // ===============================================================
    
    /**
     * Record chat message (backward compatibility)
     */
    public void recordChatMessage(ChatMessage chatMessage)
    {
        socialCollector.onChatMessage(chatMessage);
    }
    
    /**
     * Record item container change (backward compatibility)
     */
    public void recordItemContainerChange(net.runelite.api.events.ItemContainerChanged itemContainerChanged)
    {
        if (itemContainerChanged != null) {
            recentItemChanges.offer(itemContainerChanged);
            // Keep only last 50 item changes to prevent excessive memory usage
            while (recentItemChanges.size() > 50) {
                recentItemChanges.poll();
            }
            
            log.debug("[BANK-DEBUG] Recorded item container change - containerId: {}, queue size: {}", 
                itemContainerChanged.getContainerId(), recentItemChanges.size());
        }
    }
    
    /**
     * Get recent item changes for banking data collection
     */
    public Queue<ItemContainerChanged> getRecentItemChanges() {
        return recentItemChanges;
    }
    
    /**
     * Record stat change (backward compatibility)
     */
    public void recordStatChange(net.runelite.api.events.StatChanged statChanged)
    {
        // TODO: Forward to appropriate collector when migrated
        log.debug("recordStatChange - placeholder for backward compatibility");
    }
    
    /**
     * Record hitsplat (backward compatibility)
     */
    public void recordHitsplat(net.runelite.api.events.HitsplatApplied hitsplatApplied)
    {
        combatCollector.onHitsplatApplied(hitsplatApplied);
    }
    
    /**
     * Record animation change (backward compatibility)
     */
    public void recordAnimationChange(net.runelite.api.events.AnimationChanged animationChanged)
    {
        combatCollector.onAnimationChanged(animationChanged);
    }
    
    /**
     * Record interaction change (backward compatibility)
     */
    public void recordInteractionChange(net.runelite.api.events.InteractingChanged interactingChanged)
    {
        combatCollector.onInteractingChanged(interactingChanged);
    }
    
    /**
     * Record projectile (backward compatibility)
     */
    public void recordProjectile(net.runelite.api.events.ProjectileMoved projectileMoved)
    {
        if (projectileMoved != null && projectileMoved.getProjectile() != null) {
            // Forward to WorldDataCollector via InputDataCollector or directly
            if (orchestrator != null && orchestrator.getWorldCollector() != null) {
                orchestrator.getWorldCollector().recordProjectileEvent(projectileMoved);
                log.debug("[PROJECTILE-DEBUG] Forwarded ProjectileMoved event to WorldDataCollector: ID={}", 
                    projectileMoved.getProjectile().getId());
            } else {
                log.warn("Cannot record projectile - orchestrator or worldCollector is null");
            }
        }
    }
    
    /**
     * Record ground object (backward compatibility)
     */
    public void recordGroundObject(net.runelite.api.GroundObject groundObject)
    {
        // TODO: Forward to appropriate collector when migrated
        log.debug("recordGroundObject - placeholder for backward compatibility");
    }
    
    /**
     * Record game object (backward compatibility)
     */
    public void recordGameObject(net.runelite.api.GameObject gameObject)
    {
        // TODO: Forward to appropriate collector when migrated
        log.debug("recordGameObject - placeholder for backward compatibility");
    }
    
    /**
     * Record click context - FULL IMPLEMENTATION
     */
    public void recordClickContext(net.runelite.api.events.MenuOptionClicked event)
    {
        try {
            MenuEntry menuEntry = event.getMenuEntry();
            if (menuEntry == null) return;
            
            // Determine click type based on context
            String clickType = determineClickType(event);
            
            // Get basic menu information
            String menuAction = menuEntry.getType() != null ? menuEntry.getType().name() : "UNKNOWN";
            String menuOption = menuEntry.getOption();
            String menuTarget = menuEntry.getTarget();
            
            // Banking method detection
            detectBankingMethod(menuOption, menuTarget);
            
            // Classify target type and get additional context
            String targetType = classifyTargetType(menuEntry);
            String targetName = resolveTargetName(menuEntry, targetType);
            Integer targetId = menuEntry.getIdentifier();
            
            // Get coordinates
            Point mousePos = client.getMouseCanvasPosition();
            Integer screenX = mousePos != null ? mousePos.getX() : null;
            Integer screenY = mousePos != null ? mousePos.getY() : null;
            
            // Get world coordinates if applicable
            Integer worldX = null, worldY = null, plane = null;
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer != null && localPlayer.getWorldLocation() != null) {
                worldX = localPlayer.getWorldLocation().getX();
                worldY = localPlayer.getWorldLocation().getY();
                plane = localPlayer.getWorldLocation().getPlane();
            }
            
            // Determine target characteristics
            Boolean isPlayerTarget = isPlayerTarget(menuEntry);
            Boolean isEnemyTarget = isEnemyTarget(menuEntry);
            
            // Get widget information
            String widgetInfo = getWidgetInfo(menuEntry);
            
            // Build click context data
            DataStructures.ClickContextData clickContext = DataStructures.ClickContextData.builder()
                .clickType(clickType)
                .menuAction(menuAction)
                .menuOption(menuOption)
                .menuTarget(menuTarget)
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .screenX(screenX)
                .screenY(screenY)
                .worldX(worldX)
                .worldY(worldY)
                .plane(plane)
                .isPlayerTarget(isPlayerTarget)
                .isEnemyTarget(isEnemyTarget)
                .widgetInfo(widgetInfo)
                .clickTimestamp(System.currentTimeMillis())
                .param0(menuEntry.getParam0())
                .param1(menuEntry.getParam1())
                .itemId(event.isItemOp() ? event.getItemId() : null)
                .itemName(event.isItemOp() ? getItemName(event.getItemId()) : null)
                .itemOp(event.isItemOp() ? event.getItemOp() : null)
                .isItemOp(event.isItemOp())
                .build();
            
            // Forward to input collector
            updateClickContext(clickContext);
            
            log.debug("[CLICK-DEBUG] Recorded click: {} -> {} ({})", 
                clickType, targetType, targetName != null ? targetName : "Unknown");
                
        } catch (Exception e) {
            log.warn("Error recording click context", e);
        }
    }
    
    /**
     * Get animation name (backward compatibility)
     */
    public String getAnimationName(int animationId)
    {
        // TODO: Migrate implementation when combat collector is completed
        if (animationId == -1) return "IDLE";
        return "ANIMATION_" + animationId;
    }
    
    /**
     * Register player (backward compatibility)
     */
    public void registerPlayer(net.runelite.api.Player player)
    {
        // TODO: Forward to appropriate collector when migrated
        log.debug("registerPlayer - placeholder for backward compatibility");
    }
    
    /**
     * Unregister player (backward compatibility)
     */
    public void unregisterPlayer(net.runelite.api.Player player)
    {
        // TODO: Forward to appropriate collector when migrated
        log.debug("unregisterPlayer - placeholder for backward compatibility");
    }
    
    /**
     * Register NPC (backward compatibility)
     */
    public void registerNPC(net.runelite.api.NPC npc)
    {
        // TODO: Forward to appropriate collector when migrated
        log.debug("registerNPC - placeholder for backward compatibility");
    }
    
    /**
     * Unregister NPC (backward compatibility)
     */
    public void unregisterNPC(net.runelite.api.NPC npc)
    {
        // TODO: Forward to appropriate collector when migrated
        log.debug("unregisterNPC - placeholder for backward compatibility");
    }
    
    /**
     * Record security analysis data
     * This method maintains the exact same signature and functionality as the original version.
     * 
     * @param automationScore Calculated automation risk score
     * @param riskLevel Risk level classification (LOW, MEDIUM, HIGH)
     * @param totalActions Total number of actions analyzed
     * @param suspiciousActions Number of actions flagged as suspicious
     * @param timestamp Timestamp of the analysis
     */
    public void recordSecurityAnalysis(double automationScore, String riskLevel, 
                                     int totalActions, int suspiciousActions, long timestamp) {
        try {
            log.debug("Security Analysis: Score={:.2f}, Risk={}, Actions={}/{}, Timestamp={}", 
                automationScore, riskLevel, suspiciousActions, totalActions, timestamp);
            
            // Store security analysis data for database insertion
            // This could be enhanced to store in a queue for batch processing
            if (automationScore > RuneliteAIConstants.HIGH_AUTOMATION_SCORE) {
                log.warn("HIGH AUTOMATION RISK DETECTED: Score {:.2f}", automationScore);
            } else if (automationScore > RuneliteAIConstants.MEDIUM_AUTOMATION_SCORE) {
                log.warn("MODERATE AUTOMATION RISK: Score {:.2f}", automationScore);
            }
            
        } catch (Exception e) {
            log.error("Error recording security analysis", e);
        }
    }
    
    /**
     * Collect item metadata for a given item ID
     * This method maintains the exact same signature and functionality as the original version.
     * 
     * @param itemId The item ID to collect metadata for
     * @return ItemMetadata object containing item information
     */
    public ItemMetadata collectItemMetadata(int itemId) {
        String itemName = "Unknown";
        
        if (itemManager != null) {
            try {
                ItemComposition itemComposition = itemManager.getItemComposition(itemId);
                if (itemComposition != null) {
                    itemName = itemComposition.getName();
                }
            } catch (Exception e) {
                log.debug("Failed to get item composition for item ID {}: {}", itemId, e.getMessage());
            }
        }
        
        return ItemMetadata.builder()
            .itemId(itemId)
            .itemName(itemName)
            .build();
    }
    
    // ===============================================================
    // CLICK CONTEXT HELPER METHODS
    // ===============================================================
    
    /**
     * Determine click type (LEFT/RIGHT/MENU) based on context
     */
    private String determineClickType(MenuOptionClicked event)
    {
        // In RuneLite, most clicks come through as menu actions
        // We can detect right-clicks vs left-clicks based on menu complexity
        if (client.getMenuEntries() != null && client.getMenuEntries().length > 1) {
            return "MENU"; // Multiple options = right-click menu
        } else {
            return "LEFT"; // Single action = left-click
        }
    }
    
    /**
     * Classify target type based on MenuAction
     */
    private String classifyTargetType(MenuEntry menuEntry)
    {
        if (menuEntry.getType() == null) return "UNKNOWN";
        
        String action = menuEntry.getType().name();
        
        // Game objects
        if (action.startsWith("GAME_OBJECT_")) {
            return "GAME_OBJECT";
        }
        
        // NPCs
        if (action.startsWith("NPC_")) {
            return "NPC";
        }
        
        // Ground items
        if (action.startsWith("GROUND_ITEM_")) {
            return "GROUND_ITEM";
        }
        
        // Players
        if (action.contains("PLAYER")) {
            return "PLAYER";
        }
        
        // Widgets/Interface
        if (action.startsWith("WIDGET_") || action.startsWith("CC_")) {
            return "INTERFACE";
        }
        
        // Items
        if (action.contains("ITEM") || action.startsWith("CC_OP")) {
            return "INVENTORY_ITEM";
        }
        
        // Walking
        if (action.equals("WALK")) {
            return "WALK";
        }
        
        return "OTHER";
    }
    
    /**
     * Resolve target name using appropriate APIs
     */
    private String resolveTargetName(MenuEntry menuEntry, String targetType)
    {
        try {
            switch (targetType) {
                case "NPC":
                    // Use NPC name from menu target, cleaned up
                    String npcName = cleanTargetName(menuEntry.getTarget());
                    return npcName != null ? npcName : "NPC_" + menuEntry.getIdentifier();
                    
                case "GAME_OBJECT":
                    // Use object name from menu target
                    String objectName = cleanTargetName(menuEntry.getTarget());
                    return objectName != null ? objectName : "Object_" + menuEntry.getIdentifier();
                    
                case "GROUND_ITEM":
                    // Use item name from menu target
                    String itemName = cleanTargetName(menuEntry.getTarget());
                    return itemName != null ? itemName : getItemName(menuEntry.getIdentifier());
                    
                case "PLAYER":
                    // Use player name from menu target
                    String playerName = cleanTargetName(menuEntry.getTarget());
                    return playerName != null ? playerName : "Player_" + menuEntry.getIdentifier();
                    
                case "INVENTORY_ITEM":
                    // Use item name from ItemManager
                    String invItemName = cleanTargetName(menuEntry.getTarget());
                    return invItemName != null ? invItemName : getItemName(menuEntry.getIdentifier());
                    
                case "INTERFACE":
                    // Interface widget name resolution
                    return getWidgetName(menuEntry);
                    
                case "WALK":
                    return "Walk";
                    
                default:
                    String fallbackName = cleanTargetName(menuEntry.getTarget());
                    return fallbackName != null ? fallbackName : "Unknown_" + menuEntry.getIdentifier();
            }
        } catch (Exception e) {
            log.debug("[TARGET-NAME] Error resolving target name: {}", e.getMessage());
            String fallbackName = cleanTargetName(menuEntry.getTarget());
            return fallbackName != null ? fallbackName : "Error_" + menuEntry.getIdentifier();
        }
    }
    
    /**
     * Clean target name by removing color codes and HTML tags
     */
    private String cleanTargetName(String target)
    {
        if (target == null || target.trim().isEmpty()) {
            return null;
        }
        
        // Remove color codes like <col=ffff00>
        String cleaned = target.replaceAll("<col=[0-9a-fA-F]{6}>", "");
        cleaned = cleaned.replaceAll("</col>", "");
        
        // Remove other HTML-like tags
        cleaned = cleaned.replaceAll("<[^>]+>", "");
        
        // Clean up whitespace
        cleaned = cleaned.trim();
        
        return cleaned.isEmpty() ? null : cleaned;
    }
    
    /**
     * Get item name from ItemManager
     */
    private String getItemName(int itemId)
    {
        if (itemManager != null) {
            try {
                ItemComposition itemComposition = itemManager.getItemComposition(itemId);
                if (itemComposition != null) {
                    return itemComposition.getName();
                }
            } catch (Exception e) {
                log.debug("Failed to get item name for ID {}: {}", itemId, e.getMessage());
            }
        }
        return "Item_" + itemId;
    }
    
    /**
     * Get widget information for interface interactions
     */
    private String getWidgetInfo(MenuEntry menuEntry)
    {
        try {
            int widgetId = menuEntry.getParam1();
            return "widget_" + widgetId;
        } catch (Exception e) {
            return "widget_unknown";
        }
    }
    
    /**
     * Get widget name for better UI interaction tracking
     */
    private String getWidgetName(MenuEntry menuEntry)
    {
        try {
            int widgetId = menuEntry.getParam1();
            int groupId = widgetId >> 16; // Extract group ID from packed widget ID
            int childId = widgetId & 0xFFFF; // Extract child ID from packed widget ID
            
            // Enhanced widget ID to name mapping
            String widgetName = getWidgetNameByGroupId(groupId);
            if (widgetName != null) {
                return widgetName;
            }
            
            // Fallback to basic widget ID mapping
            switch (widgetId) {
                case 149: return "inventory";
                case 213: return "bank";
                case 300: return "shop";
                case 335: return "trade";
                case 465: return "grandexchange";
                default:
                    String cleanedTarget = cleanTargetName(menuEntry.getTarget());
                    if (cleanedTarget != null && !cleanedTarget.isEmpty()) {
                        return cleanedTarget;
                    }
                    return "widget_" + groupId + "_" + childId;
            }
        } catch (Exception e) {
            String cleanedTarget = cleanTargetName(menuEntry.getTarget());
            return cleanedTarget != null ? cleanedTarget : "widget_error";
        }
    }
    
    /**
     * Get widget name by group ID using RuneLite's standard widget groups
     */
    private String getWidgetNameByGroupId(int groupId)
    {
        switch (groupId) {
            case 149: return "inventory";
            case 15: return "bank";
            case 300: return "shop";
            case 335: return "trade";
            case 465: return "grandexchange";
            case 164: return "chatbox";
            case 548: return "fixed_viewport";
            case 161: return "resizable_viewport";
            case 160: return "resizable_viewport_bottom_line";
            case 165: return "combat_tab";
            case 320: return "skills_tab";
            case 274: return "quest_tab";
            case 541: return "equipment_tab";
            case 218: return "prayer_tab";
            case 259: return "magic_tab";
            case 182: return "clan_chat";
            case 429: return "friends_list";
            case 432: return "ignore_list";
            case 589: return "logout";
            case 116: return "options";
            case 593: return "emotes";
            case 187: return "music";
            case 84: return "world_map";
            case 122: return "minimap";
            case 163: return "private_messages";
            case 162: return "channel_messages";
            case 229: return "quest_journal";
            case 261: return "achievement_diary";
            case 275: return "pest_control";
            case 76: return "barbarian_assault";
            case 407: return "clan_wars";
            case 413: return "duel_arena";
            case 443: return "castle_wars";
            case 58: return "thermonuclear_smoke_devil";
            case 25: return "tutorial_island";
            case 17: return "login_screen";
            case 378: return "welcome_screen";
            case 71: return "character_creation";
            case 156: return "level_up";
            case 193: return "quest_completed";
            case 233: return "skill_guide";
            case 214: return "price_checker";
            case 464: return "grand_exchange_collect";
            case 383: return "grand_exchange_inventory";
            case 108: return "slayer_reward";
            case 310: return "smithing";
            case 311: return "fletching";
            case 270: return "crafting";
            case 312: return "cooking";
            case 251: return "herblore";
            case 367: return "farming";
            case 206: return "construction";
            case 308: return "hunter";
            case 134: return "diary";
            case 400: return "barrows";
            case 129: return "fossil_island";
            case 621: return "theatre_of_blood";
            case 633: return "chambers_of_xeric";
            case 629: return "inferno";
            case 628: return "chambers_of_xeric_reward";
            default: return null;
        }
    }
    
    /**
     * Check if target is a player
     */
    private Boolean isPlayerTarget(MenuEntry menuEntry)
    {
        return menuEntry.getType() != null && 
               menuEntry.getType().name().contains("PLAYER");
    }
    
    /**
     * Check if target is an enemy (simple heuristic)
     */
    private Boolean isEnemyTarget(MenuEntry menuEntry)
    {
        if (menuEntry.getType() == null) return false;
        
        String action = menuEntry.getType().name();
        String option = menuEntry.getOption();
        
        // Attack actions indicate enemy
        return action.startsWith("NPC_") && 
               (option != null && option.toLowerCase().contains("attack"));
    }
}