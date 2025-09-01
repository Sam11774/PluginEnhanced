/*
 * Copyright (c) 2025, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tracks ground objects and items with ownership timers and visibility states
 * 
 * Features:
 * - Ownership duration tracking (1 minute default for most items)
 * - Visibility duration tracking (3 minutes total for most items)
 * - Player-specific visibility tracking
 * - Automatic cleanup of expired items
 * - Thread-safe concurrent operations
 * 
 * @author RuneLiteAI Team
 * @version 1.0.0
 */
@Slf4j
public class GroundObjectTracker 
{
    @Data
    public static class TrackedGroundItem
    {
        private final int itemId;
        private final int quantity;
        private final WorldPoint location;
        private final long spawnTime;
        private final String originalOwner;
        private final ItemVisibility visibility;
        private boolean expired = false;
        
        public TrackedGroundItem(int itemId, int quantity, WorldPoint location, String originalOwner)
        {
            this.itemId = itemId;
            this.quantity = quantity;
            this.location = location;
            this.spawnTime = System.currentTimeMillis();
            this.originalOwner = originalOwner;
            this.visibility = ItemVisibility.OWNER_ONLY;
        }
        
        /**
         * Check if item is visible to a specific player
         */
        public boolean isVisibleToPlayer(String playerName)
        {
            long currentTime = System.currentTimeMillis();
            long timeSinceSpawn = currentTime - spawnTime;
            
            // Original owner can always see it during ownership period
            if (playerName != null && playerName.equals(originalOwner) && 
                timeSinceSpawn < RuneliteAIConstants.GROUND_ITEM_OWNERSHIP_DURATION_MS) {
                return true;
            }
            
            // Everyone can see it after ownership period but before expiry
            return timeSinceSpawn >= RuneliteAIConstants.GROUND_ITEM_OWNERSHIP_DURATION_MS &&
                   timeSinceSpawn < RuneliteAIConstants.GROUND_ITEM_VISIBILITY_DURATION_MS;
        }
        
        /**
         * Get current visibility state
         */
        public ItemVisibility getCurrentVisibility()
        {
            long currentTime = System.currentTimeMillis();
            long timeSinceSpawn = currentTime - spawnTime;
            
            if (timeSinceSpawn < RuneliteAIConstants.GROUND_ITEM_OWNERSHIP_DURATION_MS) {
                return ItemVisibility.OWNER_ONLY;
            } else if (timeSinceSpawn < RuneliteAIConstants.GROUND_ITEM_VISIBILITY_DURATION_MS) {
                return ItemVisibility.PUBLIC;
            } else {
                return ItemVisibility.EXPIRED;
            }
        }
        
        /**
         * Check if item has expired and should be cleaned up
         */
        public boolean hasExpired()
        {
            long currentTime = System.currentTimeMillis();
            return (currentTime - spawnTime) > RuneliteAIConstants.GROUND_ITEM_VISIBILITY_DURATION_MS;
        }
        
        /**
         * Get remaining ownership time in milliseconds
         */
        public long getRemainingOwnershipTime()
        {
            long currentTime = System.currentTimeMillis();
            long ownershipEnd = spawnTime + RuneliteAIConstants.GROUND_ITEM_OWNERSHIP_DURATION_MS;
            return Math.max(0, ownershipEnd - currentTime);
        }
        
        /**
         * Get remaining visibility time in milliseconds
         */
        public long getRemainingVisibilityTime()
        {
            long currentTime = System.currentTimeMillis();
            long visibilityEnd = spawnTime + RuneliteAIConstants.GROUND_ITEM_VISIBILITY_DURATION_MS;
            return Math.max(0, visibilityEnd - currentTime);
        }
    }
    
    public enum ItemVisibility
    {
        OWNER_ONLY,  // Only original owner can see
        PUBLIC,      // Everyone can see
        EXPIRED      // No longer visible
    }
    
