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
 * Represents the changes between two game state snapshots
 * 
 * This class calculates and stores the differences between consecutive
 * game ticks, enabling analysis of:
 * - Movement patterns and velocity
 * - State transitions and changes
 * - Action sequences and timing
 * - Environmental changes
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameStateDelta
{
    // ===== TEMPORAL DELTAS =====
    private Long timeDelta; // Milliseconds between snapshots
    private Integer tickDelta; // Ticks between snapshots
    
    // ===== PLAYER DELTAS =====
    private Integer playerXDelta;
    private Integer playerYDelta;
    private Integer playerPlaneDelta;
    private Integer animationChanged; // 0 = same, 1 = different
    private Integer healthRatioDelta;
    private Integer prayerPointsDelta;
    private Integer energyDelta;
    private Integer weightDelta;
    private Integer combatLevelDelta;
    
    // ===== WORLD DELTAS =====
    private Integer worldPlaneDelta;
    private Integer baseXDelta;
    private Integer baseYDelta;
    private Integer regionChanged; // 0 = same, 1 = different
    
    // ===== CAMERA DELTAS =====
    private Integer cameraXDelta;
    private Integer cameraYDelta;
    private Integer cameraZDelta;
    private Integer cameraPitchDelta;
    private Integer cameraYawDelta;
    private Double minimapZoomDelta;
    
    // ===== INTERACTION DELTAS =====
    private Integer menuStateChanged; // 0 = no change, 1 = opened, -1 = closed
    private Integer menuEntryCountDelta;
    
    // ===== CALCULATED METRICS =====
    private Double movementVelocity; // Tiles per second
    private Double cameraRotationRate; // Degrees per second
    private String movementDirection; // N, NE, E, SE, S, SW, W, NW, NONE
    private String activityTransition; // Previous -> Current activity
    
    // ===== CHANGE CLASSIFICATIONS =====
    private Boolean significantMovement; // Movement > threshold
    private Boolean significantCameraChange; // Camera change > threshold
    private Boolean stateTransition; // Major game state change
    private Boolean playerAction; // Animation or interaction change
    
    // ===== METADATA =====
    private GameStateSnapshot previousSnapshot;
    private GameStateSnapshot currentSnapshot;
    private Long deltaCalculationTime; // Time taken to calculate delta
    
    /**
     * Constructor that calculates delta from a current snapshot
     * Assumes no previous snapshot (initial state)
     * @param currentSnapshot The current game state snapshot
     */
    public GameStateDelta(GameStateSnapshot currentSnapshot)
    {
        this.currentSnapshot = currentSnapshot;
        this.previousSnapshot = null;
        
        // Initialize with zero deltas for initial state
        initializeZeroDeltas();
        
        this.deltaCalculationTime = System.nanoTime();
    }
    
    /**
     * Constructor that calculates delta between two snapshots
     * @param previousSnapshot The previous game state snapshot
     * @param currentSnapshot The current game state snapshot
     */
    public GameStateDelta(GameStateSnapshot previousSnapshot, GameStateSnapshot currentSnapshot)
    {
        long startTime = System.nanoTime();
        
        this.previousSnapshot = previousSnapshot;
        this.currentSnapshot = currentSnapshot;
        
        if (previousSnapshot == null) {
            initializeZeroDeltas();
        } else {
            calculateDeltas();
        }
        
        this.deltaCalculationTime = System.nanoTime() - startTime;
    }
    
    /**
     * Initialize all deltas to zero (for initial state)
     */
    private void initializeZeroDeltas()
    {
        timeDelta = 0L;
        tickDelta = 0;
        playerXDelta = 0;
        playerYDelta = 0;
        playerPlaneDelta = 0;
        animationChanged = 0;
        healthRatioDelta = 0;
        prayerPointsDelta = 0;
        energyDelta = 0;
        weightDelta = 0;
        combatLevelDelta = 0;
        worldPlaneDelta = 0;
        baseXDelta = 0;
        baseYDelta = 0;
        regionChanged = 0;
        cameraXDelta = 0;
        cameraYDelta = 0;
        cameraZDelta = 0;
        cameraPitchDelta = 0;
        cameraYawDelta = 0;
        minimapZoomDelta = 0.0;
        menuStateChanged = 0;
        menuEntryCountDelta = 0;
        movementVelocity = 0.0;
        cameraRotationRate = 0.0;
        movementDirection = "NONE";
        activityTransition = "INITIAL";
        significantMovement = false;
        significantCameraChange = false;
        stateTransition = false;
        playerAction = false;
    }
    
    /**
     * Calculate all deltas between the two snapshots
     */
    private void calculateDeltas()
    {
        // Temporal deltas
        timeDelta = safeSubtract(currentSnapshot.getTimestamp(), previousSnapshot.getTimestamp());
        tickDelta = safeSubtract(currentSnapshot.getTickCount(), previousSnapshot.getTickCount());
        
        // Player position deltas
        playerXDelta = safeSubtract(currentSnapshot.getPlayerWorldX(), previousSnapshot.getPlayerWorldX());
        playerYDelta = safeSubtract(currentSnapshot.getPlayerWorldY(), previousSnapshot.getPlayerWorldY());
        playerPlaneDelta = safeSubtract(currentSnapshot.getPlayerPlane(), previousSnapshot.getPlayerPlane());
        
        // Player state deltas
        animationChanged = !safeEquals(currentSnapshot.getPlayerAnimation(), previousSnapshot.getPlayerAnimation()) ? 1 : 0;
        healthRatioDelta = safeSubtract(currentSnapshot.getPlayerHealthRatio(), previousSnapshot.getPlayerHealthRatio());
        prayerPointsDelta = safeSubtract(currentSnapshot.getPlayerPrayerPoints(), previousSnapshot.getPlayerPrayerPoints());
        energyDelta = safeSubtract(currentSnapshot.getPlayerEnergy(), previousSnapshot.getPlayerEnergy());
        weightDelta = safeSubtract(currentSnapshot.getPlayerWeight(), previousSnapshot.getPlayerWeight());
        combatLevelDelta = safeSubtract(currentSnapshot.getPlayerCombatLevel(), previousSnapshot.getPlayerCombatLevel());
        
        // World deltas
        worldPlaneDelta = safeSubtract(currentSnapshot.getWorldPlane(), previousSnapshot.getWorldPlane());
        baseXDelta = safeSubtract(currentSnapshot.getBaseX(), previousSnapshot.getBaseX());
        baseYDelta = safeSubtract(currentSnapshot.getBaseY(), previousSnapshot.getBaseY());
        regionChanged = !safeEquals(currentSnapshot.getCurrentRegion(), previousSnapshot.getCurrentRegion()) ? 1 : 0;
        
        // Camera deltas
        cameraXDelta = safeSubtract(currentSnapshot.getCameraX(), previousSnapshot.getCameraX());
        cameraYDelta = safeSubtract(currentSnapshot.getCameraY(), previousSnapshot.getCameraY());
        cameraZDelta = safeSubtract(currentSnapshot.getCameraZ(), previousSnapshot.getCameraZ());
        cameraPitchDelta = safeSubtract(currentSnapshot.getCameraPitch(), previousSnapshot.getCameraPitch());
        cameraYawDelta = safeSubtract(currentSnapshot.getCameraYaw(), previousSnapshot.getCameraYaw());
        minimapZoomDelta = safeSubtract(currentSnapshot.getMinimapZoom(), previousSnapshot.getMinimapZoom());
        
        // Menu deltas
        if (previousSnapshot.getMenuOpen() != null && currentSnapshot.getMenuOpen() != null) {
            if (!previousSnapshot.getMenuOpen() && currentSnapshot.getMenuOpen()) {
                menuStateChanged = 1; // Menu opened
            } else if (previousSnapshot.getMenuOpen() && !currentSnapshot.getMenuOpen()) {
                menuStateChanged = -1; // Menu closed
            } else {
                menuStateChanged = 0; // No change
            }
        } else {
            menuStateChanged = 0;
        }
        
        menuEntryCountDelta = safeSubtract(currentSnapshot.getMenuEntryCount(), previousSnapshot.getMenuEntryCount());
        
        // Calculate derived metrics
        calculateDerivedMetrics();
    }
    
    /**
     * Calculate derived metrics from the deltas
     */
    private void calculateDerivedMetrics()
    {
        // Movement velocity (tiles per second)
        if (timeDelta != null && timeDelta > 0 && playerXDelta != null && playerYDelta != null) {
            double distance = Math.sqrt(playerXDelta * playerXDelta + playerYDelta * playerYDelta);
            movementVelocity = distance / (timeDelta / 1000.0);
        } else {
            movementVelocity = 0.0;
        }
        
        // Camera rotation rate (degrees per second)
        if (timeDelta != null && timeDelta > 0 && cameraYawDelta != null) {
            cameraRotationRate = Math.abs(cameraYawDelta) / (timeDelta / 1000.0);
        } else {
            cameraRotationRate = 0.0;
        }
        
        // Movement direction
        if (playerXDelta != null && playerYDelta != null) {
            if (playerXDelta == 0 && playerYDelta == 0) {
                movementDirection = "NONE";
            } else {
                double angle = Math.atan2(playerYDelta, playerXDelta) * 180 / Math.PI;
                movementDirection = angleToDirection(angle);
            }
        }
        
        // Activity transition
        String prevActivity = previousSnapshot != null ? previousSnapshot.getCurrentActivity() : "UNKNOWN";
        String currActivity = currentSnapshot != null ? currentSnapshot.getCurrentActivity() : "UNKNOWN";
        activityTransition = prevActivity + " -> " + currActivity;
        
        // Change classifications
        significantMovement = movementVelocity != null && movementVelocity > 0.1; // More than 0.1 tiles/sec
        significantCameraChange = cameraRotationRate != null && cameraRotationRate > 10.0; // More than 10 degrees/sec
        stateTransition = !safeEquals(
            currentSnapshot != null ? currentSnapshot.getGameState() : null,
            previousSnapshot != null ? previousSnapshot.getGameState() : null
        );
        playerAction = animationChanged != null && animationChanged == 1;
    }
    
    /**
     * Convert angle to cardinal direction
     * @param angle Angle in degrees
     * @return Cardinal direction string
     */
    private String angleToDirection(double angle)
    {
        // Normalize angle to 0-360
        while (angle < 0) angle += 360;
        while (angle >= 360) angle -= 360;
        
        if (angle >= 337.5 || angle < 22.5) return "E";
        else if (angle >= 22.5 && angle < 67.5) return "NE";
        else if (angle >= 67.5 && angle < 112.5) return "N";
        else if (angle >= 112.5 && angle < 157.5) return "NW";
        else if (angle >= 157.5 && angle < 202.5) return "W";
        else if (angle >= 202.5 && angle < 247.5) return "SW";
        else if (angle >= 247.5 && angle < 292.5) return "S";
        else return "SE";
    }
    
    /**
     * Safe subtraction that handles null values
     */
    private Integer safeSubtract(Integer a, Integer b)
    {
        if (a == null || b == null) return 0;
        return a - b;
    }
    
    /**
     * Safe subtraction for Long values
     */
    private Long safeSubtract(Long a, Long b)
    {
        if (a == null || b == null) return 0L;
        return a - b;
    }
    
    /**
     * Safe subtraction for Double values
     */
    private Double safeSubtract(Double a, Double b)
    {
        if (a == null || b == null) return 0.0;
        return a - b;
    }
    
    /**
     * Safe equality check that handles null values
     */
    private boolean safeEquals(Object a, Object b)
    {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
    
    /**
     * Get the number of data points in this delta
     * @return Data point count
     */
    public int getDataPointCount()
    {
        int count = 0;
        
        // Count non-null fields
        count += (timeDelta != null ? 1 : 0);
        count += (tickDelta != null ? 1 : 0);
        count += (playerXDelta != null ? 1 : 0);
        count += (playerYDelta != null ? 1 : 0);
        count += (playerPlaneDelta != null ? 1 : 0);
        count += (animationChanged != null ? 1 : 0);
        count += (healthRatioDelta != null ? 1 : 0);
        count += (prayerPointsDelta != null ? 1 : 0);
        count += (energyDelta != null ? 1 : 0);
        count += (weightDelta != null ? 1 : 0);
        count += (combatLevelDelta != null ? 1 : 0);
        count += (worldPlaneDelta != null ? 1 : 0);
        count += (baseXDelta != null ? 1 : 0);
        count += (baseYDelta != null ? 1 : 0);
        count += (regionChanged != null ? 1 : 0);
        count += (cameraXDelta != null ? 1 : 0);
        count += (cameraYDelta != null ? 1 : 0);
        count += (cameraZDelta != null ? 1 : 0);
        count += (cameraPitchDelta != null ? 1 : 0);
        count += (cameraYawDelta != null ? 1 : 0);
        count += (minimapZoomDelta != null ? 1 : 0);
        count += (menuStateChanged != null ? 1 : 0);
        count += (menuEntryCountDelta != null ? 1 : 0);
        count += (movementVelocity != null ? 1 : 0);
        count += (cameraRotationRate != null ? 1 : 0);
        count += (movementDirection != null ? 1 : 0);
        count += (activityTransition != null ? 1 : 0);
        count += (significantMovement != null ? 1 : 0);
        count += (significantCameraChange != null ? 1 : 0);
        count += (stateTransition != null ? 1 : 0);
        count += (playerAction != null ? 1 : 0);
        
        return count;
    }
    
    /**
     * Get estimated memory size of this delta
     * @return Estimated memory size in bytes
     */
    public long getEstimatedSize()
    {
        long size = 64; // Base object overhead
        
        // Primitive fields (mostly boxed integers = 16 bytes each)
        size += 16 * 25; // Integer fields
        size += 16 * 2;  // Long fields  
        size += 16 * 3;  // Double fields
        size += 8 * 4;   // Boolean fields
        
        // String fields
        size += (movementDirection != null ? movementDirection.length() * 2 + 32 : 0);
        size += (activityTransition != null ? activityTransition.length() * 2 + 32 : 0);
        
        return size;
    }
    
    /**
     * Check if this represents a significant change
     * @return True if significant change occurred
     */
    public boolean isSignificantChange()
    {
        return significantMovement || significantCameraChange || stateTransition || playerAction;
    }
}