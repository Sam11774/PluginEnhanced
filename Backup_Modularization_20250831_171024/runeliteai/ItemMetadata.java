/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Metadata information for game items
 * 
 * Contains comprehensive information about items including:
 * - Basic identification and properties
 * - Market data and pricing
 * - Usage statistics and context
 * - Categorization and classification
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemMetadata
{
    // ===== BASIC IDENTIFICATION =====
    private Integer itemId;
    private String itemName;
    private String description;
    private String category;
    private String subcategory;
    
    // ===== ITEM PROPERTIES =====
    private Boolean tradeable;
    private Boolean stackable;
    private Boolean notable;
    private Boolean alchable;
    private Integer alchValue;
    private Integer shopValue;
    private Integer weight;
    
    // ===== MARKET DATA =====
    private Long currentPrice;
    private Long averagePrice;
    private Long highPrice;
    private Long lowPrice;
    private Long priceDate;
    private Double volatility;
    private Integer dailyVolume;
    
    // ===== USAGE STATISTICS =====
    private Integer timesUsed;
    private Long lastUsedTime;
    private String mostCommonUse;
    private Integer averageQuantityUsed;
    private String[] commonContexts;
    
    // ===== CLASSIFICATION =====
    private String itemType; // weapon, armor, consumable, tool, etc.
    private String skillCategory; // combat, crafting, farming, etc.
    private Integer levelRequirement;
    private String[] requirements; // quest, skill level, etc.
    private Boolean questItem;
    private Boolean rareItem;
    private Integer rarity; // 1-5 scale
    
    // ===== METADATA =====
    private Long metadataCollected;
    private String dataSource;
    private Integer confidenceScore; // 0-100
    
    /**
     * Get total data points in this metadata
     * @return Data point count
     */
    public int getDataPointCount()
    {
        int count = 0;
        
        // Count non-null primitive fields
        count += (itemId != null ? 1 : 0);
        count += (tradeable != null ? 1 : 0);
        count += (stackable != null ? 1 : 0);
        count += (notable != null ? 1 : 0);
        count += (alchable != null ? 1 : 0);
        count += (alchValue != null ? 1 : 0);
        count += (shopValue != null ? 1 : 0);
        count += (weight != null ? 1 : 0);
        count += (currentPrice != null ? 1 : 0);
        count += (averagePrice != null ? 1 : 0);
        count += (highPrice != null ? 1 : 0);
        count += (lowPrice != null ? 1 : 0);
        count += (priceDate != null ? 1 : 0);
        count += (volatility != null ? 1 : 0);
        count += (dailyVolume != null ? 1 : 0);
        count += (timesUsed != null ? 1 : 0);
        count += (lastUsedTime != null ? 1 : 0);
        count += (averageQuantityUsed != null ? 1 : 0);
        count += (levelRequirement != null ? 1 : 0);
        count += (questItem != null ? 1 : 0);
        count += (rareItem != null ? 1 : 0);
        count += (rarity != null ? 1 : 0);
        count += (metadataCollected != null ? 1 : 0);
        count += (confidenceScore != null ? 1 : 0);
        
        // Count string fields
        count += (itemName != null ? 1 : 0);
        count += (description != null ? 1 : 0);
        count += (category != null ? 1 : 0);
        count += (subcategory != null ? 1 : 0);
        count += (mostCommonUse != null ? 1 : 0);
        count += (itemType != null ? 1 : 0);
        count += (skillCategory != null ? 1 : 0);
        count += (dataSource != null ? 1 : 0);
        
        // Count array fields
        count += (commonContexts != null ? commonContexts.length : 0);
        count += (requirements != null ? requirements.length : 0);
        
        return count;
    }
    
    /**
     * Check if this metadata is valid and complete
     * @return True if valid, false otherwise
     */
    public boolean isValid()
    {
        return itemId != null && 
               itemId > 0 &&
               itemName != null && 
               !itemName.trim().isEmpty();
    }
    
    /**
     * Get estimated memory size
     * @return Estimated memory size in bytes
     */
    public long getEstimatedSize()
    {
        long size = 64; // Base object overhead
        
        // Primitive fields (boxed)
        size += 16 * 15; // Integer fields
        size += 16 * 6;  // Long fields
        size += 16 * 1;  // Double field
        size += 8 * 6;   // Boolean fields
        
        // String fields
        size += (itemName != null ? itemName.length() * 2 + 32 : 0);
        size += (description != null ? description.length() * 2 + 32 : 0);
        size += (category != null ? category.length() * 2 + 32 : 0);
        size += (subcategory != null ? subcategory.length() * 2 + 32 : 0);
        size += (mostCommonUse != null ? mostCommonUse.length() * 2 + 32 : 0);
        size += (itemType != null ? itemType.length() * 2 + 32 : 0);
        size += (skillCategory != null ? skillCategory.length() * 2 + 32 : 0);
        size += (dataSource != null ? dataSource.length() * 2 + 32 : 0);
        
        // Array fields
        if (commonContexts != null) {
            size += 24; // Array header
            for (String context : commonContexts) {
                size += (context != null ? context.length() * 2 + 32 : 16);
            }
        }
        
        if (requirements != null) {
            size += 24; // Array header
            for (String req : requirements) {
                size += (req != null ? req.length() * 2 + 32 : 16);
            }
        }
        
        return size;
    }
    
    /**
     * Create a compact string representation
     * @return Compact string representation
     */
    public String toCompactString()
    {
        return String.format("ItemMetadata{id=%d, name='%s', type='%s', price=%d}", 
                            itemId, itemName, itemType, currentPrice);
    }
}