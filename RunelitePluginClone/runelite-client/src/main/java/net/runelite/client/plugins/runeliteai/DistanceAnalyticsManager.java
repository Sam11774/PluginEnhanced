/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ENHANCED Distance Analytics Manager for cross-table relationship analysis
 * 
 * Provides intelligent distance calculations between:
 * - Player and game objects (banks, altars, shops)
 * - Player and ground items (closest, valuable, owned)  
 * - Objects and interaction history (last clicked objects)
 * - Cross-table integration with click_context data
 * 
 * Features:
 * - Smart object categorization (banks, altars, shops, etc.)
 * - Ground item ownership tracking and despawn timers
 * - Click context integration for behavioral analysis
 * - Distance-based prioritization and recommendations
 * 
 * @author RuneLiteAI Team
 * @version 1.0.0
 */
@Slf4j
public class DistanceAnalyticsManager
{
    @Inject
    private Client client;
    
    @Inject 
    private ItemManager itemManager;
    
    // Caching for performance
    private final Map<Integer, String> objectCategoryCache = new ConcurrentHashMap<>();
    private final Map<String, WorldPoint> lastClickedObjects = new ConcurrentHashMap<>();
    private final Map<Integer, Long> groundItemDespawnTimes = new ConcurrentHashMap<>();
    private final Map<Integer, String> groundItemOwnership = new ConcurrentHashMap<>();
    
    // Analytics state
    private WorldPoint lastPlayerLocation = null;
    private long lastAnalyticsUpdate = 0;
    
    /**
     * Enhanced object distance analysis with smart categorization
     */
    public ObjectDistanceAnalytics analyzeObjectDistances(List<DataStructures.GameObjectData> objects, WorldPoint playerLocation)
    {
        if (objects == null || objects.isEmpty() || playerLocation == null) {
            return ObjectDistanceAnalytics.builder().build();
        }
        
        try {
            // Distance tracking for different object categories
            Integer closestBankDistance = Integer.MAX_VALUE;
            String closestBankName = null;
            Integer closestAltarDistance = Integer.MAX_VALUE;
            String closestAltarName = null;
            Integer closestShopDistance = Integer.MAX_VALUE;
            String closestShopName = null;
            
            // Last clicked object tracking
            Integer lastClickedObjectDistance = null;
            String lastClickedObjectName = null;
            Long timeSinceLastObjectClick = null;
            
            for (DataStructures.GameObjectData obj : objects) {
                if (obj.getWorldX() == null || obj.getWorldY() == null || obj.getPlane() == null) {
                    continue;
                }
                
                WorldPoint objLocation = new WorldPoint(obj.getWorldX(), obj.getWorldY(), obj.getPlane());
                int distance = playerLocation.distanceTo(objLocation);
                
                // Categorize object and track closest distances
                String category = categorizeObject(obj.getObjectId(), obj.getObjectName());
                
                switch (category) {
                    case "bank":
                        if (distance < closestBankDistance) {
                            closestBankDistance = distance;
                            closestBankName = obj.getObjectName();
                        }
                        break;
                    case "altar":
                        if (distance < closestAltarDistance) {
                            closestAltarDistance = distance;
                            closestAltarName = obj.getObjectName();
                        }
                        break;
                    case "shop":
                        if (distance < closestShopDistance) {
                            closestShopDistance = distance;
                            closestShopName = obj.getObjectName();
                        }
                        break;
                }
                
                // Check if this was the last clicked object
                String objectKey = obj.getObjectId() + "_" + obj.getObjectName();
                if (lastClickedObjects.containsKey(objectKey)) {
                    lastClickedObjectDistance = distance;
                    lastClickedObjectName = obj.getObjectName();
                    timeSinceLastObjectClick = System.currentTimeMillis() - lastClickedObjects.get(objectKey).hashCode();
                }
            }
            
            log.debug("[DISTANCE-ANALYTICS] Object analysis: Bank {}@{}, Altar {}@{}, Shop {}@{}", 
                closestBankName, closestBankDistance == Integer.MAX_VALUE ? "∞" : closestBankDistance,
                closestAltarName, closestAltarDistance == Integer.MAX_VALUE ? "∞" : closestAltarDistance,
                closestShopName, closestShopDistance == Integer.MAX_VALUE ? "∞" : closestShopDistance);
            
            return ObjectDistanceAnalytics.builder()
                .closestBankDistance(closestBankDistance == Integer.MAX_VALUE ? null : closestBankDistance)
                .closestBankName(closestBankName)
                .closestAltarDistance(closestAltarDistance == Integer.MAX_VALUE ? null : closestAltarDistance)
                .closestAltarName(closestAltarName)
                .closestShopDistance(closestShopDistance == Integer.MAX_VALUE ? null : closestShopDistance)
                .closestShopName(closestShopName)
                .lastClickedObjectDistance(lastClickedObjectDistance)
                .lastClickedObjectName(lastClickedObjectName)
                .timeSinceLastObjectClick(timeSinceLastObjectClick)
                .build();
                
        } catch (Exception e) {
            log.warn("[DISTANCE-ANALYTICS] Error analyzing object distances", e);
            return ObjectDistanceAnalytics.builder().build();
        }
    }
    
