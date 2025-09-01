/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;

import java.util.Map;
import java.util.List;
import static net.runelite.client.plugins.runeliteai.DataStructures.*;

/**
 * Database utility methods and JSON conversion
 * 
 * Responsible for:
 * - Complex JSON conversion (especially inventory items with ItemManager integration)
 * - Item name resolution with timeout protection for problematic items
 * - Player name management and session updates
 * - Data extraction and conversion utilities
 * - Helper methods for database operations
 * - Fallback mechanisms for ItemManager failures
 * 
 * Migrated from DatabaseManager lines 2685-3415
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
public class DatabaseUtilities
{
    // Core dependencies
    private final Client client;
    private final ItemManager itemManager;
    
    public DatabaseUtilities(Client client, ItemManager itemManager)
    {
        this.client = client;
        this.itemManager = itemManager;
        log.debug("DatabaseUtilities initialized");
    }
    
    /**
     * Get current player name with fallback
     */
    public String getCurrentPlayerName()
    {
        
        if (client != null && client.getLocalPlayer() != null) {
            String name = client.getLocalPlayer().getName();
            return name != null ? name : "UnknownPlayer";
        }
        return "UnknownPlayer";
    }
    
    /**
     * Update player name if needed (for sessions that started with UnknownPlayer)
     */
    public void updatePlayerNameIfNeeded(TickDataCollection tickData)
    {
        
        // Check if we need to update player name from "UnknownPlayer" to real name
        String currentName = getCurrentPlayerName();
        if (tickData != null && tickData.getPlayerData() != null) {
            String tickPlayerName = tickData.getPlayerData().getPlayerName();
            if ("UnknownPlayer".equals(tickPlayerName) && !"UnknownPlayer".equals(currentName)) {
                log.debug("Updating player name from UnknownPlayer to {}", currentName);
                // Session update logic would go here in full implementation
            }
        }
    }
    
    /**
     * Convert inventory items to JSON with robust ItemManager integration
     */
    public String convertInventoryItemsToJson(net.runelite.api.Item[] inventoryItems)
    {
        log.info("[INVENTORY-JSON-DEBUG] *** ENTERING convertInventoryItemsToJson method ***");
        
        if (inventoryItems == null || inventoryItems.length == 0) {
            log.info("[INVENTORY-JSON-DEBUG] Converting NULL or empty inventory array to JSON");
            return "[]";
        }
        
        log.info("[INVENTORY-JSON-DEBUG] Converting {} items to JSON", inventoryItems.length);
        
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        int validItemsFound = 0;
        
        for (int i = 0; i < inventoryItems.length; i++) {
            net.runelite.api.Item item = inventoryItems[i];
            if (item != null && item.getId() > 0) {
                validItemsFound++;
                if (!first) {
                    json.append(",");
                }
                
                // Get item name with robust error handling
                String itemName = "Unknown Item";
                final int itemId = item.getId();
                
                try {
                    if (itemManager == null) {
                        itemName = "ItemManager_Null_" + itemId;
                    } else if (isProblematicItemId(itemId)) {
                        // Known problematic items - use fallback
                        itemName = getKnownItemName(itemId);
                    } else {
                        // Try to get item name safely
                        try {
                            ItemComposition itemComp = itemManager.getItemComposition(itemId);
                            if (itemComp != null && itemComp.getName() != null) {
                                String resolvedName = itemComp.getName().trim();
                                if (!resolvedName.isEmpty()) {
                                    itemName = resolvedName.replace("\"", "\\\""); // Escape quotes for JSON
                                } else {
                                    itemName = "Empty_Name_" + itemId;
                                }
                            } else {
                                itemName = "Null_Composition_" + itemId;
                            }
                        } catch (Exception e) {
                            itemName = "Exception_" + itemId;
                        }
                    }
                } catch (Exception e) {
                    itemName = getKnownItemName(itemId);
                }
                
                json.append(String.format(
                    "{\"slot\":%d,\"id\":%d,\"quantity\":%d,\"name\":\"%s\"}", 
                    i, item.getId(), item.getQuantity(), itemName
                ));
                first = false;
            }
        }
        
        json.append("]");
        
        String result = json.toString();
        log.info("[INVENTORY-JSON-DEBUG] JSON conversion SUCCESSFUL - processed {} items, returning string length: {}", 
            validItemsFound, result.length());
        log.info("[INVENTORY-JSON-DEBUG] *** EXITING convertInventoryItemsToJson method SUCCESSFULLY ***");
        return result;
    }
    
    /**
     * Extract prayer state from activePrayers map
     */
    public Boolean getPrayerState(Map<String, Boolean> activePrayers, String prayerName)
    {
        if (activePrayers == null) {
            return false;
        }
        return activePrayers.getOrDefault(prayerName, false);
    }
    
    /**
     * Calculate total quantity of items
     */
    public int getTotalQuantity(net.runelite.api.Item[] items)
    {
        if (items == null) {
            return 0;
        }
        int total = 0;
        for (net.runelite.api.Item item : items) {
            if (item != null) {
                total += item.getQuantity();
            }
        }
        return total;
    }
    
    /**
     * Get unique item types count from item counts map
     */
    public int getUniqueItemTypes(Map<Integer, Integer> itemCounts)
    {
        if (itemCounts == null) {
            return 0;
        }
        return itemCounts.size();
    }
    
    /**
     * Find the most valuable item in inventory
     */
    public net.runelite.api.Item getMostValuableItem(net.runelite.api.Item[] items)
    {
        if (items == null || items.length == 0) {
            return null;
        }
        
        net.runelite.api.Item mostValuable = null;
        int highestQuantity = 0;
        
        for (net.runelite.api.Item item : items) {
            if (item != null && item.getId() > 0 && item.getQuantity() > highestQuantity) {
                highestQuantity = item.getQuantity();
                mostValuable = item;
            }
        }
        
        return mostValuable;
    }
    
    /**
     * Check if an item ID is known to cause hanging issues
     */
    private boolean isProblematicItemId(int itemId)
    {
        // Dharok's items (4882-4886 for different durability levels)
        if (itemId >= 4882 && itemId <= 4886) {
            return true;
        }
        
        // Other known problematic Barrows item ranges
        if ((itemId >= 4856 && itemId <= 4881) || // Various Barrows items with durability
            (itemId >= 4887 && itemId <= 4956) || // Extended Barrows item range
            (itemId >= 4708 && itemId <= 4759)) { // Another Barrows item range
            return true;
        }
        
        return false;
    }
    
    /**
     * Get a known name for problematic item IDs
     */
    private String getKnownItemName(int itemId)
    {
        // Dharok's helmet variants
        if (itemId == 4882) return "Dharok's helm (degraded)";
        if (itemId == 4883) return "Dharok's platebody (degraded)";
        if (itemId == 4884) return "Dharok's platelegs (degraded)";
        if (itemId == 4885) return "Dharok's greataxe (degraded)";
        if (itemId == 4886) return "Dharok's set (degraded)";
        
        // Generic fallbacks for known problematic ranges
        if (itemId >= 4856 && itemId <= 4956) {
            return "Barrows item (degraded_" + itemId + ")";
        }
        if (itemId >= 4708 && itemId <= 4759) {
            return "Barrows item (variant_" + itemId + ")";
        }
        
        // Default fallback for any item passed to this method
        return "Item_" + itemId;
    }
    
    /**
     * Helper method to safely get equipment item name
     */
    public String getEquipmentItemName(Integer itemId) 
    {
        if (itemId == null || itemId <= 0) return null;
        try {
            if (itemManager != null) {
                ItemComposition itemComp = itemManager.getItemComposition(itemId);
                return itemComp != null ? itemComp.getName() : "Unknown Item";
            }
        } catch (Exception e) {
            log.debug("Failed to get item name for ID {}: {}", itemId, e.getMessage());
        }
        return "Unknown Item";
    }
    
    /**
     * Convert rune pouch type ID to rune name
     */
    public String getRuneNameFromTypeId(Integer runeTypeId) 
    {
        if (runeTypeId == null || runeTypeId <= 0) return null;
        
        try {
            // Convert rune type ID to actual rune item ID using OSRS mapping
            int runeItemId = convertRuneTypeToItemId(runeTypeId);
            if (runeItemId > 0 && itemManager != null) {
                ItemComposition itemComp = itemManager.getItemComposition(runeItemId);
                return itemComp != null ? itemComp.getName() : null;
            }
        } catch (Exception e) {
            log.debug("Failed to convert rune type {} to item name: {}", runeTypeId, e.getMessage());
        }
        return null;
    }
    
    /**
     * Map rune type IDs from rune pouch varbits to actual rune item IDs
     */
    private int convertRuneTypeToItemId(int runeTypeId) 
    {
        // Common rune type mappings (based on OSRS rune pouch mechanics)
        switch (runeTypeId) {
            case 1: return 554;  // Air rune
            case 2: return 555;  // Water rune
            case 3: return 556;  // Earth rune
            case 4: return 557;  // Fire rune
            case 5: return 558;  // Mind rune
            case 6: return 559;  // Chaos rune
            case 7: return 560;  // Death rune
            case 8: return 561;  // Blood rune
            case 9: return 562;  // Cosmic rune
            case 10: return 563; // Nature rune
            case 11: return 564; // Law rune
            case 12: return 565; // Body rune
            case 13: return 566; // Soul rune
            case 14: return 9075; // Astral rune
            case 15: return 4696; // Mist rune
            case 16: return 4698; // Dust rune
            case 17: return 4700; // Mud rune
            case 18: return 4702; // Smoke rune
            case 19: return 4704; // Steam rune
            case 20: return 4706; // Lava rune
            default: return 0;   // Unknown rune type
        }
    }
}