/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.runeliteai.TickDataCollection.TickDataCollectionBuilder;

import java.util.*;
import static net.runelite.client.plugins.runeliteai.DataStructures.*;

/**
 * Dedicated collector for world environment data
 * 
 * Responsible for:
 * - World environment data and weather
 * - Nearby players detection and analysis
 * - Nearby NPCs tracking with combat analysis
 * - Game objects interaction tracking  
 * - Ground items detection and ownership
 * - Projectiles tracking and classification
 * 
 * Migrated from DataCollectionManager lines 1200-1943
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
public class WorldDataCollector
{
    // Core dependencies
    private final Client client;
    private final GroundObjectTracker groundObjectTracker;
    private final DistanceAnalyticsManager distanceAnalyticsManager;
    
    // Optimization - Reusable collections
    private final List<PlayerData> reusablePlayerList = new ArrayList<>();
    private final List<NPCData> reusableNPCList = new ArrayList<>();
    
    // Object change detection tracking - FULLY RESTORED
    private int previousObjectCount = 0;
    private Map<String, Integer> previousObjectTypes = new HashMap<>();
    
    // Projectile event tracking
    private final Queue<net.runelite.api.events.ProjectileMoved> recentProjectiles = new java.util.concurrent.ConcurrentLinkedQueue<>();
    
    public WorldDataCollector(Client client, GroundObjectTracker groundObjectTracker, DistanceAnalyticsManager distanceAnalyticsManager)
    {
        this.client = client;
        this.groundObjectTracker = groundObjectTracker;
        this.distanceAnalyticsManager = distanceAnalyticsManager;
        log.debug("WorldDataCollector initialized with distance analytics");
    }
    
    /**
     * Collect all world environment data
     */
    public void collectWorldData(TickDataCollection.TickDataCollectionBuilder builder)
    {
        // World environment
        WorldEnvironmentData worldData = collectWorldEnvironment();
        builder.worldData(worldData);
        
        // Nearby players
        NearbyPlayersData nearbyPlayers = collectNearbyPlayers();
        builder.nearbyPlayers(nearbyPlayers);
        
        // Nearby NPCs
        NearbyNPCsData nearbyNPCs = collectNearbyNPCs();
        builder.nearbyNPCs(nearbyNPCs);
        
        // FULLY RESTORED: Comprehensive 15-tile radius game objects scanning
        GameObjectsData gameObjects = collectRealGameObjectsComprehensive();
        builder.gameObjects(gameObjects);
        
        GroundItemsData groundItems = collectRealGroundItems();
        builder.groundItems(groundItems);
        
        ProjectilesData projectiles = collectRealProjectiles();
        builder.projectiles(projectiles);
    }
    
    /**
     * Collect world environment information
     */
    private WorldEnvironmentData collectWorldEnvironment()
    {
        Player localPlayer = client.getLocalPlayer();
        WorldPoint playerLocation = localPlayer != null ? localPlayer.getWorldLocation() : null;
        String environmentType = "overworld"; // Default
        String weatherCondition = "clear"; // Default
        Integer lightLevel = 255; // Default full brightness
        Integer regionId = null;
        Integer chunkX = null;
        Integer chunkY = null;
        
        if (playerLocation != null) {
            // Determine environment type based on coordinates and plane
            if (isInWilderness(playerLocation)) {
                environmentType = "wilderness";
            } else if (playerLocation.getPlane() > 0) {
                environmentType = "upper_level";
            } else if (playerLocation.getPlane() < 0) {
                environmentType = "underground";
            } else {
                environmentType = getDetailedEnvironmentType(playerLocation);
            }
            
            // Calculate region and chunk data
            regionId = playerLocation.getRegionID();
            chunkX = playerLocation.getX() >> 6;
            chunkY = playerLocation.getY() >> 6;
            
            weatherCondition = detectWeatherConditions(playerLocation, environmentType);
            lightLevel = estimateLightLevel(playerLocation, environmentType);
        }
        
        // Count game objects and ground items
        Integer gameObjectCount = estimateNearbyGameObjects();
        Integer groundItemCount = estimateNearbyGroundItems();
        
        return WorldEnvironmentData.builder()
            .plane(client.getPlane())
            .baseX(client.getBaseX())
            .baseY(client.getBaseY())
            .mapRegions(convertIntArrayToIntegerArray(client.getMapRegions()))
            .currentRegion(getCurrentRegionEnhanced(playerLocation, regionId))
            .nearbyPlayerCount(client.getPlayers() != null ? client.getPlayers().size() : 0)
            .nearbyNPCCount(client.getNpcs() != null ? client.getNpcs().size() : 0)
            .gameObjectCount(gameObjectCount)
            .groundItemCount(groundItemCount)
            .worldTick((long) client.getTickCount())
            .environmentType(environmentType)
            .weatherCondition(weatherCondition)
            .lightLevel(lightLevel)
            .regionId(regionId)
            .chunkX(chunkX)
            .chunkY(chunkY)
            .lightingCondition(convertLightLevelToCondition(lightLevel))
            .build();
    }
    
    /**
     * Collect nearby players data
     */
    private NearbyPlayersData collectNearbyPlayers()
    {
        List<Player> players = client.getPlayers();
        if (players == null || players.isEmpty()) {
            return NearbyPlayersData.builder().playerCount(0).build();
        }
        
        // Use reusable collection for performance
        reusablePlayerList.clear();
        
        for (Player player : players) {
            if (player != null) {
                PlayerData playerData = collectBasicPlayerData(player);
                if (playerData != null) {
                    reusablePlayerList.add(playerData);
                }
            }
        }
        
        int friendCount = 0;
        int clanCount = 0;
        int totalCombatLevel = 0;
        int combatLevelCount = 0;
        
        for (PlayerData p : reusablePlayerList) {
            if (p.getIsFriend() != null && p.getIsFriend()) friendCount++;
            if (p.getIsClanMember() != null && p.getIsClanMember()) clanCount++;
            if (p.getCombatLevel() != null) {
                totalCombatLevel += p.getCombatLevel();
                combatLevelCount++;
            }
        }
        
        double avgCombatLevel = combatLevelCount > 0 ? (double) totalCombatLevel / combatLevelCount : 0.0;
        
        return NearbyPlayersData.builder()
            .players(new ArrayList<>(reusablePlayerList))
            .playerCount(reusablePlayerList.size())
            .friendCount(friendCount)
            .clanCount(clanCount)
            .averageCombatLevel((int) avgCombatLevel)
            .build();
    }
    
    /**
     * Collect nearby NPCs data
     */
    private NearbyNPCsData collectNearbyNPCs()
    {
        List<NPC> npcs = client.getNpcs();
        if (npcs == null || npcs.isEmpty()) {
            return NearbyNPCsData.builder().npcCount(0).build();
        }
        
        // Use reusable collection for performance
        reusableNPCList.clear();
        
        int hostileCount = 0;
        int interactableCount = 0;
        int totalCombatLevel = 0;
        int combatLevelCount = 0;
        
        for (NPC npc : npcs) {
            if (npc != null) {
                NPCData npcData = collectBasicNPCData(npc);
                if (npcData != null) {
                    reusableNPCList.add(npcData);
                    
                    // Analyze NPC characteristics
                    NPCComposition composition = npc.getComposition();
                    if (composition != null) {
                        if (composition.isInteractible()) {
                            interactableCount++;
                        }
                        
                        // Check if hostile
                        if (npc.getCombatLevel() > 0) {
                            hostileCount++;
                            totalCombatLevel += npc.getCombatLevel();
                            combatLevelCount++;
                        }
                    }
                }
            }
        }
        
        double avgCombatLevel = combatLevelCount > 0 ? (double) totalCombatLevel / combatLevelCount : 0.0;
        
        return NearbyNPCsData.builder()
            .npcs(new ArrayList<>(reusableNPCList))
            .npcCount(reusableNPCList.size())
            .aggressiveNPCCount(hostileCount)
            .combatNPCCount(combatLevelCount)
            .averageNPCCombatLevel((int) avgCombatLevel)
            .build();
    }
    
    /**
     * FULLY RESTORED: Comprehensive 15-tile radius game objects collection with distance analytics
     * Migrated from original DataCollectionManager lines 1424-1633
     */
    private GameObjectsData collectRealGameObjectsComprehensive()
    {
        try {
            Scene scene = client.getScene();
            if (scene == null) {
                log.debug("[OBJECT-DEBUG] Scene is null, no game objects to collect");
                return GameObjectsData.builder().objectCount(0).build();
            }
            
            List<GameObjectData> gameObjects = new ArrayList<>();
            Map<String, Integer> objectTypeCounts = new HashMap<>();
            int totalObjects = 0;
            int interactableObjectsCount = 0;
            int closestObjectDistance = Integer.MAX_VALUE;
            Integer closestObjectId = null;
            String closestObjectName = null;
            int tilesScanned = 0;
            int nullObjectNames = 0;
            
            // Get player location for proximity filtering
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer == null || localPlayer.getWorldLocation() == null) {
                log.debug("[OBJECT-DEBUG] No local player or location, cannot scan game objects");
                return GameObjectsData.builder().objectCount(0).build();
            }
            
            WorldPoint playerLocation = localPlayer.getWorldLocation();
            int scanRadius = 15; // Full 15-tile radius scanning
            
            // Use Scene's tile dimensions for safe iteration
            Tile[][][] tiles = scene.getTiles();
            int plane = playerLocation.getPlane();
            
            if (plane >= 0 && plane < tiles.length) {
                Tile[][] planeTiles = tiles[plane];
                int sceneBaseX = scene.getBaseX();
                int sceneBaseY = scene.getBaseY();
                
                // Calculate tile coordinates relative to scene
                int playerSceneX = playerLocation.getX() - sceneBaseX;
                int playerSceneY = playerLocation.getY() - sceneBaseY;
                
                log.debug("[OBJECT-DEBUG] Scanning plane {} from player scene coords ({}, {}) with radius {} - scene dimensions: {}x{}", 
                    plane, playerSceneX, playerSceneY, scanRadius, 
                    planeTiles.length, planeTiles.length > 0 ? planeTiles[0].length : 0);
                
                // Scan tiles around player within bounds
                for (int x = Math.max(0, playerSceneX - scanRadius); 
                     x <= Math.min(planeTiles.length - 1, playerSceneX + scanRadius); 
                     x++) {
                    if (x < planeTiles.length && planeTiles[x] != null) {
                        for (int y = Math.max(0, playerSceneY - scanRadius); 
                             y <= Math.min(planeTiles[x].length - 1, playerSceneY + scanRadius); 
                             y++) {
                            
                            tilesScanned++;
                            Tile tile = planeTiles[x][y];
                            
                            if (tile != null && tile.getGameObjects() != null && tile.getGameObjects().length > 0) {
                                // Collect game objects on this tile
                                for (net.runelite.api.GameObject obj : tile.getGameObjects()) {
                                    if (obj != null && obj.getId() > 0) {
                                        // ENHANCED: Get object name with better error handling
                                        String objectName = getObjectNameEnhanced(obj.getId());
                                        if (objectName == null || objectName.equals("null")) {
                                            nullObjectNames++;
                                            objectName = "Unknown_" + obj.getId();
                                        }
                                        
                                        // Calculate distance to player
                                        WorldPoint objLocation = obj.getWorldLocation();
                                        int distance = objLocation != null ? playerLocation.distanceTo(objLocation) : Integer.MAX_VALUE;
                                        
                                        // ENHANCED: Better interactable detection
                                        boolean isInteractable = false;
                                        try {
                                            net.runelite.api.ObjectComposition objComp = client.getObjectDefinition(obj.getId());
                                            if (objComp != null && objComp.getActions() != null) {
                                                for (String action : objComp.getActions()) {
                                                    if (action != null && !action.equals("")) {
                                                        isInteractable = true;
                                                        break;
                                                    }
                                                }
                                            }
                                            if (!isInteractable && obj.getClickbox() != null) {
                                                isInteractable = true;
                                            }
                                        } catch (Exception e) {
                                            isInteractable = obj.getClickbox() != null;
                                        }
                                        
                                        if (isInteractable) {
                                            interactableObjectsCount++;
                                        }
                                        
                                        // Track closest object
                                        if (distance < closestObjectDistance) {
                                            closestObjectDistance = distance;
                                            closestObjectId = obj.getId();
                                            closestObjectName = objectName;
                                        }
                                        
                                        // Convert RuneLite API GameObject to our GameObjectData
                                        GameObjectData gameObjectData = GameObjectData.builder()
                                            .objectId(obj.getId())
                                            .objectName(objectName)
                                            .worldX(obj.getWorldLocation() != null ? obj.getWorldLocation().getX() : null)
                                            .worldY(obj.getWorldLocation() != null ? obj.getWorldLocation().getY() : null)
                                            .plane(obj.getWorldLocation() != null ? obj.getWorldLocation().getPlane() : null)
                                            .objectType("GameObject")
                                            .interactable(isInteractable)
                                            .orientation(obj.getOrientation())
                                            .build();
                                        
                                        gameObjects.add(gameObjectData);
                                        totalObjects++;
                                        objectTypeCounts.merge(objectName, 1, Integer::sum);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Change detection
            if (totalObjects != previousObjectCount) {
                log.debug("[OBJECT-DEBUG] Object count changed: {} -> {}", previousObjectCount, totalObjects);
            }
            previousObjectCount = totalObjects;
            previousObjectTypes = new HashMap<>(objectTypeCounts);
            
            return GameObjectsData.builder()
                .objectCount(totalObjects)
                .objects(gameObjects)
                .objectTypeCounts(objectTypeCounts)
                .scanRadius(15)
                .mostCommonObjectType(getMostCommonObjectType(objectTypeCounts))
                .uniqueObjectTypes(objectTypeCounts.size())
                .interactableObjectsCount(interactableObjectsCount)
                .closestObjectDistance(closestObjectDistance != Integer.MAX_VALUE ? closestObjectDistance : null)
                .closestObjectId(closestObjectId)
                .closestObjectName(closestObjectName)
                .build();
                
        } catch (Exception e) {
            log.warn("Error collecting comprehensive game objects data", e);
            return GameObjectsData.builder().objectCount(0).build();
        }
    }
    
    /**
     * Collect ground items data
     */
    private GroundItemsData collectRealGroundItems()
    {
        log.debug("[GROUND-ITEMS] Starting ground items collection");
        
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            log.debug("[GROUND-ITEMS] Local player is null, returning empty data");
            return GroundItemsData.builder().build();
        }

        WorldPoint playerLocation = localPlayer.getWorldLocation();
        if (playerLocation == null) {
            log.debug("[GROUND-ITEMS] Player location is null, returning empty data");
            return GroundItemsData.builder().build();
        }

        List<GroundItemData> groundItems = new ArrayList<>();
        Set<Integer> uniqueItemTypes = new HashSet<>();
        int totalItems = 0;
        int totalQuantity = 0;
        long totalValue = 0L;
        int scanRadius = 15;
        
        // Scan tiles within radius for ground items
        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dy = -scanRadius; dy <= scanRadius; dy++) {
                WorldPoint tileLocation = new WorldPoint(
                    playerLocation.getX() + dx, 
                    playerLocation.getY() + dy, 
                    playerLocation.getPlane()
                );
                
                Tile tile = client.getScene().getTiles()[playerLocation.getPlane()][tileLocation.getX() - client.getBaseX()][tileLocation.getY() - client.getBaseY()];
                if (tile == null) {
                    continue;
                }
                
                List<TileItem> tileItems = tile.getGroundItems();
                if (tileItems != null && !tileItems.isEmpty()) {
                    for (TileItem tileItem : tileItems) {
                        if (tileItem == null) continue;
                        
                        int itemId = tileItem.getId();
                        int quantity = tileItem.getQuantity();
                        
                        // Get item name and value (simplified - would need ItemManager injection for full implementation)
                        String itemName = "Item_" + itemId; // Fallback name format
                        long itemValue = (long) quantity * 1; // Simplified pricing, would need ItemManager for real prices
                        
                        // Calculate distance from player
                        double distance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
                        
                        // Create ground item data
                        GroundItemData groundItemData = GroundItemData.builder()
                            .itemId(itemId)
                            .itemName(itemName)
                            .quantity(quantity)
                            .itemValue((int) itemValue) // itemValue field is Integer, not Long
                            .worldX(tileLocation.getX())
                            .worldY(tileLocation.getY())
                            .plane(tileLocation.getPlane())
                            .build();
                        
                        groundItems.add(groundItemData);
                        uniqueItemTypes.add(itemId);
                        totalItems++;
                        totalQuantity += quantity;
                        totalValue += itemValue;
                        
                        log.debug("[GROUND-ITEMS] Found item: {} x{} at distance {} (value: {})", 
                            itemName, quantity, distance, itemValue);
                    }
                }
            }
        }
        
        // Use distance analytics manager for advanced calculations
        DistanceAnalyticsManager.GroundItemDistanceAnalytics analytics = null;
        if (distanceAnalyticsManager != null) {
            analytics = distanceAnalyticsManager.analyzeGroundItemDistances(groundItems, playerLocation, localPlayer.getName());
        }
        
        // Build final ground items data
        GroundItemsData.GroundItemsDataBuilder builder = GroundItemsData.builder()
            .totalItems(totalItems)
            .totalQuantity(totalQuantity)
            .totalValue(totalValue)
            .uniqueItemTypes(uniqueItemTypes.size())
            .scanRadius(scanRadius)
            .groundItems(groundItems);
            
        // Add analytics data if available
        if (analytics != null) {
            builder.closestItemDistance(analytics.getClosestItemDistance())
                   .closestItemName(analytics.getClosestItemName())
                   .closestValuableItemDistance(analytics.getClosestValuableItemDistance())
                   .closestValuableItemName(analytics.getClosestValuableItemName())
                   .myDropsCount(analytics.getMyDropsCount())
                   .myDropsTotalValue(analytics.getMyDropsTotalValue())
                   .otherPlayerDropsCount(analytics.getOtherPlayerDropsCount())
                   .shortestDespawnTimeMs(analytics.getShortestDespawnTimeMs())
                   .nextDespawnItemName(analytics.getNextDespawnItemName());
        }
        
        log.debug("[GROUND-ITEMS] Collection complete: {} items, {} types, value: {}", 
            totalItems, uniqueItemTypes.size(), totalValue);
        
        return builder.build();
    }
    
    /**
     * Collect projectiles data
     */
    private ProjectilesData collectRealProjectiles()
    {
        // Collect from recent projectile events (more comprehensive than client.getProjectiles())
        List<ProjectileData> allProjectileData = new ArrayList<>();
        Map<Integer, Integer> projectileTypeCounts = new HashMap<>();
        Set<Integer> uniqueProjectileTypes = new HashSet<>();
        
        // Process recent projectiles from event queue
        int recentProcessed = 0;
        for (net.runelite.api.events.ProjectileMoved projectileEvent : recentProjectiles) {
            if (projectileEvent != null && projectileEvent.getProjectile() != null) {
                Projectile proj = projectileEvent.getProjectile();
                if (proj.getId() > 0) {
                    recentProcessed++;
                    projectileTypeCounts.merge(proj.getId(), 1, Integer::sum);
                    uniqueProjectileTypes.add(proj.getId());
                    
                    // Create detailed projectile data
                    ProjectileData projectileData = ProjectileData.builder()
                        .projectileId(proj.getId())
                        .projectileType("RANGED") // Default type
                        .startX((int)proj.getX())
                        .startY((int)proj.getY()) 
                        .endX(proj.getTarget().getX())
                        .endY(proj.getTarget().getY())
                        .remainingCycles(proj.getRemainingCycles())
                        .slope(proj.getSlope())
                        .build();
                    
                    allProjectileData.add(projectileData);
                }
            }
        }
        
        // Also check current client projectiles
        net.runelite.api.Deque<Projectile> currentProjectiles = client.getProjectiles();
        int currentProjectileCount = 0;
        if (currentProjectiles != null) {
            for (Projectile p : currentProjectiles) {
                if (p != null) {
                    currentProjectileCount++;
                    uniqueProjectileTypes.add(p.getId());
                }
            }
        }
        
        // Determine most common projectile type
        Integer mostCommonProjectileId = projectileTypeCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        
        log.debug("[PROJECTILE-DEBUG] Collected {} recent projectiles, {} current projectiles, {} unique types", 
            recentProcessed, currentProjectileCount, uniqueProjectileTypes.size());
        
        return ProjectilesData.builder()
            .projectiles(allProjectileData)
            .activeProjectileCount(recentProcessed + currentProjectileCount)
            .activeProjectiles(recentProcessed + currentProjectileCount)
            .uniqueProjectileTypes(uniqueProjectileTypes.size())
            .mostCommonProjectileId(mostCommonProjectileId)
            .build();
    }
    
    // Helper methods
    
    private PlayerData collectBasicPlayerData(Player player)
    {
        if (player == null) return null;
        
        WorldPoint location = player.getWorldLocation();
        return PlayerData.builder()
            .playerName(player.getName())
            .combatLevel(player.getCombatLevel())
            .worldX(location != null ? location.getX() : null)
            .worldY(location != null ? location.getY() : null)
            .plane(location != null ? location.getPlane() : null)
            .animation(player.getAnimation())
            .healthRatio(player.getHealthRatio())
            .healthScale(player.getHealthScale())
            .isFriend(player.isFriend())
            .isClanMember(player.isClanMember())
            .isFriendsChatMember(player.isFriendsChatMember())
            .build();
    }
    
    private NPCData collectBasicNPCData(NPC npc)
    {
        if (npc == null) return null;
        
        WorldPoint location = npc.getWorldLocation();
        return NPCData.builder()
            .npcId(npc.getId())
            .npcName(npc.getName())
            .combatLevel(npc.getCombatLevel())
            .worldX(location != null ? location.getX() : null)
            .worldY(location != null ? location.getY() : null)
            .plane(location != null ? location.getPlane() : null)
            .animation(npc.getAnimation())
            .healthRatio(npc.getHealthRatio())
            .combatLevel(npc.getCombatLevel())
            .build();
    }
    
    private boolean isInWilderness(WorldPoint location)
    {
        return location.getY() > 3520 && location.getY() < 4000;
    }
    
    private String getDetailedEnvironmentType(WorldPoint location)
    {
        // Simplified environment detection - could be enhanced with region mapping
        if (location.getX() < 3200 && location.getY() < 3200) return "lumbridge_area";
        if (location.getX() > 3200 && location.getX() < 3300 && location.getY() > 3200 && location.getY() < 3300) return "varrock_area";
        if (location.getX() > 3000 && location.getX() < 3100 && location.getY() > 3300 && location.getY() < 3400) return "falador_area";
        return "overworld";
    }
    
    private String detectWeatherConditions(WorldPoint location, String environmentType)
    {
        // Simplified weather detection
        if ("underground".equals(environmentType)) return "indoor";
        if ("wilderness".equals(environmentType)) return "harsh";
        return "clear";
    }
    
    private Integer estimateLightLevel(WorldPoint location, String environmentType)
    {
        // Simplified light estimation
        if ("underground".equals(environmentType)) return 128;
        if ("upper_level".equals(environmentType)) return 200;
        return 255;
    }
    
    private Integer[] convertIntArrayToIntegerArray(int[] intArray)
    {
        if (intArray == null) return new Integer[0];
        
        Integer[] result = new Integer[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            result[i] = intArray[i];
        }
        return result;
    }
    
    private String getCurrentRegionEnhanced(WorldPoint playerLocation, Integer regionId)
    {
        if (regionId != null) {
            return "Region_" + regionId;
        }
        if (playerLocation != null) {
            return "Region_" + playerLocation.getRegionID();
        }
        return "Unknown_Region";
    }
    
    private String convertLightLevelToCondition(Integer lightLevel)
    {
        if (lightLevel == null) return "unknown";
        if (lightLevel >= 200) return "bright";
        if (lightLevel >= 128) return "normal";
        if (lightLevel >= 64) return "dim";
        return "dark";
    }
    
    private Integer estimateNearbyGameObjects()
    {
        try {
            Scene scene = client.getScene();
            Player localPlayer = client.getLocalPlayer();
            
            if (scene == null || localPlayer == null) {
                return 0;
            }

            WorldPoint playerLocation = localPlayer.getWorldLocation();
            if (playerLocation == null) {
                return 0;
            }

            int gameObjectCount = 0;
            int scanRadius = 10; // Smaller radius for quick estimation
            
            // Scan tiles within radius for game objects
            for (int dx = -scanRadius; dx <= scanRadius; dx++) {
                for (int dy = -scanRadius; dy <= scanRadius; dy++) {
                    int tileX = playerLocation.getX() + dx;
                    int tileY = playerLocation.getY() + dy;
                    
                    // Bounds check
                    int sceneX = tileX - client.getBaseX();
                    int sceneY = tileY - client.getBaseY();
                    
                    if (sceneX < 0 || sceneX >= 104 || sceneY < 0 || sceneY >= 104) {
                        continue;
                    }
                    
                    try {
                        Tile tile = scene.getTiles()[playerLocation.getPlane()][sceneX][sceneY];
                        if (tile != null && tile.getGameObjects() != null) {
                            // Count non-null game objects on this tile
                            for (net.runelite.api.GameObject obj : tile.getGameObjects()) {
                                if (obj != null && obj.getId() > 0) {
                                    gameObjectCount++;
                                }
                            }
                        }
                    } catch (Exception tileException) {
                        // Skip problematic tiles
                        continue;
                    }
                }
            }
            
            log.debug("[OBJECT-COUNT] Estimated {} game objects within {} tile radius", 
                gameObjectCount, scanRadius);
            return gameObjectCount;
            
        } catch (Exception e) {
            log.debug("Error estimating game objects: {}", e.getMessage());
            return 0;
        }
    }
    
    private Integer estimateNearbyGroundItems()
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return 0;
        }

        WorldPoint playerLocation = localPlayer.getWorldLocation();
        if (playerLocation == null) {
            return 0;
        }

        int totalItems = 0;
        int scanRadius = 10; // Smaller radius for quick estimation
        
        try {
            // Scan tiles within radius for ground items
            for (int dx = -scanRadius; dx <= scanRadius; dx++) {
                for (int dy = -scanRadius; dy <= scanRadius; dy++) {
                    int tileX = playerLocation.getX() + dx;
                    int tileY = playerLocation.getY() + dy;
                    
                    // Bounds check
                    int sceneX = tileX - client.getBaseX();
                    int sceneY = tileY - client.getBaseY();
                    
                    if (sceneX < 0 || sceneX >= 104 || sceneY < 0 || sceneY >= 104) {
                        continue;
                    }
                    
                    Tile tile = client.getScene().getTiles()[playerLocation.getPlane()][sceneX][sceneY];
                    if (tile != null) {
                        List<TileItem> tileItems = tile.getGroundItems();
                        if (tileItems != null) {
                            totalItems += tileItems.size();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[GROUND-ITEMS] Error estimating nearby ground items: {}", e.getMessage());
            return 0;
        }
        
        return totalItems;
    }
    
    // =============== MISSING UTILITY METHODS - FULLY RESTORED ===============
    
    /**
     * Enhanced object name resolution with ObjectComposition lookup
     */
    private String getObjectNameEnhanced(int objectId)
    {
        if (objectId <= 0) {
            return null;
        }
        
        if (client == null) {
            return null;
        }
        
        try {
            net.runelite.api.ObjectComposition objComp = client.getObjectDefinition(objectId);
            if (objComp != null) {
                String name = objComp.getName();
                if (name != null && !name.trim().isEmpty() && !name.equals("null")) {
                    return name.trim();
                }
            }
        } catch (Exception e) {
            log.debug("Exception getting object definition for ID {}: {}", objectId, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get most common object type from counts
     */
    private String getMostCommonObjectType(Map<String, Integer> objectTypeCounts)
    {
        if (objectTypeCounts == null || objectTypeCounts.isEmpty()) {
            return null;
        }
        
        return objectTypeCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Record projectile event for later processing
     */
    public void recordProjectileEvent(net.runelite.api.events.ProjectileMoved projectileEvent)
    {
        if (projectileEvent != null && projectileEvent.getProjectile() != null) {
            recentProjectiles.offer(projectileEvent);
            
            // Keep only last 150 projectiles
            while (recentProjectiles.size() > 150) {
                recentProjectiles.poll();
            }
            
            log.debug("[PROJECTILE-DEBUG] Recorded projectile event - ID: {}, queue size: {}", 
                projectileEvent.getProjectile().getId(), recentProjectiles.size());
        }
    }
}