    /**
     * Enhanced ground item distance analysis with ownership tracking
     */
    public GroundItemDistanceAnalytics analyzeGroundItemDistances(List<DataStructures.GroundItemData> groundItems, WorldPoint playerLocation, String playerName)
    {
        if (groundItems == null || groundItems.isEmpty() || playerLocation == null) {
            return GroundItemDistanceAnalytics.builder().build();
        }
        
        try {
            Integer closestItemDistance = Integer.MAX_VALUE;
            String closestItemName = null;
            Integer closestValuableItemDistance = Integer.MAX_VALUE;
            String closestValuableItemName = null;
            
            int myDropsCount = 0;
            long myDropsTotalValue = 0;
            int otherPlayerDropsCount = 0;
            
            long shortestDespawnTimeMs = Long.MAX_VALUE;
            String nextDespawnItemName = null;
            
            for (DataStructures.GroundItemData item : groundItems) {
                if (item.getWorldX() == null || item.getWorldY() == null) {
                    continue;
                }
                
                WorldPoint itemLocation = new WorldPoint(item.getWorldX(), item.getWorldY(), playerLocation.getPlane());
                int distance = playerLocation.distanceTo(itemLocation);
                
                // Track closest item
                if (distance < closestItemDistance) {
                    closestItemDistance = distance;
                    closestItemName = item.getItemName();
                }
                
                // Track closest valuable item (>1000gp)
                if (item.getTotalValue() != null && item.getTotalValue() > 1000 && distance < closestValuableItemDistance) {
                    closestValuableItemDistance = distance;
                    closestValuableItemName = item.getItemName();
                }
                
                // Ownership analysis
                String owner = groundItemOwnership.get(item.getItemId());
                if (playerName != null && playerName.equals(owner)) {
                    myDropsCount++;
                    myDropsTotalValue += item.getTotalValue() != null ? item.getTotalValue() : 0;
                } else if (owner != null && !owner.equals(playerName)) {
                    otherPlayerDropsCount++;
                }
                
                // Despawn time estimation
                Long despawnTime = groundItemDespawnTimes.get(item.getItemId());
                if (despawnTime != null && despawnTime < shortestDespawnTimeMs) {
                    shortestDespawnTimeMs = despawnTime;
                    nextDespawnItemName = item.getItemName();
                }
            }
            
            log.debug("[DISTANCE-ANALYTICS] Ground items: Closest {}@{}, Valuable {}@{}, MyDrops: {}, Others: {}", 
                closestItemName, closestItemDistance == Integer.MAX_VALUE ? "∞" : closestItemDistance,
                closestValuableItemName, closestValuableItemDistance == Integer.MAX_VALUE ? "∞" : closestValuableItemDistance,
                myDropsCount, otherPlayerDropsCount);
            
            return GroundItemDistanceAnalytics.builder()
                .closestItemDistance(closestItemDistance == Integer.MAX_VALUE ? null : closestItemDistance)
                .closestItemName(closestItemName)
                .closestValuableItemDistance(closestValuableItemDistance == Integer.MAX_VALUE ? null : closestValuableItemDistance)
                .closestValuableItemName(closestValuableItemName)
                .myDropsCount(myDropsCount)
                .myDropsTotalValue(myDropsTotalValue)
                .otherPlayerDropsCount(otherPlayerDropsCount)
                .shortestDespawnTimeMs(shortestDespawnTimeMs == Long.MAX_VALUE ? null : shortestDespawnTimeMs)
                .nextDespawnItemName(nextDespawnItemName)
                .build();
                
        } catch (Exception e) {
            log.warn("[DISTANCE-ANALYTICS] Error analyzing ground item distances", e);
            return GroundItemDistanceAnalytics.builder().build();
        }
    }
    
