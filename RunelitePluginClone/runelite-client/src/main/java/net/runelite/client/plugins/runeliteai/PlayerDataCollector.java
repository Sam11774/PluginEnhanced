/*
 * Copyright (c) 2024, RuneLiteAI Team  
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.runeliteai.TickDataCollection.TickDataCollectionBuilder;

import java.util.*;
import static net.runelite.client.plugins.runeliteai.DataStructures.*;

/**
 * Dedicated collector for player-specific data
 * 
 * Responsible for:
 * - Player vitals (HP, prayer, energy, weight, special attack)
 * - Player stats (all skill levels and experience)
 * - Player location (world coordinates, region, area types)
 * - Player equipment (gear, bonuses, equipment changes)
 * - Player inventory (items, value, changes)
 * - Player prayers and spells (active states)
 * 
 * Migrated from DataCollectionManager lines 328-1195
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
public class PlayerDataCollector
{
    // Core dependencies
    private final Client client;
    private final ItemManager itemManager;
    
    // Optimization - Reusable maps to reduce object allocation overhead
    private final Map<String, Integer> reusableStatsMap = new HashMap<>();
    private final Map<String, Integer> reusableExperienceMap = new HashMap<>();
    private final Map<String, Integer> reusableInventoryMap = new HashMap<>();
    
    // Equipment change detection
    private Map<String, Integer> previousEquipmentItems = new HashMap<>();
    
    // Inventory change detection - track item ID to quantity mapping
    private Map<Integer, Integer> previousInventoryItems = new HashMap<>();
    private long previousInventoryValue = 0;
    
    public PlayerDataCollector(Client client, ItemManager itemManager)
    {
        this.client = client;
        this.itemManager = itemManager;
        log.debug("PlayerDataCollector initialized");
    }
    
    /**
     * Collect all player-related data
     */
    public void collectPlayerData(TickDataCollection.TickDataCollectionBuilder builder)
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return;
        }
        
        // Cache frequently accessed data to reduce API calls
        WorldPoint worldLocation = localPlayer.getWorldLocation();
        
        // Basic player data (pass cached worldLocation to avoid redundant calls)
        PlayerData playerData = collectBasicPlayerDataOptimized(localPlayer, worldLocation);
        builder.playerData(playerData);
        
        // Player vitals (cache skill levels to reduce multiple client calls)
        PlayerVitals vitals = collectPlayerVitalsOptimized();
        builder.playerVitals(vitals);
        
        // Player location (use cached worldLocation)
        PlayerLocation location = collectPlayerLocationOptimized(localPlayer, worldLocation);
        builder.playerLocation(location);
        
        // Player stats (batch skill level calls)
        PlayerStats stats = collectPlayerStatsOptimized();
        builder.playerStats(stats);
        
        // Equipment and inventory (cache container states)
        PlayerEquipment equipment = collectPlayerEquipmentOptimized();
        builder.playerEquipment(equipment);
        
        PlayerInventory inventory = collectPlayerInventoryOptimized();
        builder.playerInventory(inventory);
        
        // Active prayers and spells (cache varp values)
        PlayerActivePrayers prayers = collectActivePrayersOptimized();
        builder.playerPrayers(prayers);
        
        PlayerActiveSpells spells = collectActiveSpellsOptimized();
        builder.playerSpells(spells);
    }
    
    /**
     * Collect basic player data - OPTIMIZED VERSION
     */
    private PlayerData collectBasicPlayerDataOptimized(Player player, WorldPoint worldLocation)
    {
        return PlayerData.builder()
            .playerName(player.getName())
            .combatLevel(player.getCombatLevel())
            .worldX(worldLocation != null ? worldLocation.getX() : null)
            .worldY(worldLocation != null ? worldLocation.getY() : null)
            .plane(worldLocation != null ? worldLocation.getPlane() : null)
            .animation(player.getAnimation())
            .poseAnimation(player.getPoseAnimation())
            .healthRatio(player.getHealthRatio())
            .healthScale(player.getHealthScale())
            .overhead(player.getOverheadIcon() != null ? player.getOverheadIcon().toString() : null)
            .skullIcon(player.getSkullIcon() != -1 ? String.valueOf(player.getSkullIcon()) : null)
            .team(player.getTeam())
            .isFriend(player.isFriend())
            .isClanMember(player.isClanMember())
            .isFriendsChatMember(player.isFriendsChatMember())
            .build();
    }
    
    /**
     * Collect player vital statistics - OPTIMIZED VERSION
     */
    private PlayerVitals collectPlayerVitalsOptimized()
    {
        // Cache VarPlayer values to avoid multiple client calls
        int poisonValue = client.getVarpValue(VarPlayer.POISON);
        int diseaseValue = client.getVarpValue(VarPlayer.DISEASE_VALUE);
        
        return PlayerVitals.builder()
            .currentHitpoints(client.getBoostedSkillLevel(Skill.HITPOINTS))
            .maxHitpoints(client.getRealSkillLevel(Skill.HITPOINTS))
            .currentPrayer(client.getBoostedSkillLevel(Skill.PRAYER))
            .maxPrayer(client.getRealSkillLevel(Skill.PRAYER))
            .energy(client.getEnergy() / 100)
            .weight(client.getWeight())
            .specialAttackPercent(client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) / 10)
            .poisoned(poisonValue > 0)
            .diseased(diseaseValue > 0)
            .venomed(poisonValue < 0)
            .build();
    }
    
    /**
     * Collect player location information - OPTIMIZED VERSION
     */
    private PlayerLocation collectPlayerLocationOptimized(Player player, WorldPoint worldLocation)
    {
        if (worldLocation == null) {
            return PlayerLocation.builder().build();
        }
        
        // Calculate local coordinates within the current chunk (0-63 range)
        int localX = worldLocation.getX() & 63;
        int localY = worldLocation.getY() & 63;
        
        return PlayerLocation.builder()
            .worldX(worldLocation.getX())
            .worldY(worldLocation.getY())
            .plane(worldLocation.getPlane())
            .regionX(worldLocation.getRegionX())
            .regionY(worldLocation.getRegionY())
            .regionId(worldLocation.getRegionID())
            .chunkX(worldLocation.getX() >> 6)
            .chunkY(worldLocation.getY() >> 6)
            .localX(localX)
            .localY(localY)
            .locationName(getLocationName(worldLocation))
            .areaType(getAreaType(worldLocation))
            .inWilderness(isInWilderness(worldLocation))
            .wildernessLevel(getWildernessLevel(worldLocation))
            .inPvp(isInPvp(worldLocation))
            .inMultiCombat(isInMultiCombat(worldLocation))
            .build();
    }
    
    /**
     * Collect player skill statistics
     */
    private PlayerStats collectPlayerStatsOptimized()
    {
        // Use pre-warmed reusable maps to reduce object allocation overhead
        reusableStatsMap.clear();
        reusableExperienceMap.clear();
        Map<String, Integer> currentLevels = reusableStatsMap;
        Map<String, Integer> realLevels = new HashMap<>(); // Keep as new for now, reuse later
        Map<String, Integer> experience = reusableExperienceMap;
        
        int totalLevel = 0;
        long totalXp = 0;
        
        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL) continue;
            
            String skillName = skill.name().toLowerCase();
            int currentLevel = client.getBoostedSkillLevel(skill);
            int realLevel = client.getRealSkillLevel(skill);
            int xp = client.getSkillExperience(skill);
            
            currentLevels.put(skillName, currentLevel);
            realLevels.put(skillName, realLevel);
            experience.put(skillName, xp);
            
            // Debug log to check if levels are actually varying
            if (skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE) {
                log.debug("[STATS-DEBUG] {} - current: {}, real: {}", skillName, currentLevel, realLevel);
            }
            
            totalLevel += realLevel;
            totalXp += xp;
        }
        
        return PlayerStats.builder()
            .currentLevels(currentLevels)
            .realLevels(realLevels)
            .experience(experience)
            .totalLevel(totalLevel)
            .combatLevel(client.getLocalPlayer() != null ? client.getLocalPlayer().getCombatLevel() : 0)
            .totalExperience(totalXp)
            .build();
    }
    
    /**
     * Collect player equipment with friendly name resolution
     */
    private PlayerEquipment collectPlayerEquipmentOptimized()
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null) {
            log.debug("[EQUIPMENT-DEBUG] Equipment container is NULL - no equipment equipped");
            return PlayerEquipment.builder().build();
        }
        
        Item[] items = equipment.getItems();
        Map<String, Integer> equipmentIds = new HashMap<>();
        
        // Calculate equipment value and weight
        long totalEquipmentValue = 0;
        int equipmentWeight = client.getWeight(); // Use total player weight from client
        
        log.debug("[EQUIPMENT-DEBUG] Starting equipment collection - items.length={}, playerWeight={}", 
            items != null ? items.length : "null", equipmentWeight);
        
        // Calculate equipment stats (total bonuses from all equipped items)
        int totalAttackSlashBonus = 0;
        int totalAttackStabBonus = 0;
        int totalAttackCrushBonus = 0;
        int totalAttackMagicBonus = 0;
        int totalAttackRangedBonus = 0;
        int totalDefenseSlashBonus = 0;
        int totalDefenseStabBonus = 0;
        int totalDefenseCrushBonus = 0;
        int totalDefenseMagicBonus = 0;
        int totalDefenseRangedBonus = 0;
        int totalStrengthBonus = 0;
        int totalRangedStrengthBonus = 0;
        float totalMagicDamageBonus = 0.0f;
        int totalPrayerBonus = 0;
        
        for (Item item : items) {
            if (item.getId() > 0) {
                // Calculate value using ItemManager
                if (itemManager != null) {
                    try {
                        int price = itemManager.getItemPrice(item.getId());
                        totalEquipmentValue += (long) price * item.getQuantity();
                    } catch (Exception e) {
                        // Ignore pricing errors
                    }
                }
                
                // Calculate equipment stats using ItemManager
                if (itemManager != null) {
                    try {
                        net.runelite.client.game.ItemStats itemStats = itemManager.getItemStats(item.getId());
                        if (itemStats != null) {
                            net.runelite.client.game.ItemEquipmentStats equipmentStats = itemStats.getEquipment();
                            if (equipmentStats != null) {
                                totalAttackSlashBonus += equipmentStats.getAslash();
                                totalAttackStabBonus += equipmentStats.getAstab();
                                totalAttackCrushBonus += equipmentStats.getAcrush();
                                totalAttackMagicBonus += equipmentStats.getAmagic();
                                totalAttackRangedBonus += equipmentStats.getArange();
                                totalDefenseSlashBonus += equipmentStats.getDslash();
                                totalDefenseStabBonus += equipmentStats.getDstab();
                                totalDefenseCrushBonus += equipmentStats.getDcrush();
                                totalDefenseMagicBonus += equipmentStats.getDmagic();
                                totalDefenseRangedBonus += equipmentStats.getDrange();
                                totalStrengthBonus += equipmentStats.getStr();
                                totalRangedStrengthBonus += equipmentStats.getRstr();
                                totalMagicDamageBonus += equipmentStats.getMdmg();
                                totalPrayerBonus += equipmentStats.getPrayer();
                            }
                        }
                    } catch (Exception e) {
                        log.warn("[EQUIPMENT-DEBUG] Error getting equipment stats for item {}: {}", item.getId(), e.getMessage());
                    }
                }
            }
        }
        
        // Extract individual equipment slot IDs
        Integer helmetId = items.length > EquipmentInventorySlot.HEAD.getSlotIdx() && items[EquipmentInventorySlot.HEAD.getSlotIdx()].getId() > 0 ? 
            items[EquipmentInventorySlot.HEAD.getSlotIdx()].getId() : -1;
        Integer capeId = items.length > EquipmentInventorySlot.CAPE.getSlotIdx() && items[EquipmentInventorySlot.CAPE.getSlotIdx()].getId() > 0 ? 
            items[EquipmentInventorySlot.CAPE.getSlotIdx()].getId() : -1;
        Integer amuletId = items.length > EquipmentInventorySlot.AMULET.getSlotIdx() && items[EquipmentInventorySlot.AMULET.getSlotIdx()].getId() > 0 ? 
            items[EquipmentInventorySlot.AMULET.getSlotIdx()].getId() : -1;
        Integer weaponId = items.length > EquipmentInventorySlot.WEAPON.getSlotIdx() && items[EquipmentInventorySlot.WEAPON.getSlotIdx()].getId() > 0 ? 
            items[EquipmentInventorySlot.WEAPON.getSlotIdx()].getId() : -1;
        Integer bodyId = items.length > EquipmentInventorySlot.BODY.getSlotIdx() && items[EquipmentInventorySlot.BODY.getSlotIdx()].getId() > 0 ? 
            items[EquipmentInventorySlot.BODY.getSlotIdx()].getId() : -1;
        Integer shieldId = items.length > EquipmentInventorySlot.SHIELD.getSlotIdx() && items[EquipmentInventorySlot.SHIELD.getSlotIdx()].getId() > 0 ? 
            items[EquipmentInventorySlot.SHIELD.getSlotIdx()].getId() : -1;
        Integer legsId = items.length > EquipmentInventorySlot.LEGS.getSlotIdx() && items[EquipmentInventorySlot.LEGS.getSlotIdx()].getId() > 0 ? 
            items[EquipmentInventorySlot.LEGS.getSlotIdx()].getId() : -1;
        Integer glovesId = items.length > EquipmentInventorySlot.GLOVES.getSlotIdx() && items[EquipmentInventorySlot.GLOVES.getSlotIdx()].getId() > 0 ? 
            items[EquipmentInventorySlot.GLOVES.getSlotIdx()].getId() : -1;
        Integer bootsId = items.length > EquipmentInventorySlot.BOOTS.getSlotIdx() && items[EquipmentInventorySlot.BOOTS.getSlotIdx()].getId() > 0 ? 
            items[EquipmentInventorySlot.BOOTS.getSlotIdx()].getId() : -1;
        Integer ringId = items.length > EquipmentInventorySlot.RING.getSlotIdx() && items[EquipmentInventorySlot.RING.getSlotIdx()].getId() > 0 ? 
            items[EquipmentInventorySlot.RING.getSlotIdx()].getId() : -1;
        Integer ammoId = items.length > EquipmentInventorySlot.AMMO.getSlotIdx() && items[EquipmentInventorySlot.AMMO.getSlotIdx()].getId() > 0 ? 
            items[EquipmentInventorySlot.AMMO.getSlotIdx()].getId() : -1;
        
        for (int i = 0; i < items.length && i < EquipmentInventorySlot.values().length; i++) {
            Item item = items[i];
            if (item.getId() > 0) {
                EquipmentInventorySlot slot = EquipmentInventorySlot.values()[i];
                equipmentIds.put(slot.name().toLowerCase(), item.getId());
            }
        }
        
        // Equipment change detection
        Map<String, Integer> currentEquipment = new HashMap<>();
        currentEquipment.put("helmet", helmetId);
        currentEquipment.put("cape", capeId);
        currentEquipment.put("amulet", amuletId);
        currentEquipment.put("weapon", weaponId);
        currentEquipment.put("body", bodyId);
        currentEquipment.put("shield", shieldId);
        currentEquipment.put("legs", legsId);
        currentEquipment.put("gloves", glovesId);
        currentEquipment.put("boots", bootsId);
        currentEquipment.put("ring", ringId);
        currentEquipment.put("ammo", ammoId);
        
        // Detect equipment changes
        int equipmentChangesCount = 0;
        boolean weaponChanged = false;
        boolean armorChanged = false;
        boolean accessoryChanged = false;
        
        for (Map.Entry<String, Integer> entry : currentEquipment.entrySet()) {
            String slot = entry.getKey();
            Integer currentId = entry.getValue();
            Integer previousId = previousEquipmentItems.getOrDefault(slot, -1);
            
            if (!currentId.equals(previousId)) {
                equipmentChangesCount++;
                
                // Categorize change type
                if ("weapon".equals(slot)) {
                    weaponChanged = true;
                } else if ("helmet".equals(slot) || "body".equals(slot) || "legs".equals(slot) || 
                          "gloves".equals(slot) || "boots".equals(slot)) {
                    armorChanged = true;
                } else if ("cape".equals(slot) || "amulet".equals(slot) || "shield".equals(slot) || 
                          "ring".equals(slot) || "ammo".equals(slot)) {
                    accessoryChanged = true;
                }
            }
        }
        
        // Update previous equipment state for next tick
        previousEquipmentItems = new HashMap<>(currentEquipment);
        
        return PlayerEquipment.builder()
            .equipmentItems(items)
            .equipmentIds(equipmentIds)
            .weaponTypeId(client.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE))
            .weaponType(getWeaponType())
            .attackStyle(getAttackStyle())
            .autoRetaliate(client.getVarpValue(RuneliteAIConstants.AUTO_RETALIATE_VARP) == 1)
            .combatStyle(client.getVarpValue(VarPlayer.ATTACK_STYLE))
            // Individual equipment slot IDs
            .helmetId(helmetId)
            .capeId(capeId)
            .amuletId(amuletId)
            .weaponId(weaponId)
            .bodyId(bodyId)
            .shieldId(shieldId)
            .legsId(legsId)
            .glovesId(glovesId)
            .bootsId(bootsId)
            .ringId(ringId)
            .ammoId(ammoId)
            // Individual equipment slot names (friendly name resolution)
            .helmetName(getEquipmentItemName(helmetId))
            .capeName(getEquipmentItemName(capeId))
            .amuletName(getEquipmentItemName(amuletId))
            .weaponName(getEquipmentItemName(weaponId))
            .bodyName(getEquipmentItemName(bodyId))
            .shieldName(getEquipmentItemName(shieldId))
            .legsName(getEquipmentItemName(legsId))
            .glovesName(getEquipmentItemName(glovesId))
            .bootsName(getEquipmentItemName(bootsId))
            .ringName(getEquipmentItemName(ringId))
            .ammoName(getEquipmentItemName(ammoId))
            // Equipment analytics  
            .totalEquipmentValue(totalEquipmentValue)
            .equipmentWeight(equipmentWeight)
            // Equipment change detection
            .equipmentChangesCount(equipmentChangesCount)
            .weaponChanged(weaponChanged)
            .armorChanged(armorChanged)
            .accessoryChanged(accessoryChanged)
            // Equipment stats and bonuses
            .attackSlashBonus(totalAttackSlashBonus)
            .attackStabBonus(totalAttackStabBonus)
            .attackCrushBonus(totalAttackCrushBonus)
            .attackMagicBonus(totalAttackMagicBonus)
            .attackRangedBonus(totalAttackRangedBonus)
            .defenseSlashBonus(totalDefenseSlashBonus)
            .defenseStabBonus(totalDefenseStabBonus)
            .defenseCrushBonus(totalDefenseCrushBonus)
            .defenseMagicBonus(totalDefenseMagicBonus)
            .defenseRangedBonus(totalDefenseRangedBonus)
            .strengthBonus(totalStrengthBonus)
            .rangedStrengthBonus(totalRangedStrengthBonus)
            .magicDamageBonus(totalMagicDamageBonus)
            .prayerBonus(totalPrayerBonus)
            .build();
    }
    
    /**
     * Collect comprehensive player inventory with change detection and analytics - FULLY RESTORED
     */
    private PlayerInventory collectPlayerInventoryOptimized()
    {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        if (inventory == null) {
            log.debug("[INVENTORY-DEBUG] Inventory container is NULL - no inventory available");
            return PlayerInventory.builder()
                .inventoryItems(new Item[0])
                .usedSlots(0).freeSlots(28).totalQuantity(0).totalValue(0L)
                .itemCounts(new HashMap<>())
                .itemsAdded(0).itemsRemoved(0)
                .quantityGained(0).quantityLost(0)
                .valueGained(0L).valueLost(0L)
                .build();
        }
        
        Item[] items = inventory.getItems();
        int usedSlots = 0;
        int totalQuantity = 0;  // Track total quantity of all items
        long totalValue = 0;
        Map<Integer, Integer> itemCounts = new HashMap<>();
        
        // Most valuable item tracking
        int mostValuableItemId = -1;
        String mostValuableItemName = null;
        int mostValuableItemQuantity = 0;
        long mostValuableItemValue = 0;
        
        // Noted items detection
        int notedItemsCount = 0;
        
        log.debug("[INVENTORY-DEBUG] Starting inventory collection - items.length={}", 
            items != null ? items.length : "null");
        
        for (Item item : items) {
            if (item.getId() > 0) {
                usedSlots++;
                totalQuantity += item.getQuantity();  // Add to total quantity
                itemCounts.put(item.getId(), itemCounts.getOrDefault(item.getId(), 0) + item.getQuantity());
                
                // Calculate value using ItemManager
                if (itemManager != null) {
                    try {
                        int price = itemManager.getItemPrice(item.getId());
                        long itemValue = (long) price * item.getQuantity();
                        totalValue += itemValue;
                        
                        // Track most valuable item
                        if (itemValue > mostValuableItemValue) {
                            mostValuableItemId = item.getId();
                            mostValuableItemQuantity = item.getQuantity();
                            mostValuableItemValue = itemValue;
                            
                            // Get item name for most valuable item
                            try {
                                ItemComposition itemComp = itemManager.getItemComposition(item.getId());
                                mostValuableItemName = itemComp != null ? itemComp.getName() : "Unknown Item";
                            } catch (Exception e) {
                                mostValuableItemName = isProblematicItemId(item.getId()) ? 
                                    getKnownItemName(item.getId()) : "Unknown Item";
                            }
                        }
                        
                        // Detect noted items
                        try {
                            ItemComposition itemComp = itemManager.getItemComposition(item.getId());
                            if (itemComp != null && itemComp.getNote() != -1) {
                                notedItemsCount++;
                            }
                        } catch (Exception e) {
                            // Ignore composition errors
                        }
                        
                    } catch (Exception e) {
                        // Ignore pricing errors
                    }
                }
            }
        }
        
        // CHANGE DETECTION SYSTEM - FULLY RESTORED
        int itemsAdded = 0;
        int itemsRemoved = 0;
        int quantityGained = 0;
        int quantityLost = 0;
        long valueGained = 0;
        long valueLost = 0;
        
        try {
            // Check for new items (items that weren't in previous inventory)
            for (Map.Entry<Integer, Integer> entry : itemCounts.entrySet()) {
                int itemId = entry.getKey();
                int currentQuantity = entry.getValue();
                int previousQuantity = previousInventoryItems.getOrDefault(itemId, 0);
                
                if (previousQuantity == 0 && currentQuantity > 0) {
                    // New item added
                    itemsAdded++;
                    quantityGained += currentQuantity;
                    
                    // Calculate value gained
                    if (itemManager != null) {
                        try {
                            int price = itemManager.getItemPrice(itemId);
                            valueGained += (long) price * currentQuantity;
                        } catch (Exception e) {
                            // Ignore pricing errors
                        }
                    }
                } else if (currentQuantity > previousQuantity) {
                    // Quantity increased for existing item
                    int quantityIncrease = currentQuantity - previousQuantity;
                    quantityGained += quantityIncrease;
                    
                    // Calculate value gained
                    if (itemManager != null) {
                        try {
                            int price = itemManager.getItemPrice(itemId);
                            valueGained += (long) price * quantityIncrease;
                        } catch (Exception e) {
                            // Ignore pricing errors
                        }
                    }
                }
            }
            
            // Check for removed items or quantity decreases
            for (Map.Entry<Integer, Integer> entry : previousInventoryItems.entrySet()) {
                int itemId = entry.getKey();
                int previousQuantity = entry.getValue();
                int currentQuantity = itemCounts.getOrDefault(itemId, 0);
                
                if (currentQuantity == 0 && previousQuantity > 0) {
                    // Item removed completely
                    itemsRemoved++;
                    quantityLost += previousQuantity;
                    
                    // Calculate value lost
                    if (itemManager != null) {
                        try {
                            int price = itemManager.getItemPrice(itemId);
                            valueLost += (long) price * previousQuantity;
                        } catch (Exception e) {
                            // Ignore pricing errors
                        }
                    }
                } else if (currentQuantity < previousQuantity) {
                    // Quantity decreased for existing item
                    int quantityDecrease = previousQuantity - currentQuantity;
                    quantityLost += quantityDecrease;
                    
                    // Calculate value lost
                    if (itemManager != null) {
                        try {
                            int price = itemManager.getItemPrice(itemId);
                            valueLost += (long) price * quantityDecrease;
                        } catch (Exception e) {
                            // Ignore pricing errors
                        }
                    }
                }
            }
            
            if (itemsAdded > 0 || itemsRemoved > 0 || quantityGained > 0 || quantityLost > 0) {
                log.debug("[INVENTORY-DEBUG] Changes detected: +{} items, -{} items, +{} qty, -{} qty, +{} value, -{} value", 
                    itemsAdded, itemsRemoved, quantityGained, quantityLost, valueGained, valueLost);
            }
            
        } catch (Exception e) {
            log.warn("Error calculating inventory changes", e);
            // Reset change values on error
            itemsAdded = itemsRemoved = quantityGained = quantityLost = 0;
            valueGained = valueLost = 0;
        }
        
        // Update previous inventory state for next tick
        previousInventoryItems.clear();
        for (Map.Entry<Integer, Integer> entry : itemCounts.entrySet()) {
            previousInventoryItems.put(entry.getKey(), entry.getValue());
        }
        previousInventoryValue = totalValue;
        
        // Debug final inventory state
        log.debug("[INVENTORY-DEBUG] Final inventory: usedSlots={}, totalValue={}, itemCounts.size()={}, items.length={}, notedItems={}", 
            usedSlots, totalValue, itemCounts.size(), items.length, notedItemsCount);
        
        // CRITICAL: Thread-safe JSON generation
        String inventoryJson = generateInventoryJsonOnClientThread(items);
        
        return PlayerInventory.builder()
            .inventoryItems(items)
            .usedSlots(usedSlots)
            .freeSlots(28 - usedSlots)
            .totalQuantity(totalQuantity)  // Add total quantity
            .totalValue(totalValue)
            .itemCounts(itemCounts)
            // Most valuable item tracking
            .mostValuableItemId(mostValuableItemId)
            .mostValuableItemName(mostValuableItemName)
            .mostValuableItemQuantity(mostValuableItemQuantity)
            .mostValuableItemValue(mostValuableItemValue)
            // Change detection analytics
            .itemsAdded(itemsAdded)
            .itemsRemoved(itemsRemoved)
            .quantityGained(quantityGained)
            .quantityLost(quantityLost)
            .valueGained(valueGained)
            .valueLost(valueLost)
            // Noted items tracking
            .notedItemsCount(notedItemsCount)
            // Pre-resolved JSON (generated on Client thread where ItemManager works)
            .inventoryJson(inventoryJson)
            .build();
    }
    
    /**
     * Collect active prayers
     */
    private PlayerActivePrayers collectActivePrayersOptimized()
    {
        Map<String, Boolean> activePrayers = new HashMap<>();
        int activeCount = 0;
        
        for (Prayer prayer : Prayer.values()) {
            boolean isActive = client.isPrayerActive(prayer);
            activePrayers.put(prayer.name(), isActive);
            if (isActive) {
                activeCount++;
                log.debug("[PRAYER-DEBUG] Active prayer detected: {}", prayer.name());
            }
        }
        
        if (activeCount > 0) {
            log.debug("[PRAYER-DEBUG] Total active prayers: {}", activeCount);
        }
        
        return PlayerActivePrayers.builder()
            .activePrayers(activePrayers)
            .activePrayerCount(activeCount) // Fixed: count only active prayers
            .prayerDrainRate(calculatePrayerDrainRate())
            .build();
    }
    
    /**
     * Collect active spells
     */
    private PlayerActiveSpells collectActiveSpellsOptimized()
    {
        return PlayerActiveSpells.builder()
            .spellbook(getSelectedSpellbook())
            .selectedSpell(getSelectedSpell())
            .autocastSpell(getAutocastSpell())
            .build();
    }
    
    // Helper methods
    private String getLocationName(WorldPoint location)
    {
        if (location == null) {
            return "Unknown";
        }
        
        int regionId = location.getRegionID();
        int x = location.getX();
        int y = location.getY();
        
        // Use comprehensive region mapping for friendly names
        return getRegionName(regionId, x, y);
    }
    
    /**
     * Get friendly region name based on region ID and coordinates
     */
    private String getRegionName(int regionId, int x, int y)
    {
        // Major cities and areas based on region IDs and coordinates
        switch (regionId) {
            case 12850: return "Lumbridge";
            case 12849: return "Lumbridge Swamp";
            case 12851: return "Al Kharid";
            case 12342: return "Varrock";
            case 12853: return "Draynor Village";
            case 11828: return "Falador";
            case 12084: return "Barbarian Village";
            case 12340: return "Grand Exchange";
            case 12341: return "Grand Exchange";
            case 11573: return "Camelot";
            case 10806: return "Catherby";
            case 10549: return "White Wolf Mountain";
            case 11317: return "Taverly";
            case 12336: return "Edgeville";
            case 12848: return "Karamja";
            case 11570: return "Seers Village";
            case 10293: return "Fishing Guild";
            case 12339: return "Wilderness";
            case 12597: return "East Ardougne";
            case 12596: return "West Ardougne";
            case 10790: return "Tree Gnome Stronghold";
            case 9780: return "Tree Gnome Village";
            case 10033: return "Yanille";
            case 11318: return "Burthorpe";
            case 12852: return "Port Sarim";
            case 12854: return "Rimmington";
            case 11062: return "Castle Wars";
            case 12589: return "Clock Tower";
            case 12845: return "Crafting Guild";
            case 12855: return "Mudskipper Point";
            
            // Wilderness regions
            case 12188:
            case 12189:
            case 12190:
            case 12191:
            case 12444:
            case 12445:
            case 12446:
            case 12447:
            case 12700:
            case 12701:
            case 12702:
            case 12703:
            case 12956:
            case 12957:
            case 12958:
            case 12959:
                return "Wilderness";
                
            // Morytania regions
            case 13878:
            case 13877:
            case 14134:
            case 14133:
                return "Morytania";
                
            // Desert regions
            case 13104:
            case 13105:
            case 13360:
            case 13361:
                return "Kharidian Desert";
                
            // Default based on coordinate ranges for major areas
            default:
                // Lumbridge area
                if (x >= 3200 && x <= 3230 && y >= 3200 && y <= 3230) {
                    return "Lumbridge";
                }
                // Varrock area
                else if (x >= 3210 && x <= 3260 && y >= 3380 && y <= 3430) {
                    return "Varrock";
                }
                // Falador area  
                else if (x >= 2940 && x <= 3000 && y >= 3310 && y <= 3390) {
                    return "Falador";
                }
                // Al Kharid area
                else if (x >= 3270 && x <= 3320 && y >= 3160 && y <= 3200) {
                    return "Al Kharid";
                }
                // Draynor area
                else if (x >= 3080 && x <= 3120 && y >= 3220 && y <= 3270) {
                    return "Draynor Village";
                }
                // Edgeville area
                else if (x >= 3080 && x <= 3130 && y >= 3480 && y <= 3520) {
                    return "Edgeville";
                }
                // Wilderness check (y > 3520)
                else if (y > 3520) {
                    return "Wilderness";
                }
                // Karamja check (y < 2950)
                else if (y < 2950) {
                    return "Karamja";
                }
                // Fallback with region ID
                else {
                    return "Region_" + regionId;
                }
        }
    }
    
    private String getAreaType(WorldPoint location)
    {
        if (isInWilderness(location)) return "WILDERNESS";
        if (location.getPlane() > 0) return "UPPER_LEVEL";
        return "GROUND_LEVEL";
    }
    
    private boolean isInWilderness(WorldPoint location)
    {
        return location.getY() > 3520 && location.getY() < 4000;
    }
    
    private int getWildernessLevel(WorldPoint location)
    {
        if (!isInWilderness(location)) return 0;
        return Math.max(1, (location.getY() - 3520) / 8 + 1);
    }
    
    private boolean isInPvp(WorldPoint location)
    {
        return isInWilderness(location);
    }
    
    private boolean isInMultiCombat(WorldPoint location)
    {
        // Simplified - would need proper multi-combat area detection
        return false;
    }
    
    private String getWeaponType()
    {
        try {
            int weaponTypeId = client.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);
            
            switch (weaponTypeId) {
                case 0: return "unarmed";
                case 1: return "axe";
                case 2: return "hammer";  
                case 3: return "bow";
                case 4: return "crossbow";
                case 5: return "mace";
                case 6: return "sword";
                case 7: return "dagger";
                case 8: return "thrown";
                case 9: return "staff";
                case 10: return "2h_sword";
                case 11: return "pickaxe";
                case 12: return "halberd";
                case 13: return "spear";
                case 14: return "claw";
                case 15: return "whip";
                case 16: return "banner";
                case 17: return "2h_crossbow";
                case 18: return "chinchompa";
                case 19: return "fixed_device";
                case 20: return "magic_staff";
                case 21: return "trident";
                case 22: return "ballista";
                case 23: return "blaster";
                case 24: return "2h_axe";
                case 25: return "bulwark";
                case 26: return "gunpowder";
                case 27: return "scythe";
                default: return "weapon_type_" + weaponTypeId;
            }
        } catch (Exception e) {
            log.warn("Error getting weapon type", e);
            return "error";
        }
    }
    
    private String getAttackStyle()
    {
        int attackStyle = client.getVarpValue(VarPlayer.ATTACK_STYLE);
        return "ATTACK_STYLE_" + attackStyle;
    }
    
    private String getEquipmentItemName(Integer itemId)
    {
        if (itemId == null || itemId <= 0) return null;
        
        if (itemManager != null) {
            try {
                ItemComposition itemComposition = itemManager.getItemComposition(itemId);
                if (itemComposition != null) {
                    return itemComposition.getName();
                }
            } catch (Exception e) {
                // Ignore errors and fall back
            }
        }
        
        return "Item_" + itemId;
    }
    
    private int calculatePrayerDrainRate()
    {
        // Simplified prayer drain calculation
        return 1; // Could be enhanced with actual drain rate calculation
    }
    
    private String getSelectedSpellbook()
    {
        try {
            int spellbook = client.getVarbitValue(Varbits.SPELLBOOK);
            switch (spellbook) {
                case 0: return "NORMAL";
                case 1: return "ANCIENT";
                case 2: return "LUNAR";
                case 3: return "ARCEUUS";
                default: return "NORMAL";
            }
        } catch (Exception e) {
            log.warn("Error getting spellbook", e);
            return "NORMAL";
        }
    }
    
    private String getSelectedSpell()
    {
        try {
            // Check if a spell is selected for casting
            Widget spellWidget = client.getSelectedWidget();
            if (spellWidget != null && spellWidget.getName() != null) {
                String spellName = spellWidget.getName();
                if (spellName.contains("<col=")) {
                    // Extract spell name from colored text
                    spellName = spellName.replaceAll("<[^>]+>", "").trim();
                }
                log.debug("[SPELL-DEBUG] Selected spell: {}", spellName);
                return spellName;
            }
        } catch (Exception e) {
            log.warn("Error getting selected spell", e);
        }
        return null;
    }
    
    private String getAutocastSpell()
    {
        try {
            // Check autocast varbit (different for each spellbook)
            int spellbook = client.getVarbitValue(Varbits.SPELLBOOK);
            
            // Get autocast spell ID based on spellbook
            // Note: Need to find the correct var for autocast spell
            int autocastId = -1;
            // Autocast is typically stored in a varbit, not VarPlayer
            // This would need proper research to find the correct varbit
            // For now, we'll skip autocast detection
            
            if (autocastId > 0) {
                // Map autocast ID to spell name (simplified - would need full mapping)
                String spellName = mapAutocastIdToSpell(autocastId);
                log.debug("[SPELL-DEBUG] Autocast spell ID: {}, Name: {}", autocastId, spellName);
                return spellName;
            }
            
            // Check defensive casting mode
            boolean defensiveCasting = client.getVarbitValue(Varbits.DEFENSIVE_CASTING_MODE) == 1;
            if (defensiveCasting) {
                log.debug("[SPELL-DEBUG] Defensive casting mode enabled");
            }
        } catch (Exception e) {
            log.warn("Error getting autocast spell", e);
        }
        return null;
    }
    
    private String mapAutocastIdToSpell(int autocastId) {
        // Common autocast spell IDs (simplified mapping)
        switch (autocastId) {
            case 3: return "Wind Strike";
            case 5: return "Water Strike";
            case 7: return "Earth Strike";
            case 9: return "Fire Strike";
            case 11: return "Wind Bolt";
            case 13: return "Water Bolt";
            case 15: return "Earth Bolt";
            case 17: return "Fire Bolt";
            case 19: return "Wind Blast";
            case 21: return "Water Blast";
            case 23: return "Earth Blast";
            case 25: return "Fire Blast";
            case 27: return "Wind Wave";
            case 29: return "Water Wave";
            case 31: return "Earth Wave";
            case 33: return "Fire Wave";
            default: return "Spell_" + autocastId;
        }
    }
    
    // =============== CRITICAL MISSING UTILITY METHODS - FULLY RESTORED ===============
    
    /**
     * Thread-safe inventory JSON generation - CRITICAL for database compatibility
     */
    private String generateInventoryJsonOnClientThread(Item[] items) {
        log.debug("[CLIENT-THREAD-JSON] Generating inventory JSON on Client thread...");
        
        if (items == null || items.length == 0) {
            log.debug("[CLIENT-THREAD-JSON] No items, returning empty array");
            return "[]";
        }
        
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        int processedItems = 0;
        
        for (int i = 0; i < items.length && i < 28; i++) {
            Item item = items[i];
            if (item.getId() > 0) {
                if (!first) {
                    json.append(",");
                }
                
                String itemName;
                try {
                    if (itemManager != null) {
                        ItemComposition itemComp = itemManager.getItemComposition(item.getId());
                        if (itemComp != null) {
                            String resolvedName = itemComp.getName();
                            if (resolvedName != null && !resolvedName.trim().isEmpty()) {
                                itemName = resolvedName.replace("\"", "\\\""); // Escape quotes for JSON
                            } else {
                                itemName = isProblematicItemId(item.getId()) ? getKnownItemName(item.getId()) : "Empty_Name_" + item.getId();
                            }
                        } else {
                            log.debug("[CLIENT-THREAD-JSON] Null composition for ID: {}, using fallback", item.getId());
                            itemName = isProblematicItemId(item.getId()) ? getKnownItemName(item.getId()) : "Null_Composition_" + item.getId();
                        }
                    } else {
                        log.debug("[CLIENT-THREAD-JSON] ItemManager null, using fallback for ID: {}", item.getId());
                        itemName = isProblematicItemId(item.getId()) ? getKnownItemName(item.getId()) : "ItemManager_Null_" + item.getId();
                    }
                } catch (Exception e) {
                    log.debug("[CLIENT-THREAD-JSON] Exception resolving item name for ID {}: {}, using fallback", item.getId(), e.getMessage());
                    itemName = isProblematicItemId(item.getId()) ? getKnownItemName(item.getId()) : "Exception_" + item.getId();
                }
                
                json.append(String.format(
                    "{\"slot\":%d,\"id\":%d,\"quantity\":%d,\"name\":\"%s\"}", 
                    i, item.getId(), item.getQuantity(), itemName
                ));
                
                first = false;
                processedItems++;
            }
        }
        
        json.append("]");
        String result = json.toString();
        log.debug("[CLIENT-THREAD-JSON] JSON generation SUCCESSFUL - processed {} items, returning string length: {}", 
            processedItems, result.length());
        
        return result;
    }
    
    /**
     * Get known item name for problematic items that cause ItemManager hanging
     */
    private String getKnownItemName(int itemId) {
        // Known Barrows items that cause hanging
        if (itemId >= 4856 && itemId <= 4956) {
            return "Barrows item (degraded_" + itemId + ")";
        } else if (itemId >= 4708 && itemId <= 4759) {
            return "Barrows item (new_" + itemId + ")";
        }
        return "Item_" + itemId;
    }
    
    /**
     * Check if item ID is problematic and causes ItemManager to hang
     */
    private boolean isProblematicItemId(int itemId) {
        // Known problematic items that cause ItemManager to hang or timeout
        return (itemId >= 4856 && itemId <= 4956) || // Degraded Barrows
               (itemId >= 4708 && itemId <= 4759);   // New Barrows
    }
}