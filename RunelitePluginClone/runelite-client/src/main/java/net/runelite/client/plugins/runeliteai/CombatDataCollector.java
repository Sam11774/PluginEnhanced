/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.plugins.runeliteai.TickDataCollection.TickDataCollectionBuilder;
import net.runelite.api.coords.WorldPoint;

import java.util.*;
import java.util.stream.Collectors;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import static net.runelite.client.plugins.runeliteai.DataStructures.*;

/**
 * Dedicated collector for combat-related data
 * 
 * Responsible for:
 * - Combat events and damage tracking
 * - Hitsplat data analysis and classification
 * - Animation state tracking for combat actions
 * - Player and NPC interaction events
 * - Attack animation detection and type analysis
 * - Combat timing and duration estimation
 * 
 * Migrated from DataCollectionManager lines 2873-3664
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
public class CombatDataCollector
{
    // Core dependencies
    private final Client client;
    
    // Combat event tracking queues
    private final Queue<TimestampedHitsplat> recentHitsplats = new ConcurrentLinkedQueue<>();
    private final Queue<AnimationChanged> recentAnimationChanges = new ConcurrentLinkedQueue<>();
    private final Queue<TimestampedInteractionChanged> recentInteractionChanges = new ConcurrentLinkedQueue<>();
    
    public CombatDataCollector(Client client)
    {
        this.client = client;
        log.debug("CombatDataCollector initialized");
    }
    
    /**
     * Collect all combat-related data
     */
    public void collectCombatData(TickDataCollection.TickDataCollectionBuilder builder)
    {
        CombatData combatData = collectCombatEvents();
        builder.combatData(combatData);
        
        HitsplatData hitsplatData = collectHitsplatData();
        builder.hitsplatData(hitsplatData);
        
        AnimationData animationData = collectAnimationData();
        builder.animationData(animationData);
        
        InteractionData interactionData = collectInteractionData();
        builder.interactionData(interactionData);
    }
    
    /**
     * Collect combat events data
     */
    private CombatData collectCombatEvents()
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return CombatData.builder().build();
        }
        
        try {
            Actor interacting = localPlayer.getInteracting();
            String targetName = null;
            String targetType = null;
            Integer targetCombatLevel = null;
            
            if (interacting != null) {
                targetName = interacting.getName();
                if (interacting instanceof Player) {
                    targetType = "player";
                    targetCombatLevel = ((Player) interacting).getCombatLevel();
                } else if (interacting instanceof NPC) {
                    targetType = "npc";
                    targetCombatLevel = ((NPC) interacting).getCombatLevel();
                }
            }
            
            int currentAnimation = localPlayer.getAnimation();
            boolean isAttacking = isAttackAnimation(currentAnimation);
            
            // Player is in combat if:
            // 1. Currently interacting with a target AND has attack animation, OR
            // 2. Has attack animation (even without current target - combat cooldown), OR  
            // 3. Currently interacting with NPC/Player (preparation/targeting phase)
            boolean inCombat = (interacting != null && isAttacking) || 
                              isAttacking || 
                              (interacting != null && (interacting instanceof NPC || interacting instanceof Player));
            
            log.debug("Combat state - interacting: {}, animation: {}, isAttacking: {}, inCombat: {}, targetName: {}", 
                     interacting != null ? interacting.getName() : "none", 
                     currentAnimation, isAttacking, inCombat, targetName);
            
            return CombatData.builder()
                .inCombat(inCombat)
                .isAttacking(isAttacking)
                .targetName(targetName)
                .targetType(targetType)
                .targetCombatLevel(targetCombatLevel)
                .currentAnimation(currentAnimation)
                .lastCombatTick(System.currentTimeMillis())
                .specialAttackPercent(client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) / 10)
                .weaponType(getWeaponType())
                .attackStyle(getAttackStyle())
                .build();
        } catch (Exception e) {
            log.warn("Error collecting combat data", e);
            return CombatData.builder().build();
        }
    }
    
    /**
     * Collect hitsplat data
     */
    private HitsplatData collectHitsplatData()
    {
        try {
            List<HitsplatApplied> recentHitsplatList = new ArrayList<>();
            List<Integer> recentHits = new ArrayList<>();
            int totalDamage = 0;
            int maxHit = 0;
            int hitCount = 0;
            String lastHitType = null;
            Long lastHitTime = null;
            
            // Only consider hitsplats from the last 10 seconds to avoid stale data
            long currentTime = System.currentTimeMillis();
            long timeThreshold = currentTime - 10000; // 10 seconds
            
            // Collect recent hitsplats from the queue (time-filtered)
            for (TimestampedHitsplat timestampedHitsplat : recentHitsplats) {
                if (timestampedHitsplat != null && timestampedHitsplat.getHitsplat() != null && 
                    timestampedHitsplat.getHitsplat().getHitsplat() != null) {
                    
                    // Only include hitsplats from the last 10 seconds
                    if (timestampedHitsplat.getTimestamp() >= timeThreshold) {
                        HitsplatApplied hitsplat = timestampedHitsplat.getHitsplat();
                        recentHitsplatList.add(hitsplat);
                        int damage = hitsplat.getHitsplat().getAmount();
                        recentHits.add(damage);
                        totalDamage += damage;
                        maxHit = Math.max(maxHit, damage);
                        hitCount++;
                        
                        // Track last hit details
                        lastHitType = getHitsplatTypeName(hitsplat.getHitsplat().getHitsplatType());
                        lastHitTime = timestampedHitsplat.getTimestamp();
                    }
                }
            }
            
            // Calculate average hit if we have hits
            Integer averageHit = hitCount > 0 ? totalDamage / hitCount : null;
            Double averageDamage = hitCount > 0 ? (double) totalDamage / hitCount : null;
            
            return HitsplatData.builder()
                .recentHitsplats(recentHitsplatList)
                .recentHits(recentHits)
                .totalRecentDamage(totalDamage)
                .maxRecentHit(maxHit)
                .hitCount(hitCount)
                .lastHitType(lastHitType)
                .lastHitTime(lastHitTime)
                .averageHit(averageHit)
                .averageDamage(averageDamage)
                .build();
        } catch (Exception e) {
            log.warn("Error collecting hitsplat data", e);
            return HitsplatData.builder().build();
        }
    }
    
    /**
     * Collect animation data
     */
    private AnimationData collectAnimationData()
    {
        try {
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer == null) {
                return AnimationData.builder().build();
            }
            
            List<AnimationChanged> recentAnimationList = new ArrayList<>(recentAnimationChanges);
            // Convert AnimationChanged events to Integer list of animation IDs
            List<Integer> recentAnimationIds = recentAnimationList.stream()
                .map(event -> event.getActor().getAnimation())
                .filter(animId -> animId != -1) // Filter out idle animations from recent list
                .collect(Collectors.toList());
                
            int currentAnimation = localPlayer.getAnimation();
            int poseAnimation = localPlayer.getPoseAnimation();
            String animationType = getAnimationType(currentAnimation);
            
            // Find the most recent non-idle animation for lastAnimation
            String lastAnimation = null;
            Long animationStartTime = null;
            Integer animationDuration = null;
            
            if (!recentAnimationIds.isEmpty()) {
                Integer lastAnimId = recentAnimationIds.get(recentAnimationIds.size() - 1);
                lastAnimation = getAnimationType(lastAnimId);
                animationStartTime = System.currentTimeMillis(); // Approximate since we don't track exact start times
                animationDuration = getAnimationDuration(lastAnimId);
            }
            
            // DEBUG: Log animation data collection
            if (currentAnimation != -1) {
                log.debug("[ANIMATION-DEBUG] Current: {} (type: {}), Pose: {}, Recent: {}", 
                    currentAnimation, animationType, poseAnimation, recentAnimationIds.size());
            }
            
            String animationName = getAnimationName(currentAnimation);
            
            return AnimationData.builder()
                .currentAnimation(currentAnimation)
                .animationName(animationName)
                .poseAnimation(poseAnimation)
                .animationType(animationType)
                .recentAnimations(recentAnimationIds)
                .animationChangeCount(recentAnimationList.size())
                .lastAnimation(lastAnimation)
                .animationStartTime(animationStartTime)
                .animationDuration(animationDuration)
                .build();
        } catch (Exception e) {
            log.warn("Error collecting animation data", e);
            return AnimationData.builder().build();
        }
    }
    
    /**
     * Collect interaction data
     */
    private InteractionData collectInteractionData()
    {
        try {
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer == null) {
                return InteractionData.builder().build();
            }
            
            long currentTime = System.currentTimeMillis();
            long analysisTimeThreshold = currentTime - 10000; // 10 seconds for analysis/counting
            long currentInteractionThreshold = currentTime - 2000; // 2 seconds for "last interaction" timestamp
            
            // Collect recent interaction changes (10-second window for analysis)
            List<InteractingChanged> recentActorInteractions = new ArrayList<>();
            for (TimestampedInteractionChanged timestampedInteraction : recentInteractionChanges) {
                if (timestampedInteraction != null && timestampedInteraction.getInteraction() != null &&
                    timestampedInteraction.getTimestamp() >= analysisTimeThreshold) {
                    recentActorInteractions.add(timestampedInteraction.getInteraction());
                }
            }
            
            // Calculate total interaction count
            int totalInteractionCount = recentActorInteractions.size();
            
            // Get current target info
            Actor currentTarget = localPlayer.getInteracting();
            String targetName = currentTarget != null ? currentTarget.getName() : null;
            String interactionType = getInteractionType(currentTarget);
            
            // Get last interaction details
            String lastInteractionType = null;
            String lastInteractionTarget = null;
            Long lastInteractionTime = null;
            
            // Find current interactions using shorter time window
            TimestampedInteractionChanged currentActorInteraction = null;
            for (TimestampedInteractionChanged timestampedInteraction : recentInteractionChanges) {
                if (timestampedInteraction != null && timestampedInteraction.getTimestamp() >= currentInteractionThreshold) {
                    if (currentActorInteraction == null || timestampedInteraction.getTimestamp() > currentActorInteraction.getTimestamp()) {
                        currentActorInteraction = timestampedInteraction;
                    }
                }
            }
            
            if (currentActorInteraction != null) {
                lastInteractionType = "combat";
                lastInteractionTarget = targetName;
                lastInteractionTime = currentActorInteraction.getTimestamp();
                
                log.debug("[INTERACTION-DEBUG] Current combat interaction - Target: {}, Time: {}", lastInteractionTarget, lastInteractionTime);
            }
            
            // Calculate average interaction interval
            Double averageInteractionInterval = null;
            if (totalInteractionCount > 1) {
                List<Long> allTimestamps = new ArrayList<>();
                
                // Add actor interaction timestamps
                for (TimestampedInteractionChanged timestampedInteraction : recentInteractionChanges) {
                    if (timestampedInteraction != null && timestampedInteraction.getTimestamp() >= analysisTimeThreshold) {
                        allTimestamps.add(timestampedInteraction.getTimestamp());
                    }
                }
                
                // Sort timestamps and calculate intervals
                if (allTimestamps.size() > 1) {
                    Collections.sort(allTimestamps);
                    long totalInterval = 0;
                    int intervalCount = 0;
                    
                    for (int i = 1; i < allTimestamps.size(); i++) {
                        long interval = allTimestamps.get(i) - allTimestamps.get(i - 1);
                        // Only count reasonable intervals (less than 30 seconds between interactions)
                        if (interval < 30000) {
                            totalInterval += interval;
                            intervalCount++;
                        }
                    }
                    
                    if (intervalCount > 0) {
                        averageInteractionInterval = (double) totalInterval / intervalCount;
                    }
                }
            }
            
            return InteractionData.builder()
                .currentTarget(targetName)
                .interactionType(interactionType)
                .recentInteractions(recentActorInteractions)
                .recentInteractionsJsonb("[]") // Simplified for now
                .interactionCount(totalInteractionCount)
                .lastInteractionType(lastInteractionType)
                .lastInteractionTarget(lastInteractionTarget)
                .lastInteractionTime(lastInteractionTime)
                .mostCommonInteraction("combat")
                .averageInteractionInterval(averageInteractionInterval)
                .build();
        } catch (Exception e) {
            log.warn("Error collecting interaction data", e);
            return InteractionData.builder().build();
        }
    }
    
    /**
     * Event handler for hitsplat events
     */
    public void onHitsplatApplied(HitsplatApplied hitsplatApplied)
    {
        TimestampedHitsplat timestamped = new TimestampedHitsplat(hitsplatApplied, System.currentTimeMillis());
        recentHitsplats.offer(timestamped);
        
        // Keep queue bounded
        while (recentHitsplats.size() > 50) {
            recentHitsplats.poll();
        }
    }
    
    /**
     * Event handler for animation changes
     */
    public void onAnimationChanged(AnimationChanged animationChanged)
    {
        recentAnimationChanges.offer(animationChanged);
        
        // Keep queue bounded
        while (recentAnimationChanges.size() > 50) {
            recentAnimationChanges.poll();
        }
    }
    
    /**
     * Event handler for interaction changes
     */
    public void onInteractingChanged(InteractingChanged interactingChanged)
    {
        TimestampedInteractionChanged timestamped = new TimestampedInteractionChanged(interactingChanged, System.currentTimeMillis());
        recentInteractionChanges.offer(timestamped);
        
        // Keep queue bounded
        while (recentInteractionChanges.size() > 50) {
            recentInteractionChanges.poll();
        }
    }
    
    /**
     * Helper method to determine if animation is attack-related
     */
    private boolean isAttackAnimation(int animationId)
    {
        if (animationId == -1) return false;
        
        // Common attack animation IDs
        switch (animationId) {
            case 422: // Unarmed punch
            case 423: // Unarmed kick
            case 428: // Sword stab
            case 440: // Sword slash
            case 412: // Dagger stab
            case 451: // Mace pound
            case 426: // Axe hack
            case 1167: // Bow draw
            case 7552: // Crossbow aim
            case 1979: // Whip crack
                return true;
            default:
                // Check if animation ID is in known attack animation ranges
                return (animationId >= 400 && animationId <= 500) ||
                       (animationId >= 1150 && animationId <= 1200) ||
                       (animationId >= 7500 && animationId <= 7600);
        }
    }
    
    /**
     * Helper method to get animation type
     */
    private String getAnimationType(int animationId)
    {
        if (animationId == -1) return "idle";
        if (isAttackAnimation(animationId)) return "attack";
        
        // Common animation types
        if (animationId >= 800 && animationId <= 900) return "movement";
        if (animationId >= 1200 && animationId <= 1300) return "magic";
        if (animationId >= 700 && animationId <= 800) return "skilling";
        
        return "other";
    }
    
    /**
     * Helper method to get animation duration
     */
    private Integer getAnimationDuration(int animationId)
    {
        if (animationId == -1) return null;
        
        // Estimate animation durations based on type (in milliseconds)
        String type = getAnimationType(animationId);
        switch (type) {
            case "attack":
                return 1800; // ~1.8 seconds for most attack animations
            case "magic":
                return 2400; // ~2.4 seconds for spell casting
            case "skilling":
                return 3600; // ~3.6 seconds for skill animations
            case "movement":
                return 600;  // ~0.6 seconds for movement
            default:
                return 1200; // ~1.2 seconds default
        }
    }
    
    /**
     * Helper method to get weapon type
     */
    private String getWeaponType() {
        try {
            // Get weapon type from varbit
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
    
    /**
     * Helper method to get attack style
     */
    private String getAttackStyle() {
        try {
            // Get attack style from VarPlayer
            int attackStyle = client.getVarpValue(VarPlayer.ATTACK_STYLE);
            String weaponType = getWeaponType();
            
            // Attack styles depend on weapon type
            switch (weaponType) {
                case "unarmed":
                    switch (attackStyle) {
                        case 0: return "punch";
                        case 1: return "kick";
                        case 2: return "block";
                        default: return "unarmed_style_" + attackStyle;
                    }
                case "sword":
                case "dagger":
                case "mace":
                    switch (attackStyle) {
                        case 0: return "stab";
                        case 1: return "lunge";
                        case 2: return "slash";
                        case 3: return "block";
                        default: return "melee_style_" + attackStyle;
                    }
                case "2h_sword":
                case "halberd":
                    switch (attackStyle) {
                        case 0: return "chop";
                        case 1: return "slash";
                        case 2: return "lunge";
                        case 3: return "block";
                        default: return "2h_style_" + attackStyle;
                    }
                case "bow":
                    switch (attackStyle) {
                        case 0: return "accurate";
                        case 1: return "rapid";
                        case 2: return "longrange";
                        default: return "bow_style_" + attackStyle;
                    }
                case "crossbow":
                    switch (attackStyle) {
                        case 0: return "accurate";
                        case 1: return "rapid";
                        case 2: return "longrange";
                        default: return "crossbow_style_" + attackStyle;
                    }
                case "staff":
                case "magic_staff":
                    switch (attackStyle) {
                        case 0: return "bash";
                        case 1: return "pound";
                        case 2: return "focus";
                        case 3: return "spell";
                        default: return "staff_style_" + attackStyle;
                    }
                case "whip":
                    switch (attackStyle) {
                        case 0: return "flick";
                        case 1: return "lash";
                        case 2: return "deflect";
                        default: return "whip_style_" + attackStyle;
                    }
                default:
                    return weaponType + "_style_" + attackStyle;
            }
        } catch (Exception e) {
            log.warn("Error getting attack style", e);
            return "error";
        }
    }
    
    /**
     * Helper method to get interaction type
     */
    private String getInteractionType(Actor target)
    {
        if (target == null) return "none";
        if (target instanceof Player) return "player";
        if (target instanceof NPC) return "npc";
        return "unknown";
    }
    
    /**
     * Helper method to convert hitsplat type int to string name
     */
    private String getHitsplatTypeName(int hitsplatType)
    {
        // Use RuneLite API HitsplatID constants for true name lookup
        try {
            // Use reflection to find the constant name from HitsplatID
            java.lang.reflect.Field[] fields = HitsplatID.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (field.getType() == int.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    try {
                        int value = field.getInt(null);
                        if (value == hitsplatType) {
                            return field.getName();
                        }
                    } catch (IllegalAccessException e) {
                        // Continue to next field
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to lookup hitsplat type {} using HitsplatID reflection: {}", hitsplatType, e.getMessage());
        }
        
        // Fallback for any unmapped values
        return "UNKNOWN_" + hitsplatType;
    }
    
    /**
     * Helper method to convert animation ID to string name using RuneLite API
     */
    public String getAnimationName(int animationId)
    {
        // Handle idle animation specifically
        if (animationId == -1) return "IDLE";
        
        // Use RuneLite API AnimationID constants for true name lookup
        try {
            // Use reflection to find the constant name from AnimationID
            java.lang.reflect.Field[] fields = AnimationID.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (field.getType() == int.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    try {
                        int value = field.getInt(null);
                        if (value == animationId) {
                            return field.getName();
                        }
                    } catch (IllegalAccessException e) {
                        // Continue to next field
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to lookup animation {} using AnimationID reflection: {}", animationId, e.getMessage());
        }
        
        // Fallback - try to get animation type for better context
        String type = getAnimationType(animationId);
        return type.toUpperCase() + "_" + animationId;
    }
    
    // =============== CRITICAL EVENT HANDLING METHODS - FULLY RESTORED ===============
    
}