    /**
     * Smart object categorization for distance analysis
     */
    private String categorizeObject(Integer objectId, String objectName)
    {
        if (objectId == null) return "unknown";
        
        // Use cache for performance
        String cached = objectCategoryCache.get(objectId);
        if (cached != null) return cached;
        
        String category = "unknown";
        
        if (objectName != null) {
            String nameLower = objectName.toLowerCase();
            
            if (nameLower.contains("bank") || nameLower.contains("deposit")) {
                category = "bank";
            } else if (nameLower.contains("altar")) {
                category = "altar";  
            } else if (nameLower.contains("shop") || nameLower.contains("store") || nameLower.contains("trader")) {
                category = "shop";
            } else if (nameLower.contains("furnace") || nameLower.contains("anvil")) {
                category = "smithing";
            } else if (nameLower.contains("tree") || nameLower.contains("log")) {
                category = "woodcutting";
            } else if (nameLower.contains("rock") || nameLower.contains("ore")) {
                category = "mining";
            }
        }
        
        // Cache the result
        objectCategoryCache.put(objectId, category);
        return category;
    }
    
    /**
     * Track clicked object for distance analytics
     */
    public void recordObjectClick(Integer objectId, String objectName, WorldPoint location)
    {
        if (objectId != null && objectName != null && location != null) {
            String objectKey = objectId + "_" + objectName;
            lastClickedObjects.put(objectKey, location);
            
            log.debug("[DISTANCE-ANALYTICS] Recorded object click: {} at ({}, {}, {})", 
                objectName, location.getX(), location.getY(), location.getPlane());
        }
    }
    
    /**
     * Track ground item ownership for despawn calculations
     */
    public void recordGroundItemDrop(Integer itemId, String playerName)
    {
        if (itemId != null && playerName != null) {
            groundItemOwnership.put(itemId, playerName);
            
            // Estimate despawn time (2 minutes for most items, 1 hour for valuable items)
            long despawnTime = System.currentTimeMillis() + (120 * 1000); // 2 minutes default
            groundItemDespawnTimes.put(itemId, despawnTime);
            
            log.debug("[DISTANCE-ANALYTICS] Recorded ground item drop: {} by {}", itemId, playerName);
        }
    }
    
    // Data transfer objects for analytics results
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ObjectDistanceAnalytics
    {
        private Integer closestBankDistance;
        private String closestBankName;
        private Integer closestAltarDistance;
        private String closestAltarName;
        private Integer closestShopDistance;
        private String closestShopName;
        private Integer lastClickedObjectDistance;
        private String lastClickedObjectName;
        private Long timeSinceLastObjectClick;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class GroundItemDistanceAnalytics
    {
        private Integer closestItemDistance;
        private String closestItemName;
        private Integer closestValuableItemDistance;
        private String closestValuableItemName;
        private Integer myDropsCount;
        private Long myDropsTotalValue;
        private Integer otherPlayerDropsCount;
        private Long shortestDespawnTimeMs;
        private String nextDespawnItemName;
    }
}