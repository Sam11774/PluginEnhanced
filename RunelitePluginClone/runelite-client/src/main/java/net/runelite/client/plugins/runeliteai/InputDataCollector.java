/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.runeliteai.TickDataCollection.TickDataCollectionBuilder;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.MenuOptionClicked;

import java.util.List;
import java.util.ArrayList;
import static net.runelite.client.plugins.runeliteai.DataStructures.*;

/**
 * Dedicated collector for input data and analytics
 * 
 * Responsible for:
 * - Mouse input tracking (position, clicks, movement analysis)
 * - Keyboard input tracking and key combinations
 * - Camera data (angle, zoom, movement)
 * - Menu interactions and click context
 * - Movement analytics (distance, speed calculations)
 * - Ultimate input analytics (detailed key/mouse tracking)
 * 
 * Migrated from DataCollectionManager lines 1944-2872
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
public class InputDataCollector
{
    // Core dependencies
    private final Client client;
    private final DistanceAnalyticsManager distanceAnalyticsManager;
    private final RuneliteAIPlugin plugin;
    
    // Click context tracking
    private volatile ClickContextData lastClickContext = null;
    
    // Mouse tracking
    private Integer lastMouseX = null;
    private Integer lastMouseY = null; 
    private long lastMouseMoveTime = System.currentTimeMillis();
    
    // Movement tracking for analytics
    private WorldPoint lastPlayerLocation = null;
    private long lastMovementTime = System.currentTimeMillis();
    
    public InputDataCollector(Client client, DistanceAnalyticsManager distanceAnalyticsManager, RuneliteAIPlugin plugin)
    {
        this.client = client;
        this.distanceAnalyticsManager = distanceAnalyticsManager;
        this.plugin = plugin;
        log.debug("InputDataCollector initialized with plugin reference");
    }
    
    /**
     * Collect all input-related data
     */
    public void collectInputData(TickDataCollection.TickDataCollectionBuilder builder)
    {
        MouseInputData mouseData = collectMouseData();
        builder.mouseInput(mouseData);
        
        KeyboardInputData keyboardData = collectKeyboardData();
        builder.keyboardInput(keyboardData);
        
        CameraData cameraData = collectCameraData();
        log.debug("[CAMERA-DEBUG] Setting cameraData on builder: {}", cameraData != null ? "NOT NULL" : "NULL");
        builder.cameraData(cameraData);
        
        MenuInteractionData menuData = collectMenuData();
        builder.menuData(menuData);
        
        // Add click context if we captured one since the last tick
        if (lastClickContext != null) {
            builder.clickContext(lastClickContext);
            log.debug("[CLICK-DEBUG] Added click context to tick: {} -> {}", 
                lastClickContext.getClickType(), lastClickContext.getTargetType());
            // Reset after using it
            lastClickContext = null;
        }
        
        // Ultimate Input Analytics - collect detailed keyboard and mouse analytics
        try {
            List<KeyPressData> keyPressDetails = collectKeyPressDetails();
            builder.keyPressDetails(keyPressDetails);
            
            List<MouseButtonData> mouseButtonDetails = collectMouseButtonDetails();
            builder.mouseButtonDetails(mouseButtonDetails);
            
            List<KeyCombinationData> keyCombinations = collectKeyCombinations();
            builder.keyCombinations(keyCombinations);
            
            log.debug("[ULTIMATE-INPUT-DEBUG] Collected {} key presses, {} mouse buttons, {} key combinations", 
                keyPressDetails != null ? keyPressDetails.size() : 0,
                mouseButtonDetails != null ? mouseButtonDetails.size() : 0,
                keyCombinations != null ? keyCombinations.size() : 0);
        } catch (Exception e) {
            log.warn("Failed to collect Ultimate Input Analytics data: {}", e.getMessage());
        }
    }
    
    /**
     * Calculate movement analytics
     */
    public void calculateMovementAnalytics(TickDataCollection.TickDataCollectionBuilder builder)
    {
        try {
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer == null || localPlayer.getWorldLocation() == null) {
                return;
            }
            
            WorldPoint currentLocation = localPlayer.getWorldLocation();
            int currentX = currentLocation.getX();
            int currentY = currentLocation.getY();
            int currentPlane = currentLocation.getPlane();
            long currentTime = System.currentTimeMillis();
            
            Double movementDistance = 0.0;
            Double movementSpeed = 0.0;
            
            if (lastPlayerLocation != null) {
                // Calculate distance moved since last tick (in game tiles)
                int deltaX = currentX - lastPlayerLocation.getX();
                int deltaY = currentY - lastPlayerLocation.getY();
                int deltaPlane = currentPlane - lastPlayerLocation.getPlane();
                
                // Use Euclidean distance for movement calculation
                movementDistance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaPlane * deltaPlane);
                
                // Calculate speed (tiles per millisecond, then convert to tiles per second)
                long deltaTime = currentTime - lastMovementTime;
                if (deltaTime > 0 && movementDistance > 0) {
                    movementSpeed = (movementDistance / deltaTime) * 1000.0; // tiles per second
                }
                
                // Update distance analytics manager if available
                if (distanceAnalyticsManager != null) {
                    // Analytics manager integration would go here
                }
                
                log.debug("[MOVEMENT-DEBUG] Movement - distance: {}, speed: {} tiles/sec", 
                    movementDistance, movementSpeed);
            }
            
            // Update tracking variables
            lastPlayerLocation = currentLocation;
            lastMovementTime = currentTime;
            
            // Set movement analytics on builder
            builder.movementDistance(movementDistance);
            builder.movementSpeed(movementSpeed);
            
        } catch (Exception e) {
            log.warn("Error calculating movement analytics: {}", e.getMessage());
            builder.movementDistance(0.0);
            builder.movementSpeed(0.0);
        }
    }
    
    /**
     * Collect mouse input data
     */
    private MouseInputData collectMouseData()
    {
        int currentMouseX = client.getMouseCanvasPosition().getX();
        int currentMouseY = client.getMouseCanvasPosition().getY();
        long currentTime = System.currentTimeMillis();
        
        // Calculate mouse idle time
        Integer mouseIdleTime = null;
        if (lastMouseX != null && lastMouseY != null) {
            // Check if mouse position changed
            if (!lastMouseX.equals(currentMouseX) || !lastMouseY.equals(currentMouseY)) {
                // Mouse moved, reset idle time
                lastMouseMoveTime = currentTime;
                mouseIdleTime = 0;
            } else {
                // Mouse didn't move, calculate idle time in milliseconds
                mouseIdleTime = (int) (currentTime - lastMouseMoveTime);
            }
        } else {
            // First time, initialize
            mouseIdleTime = 0;
            lastMouseMoveTime = currentTime;
        }
        
        // Update tracking variables
        lastMouseX = currentMouseX;
        lastMouseY = currentMouseY;
        
        return MouseInputData.builder()
            .mouseX(currentMouseX)
            .mouseY(currentMouseY)
            .mouseIdleTime(mouseIdleTime)
            .build();
    }
    
    /**
     * Collect keyboard input data
     */
    private KeyboardInputData collectKeyboardData()
    {
        try {
            // Get keyboard activity from plugin
            int keyPressCount = plugin.getAndResetKeyPressCount();
            int activeKeysCount = plugin.getCurrentlyHeldKeysCount();
            
            log.debug("[KEYBOARD-DEBUG] Collecting keyboard data - keyPressCount: {}, activeKeysCount: {}", 
                keyPressCount, activeKeysCount);
            
            return KeyboardInputData.builder()
                .keyPressCount(keyPressCount)
                .activeKeysCount(activeKeysCount)
                .build();
        } catch (Exception e) {
            log.warn("Error collecting keyboard input data: {}", e.getMessage());
            return KeyboardInputData.builder()
                .keyPressCount(0)
                .activeKeysCount(0)
                .build();
        }
    }
    
    /**
     * Collect camera data
     */
    private CameraData collectCameraData()
    {
        return CameraData.builder()
            .cameraX(client.getCameraX())
            .cameraY(client.getCameraY())
            .cameraZ(client.getCameraZ())
            .cameraYaw(client.getCameraYaw())
            .cameraPitch(client.getCameraPitch())
            .build();
    }
    
    /**
     * Collect menu interaction data
     */
    private MenuInteractionData collectMenuData()
    {
        MenuEntry[] menuEntries = client.getMenuEntries();
        if (menuEntries == null || menuEntries.length == 0) {
            return MenuInteractionData.builder()
                .menuEntryCount(0)
                .build();
        }
        
        return MenuInteractionData.builder()
            .menuEntryCount(menuEntries.length)
            .lastMenuAction(menuEntries[menuEntries.length - 1].getOption())
            .lastMenuTarget(menuEntries[menuEntries.length - 1].getTarget())
            .build();
    }
    
    /**
     * Collect key press details for ultimate input analytics
     */
    private List<KeyPressData> collectKeyPressDetails()
    {
        try {
            DataStructures.EnhancedInputData enhancedInput = plugin.getEnhancedInputData();
            if (enhancedInput != null) {
                if (enhancedInput.getKeyPresses() != null && !enhancedInput.getKeyPresses().isEmpty()) {
                    log.debug("[KEY-DEBUG] Collecting {} key press details for database", enhancedInput.getKeyPresses().size());
                    return new ArrayList<>(enhancedInput.getKeyPresses());
                } else {
                    log.debug("[KEY-DEBUG] No key presses to collect this tick");
                }
            } else {
                log.debug("[KEY-DEBUG] Enhanced input data is null");
            }
        } catch (Exception e) {
            log.warn("Failed to collect key press details: {}", e.getMessage());
        }
        return new ArrayList<>();
    }
    
    /**
     * Collect mouse button details for ultimate input analytics
     */
    private List<MouseButtonData> collectMouseButtonDetails()
    {
        try {
            DataStructures.EnhancedInputData enhancedInput = plugin.getEnhancedInputData();
            if (enhancedInput != null && enhancedInput.getMouseButtons() != null) {
                return new ArrayList<>(enhancedInput.getMouseButtons());
            }
        } catch (Exception e) {
            log.warn("Failed to collect mouse button details: {}", e.getMessage());
        }
        return new ArrayList<>();
    }
    
    /**
     * Collect key combinations for ultimate input analytics
     */
    private List<KeyCombinationData> collectKeyCombinations()
    {
        try {
            DataStructures.EnhancedInputData enhancedInput = plugin.getEnhancedInputData();
            if (enhancedInput != null && enhancedInput.getKeyCombinations() != null) {
                return new ArrayList<>(enhancedInput.getKeyCombinations());
            }
        } catch (Exception e) {
            log.warn("Failed to collect key combinations: {}", e.getMessage());
        }
        return new ArrayList<>();
    }
    
    /**
     * Update click context data
     */
    public void updateClickContext(ClickContextData clickContext)
    {
        this.lastClickContext = clickContext;
    }
    
    // =============== CRITICAL MISSING FUNCTIONALITY - FULLY RESTORED ===============
    
    /**
     * FULLY RESTORED: Record comprehensive click context from MenuOptionClicked events
     * This is the core of the Ultimate Input Analytics system
     */
    public void recordClickContext(MenuOptionClicked event)
    {
        try {
            MenuEntry menuEntry = event.getMenuEntry();
            if (menuEntry == null) return;
            
            log.debug("[CLICK-CONTEXT] Recording click: option={}, target={}, type={}, id={}", 
                menuEntry.getOption(), menuEntry.getTarget(), 
                menuEntry.getType() != null ? menuEntry.getType().name() : "null",
                menuEntry.getIdentifier());
            
            String clickType = menuEntry.getOption() != null ? menuEntry.getOption() : "Unknown";
            String targetType = classifyTargetType(menuEntry);
            String targetName = resolveTargetName(menuEntry, targetType);
            Integer targetId = menuEntry.getIdentifier();
            
            // Get coordinates
            Point mousePos = client.getMouseCanvasPosition();
            Integer screenX = mousePos != null ? mousePos.getX() : null;
            Integer screenY = mousePos != null ? mousePos.getY() : null;
            
            // Get world coordinates if possible
            Integer worldX = null;
            Integer worldY = null;
            Integer plane = null;
            
            try {
                if (targetType.equals("TILE") || targetType.equals("GROUND_ITEM")) {
                    WorldPoint worldPoint = client.isMenuOpen() ? 
                        WorldPoint.fromLocal(client, client.getLocalDestinationLocation() != null ? client.getLocalDestinationLocation() : LocalPoint.fromWorld(client, client.getLocalPlayer().getWorldLocation())) : 
                        client.getLocalPlayer().getWorldLocation();
                    if (worldPoint != null) {
                        worldX = worldPoint.getX();
                        worldY = worldPoint.getY();
                        plane = worldPoint.getPlane();
                    }
                }
            } catch (Exception e) {
                log.debug("[CLICK-CONTEXT] Error getting world coordinates: {}", e.getMessage());
            }
            
            // Build comprehensive click context
            ClickContextData clickContext = ClickContextData.builder()
                .clickType(clickType)
                .targetType(targetType)
                .targetName(targetName)
                .targetId(targetId)
                .screenX(screenX)
                .screenY(screenY)
                .worldX(worldX)
                .worldY(worldY)
                .plane(plane)
                .build();
            
            this.lastClickContext = clickContext;
            
            log.debug("[CLICK-CONTEXT] Recorded: {} -> {} ({})", clickType, targetName, targetType);
            
        } catch (Exception e) {
            log.warn("Error recording click context", e);
        }
    }
    
    /**
     * FULLY RESTORED: Advanced target type classification system
     */
    private String classifyTargetType(MenuEntry menuEntry)
    {
        if (menuEntry.getType() == null) return "UNKNOWN";
        
        String action = menuEntry.getType().name();
        
        // Map MenuAction types to our classification system
        switch (action) {
            case "GROUND_ITEM_FIRST_OPTION":
            case "GROUND_ITEM_SECOND_OPTION":
            case "GROUND_ITEM_THIRD_OPTION":
            case "GROUND_ITEM_FOURTH_OPTION":
            case "GROUND_ITEM_FIFTH_OPTION":
                return "GROUND_ITEM";
                
            case "GAME_OBJECT_FIRST_OPTION":
            case "GAME_OBJECT_SECOND_OPTION":
            case "GAME_OBJECT_THIRD_OPTION":
            case "GAME_OBJECT_FOURTH_OPTION":
            case "GAME_OBJECT_FIFTH_OPTION":
                return "GAME_OBJECT";
                
            case "NPC_FIRST_OPTION":
            case "NPC_SECOND_OPTION": 
            case "NPC_THIRD_OPTION":
            case "NPC_FOURTH_OPTION":
            case "NPC_FIFTH_OPTION":
                return "NPC";
                
            case "PLAYER_FIRST_OPTION":
            case "PLAYER_SECOND_OPTION":
            case "PLAYER_THIRD_OPTION":
            case "PLAYER_FOURTH_OPTION":
            case "PLAYER_FIFTH_OPTION":
                return "PLAYER";
                
            case "ITEM_FIRST_OPTION":
            case "ITEM_SECOND_OPTION":
            case "ITEM_THIRD_OPTION":
            case "ITEM_FOURTH_OPTION":  
            case "ITEM_FIFTH_OPTION":
            case "ITEM_USE":
            case "ITEM_USE_ON_COMPONENT":
            case "ITEM_USE_ON_GAME_OBJECT":
            case "ITEM_USE_ON_GROUND_ITEM":
            case "ITEM_USE_ON_NPC":
            case "ITEM_USE_ON_PLAYER":
                return "INVENTORY_ITEM";
                
            case "WIDGET":
            case "WIDGET_FIRST_OPTION":
            case "WIDGET_SECOND_OPTION":
            case "WIDGET_THIRD_OPTION":
            case "WIDGET_FOURTH_OPTION":
            case "WIDGET_FIFTH_OPTION":
                return "INTERFACE";
                
            case "WALK":
                return "TILE";
                
            default:
                log.debug("[CLICK-CONTEXT] Unknown action type: {}", action);
                return "UNKNOWN";
        }
    }
    
    /**
     * FULLY RESTORED: Enhanced target name resolution with multiple fallback strategies
     */
    private String resolveTargetName(MenuEntry menuEntry, String targetType)
    {
        try {
            switch (targetType) {
                case "GAME_OBJECT":
                    // Enhanced ObjectComposition lookup
                    try {
                        net.runelite.api.ObjectComposition objComp = client.getObjectDefinition(menuEntry.getIdentifier());
                        if (objComp != null && objComp.getName() != null && !objComp.getName().trim().isEmpty()) {
                            return objComp.getName().trim();
                        }
                    } catch (Exception e) {
                        log.debug("[TARGET-NAME] ObjectComposition failed for {}: {}", menuEntry.getIdentifier(), e.getMessage());
                    }
                    return cleanTargetName(menuEntry.getTarget()) != null ? 
                        cleanTargetName(menuEntry.getTarget()) : "Object_" + menuEntry.getIdentifier();
                    
                case "NPC":
                    // NPC name resolution
                    String npcName = cleanTargetName(menuEntry.getTarget());
                    return npcName != null ? npcName : "NPC_" + menuEntry.getIdentifier();
                    
                case "GROUND_ITEM":
                    // Ground item name resolution - would integrate with ItemManager
                    String itemName = cleanTargetName(menuEntry.getTarget());
                    return itemName != null ? itemName : "Item_" + menuEntry.getIdentifier();
                    
                case "PLAYER":
                    // Player name
                    String playerName = cleanTargetName(menuEntry.getTarget());
                    return playerName != null ? playerName : "Player_" + menuEntry.getIdentifier();
                    
                case "INVENTORY_ITEM":
                    // Inventory item name
                    String invItemName = cleanTargetName(menuEntry.getTarget());
                    return invItemName != null ? invItemName : "InventoryItem_" + menuEntry.getIdentifier();
                    
                case "INTERFACE":
                    // Interface widget name resolution
                    return getInterfaceWidgetName(menuEntry);
                    
                case "TILE":
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
     * Get interface widget name for better UI interaction tracking
     */
    private String getInterfaceWidgetName(MenuEntry menuEntry)
    {
        try {
            int widgetId = menuEntry.getParam1();
            // Basic widget ID to name mapping - could be expanded
            switch (widgetId) {
                case 149: return "inventory";
                case 213: return "bank";
                case 300: return "shop";
                case 335: return "trade";
                case 465: return "grandexchange";
                default:
                    String cleanedTarget = cleanTargetName(menuEntry.getTarget());
                    return cleanedTarget != null ? cleanedTarget : "widget_" + widgetId;
            }
        } catch (Exception e) {
            return cleanTargetName(menuEntry.getTarget());
        }
    }
    
}