    // Thread-safe tracking maps
    private final ConcurrentMap<String, TrackedGroundItem> trackedItems = new ConcurrentHashMap<>();
    private long lastCleanupTime = System.currentTimeMillis();
    
    /**
     * Track a new ground item
     */
    public void trackGroundItem(TileItem item, WorldPoint location, String ownerName)
    {
        if (item == null || location == null) {
            return;
        }
        
        String key = generateItemKey(item.getId(), location, item.getQuantity());
        TrackedGroundItem trackedItem = new TrackedGroundItem(
            item.getId(), 
            item.getQuantity(), 
            location, 
            ownerName
        );
        
        trackedItems.put(key, trackedItem);
        log.debug("Tracking ground item: {} x{} at {} owned by {}", 
                 item.getId(), item.getQuantity(), location, ownerName);
        
        // Perform cleanup if needed
        performPeriodicCleanup();
    }
    
    /**
     * Remove tracking for a ground item
     */
    public void untrackGroundItem(TileItem item, WorldPoint location)
    {
        if (item == null || location == null) {
            return;
        }
        
        String key = generateItemKey(item.getId(), location, item.getQuantity());
        TrackedGroundItem removed = trackedItems.remove(key);
        
        if (removed != null) {
            log.debug("Stopped tracking ground item: {} x{} at {}", 
                     item.getId(), item.getQuantity(), location);
        }
    }
    
    /**
     * Get all tracked items visible to a specific player
     */
    public List<TrackedGroundItem> getVisibleItems(String playerName)
    {
        return trackedItems.values().stream()
                .filter(item -> !item.hasExpired())
                .filter(item -> item.isVisibleToPlayer(playerName))
                .collect(Collectors.toList());
    }
    
    /**
     * Get all tracked items by visibility state
     */
    public List<TrackedGroundItem> getItemsByVisibility(ItemVisibility visibility)
    {
        return trackedItems.values().stream()
                .filter(item -> item.getCurrentVisibility() == visibility)
                .collect(Collectors.toList());
    }
    
    /**
     * Get statistics about tracked items
     */
    public Map<String, Integer> getTrackingStatistics()
    {
        Map<String, Integer> stats = new ConcurrentHashMap<>();
        
        stats.put("total_tracked", trackedItems.size());
        stats.put("owner_only_items", getItemsByVisibility(ItemVisibility.OWNER_ONLY).size());
        stats.put("public_items", getItemsByVisibility(ItemVisibility.PUBLIC).size());
        stats.put("expired_items", getItemsByVisibility(ItemVisibility.EXPIRED).size());
        
        return stats;
    }
    
    /**
     * Clean up expired items
     */
    public void performCleanup()
    {
        long currentTime = System.currentTimeMillis();
        
        List<String> expiredKeys = trackedItems.entrySet().stream()
                .filter(entry -> entry.getValue().hasExpired())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        for (String key : expiredKeys) {
            trackedItems.remove(key);
        }
        
        if (!expiredKeys.isEmpty()) {
            log.debug("Cleaned up {} expired ground items", expiredKeys.size());
        }
        
        lastCleanupTime = currentTime;
    }
    
    /**
     * Perform periodic cleanup if enough time has passed
     */
    private void performPeriodicCleanup()
    {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastCleanupTime > RuneliteAIConstants.GROUND_ITEM_CLEANUP_INTERVAL_MS) {
            performCleanup();
        }
    }
    
    /**
     * Generate a unique key for tracking items
     */
    private String generateItemKey(int itemId, WorldPoint location, int quantity)
    {
        return String.format("%d_%d_%d_%d_%d", 
                itemId, location.getX(), location.getY(), location.getPlane(), quantity);
    }
    
    /**
     * Get the count of items tracked
     */
    public int getTrackedItemCount()
    {
        return trackedItems.size();
    }
    
    /**
     * Clear all tracked items (for testing or reset)
     */
    public void clearAll()
    {
        trackedItems.clear();
        log.debug("Cleared all tracked ground items");
    }
}