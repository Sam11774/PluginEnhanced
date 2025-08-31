/*
 * Copyright (c) 2024, RuneLiteAI Team  
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.runelite.api.Item;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;

import java.util.List;
import java.util.Map;

/**
 * Collection of data structure classes for RuneLiteAI Plugin
 * 
 * This file contains all the supporting data structures used throughout
 * the plugin for organizing and storing collected data.
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
public class DataStructures
{
    // ===== PLAYER DATA STRUCTURES =====
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerData
    {
        private String playerName;
        private Integer combatLevel;
        private Integer worldX;
        private Integer worldY;
        private Integer plane;
        private Integer animation;
        private Integer poseAnimation;
        private Integer healthRatio;
        private Integer healthScale;
        private String overhead;
        private String skullIcon;
        private Integer team;
        private Boolean isFriend;
        private Boolean isClanMember;
        private Boolean isFriendsChatMember;
        
        public int getDataPointCount() { return 15; }
        public long getEstimatedSize() { return 64 + (16 * 10) + (8 * 3) + (playerName != null ? playerName.length() * 2 : 0); }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerVitals
    {
        private Integer currentHitpoints;
        private Integer maxHitpoints;
        private Integer currentPrayer;
        private Integer maxPrayer;
        private Integer energy;
        private Integer weight;
        private Integer specialAttackPercent;
        private Boolean poisoned;
        private Boolean diseased;
        private Boolean venomed;
        
        public int getDataPointCount() { return 10; }
        public long getEstimatedSize() { return 64 + (16 * 7) + (8 * 3); }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerLocation
    {
        private Integer worldX;
        private Integer worldY;
        private Integer plane;
        private Integer regionX;
        private Integer regionY;
        private Integer regionId;
        private Integer chunkX;
        private Integer chunkY;
        private String locationName;
        private String areaType;
        private Boolean inWilderness;
        private Integer wildernessLevel;
        private Boolean inPvp;
        private Boolean inMultiCombat;
        
        public int getDataPointCount() { return 14; }
        public long getEstimatedSize() { return 64 + (16 * 8) + (8 * 3) + 
            (locationName != null ? locationName.length() * 2 : 0) +
            (areaType != null ? areaType.length() * 2 : 0); }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerStats
    {
        private Map<String, Integer> currentLevels;
        private Map<String, Integer> realLevels;
        private Map<String, Integer> experience;
        private Integer totalLevel;
        private Integer combatLevel;
        private Long totalExperience;
        
        public int getDataPointCount() { 
            int count = 3; // totalLevel, combatLevel, totalExperience
            count += (currentLevels != null ? currentLevels.size() : 0);
            count += (realLevels != null ? realLevels.size() : 0);
            count += (experience != null ? experience.size() : 0);
            return count;
        }
        public long getEstimatedSize() { return 64 + (16 * 3) + 
            (currentLevels != null ? currentLevels.size() * 32 : 0) +
            (realLevels != null ? realLevels.size() * 32 : 0) +
            (experience != null ? experience.size() * 32 : 0); }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerEquipment
    {
        private Item[] equipmentItems;
        private Map<String, Integer> equipmentIds;
        private Integer weaponTypeId;
        private String weaponType;
        private String attackStyle;
        private Boolean autoRetaliate;
        private Integer combatStyle;
        
        // Individual equipment slot IDs (for database storage)
        private Integer helmetId;
        private Integer capeId;
        private Integer amuletId;
        private Integer weaponId;
        private Integer bodyId;
        private Integer shieldId;
        private Integer legsId;
        private Integer glovesId;
        private Integer bootsId;
        private Integer ringId;
        private Integer ammoId;
        
        // Individual equipment slot names (friendly name resolution)
        private String helmetName;
        private String capeName;
        private String amuletName;
        private String weaponName;
        private String bodyName;
        private String shieldName;
        private String legsName;
        private String glovesName;
        private String bootsName;
        private String ringName;
        private String ammoName;
        
        // Equipment analytics
        private Long totalEquipmentValue;
        private Integer equipmentWeight;
        
        // Equipment change detection
        private Integer equipmentChangesCount;
        private Boolean weaponChanged;
        private Boolean armorChanged;
        private Boolean accessoryChanged;
        
        // Equipment stats and bonuses
        private Integer attackSlashBonus;
        private Integer attackStabBonus;
        private Integer attackCrushBonus;
        private Integer attackMagicBonus;
        private Integer attackRangedBonus;
        private Integer defenseSlashBonus;
        private Integer defenseStabBonus;
        private Integer defenseCrushBonus;
        private Integer defenseMagicBonus;
        private Integer defenseRangedBonus;
        private Integer strengthBonus;
        private Integer rangedStrengthBonus;
        private Float magicDamageBonus;
        private Integer prayerBonus;
        
        public int getDataPointCount() { 
            int count = 47; // weaponTypeId, weaponType, attackStyle, autoRetaliate, combatStyle + 11 IDs + 11 names + totalEquipmentValue + equipmentWeight + equipmentChangesCount + weaponChanged + armorChanged + accessoryChanged + 14 equipment stat bonuses
            count += (equipmentItems != null ? equipmentItems.length : 0);
            count += (equipmentIds != null ? equipmentIds.size() : 0);
            return count;
        }
        public long getEstimatedSize() { return 64 + (16 * 47) + 
            (equipmentItems != null ? equipmentItems.length * 16 : 0) +
            (equipmentIds != null ? equipmentIds.size() * 32 : 0) +
            (weaponType != null ? weaponType.length() * 2 : 0) +
            (attackStyle != null ? attackStyle.length() * 2 : 0) +
            (helmetName != null ? helmetName.length() * 2 : 0) +
            (capeName != null ? capeName.length() * 2 : 0) +
            (amuletName != null ? amuletName.length() * 2 : 0) +
            (weaponName != null ? weaponName.length() * 2 : 0) +
            (bodyName != null ? bodyName.length() * 2 : 0) +
            (shieldName != null ? shieldName.length() * 2 : 0) +
            (legsName != null ? legsName.length() * 2 : 0) +
            (glovesName != null ? glovesName.length() * 2 : 0) +
            (bootsName != null ? bootsName.length() * 2 : 0) +
            (ringName != null ? ringName.length() * 2 : 0) +
            (ammoName != null ? ammoName.length() * 2 : 0); }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerInventory
    {
        private Item[] inventoryItems;
        private Integer usedSlots;
        private Integer freeSlots;
        private Long totalValue;
        private Map<Integer, Integer> itemCounts;
        private String lastItemUsed;
        private Integer lastItemId;
        
        // Most valuable item tracking
        private Integer mostValuableItemId;
        private String mostValuableItemName;
        private Integer mostValuableItemQuantity;
        private Long mostValuableItemValue;
        
        // Change tracking fields (match database schema)
        private Integer itemsAdded;
        private Integer itemsRemoved;
        private Integer quantityGained;
        private Integer quantityLost;
        private Long valueGained;
        private Long valueLost;
        
        // Noted items tracking
        private Integer notedItemsCount;
        
        // Pre-resolved inventory JSON (resolved on Client thread where ItemManager works)
        private String inventoryJson;
        
        public int getDataPointCount() {
            int count = 17; // usedSlots, freeSlots, totalValue, lastItemUsed, lastItemId, mostValuableItemId, mostValuableItemName, mostValuableItemQuantity, mostValuableItemValue, itemsAdded, itemsRemoved, quantityGained, quantityLost, valueGained, valueLost, notedItemsCount, inventoryJson
            count += (inventoryItems != null ? inventoryItems.length : 0);
            count += (itemCounts != null ? itemCounts.size() : 0);
            return count;
        }
        public long getEstimatedSize() { return 64 + (17 * 16) + 
            (inventoryItems != null ? inventoryItems.length * 16 : 0) +
            (itemCounts != null ? itemCounts.size() * 32 : 0) +
            (lastItemUsed != null ? lastItemUsed.length() * 2 : 0) +
            (mostValuableItemName != null ? mostValuableItemName.length() * 2 : 0) +
            (inventoryJson != null ? inventoryJson.length() * 2 : 0); }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerActivePrayers
    {
        private Map<String, Boolean> activePrayers;
        private Integer activePrayerCount;
        private Integer prayerDrainRate;
        private String quickPrayerSet;
        private Boolean quickPrayerActive;
        
        public int getDataPointCount() {
            int count = 4; // activePrayerCount, prayerDrainRate, quickPrayerSet, quickPrayerActive
            count += (activePrayers != null ? activePrayers.size() : 0);
            return count;
        }
        public long getEstimatedSize() { return 64 + (16 * 4) + 
            (activePrayers != null ? activePrayers.size() * 24 : 0) +
            (quickPrayerSet != null ? quickPrayerSet.length() * 2 : 0); }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerActiveSpells
    {
        private String selectedSpell;
        private String spellbook;
        private String lastCastSpell;
        private Boolean autocastEnabled;
        private String autocastSpell;
        
        // Rune pouch item IDs (for database storage)
        private Integer runePouch1;
        private Integer runePouch2;
        private Integer runePouch3;
        
        // Rune pouch item names (friendly name resolution)
        private String runePouch1Name;
        private String runePouch2Name;
        private String runePouch3Name;
        
        public int getDataPointCount() { return 11; } // 8 original + 3 new names
        public long getEstimatedSize() { return 64 + (16 * 7) + (8 * 1) +
            (selectedSpell != null ? selectedSpell.length() * 2 : 0) +
            (spellbook != null ? spellbook.length() * 2 : 0) +
            (lastCastSpell != null ? lastCastSpell.length() * 2 : 0) +
            (autocastSpell != null ? autocastSpell.length() * 2 : 0) +
            (runePouch1Name != null ? runePouch1Name.length() * 2 : 0) +
            (runePouch2Name != null ? runePouch2Name.length() * 2 : 0) +
            (runePouch3Name != null ? runePouch3Name.length() * 2 : 0); }
    }
    
    // ===== WORLD DATA STRUCTURES =====
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorldEnvironmentData
    {
        private Integer plane;
        private Integer baseX;
        private Integer baseY;
        private Integer[] mapRegions;
        private String currentRegion;
        private Integer nearbyPlayerCount;
        private Integer nearbyNPCCount;
        private Integer gameObjectCount;
        private Integer groundItemCount;
        private String weatherCondition;
        private Integer lightLevel;
        private Long worldTick;
        private String environmentType;
        
        // FIXED: Added missing fields for database schema compatibility
        private Integer regionId;
        private Integer chunkX;
        private Integer chunkY;
        private String lightingCondition;
        
        public int getDataPointCount() { 
            int count = 15; // primitive/string fields (added environmentType + regionId, chunkX, chunkY, lightingCondition)
            count += (mapRegions != null ? mapRegions.length : 0);
            return count;
        }
        public long getEstimatedSize() { return 64 + (16 * 14) + 
            (mapRegions != null ? mapRegions.length * 4 : 0) +
            (currentRegion != null ? currentRegion.length() * 2 : 0) +
            (weatherCondition != null ? weatherCondition.length() * 2 : 0) +
            (lightingCondition != null ? lightingCondition.length() * 2 : 0); }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NearbyPlayersData
    {
        private List<PlayerData> players;
        private Integer playerCount;
        private Integer friendCount;
        private Integer clanCount;
        private Integer pkCount;
        private Integer averageCombatLevel;
        private String mostCommonActivity;
        
        public int getDataPointCount() {
            int count = 6; // primitive fields
            count += (players != null ? players.size() * 15 : 0); // 15 data points per player
            return count;
        }
        public long getEstimatedSize() { return 64 + (16 * 6) + 
            (players != null ? players.size() * 200 : 0) +
            (mostCommonActivity != null ? mostCommonActivity.length() * 2 : 0); }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NearbyNPCsData
    {
        private List<NPCData> npcs;
        private Integer npcCount;
        private Integer aggressiveNPCCount;
        private Integer combatNPCCount;
        private String mostCommonNPCType;
        private Integer averageNPCCombatLevel;
        
        public int getDataPointCount() {
            int count = 5; // primitive/string fields
            count += (npcs != null ? npcs.size() * 12 : 0); // 12 data points per NPC
            return count;
        }
        public long getEstimatedSize() { return 64 + (16 * 5) + 
            (npcs != null ? npcs.size() * 150 : 0) +
            (mostCommonNPCType != null ? mostCommonNPCType.length() * 2 : 0); }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NPCData
    {
        private Integer npcId;
        private String npcName;
        private Integer worldX;
        private Integer worldY;
        private Integer plane;
        private Integer animation;
        private Integer healthRatio;
        private Integer combatLevel;
        private Boolean isInteracting;
        private String targetName;
        private Integer orientation;
        private String npcType;
        
        public int getDataPointCount() { return 12; }
        public long getEstimatedSize() { return 64 + (16 * 8) + (8 * 1) +
            (npcName != null ? npcName.length() * 2 : 0) +
            (targetName != null ? targetName.length() * 2 : 0) +
            (npcType != null ? npcType.length() * 2 : 0); }
    }
    
    // ===== INPUT DATA STRUCTURES =====
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MouseInputData
    {
        private Integer mouseX;
        private Integer mouseY;
        private Integer mouseIdleTime;
        private Boolean leftButtonPressed;
        private Boolean rightButtonPressed;
        private Boolean middleButtonPressed;
        private Integer lastClickX;
        private Integer lastClickY;
        private Long lastClickTime;
        private String lastClickTarget;
        private Integer clickCount;
        private Double averageClickInterval;
        
        public int getDataPointCount() { return 12; }
        public long getEstimatedSize() { return 64 + (16 * 8) + (8 * 3) +
            (lastClickTarget != null ? lastClickTarget.length() * 2 : 0); }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KeyboardInputData
    {
        private Map<Integer, Boolean> pressedKeys;
        private String lastKeyPressed;
        private Long lastKeyTime;
        private Integer keyPressCount;
        private Integer activeKeysCount;
        private Double averageKeyInterval;
        private Boolean shiftPressed;
        private Boolean ctrlPressed;
        private Boolean altPressed;
        
        public int getDataPointCount() {
            int count = 8; // primitive/string fields (added activeKeysCount)
            count += (pressedKeys != null ? pressedKeys.size() : 0);
            return count;
        }
        public long getEstimatedSize() { return 64 + (16 * 7) + (8 * 3) +
            (pressedKeys != null ? pressedKeys.size() * 8 : 0) +
            (lastKeyPressed != null ? lastKeyPressed.length() * 2 : 0); }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CameraData
    {
        private Integer cameraX;
        private Integer cameraY;
        private Integer cameraZ;
        private Integer cameraPitch;
        private Integer cameraYaw;
        private Double minimapZoom;
        private Double cameraRotationRate;
        private String movementDirection;
        private Boolean significantChange;
        
        public int getDataPointCount() { return 9; }
        public long getEstimatedSize() { return 64 + (16 * 7) + (8 * 1) +
            (movementDirection != null ? movementDirection.length() * 2 : 0); }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MenuInteractionData
    {
        private Boolean menuOpen;
        private Integer menuEntryCount;
        private String[] menuOptions;
        private String lastMenuAction;
        private String lastMenuTarget;
        private Long lastMenuTime;
        private Integer menuInteractionCount;
        
        public int getDataPointCount() {
            int count = 6; // primitive/string fields
            count += (menuOptions != null ? menuOptions.length : 0);
            return count;
        }
        public long getEstimatedSize() { return 64 + (16 * 6) + (8 * 1) +
            (menuOptions != null ? menuOptions.length * 32 : 0) +
            (lastMenuAction != null ? lastMenuAction.length() * 2 : 0) +
            (lastMenuTarget != null ? lastMenuTarget.length() * 2 : 0); }
    }
    
    // ===== ADDITIONAL SUPPORTING CLASSES =====
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GameObjectsData
    {
        private List<GameObjectData> objects;
        private Integer objectCount;
        private String mostCommonObjectType;
        private Integer scanRadius;
        private Integer uniqueObjectTypes;
        private Map<String, Integer> objectTypeCounts;
        
        // New fields for object interaction tracking
        private Integer interactableObjectsCount;
        private Integer closestObjectDistance;
        private Integer closestObjectId;
        private String closestObjectName;
        
        // ENHANCED: Distance analytics for objects
        private Integer closestBankDistance;
        private String closestBankName;
        private Integer closestAltarDistance;
        private String closestAltarName;
        private Integer closestShopDistance;
        private String closestShopName;
        private Integer lastClickedObjectDistance;
        private String lastClickedObjectName;
        private Long timeSinceLastObjectClick;
        
        public int getDataPointCount() {
            int count = 15; // Base fields + enhanced distance analytics fields
            count += (objects != null ? objects.size() * 8 : 0);
            count += (objectTypeCounts != null ? objectTypeCounts.size() : 0);
            return count;
        }
        public long getEstimatedSize() { return 64 + (16 * 7) + (8 * 1) + 
            (objects != null ? objects.size() * 100 : 0) +
            (objectTypeCounts != null ? objectTypeCounts.size() * 32 : 0) +
            (mostCommonObjectType != null ? mostCommonObjectType.length() * 2 : 0) +
            (closestObjectName != null ? closestObjectName.length() * 2 : 0); }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GameObjectData
    {
        private Integer objectId;
        private String objectName;
        private Integer worldX;
        private Integer worldY;
        private Integer plane;
        private String objectType;
        private Boolean interactable;
        private Integer orientation;
        
        public int getDataPointCount() { return 8; }
        public long getEstimatedSize() { return 64 + (16 * 6) + (8 * 1) +
            (objectName != null ? objectName.length() * 2 : 0) +
            (objectType != null ? objectType.length() * 2 : 0); }
    }
    
    // ===== GROUND ITEMS DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GroundItemsData {
        private Integer totalItems;
        private Integer totalQuantity;
        private List<GroundItemData> items;
        private Long totalValue;
        private String mostValuableItem;
        private Integer uniqueItemTypes;
        private Integer scanRadius;
        private List<GroundItemData> groundItems;
        
        // ENHANCED: Distance analytics for ground items
        private Integer closestItemDistance;
        private String closestItemName;
        private Integer closestValuableItemDistance; // Items >1000gp
        private String closestValuableItemName;
        private Integer myDropsCount; // Items dropped by this player
        private Long myDropsTotalValue;
        private Integer otherPlayerDropsCount;
        private Long shortestDespawnTimeMs; // Time until next item despawns
        private String nextDespawnItemName;
        
        public int getDataPointCount() { 
            int count = 15; // Base fields + enhanced distance analytics fields  
            count += (items != null ? items.size() * 6 : 0); // 6 data points per ground item
            count += (groundItems != null ? groundItems.size() * 6 : 0); // additional ground items
            return count;
        }
        public long getEstimatedSize() { 
            return 64 + (16 * 5) + 
                (items != null ? items.size() * 100 : 0) +
                (groundItems != null ? groundItems.size() * 100 : 0) +
                (mostValuableItem != null ? mostValuableItem.length() * 2 : 0);
        }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GroundItemData {
        private Integer itemId;
        private String itemName;
        private Integer quantity;
        private Integer itemValue; // Individual item value
        private Integer worldX;
        private Integer worldY;
        private Integer plane;
        
        // Computed total value for this stack
        public Integer getTotalValue() {
            if (itemValue != null && quantity != null) {
                return itemValue * quantity;
            }
            return 0;
        }
        
        public int getDataPointCount() { return 7; }
        public long getEstimatedSize() { 
            return 64 + (16 * 5) + 
                (itemName != null ? itemName.length() * 2 : 0);
        }
    }
    
    // ===== PROJECTILES DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProjectilesData {
        private List<ProjectileData> projectiles;
        private Integer activeProjectileCount;
        private String mostCommonProjectileType;
        private Integer activeProjectiles;
        private Integer uniqueProjectileTypes;
        
        // ENHANCED: Combat vs Magic projectile classification (for database storage)
        private Integer mostCommonProjectileId;
        private Integer combatProjectiles;
        private Integer magicProjectiles;
        
        public int getDataPointCount() { 
            int count = 5; // activeProjectiles, uniqueProjectileTypes, mostCommonProjectileType, combatProjectiles, magicProjectiles
            count += (projectiles != null ? projectiles.size() * 8 : 0); // 8 data points per projectile
            return count;
        }
        public long getEstimatedSize() { 
            return 64 + (16 * 5) + 
                (projectiles != null ? projectiles.size() * 120 : 0) +
                (mostCommonProjectileType != null ? mostCommonProjectileType.length() * 2 : 0);
        }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProjectileData {
        private Integer projectileId;
        private String projectileType;
        private Integer startX;
        private Integer startY;
        private Integer endX;
        private Integer endY;
        private Integer remainingCycles;
        private Integer slope;
        
        public int getDataPointCount() { return 8; }
        public long getEstimatedSize() { 
            return 64 + (16 * 7) + 
                (projectileType != null ? projectileType.length() * 2 : 0);
        }
    }
    
    // ===== COMBAT DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CombatData {
        private Boolean inCombat;
        private Boolean isAttacking;
        private String targetName;
        private String targetType;
        private Integer targetCombatLevel;
        private Integer currentAnimation;
        private String weaponType;
        private String attackStyle;
        private Integer specialAttackPercent;
        private String combatState;
        private Long lastAttackTime;
        private Integer damageDealt;
        private Long lastCombatTick;
        
        public int getDataPointCount() { return 13; }
        public long getEstimatedSize() { 
            return 64 + (16 * 8) + (8 * 1) +
                (targetName != null ? targetName.length() * 2 : 0) +
                (targetType != null ? targetType.length() * 2 : 0) +
                (weaponType != null ? weaponType.length() * 2 : 0) +
                (attackStyle != null ? attackStyle.length() * 2 : 0) +
                (combatState != null ? combatState.length() * 2 : 0);
        }
    }
    
    // ===== HITSPLAT DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HitsplatData {
        private Integer totalRecentDamage;
        private Integer maxRecentHit;
        private Integer hitCount;
        private String lastHitType;
        private Long lastHitTime;
        private Integer averageHit;
        private List<Integer> recentHits;
        private List<HitsplatApplied> recentHitsplats;
        private Double averageDamage;
        
        public int getDataPointCount() {
            int count = 7; // totalRecentDamage, maxRecentHit, hitCount, lastHitType, lastHitTime, averageHit, averageDamage
            count += (recentHits != null ? recentHits.size() : 0);
            count += (recentHitsplats != null ? recentHitsplats.size() * 3 : 0); // approximate 3 data points per hitsplat
            return count;
        }
        public long getEstimatedSize() { 
            return 64 + (16 * 6) +
                (lastHitType != null ? lastHitType.length() * 2 : 0) +
                (recentHits != null ? recentHits.size() * 4 : 0) +
                (recentHitsplats != null ? recentHitsplats.size() * 64 : 0);
        }
    }
    
    // ===== ANIMATION DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AnimationData {
        private Integer currentAnimation;
        private String animationName;  // Friendly name from RuneLite AnimationID constants
        private String animationType;
        private Integer animationDuration;
        private Long animationStartTime;
        private String lastAnimation;
        private Integer animationChangeCount;
        private Integer poseAnimation;
        private List<Integer> recentAnimations;
        
        public int getDataPointCount() { return 9; }  // Updated for animationName field
        public long getEstimatedSize() { 
            return 64 + (16 * 5) + (8 * 1) +
                (animationName != null ? animationName.length() * 2 : 0) +
                (animationType != null ? animationType.length() * 2 : 0) +
                (lastAnimation != null ? lastAnimation.length() * 2 : 0);
        }
    }
    
    // ===== INTERACTION DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InteractionData {
        private String lastInteractionType;
        private String lastInteractionTarget;
        private Long lastInteractionTime;
        private Integer interactionCount;
        private String mostCommonInteraction;
        private Double averageInteractionInterval;
        private String currentTarget;
        private String interactionType;
        private List<InteractingChanged> recentInteractions;
        private String recentInteractionsJsonb; // ENHANCED: Rich JSONB with accurate timestamps and context
        
        public int getDataPointCount() { return 8; }
        public long getEstimatedSize() { 
            return 64 + (16 * 4) + (8 * 1) +
                (lastInteractionType != null ? lastInteractionType.length() * 2 : 0) +
                (lastInteractionTarget != null ? lastInteractionTarget.length() * 2 : 0) +
                (mostCommonInteraction != null ? mostCommonInteraction.length() * 2 : 0) +
                (currentTarget != null ? currentTarget.length() * 2 : 0);
        }
    }
    
    // ===== CHAT DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatData {
        private Integer totalMessageCount;
        private Integer publicChatCount;
        private Integer privateChatCount;
        private Integer clanChatCount;
        private Integer systemMessageCount;
        private Double averageMessageLength;
        private String mostActiveMessageType;
        private String lastMessage;
        private Long lastMessageTime;
        private Integer spamScore;
        private List<ChatMessage> recentMessages;
        private List<String> messageHistory;
        private Map<String, Integer> messageTypeCounts;
        
        public int getDataPointCount() { 
            int count = 10; // basic fields
            count += (recentMessages != null ? recentMessages.size() * 3 : 0); // approximate 3 data points per message
            count += (messageHistory != null ? messageHistory.size() : 0); // message history strings
            return count;
        }
        public long getEstimatedSize() { 
            return 64 + (16 * 7) + (8 * 1) +
                (mostActiveMessageType != null ? mostActiveMessageType.length() * 2 : 0) +
                (lastMessage != null ? lastMessage.length() * 2 : 0) +
                (recentMessages != null ? recentMessages.size() * 128 : 0);
        }
    }
    
    // ===== CLAN DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ClanData {
        private String clanName;
        private Integer clanMemberCount;
        private Integer onlineClanMembers;
        private String clanRank;
        private Boolean inClanChannel;
        private String lastClanMessage;
        private Long lastClanActivity;
        private Boolean inClan;
        private Integer memberCount;
        
        public int getDataPointCount() { return 9; }
        public long getEstimatedSize() { 
            return 64 + (16 * 5) + (8 * 1) +
                (clanName != null ? clanName.length() * 2 : 0) +
                (clanRank != null ? clanRank.length() * 2 : 0) +
                (lastClanMessage != null ? lastClanMessage.length() * 2 : 0);
        }
    }
    
    // ===== TRADE DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TradeData {
        private Boolean inTrade;
        private String tradePartner;
        private Integer itemsOffered;
        private Integer itemsRequested;
        private Long totalValueOffered;
        private Long totalValueRequested;
        private String tradeState;
        private Long tradeStartTime;
        private String tradeStage;
        private Long totalOffered;
        private List<TradeItem> playerOfferedItems;
        private List<TradeItem> partnerOfferedItems;
        private Long tradeValue;
        
        public int getDataPointCount() { return 10; }
        public long getEstimatedSize() { 
            return 64 + (16 * 7) + (8 * 1) +
                (tradePartner != null ? tradePartner.length() * 2 : 0) +
                (tradeState != null ? tradeState.length() * 2 : 0) +
                (tradeStage != null ? tradeStage.length() * 2 : 0);
        }
    }
    
    // ===== INTERFACE DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InterfaceData {
        private Integer totalOpenInterfaces;
        private String primaryInterface;
        private Boolean chatboxOpen;
        private Boolean inventoryOpen;
        private Boolean skillsInterfaceOpen;
        private Boolean questInterfaceOpen;
        private Boolean settingsInterfaceOpen;
        private String currentInterfaceTab; // ENHANCED: Added interface tab tracking
        private Integer interfaceInteractionCount; // ENHANCED: Added interface interaction counting
        private String interfaceClickCorrelation; // ENHANCED: Correlation with click_context data
        private List<String> openInterfaces;
        private String lastInterfaceOpened;
        private Long lastInterfaceTime;
        private List<Item> inventoryItems;
        private List<Item> equipmentItems;
        private List<Integer> visibleWidgets;
        
        public int getDataPointCount() {
            int count = 10; // totalOpenInterfaces, primaryInterface, boolean fields, currentInterfaceTab, interfaceInteractionCount, lastInterfaceOpened, lastInterfaceTime
            count += (openInterfaces != null ? openInterfaces.size() : 0);
            return count;
        }
        public long getEstimatedSize() { 
            return 64 + (16 * 9) + (8 * 1) + // Updated for currentInterfaceTab and interfaceInteractionCount fields
                (primaryInterface != null ? primaryInterface.length() * 2 : 0) +
                (currentInterfaceTab != null ? currentInterfaceTab.length() * 2 : 0) + // ENHANCED: Size for new field
                (openInterfaces != null ? openInterfaces.size() * 32 : 0) +
                (lastInterfaceOpened != null ? lastInterfaceOpened.length() * 2 : 0);
        }
    }
    
    // ===== DIALOGUE DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DialogueData {
        private Boolean dialogueOpen;
        private String dialogueType;
        private String speakerName;
        private String dialogueText;
        private Integer numberOfDialogueOptions;
        private String lastDialogueAction;
        private Long dialogueStartTime;
        private Boolean inDialogue;
        private List<String> dialogueOptions;
        private String npcName;
        
        public int getDataPointCount() { return 8; }
        public long getEstimatedSize() { 
            return 64 + (16 * 5) + (8 * 1) +
                (dialogueType != null ? dialogueType.length() * 2 : 0) +
                (speakerName != null ? speakerName.length() * 2 : 0) +
                (dialogueText != null ? dialogueText.length() * 2 : 0) +
                (lastDialogueAction != null ? lastDialogueAction.length() * 2 : 0);
        }
    }
    
    // ===== SHOP DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ShopData {
        private Boolean shopOpen;
        private String shopName;
        private Integer availableItems;
        private Integer shopType;
        private Long totalShopValue;
        private List<Item> shopItems;
        private String lastItemPurchased;
        private String lastItemSold;
        private Long lastShopActivity;
        private Boolean inShop;
        
        public int getDataPointCount() { return 9; }
        public long getEstimatedSize() { 
            return 64 + (16 * 6) + (8 * 1) +
                (shopName != null ? shopName.length() * 2 : 0) +
                (lastItemPurchased != null ? lastItemPurchased.length() * 2 : 0) +
                (lastItemSold != null ? lastItemSold.length() * 2 : 0);
        }
    }
    
    // ===== BANK DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BankData {
        private Boolean bankOpen;
        private Integer totalUniqueItems;
        private Integer usedBankSlots;
        private Integer maxBankSlots;
        private Long totalBankValue;
        private Long lastModified;
        private String lastBankAction;
        private String mostValuableBankItem;
        private Long lastBankActivity;
        private List<BankItemData> bankItems; // ENHANCED: Changed from Item to BankItemData
        private Integer recentDeposits;
        private Integer recentWithdrawals;
        
        // ENHANCED: Advanced banking features
        private Integer currentTab;
        private String searchQuery;
        private String bankInterfaceType; // booth, chest, deposit_box
        private String lastDepositMethod; // 1, 5, 10, All, X
        private String lastWithdrawMethod; // 1, 5, 10, All, X
        private Integer bankLocationId;
        private Boolean searchActive;
        private Float bankOrganizationScore;
        private List<BankActionData> recentActions;
        private Integer tabSwitchCount;
        private Integer totalDeposits;
        private Integer totalWithdrawals;
        private Long timeSpentInBank;
        
        // ENHANCED: Noted items and placeholder tracking
        private Integer notedItemsCount; // Count of noted items in bank
        
        public int getDataPointCount() { 
            int count = 22; // enhanced fields count (added placeholder and noted item tracking)
            count += (bankItems != null ? bankItems.size() * 13 : 0); // 13 data points per bank item (updated)
            count += (recentActions != null ? recentActions.size() * 6 : 0); // 6 data points per action
            return count;
        }
        public long getEstimatedSize() { 
            return 64 + (16 * 15) + (8 * 5) +
                (lastBankAction != null ? lastBankAction.length() * 2 : 0) +
                (mostValuableBankItem != null ? mostValuableBankItem.length() * 2 : 0) +
                (searchQuery != null ? searchQuery.length() * 2 : 0) +
                (bankInterfaceType != null ? bankInterfaceType.length() * 2 : 0) +
                (bankItems != null ? bankItems.size() * 64 : 0) +
                (recentActions != null ? recentActions.size() * 48 : 0);
        }
    }
    
    /**
     * ENHANCED: Individual bank item data with position and metadata
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BankItemData {
        private Integer itemId;
        private String itemName;
        private Integer quantity;
        private Long itemValue;
        private Integer slotPosition;
        private Integer tabNumber;
        private Integer coordinateX;
        private Integer coordinateY;
        private Boolean isNoted;
        private Boolean isStackable;
        private Boolean isPlaceholder; // ENHANCED: Detect placeholder items (quantity = 0)
        private String category; // weapon, armor, food, potion, etc.
        private Integer gePrice; // Grand Exchange price if available
        
        public int getDataPointCount() { return 13; } // Updated count
        public long getEstimatedSize() { 
            return 64 + (16 * 8) + 
                (itemName != null ? itemName.length() * 2 : 0) +
                (category != null ? category.length() * 2 : 0);
        }
    }
    
    /**
     * ENHANCED: Bank action tracking for transaction history and behavioral analysis
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BankActionData {
        private String actionType; // deposit, withdraw, search, tab_switch, pin_entry
        private Integer itemId;
        private String itemName;
        private Integer quantity;
        private String methodUsed; // 1, 5, 10, All, X, search_query
        private Long actionTimestamp;
        private Integer fromTab;
        private Integer toTab;
        private String searchQuery;
        private Integer durationMs; // Time taken for action
        private Boolean isNoted; // true if this action involved noted items
        
        public int getDataPointCount() { return 11; }
        public long getEstimatedSize() { 
            return 64 + (16 * 6) + 
                (actionType != null ? actionType.length() * 2 : 0) +
                (itemName != null ? itemName.length() * 2 : 0) +
                (methodUsed != null ? methodUsed.length() * 2 : 0) +
                (searchQuery != null ? searchQuery.length() * 2 : 0);
        }
    }
    
    // ===== SYSTEM METRICS =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SystemMetrics {
        private Integer usedMemoryMB;
        private Integer maxMemoryMB;
        private Double memoryUsagePercent;
        private Double cpuUsagePercent;
        private Integer clientFPS;
        private Boolean clientFocused;
        private Boolean clientResized;
        private Integer gameState;
        private Integer freeMemoryMB;
        private Long gcCount;
        private Long gcTime;
        private Long uptime;
        private Integer threadCount;
        private Double averageTickTime;
        private Integer totalMemoryMB;
        private Integer heapUsedMB;
        
        public int getDataPointCount() { return 14; }
        public long getEstimatedSize() { 
            return 64 + (16 * 10) + (8 * 4);
        }
    }
    
    // ===== CLICK CONTEXT DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ClickContextData
    {
        // Core Click Information
        private String clickType;           // "LEFT", "RIGHT", "MENU"
        private String menuAction;          // From MenuAction enum
        private String menuOption;          // "Attack", "Use", "Examine", etc.
        private String menuTarget;          // Target name from RuneLite
        
        // Target Classification  
        private String targetType;          // "GAME_OBJECT", "NPC", "GROUND_ITEM", "INVENTORY_ITEM", "MENU", "PLAYER"
        private Integer targetId;           // Item/NPC/Object ID
        private String targetName;          // Resolved name from ItemManager/APIs
        
        // Coordinates & Context
        private Integer screenX;            // Click screen coordinates
        private Integer screenY;            // Click screen coordinates  
        private Integer worldX;             // World coordinates if applicable
        private Integer worldY;             // World coordinates if applicable
        private Integer plane;              // Game plane level
        
        // Additional Context
        private Boolean isPlayerTarget;     // If clicking another player
        private Boolean isEnemyTarget;      // If clicking hostile NPC
        private String widgetInfo;          // Interface/menu context
        private Long clickTimestamp;        // Precise timing
        private Integer param0;             // MenuEntry param0
        private Integer param1;             // MenuEntry param1
        
        // Item-specific context
        private Integer itemId;             // Item ID if item click
        private String itemName;            // Item name if item click
        private Integer itemOp;             // Item operation (1-5) if item click
        private Boolean isItemOp;           // Whether this is an item operation
        
        public int getDataPointCount() { return 22; }
        public long getEstimatedSize() { 
            return 64 + (16 * 11) + (8 * 5) +
                (menuAction != null ? menuAction.length() * 2 : 0) +
                (menuOption != null ? menuOption.length() * 2 : 0) +
                (menuTarget != null ? menuTarget.length() * 2 : 0) +
                (targetType != null ? targetType.length() * 2 : 0) +
                (targetName != null ? targetName.length() * 2 : 0) +
                (widgetInfo != null ? widgetInfo.length() * 2 : 0) +
                (itemName != null ? itemName.length() * 2 : 0);
        }
    }
    
    // ===== ERROR DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorData {
        private String errorType;
        private String errorMessage;
        private String errorSource;
        private Long errorTime;
        private Integer errorCount;
        private List<String> errorMessages;
        private String errorSeverity;
        private Boolean recovered;
        private Integer totalErrors;
        private Map<String, Integer> errorCounts;
        private Double errorRate;
        private String mostCommonError;
        
        public int getDataPointCount() { return 10; }
        public long getEstimatedSize() { 
            return 64 + (16 * 7) + (8 * 1) +
                (errorType != null ? errorType.length() * 2 : 0) +
                (errorMessage != null ? errorMessage.length() * 2 : 0) +
                (errorSource != null ? errorSource.length() * 2 : 0) +
                (errorSeverity != null ? errorSeverity.length() * 2 : 0) +
                (mostCommonError != null ? mostCommonError.length() * 2 : 0);
        }
    }
    
    // ===== TIMING BREAKDOWN =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimingBreakdown {
        private Long totalProcessingTime;
        private Long playerDataTime;
        private Long worldDataTime;
        private Long combatDataTime;
        private Long inputDataTime;
        private Map<String, Double> timingPercentages;
        private Long databaseWriteTime;
        private Long qualityValidationTime;
        private Long behavioralAnalysisTime;
        private Double averageTickTime;
        private Integer slowTickCount;
        private Double performanceScore;
        private Map<String, Long> componentTimings;
        private Long totalProcessingTimeNanos;
        
        public int getDataPointCount() { 
            int count = 11; // basic fields
            count += (componentTimings != null ? componentTimings.size() : 0);
            return count;
        }
        public long getEstimatedSize() { 
            return 64 + (16 * 9) + (8 * 2) +
                (componentTimings != null ? componentTimings.size() * 32 : 0);
        }
    }
    
    // ===== TRADE ITEM DATA =====
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TradeItem {
        private Integer itemId;
        private String itemName;
        private Integer quantity;
        private Long value;
        
        public int getDataPointCount() { return 4; }
        public long getEstimatedSize() { 
            return 64 + (16 * 3) + 
                (itemName != null ? itemName.length() * 2 : 0);
        }
    }
    
    /**
     * Enhanced keyboard tracking - Individual key press details
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KeyPressData
    {
        private Integer keyCode;              // Java KeyEvent key code
        private String keyName;               // Friendly name (F1, SPACE, W, etc.)
        private String keyChar;               // Actual character if printable
        
        // Timing details
        private Long pressTimestamp;          // Precise press time in milliseconds
        private Long releaseTimestamp;        // Precise release time (NULL if still held)
        private Integer durationMs;           // How long key was held
        
        // Context flags
        private Boolean isFunctionKey;        // F1-F12 keys
        private Boolean isModifierKey;        // Ctrl, Alt, Shift
        private Boolean isMovementKey;        // WASD, arrow keys
        private Boolean isActionKey;          // Space, Enter, etc.
        
        // Modifier states at press time
        private Boolean ctrlHeld;
        private Boolean altHeld;
        private Boolean shiftHeld;
        
        public int getDataPointCount() { return 13; }
        public long getEstimatedSize() { 
            return 64 + (13 * 8) + 
                (keyName != null ? keyName.length() * 2 : 0) +
                (keyChar != null ? keyChar.length() * 2 : 0);
        }
    }
    
    /**
     * Enhanced mouse button tracking - All mouse buttons with timing
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MouseButtonData
    {
        private String buttonType;            // LEFT, RIGHT, MIDDLE
        private Integer buttonCode;           // Mouse button code (1=left, 2=middle, 3=right)
        
        // Timing details
        private Long pressTimestamp;          // Precise press time in milliseconds
        private Long releaseTimestamp;        // Precise release time (NULL if still held)
        private Integer durationMs;           // How long button was held
        
        // Position at press/release
        private Integer pressX;               // Screen X at press
        private Integer pressY;               // Screen Y at press
        private Integer releaseX;             // Screen X at release
        private Integer releaseY;             // Screen Y at release
        
        // Context flags
        private Boolean isClick;              // Short press/release (< 500ms)
        private Boolean isDrag;               // Movement while held
        private Boolean isCameraRotation;     // Middle mouse camera control
        
        // Camera rotation details (for middle mouse)
        private Integer cameraStartPitch;     // Camera pitch at start
        private Integer cameraStartYaw;       // Camera yaw at start
        private Integer cameraEndPitch;       // Camera pitch at end
        private Integer cameraEndYaw;         // Camera yaw at end
        private Double rotationDistance;      // Total rotation amount
        
        public int getDataPointCount() { return 17; }
        public long getEstimatedSize() { 
            return 64 + (17 * 8) + 
                (buttonType != null ? buttonType.length() * 2 : 0);
        }
    }
    
    /**
     * Key combination tracking - Hotkeys and key combinations
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KeyCombinationData
    {
        private String keyCombination;        // "Ctrl+C", "Alt+Tab", "Shift+F1", etc.
        private Integer primaryKeyCode;       // Main key in combination
        private java.util.List<Integer> modifierKeys; // Array of modifier key codes
        
        // Timing
        private Long combinationTimestamp;
        private Integer durationMs;
        
        // Classification
        private String combinationType;       // HOTKEY, SHORTCUT, FUNCTION, MOVEMENT
        private Boolean isGameHotkey;         // F1-F12 game functions
        private Boolean isSystemShortcut;     // Alt+Tab, etc.
        
        public int getDataPointCount() { return 8; }
        public long getEstimatedSize() { 
            return 64 + (8 * 8) + 
                (keyCombination != null ? keyCombination.length() * 2 : 0) +
                (combinationType != null ? combinationType.length() * 2 : 0) +
                (modifierKeys != null ? modifierKeys.size() * 4 : 0);
        }
    }
    
    /**
     * Enhanced input data collection - Contains all input tracking
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EnhancedInputData
    {
        // Current tick input summary
        private Integer totalKeyPresses;      // Total key presses this tick
        private Integer totalMouseClicks;     // Total mouse clicks this tick
        private Integer totalKeyCombinations; // Total key combinations this tick
        
        // Active states
        private Integer activeKeys;           // Currently held keys
        private Integer activeMouseButtons;   // Currently held mouse buttons
        
        // Detailed tracking collections
        private java.util.List<KeyPressData> keyPresses;
        private java.util.List<MouseButtonData> mouseButtons;
        private java.util.List<KeyCombinationData> keyCombinations;
        
        // Camera rotation detection
        private Boolean cameraRotationActive; // Middle mouse camera control
        private Double cameraRotationAmount;   // Total rotation this tick
        
        public int getDataPointCount() { 
            int base = 7;
            base += (keyPresses != null ? keyPresses.size() * 13 : 0);
            base += (mouseButtons != null ? mouseButtons.size() * 17 : 0);
            base += (keyCombinations != null ? keyCombinations.size() * 8 : 0);
            return base;
        }
        
        public long getEstimatedSize() { 
            long size = 64 + (7 * 8);
            size += (keyPresses != null ? keyPresses.stream().mapToLong(KeyPressData::getEstimatedSize).sum() : 0);
            size += (mouseButtons != null ? mouseButtons.stream().mapToLong(MouseButtonData::getEstimatedSize).sum() : 0);
            size += (keyCombinations != null ? keyCombinations.stream().mapToLong(KeyCombinationData::getEstimatedSize).sum() : 0);
            return size;
        }
    }

    /**
     * Comprehensive Click Intelligence - Full contextual analysis of player clicks
     * Combines click data with environmental, tactical, and predictive intelligence
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ClickIntelligence
    {
        // ===== ENHANCED CLICK DATA =====
        private String clickType;                // LEFT_CLICK, RIGHT_CLICK, DRAG, etc.
        private String menuAction;               // WALK, CC_OP, NPC_FIRST_OPTION, etc.
        private String menuOption;               // "Attack", "Walk here", "Withdraw-All"
        private String menuTarget;               // Original menu target text
        
        // Target Resolution (API-driven)
        private String targetType;               // NPC, GAME_OBJECT, INTERFACE, WALK, etc.
        private Integer targetId;                // NPC ID, Object ID, Item ID, etc.
        private String targetName;               // API-resolved name (not hardcoded)
        private String targetDescription;        // Additional context from API
        
        // Coordinates & Positioning
        private Integer screenX;                 // Screen click coordinates
        private Integer screenY;
        private Integer worldX;                  // World coordinates
        private Integer worldY;
        private Integer plane;                   // Game plane
        
        // Item Context (if item interaction)
        private Integer itemId;                  // Item being interacted with
        private String itemName;                 // API-resolved item name
        private Boolean isItemOperation;         // Was this an item-based action
        private Integer itemSlot;                // Inventory/equipment slot
        
        // Timing Intelligence
        private Long clickTimestamp;             // Precise click timestamp
        private Integer clickDurationMs;         // How long the click lasted
        private Integer tickNumber;              // Game tick when click occurred
        
        // ===== PLAYER STATE CONTEXT =====
        private PlayerStateContext playerState;
        
        // ===== ENVIRONMENTAL INTELLIGENCE =====
        private EnvironmentalContext environment;
        
        // ===== TIMING CORRELATIONS =====
        private TimingCorrelations timing;
        
        // ===== INVENTORY CONTEXT =====
        private InventoryContext inventory;
        
        // ===== TACTICAL ASSESSMENT =====
        private TacticalAssessment tactical;
        
        // ===== PREDICTIVE ANALYTICS =====
        private PredictiveAnalytics predictions;
        
        public int getDataPointCount() { return 50; } // Comprehensive data points across all contexts
    }
    
    /**
     * Player state at the moment of click
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerStateContext
    {
        // Vitals
        private Integer currentHitpoints;
        private Integer maxHitpoints;
        private Integer currentPrayer;
        private Integer maxPrayer;
        private Integer energy;
        private Integer specialAttackPercent;
        
        // Status conditions
        private Boolean isPoisoned;
        private Boolean isDiseased;
        private Boolean isVenomed;
        
        // Equipment context
        private String weaponName;               // API-resolved weapon name
        private String weaponType;               // Sword, bow, staff, etc.
        private String attackStyle;              // Current attack style
        private Integer combatLevel;             // Total combat level
        private Long totalEquipmentValue;        // Total gear value
        
        // Current animation
        private Integer currentAnimation;        // Animation ID
        private String animationName;            // API-resolved animation name
        private String animationType;            // idle, combat, skilling, movement
        
        // Recent activity (last 5 ticks)
        private Boolean recentCombat;            // Was in combat recently
        private Boolean recentMovement;          // Was moving recently
        private Boolean recentBanking;           // Was banking recently
        private Boolean recentSkilling;          // Was skilling recently
    }
    
    /**
     * Environmental context surrounding the click
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EnvironmentalContext
    {
        // NPC Environment
        private Integer totalNpcCount;           // Total NPCs nearby
        private Integer aggressiveNpcCount;      // Aggressive NPCs nearby
        private Integer combatNpcCount;          // Combat-capable NPCs
        private String mostCommonNpcType;        // API-resolved most common NPC
        private Integer averageNpcCombatLevel;   // Average combat level of NPCs
        private java.util.List<NearbyNPC> nearbyNpcs; // Detailed NPC list
        
        // Object Environment  
        private Integer totalObjectCount;        // Interactive objects nearby
        private String mostCommonObjectType;     // API-resolved most common object
        private java.util.List<NearbyObject> nearbyObjects; // Detailed object list
        
        // Player Environment
        private Integer nearbyPlayerCount;       // Other players nearby
        private Boolean pvpArea;                 // Is this a PvP area
        private Boolean safeArea;                // Is this a safe area (bank, etc.)
        
        // Location Intelligence
        private Integer regionId;                // Current region
        private String locationName;             // API-resolved location name
        private String areaType;                 // BANK, COMBAT, SKILLING, WILDERNESS, etc.
    }
    
    /**
     * Timing correlations with other input events
     */
    @Data
    @Builder  
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimingCorrelations
    {
        // Mouse button correlation
        private String mouseButtonType;          // LEFT, RIGHT, MIDDLE
        private Integer mouseButtonDuration;     // Duration of mouse button press
        private Boolean perfectCoordinateMatch;  // Did mouse coords exactly match click coords
        
        // Recent keyboard activity (last 3 ticks)
        private java.util.List<String> recentKeys; // Keys pressed recently
        private Boolean recentHotkeys;           // F-keys pressed recently
        private String lastHotkeyPressed;        // Last F-key pressed
        private Integer timeSinceLastHotkey;     // Ms since last hotkey
        
        // Animation correlation
        private Boolean triggeredAnimation;      // Did click trigger animation
        private String resultingAnimation;       // What animation was triggered
        private Integer animationDelay;          // Delay between click and animation
        
        // Interface correlation
        private Boolean openedInterface;         // Did click open interface
        private Boolean closedInterface;         // Did click close interface
        private String interfaceChanged;         // Which interface changed
    }
    
    /**
     * Inventory context and changes
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InventoryContext
    {
        // Current inventory state
        private Integer totalItems;              // Total items in inventory
        private Integer freeSlots;               // Free inventory slots
        private Long totalValue;                 // Total inventory value
        private String mostValuableItem;         // API-resolved most valuable item name
        
        // Recent changes (this tick and previous)
        private Integer itemsAdded;              // Items gained this tick
        private Integer itemsRemoved;            // Items lost this tick
        private Integer quantityGained;          // Quantity gained this tick
        private Integer quantityLost;            // Quantity lost this tick
        private Long valueGained;                // Value gained this tick
        private Long valueLost;                  // Value lost this tick
        
        // Special states
        private Integer notedItemsCount;         // Count of noted items
        private Boolean inventoryFull;           // Is inventory full
        private Boolean hasConsumables;          // Has food/potions
        private Boolean hasCombatSupplies;       // Has arrows/runes/etc.
    }
    
    /**
     * Tactical assessment of the click situation
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TacticalAssessment
    {
        // Threat Analysis
        private String threatLevel;              // LOW, MEDIUM, HIGH, EXTREME
        private Integer threatsNearby;           // Number of potential threats
        private Integer escapeRoutes;            // Number of escape options
        private Boolean canTeleport;             // Player has teleport options
        
        // Resource Evaluation
        private String resourceState;            // ABUNDANT, ADEQUATE, LOW, CRITICAL
        private Boolean hasFood;                 // Has healing items
        private Boolean hasPotions;              // Has stat potions
        private Boolean hasSupplies;             // Has activity-specific supplies
        private Integer estimatedSupplyDuration; // How long supplies will last (ticks)
        
        // Strategic Position
        private String positionAssessment;       // OPTIMAL, GOOD, RISKY, DANGEROUS  
        private Boolean hasAdvantage;            // Tactical advantage present
        private String advantage;                // What advantage (RANGE, HEIGHT, NUMBERS, etc.)
        
        // Click Efficiency
        private String clickQuality;             // PERFECT, GOOD, AVERAGE, POOR
        private Boolean optimalTiming;           // Was click optimally timed
        private String inefficiency;             // What could be improved
    }
    
    /**
     * Predictive analytics based on current situation
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PredictiveAnalytics
    {
        // Immediate Predictions (next 1-5 ticks)
        private String likelyNextAction;         // ATTACK, MOVE, BANK, TELEPORT, etc.
        private Long predictedDuration;          // Expected duration of action in milliseconds
        private Double nextActionConfidence;     // 0.0-1.0 confidence score
        private java.util.List<String> possibleActions; // List of possible next actions
        
        // Outcome Predictions
        private Double successLikelihood;        // 0.0-1.0 success probability
        private String predictedOutcome;         // SUCCESS, FAILURE, INTERRUPTED, etc.
        private Double outcomeConfidence;        // 0.0-1.0 confidence score
        private String reasoningBasis;           // Why this prediction was made
        
        // Pattern Analysis
        private Double patternMatchConfidence;   // 0.0-1.0 confidence in pattern match
        private String behaviorClassification;   // COMBAT_TRAINING, RESOURCE_GATHERING, etc.
        private Double automationRisk;           // 0.0-1.0 likelihood of automation
        private String skillProgressionContext;  // Current skill training context
        private String questContext;             // Current quest context
        private String activityClassification;   // Primary activity classification
        
        // Learning and Optimization
        private String learningOpportunity;     // Identified learning opportunity
        private String optimizationSuggestion;  // Performance improvement suggestion
        private String warningFlags;            // Any warning flags detected
        private Double predictiveAccuracy;      // Historical accuracy of predictions
        private String historicalPatterns;      // Relevant historical patterns
        private String contextualInsights;      // Additional contextual insights
        
        // Risk Assessment
        private String riskLevel;                // MINIMAL, LOW, MODERATE, HIGH, EXTREME
        private java.util.List<String> riskFactors; // Specific risk factors identified
        private String recommendedAction;        // What should player do
        
        // Performance Assessment
        private String skillLevel;               // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
        private Double efficiencyScore;          // 0.0-1.0 efficiency rating
        private String improvementSuggestion;    // How to play better
        
        // Pattern Recognition
        private String behaviorPattern;          // COMBAT_TRAINING, RESOURCE_GATHERING, etc.
        private Boolean repeatAction;            // Is this part of repetitive activity
        private Integer sequencePosition;        // Position in action sequence (if applicable)
    }
    
    /**
     * Nearby NPC details with API resolution
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NearbyNPC
    {
        private Integer npcId;                   // RuneLite NPC ID
        private String npcName;                  // API-resolved NPC name
        private Integer combatLevel;             // NPC combat level
        private Integer worldX;                  // NPC world X coordinate
        private Integer worldY;                  // NPC world Y coordinate
        private Integer distance;                // Distance from player
        private Boolean isAggressive;            // Is NPC aggressive
        private Boolean isInteracting;           // Is NPC currently interacting
        private Integer currentAnimation;        // NPC current animation
        private String animationName;            // API-resolved animation name
    }
    
    /**
     * Nearby object details with API resolution
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NearbyObject
    {
        private Integer objectId;                // RuneLite Object ID
        private String objectName;               // API-resolved object name
        private String objectType;               // BANK, ALTAR, TREE, ROCK, etc.
        private Integer worldX;                  // Object world X coordinate
        private Integer worldY;                  // Object world Y coordinate
        private Integer distance;                // Distance from player
        private Boolean isInteractable;          // Can player interact with it
        private java.util.List<String> availableActions; // Available right-click options
    }
}