/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.runelite.api.GameState;
import net.runelite.api.Player;

/**
 * Immutable snapshot of the game state at a specific point in time
 * 
 * Captures the essential state information that can be used to:
 * - Calculate deltas between ticks
 * - Provide context for data collection
 * - Enable state-based analysis
 * - Support game state reconstruction
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameStateSnapshot
{
    // ===== TEMPORAL DATA =====
    private Long timestamp;
    private Integer tickCount;
    private GameState gameState;
    
    // ===== PLAYER STATE =====
    private Player localPlayer;
    private String playerName;
    private Integer playerCombatLevel;
    private Integer playerWorldX;
    private Integer playerWorldY;
    private Integer playerPlane;
    private Integer playerAnimation;
    private Integer playerHealthRatio;
    private Integer playerPrayerPoints;
    private Integer playerEnergy;
    private Integer playerWeight;
    
    // ===== WORLD STATE =====
    private Integer worldPlane;
    private Integer baseX;
    private Integer baseY;
    private Integer[] mapRegions;
    
    // ===== CLIENT STATE =====
    private Integer cameraX;
    private Integer cameraY;
    private Integer cameraZ;
    private Integer cameraPitch;
    private Integer cameraYaw;
    private Double minimapZoom;
    private Boolean menuOpen;
    private Integer menuEntryCount;
    
    // ===== DERIVED STATE =====
    private String currentRegion;
    private String currentActivity;
    private Long stateHash;
    
    /**
     * Constructor that extracts state from current game client
     * @param timestamp Current timestamp
     * @param tickCount Current tick count
     * @param gameState Current game state
     * @param localPlayer Local player reference
     * @param plane Current plane
     * @param baseX Current base X coordinate
     * @param baseY Current base Y coordinate
     */
    public GameStateSnapshot(long timestamp, int tickCount, GameState gameState, 
                           Player localPlayer, int plane, int baseX, int baseY)
    {
        this.timestamp = timestamp;
        this.tickCount = tickCount;
        this.gameState = gameState;
        this.localPlayer = localPlayer;
        this.worldPlane = plane;
        this.baseX = baseX;
        this.baseY = baseY;
        
        // Extract player data if available
        if (localPlayer != null) {
            this.playerName = localPlayer.getName();
            this.playerCombatLevel = localPlayer.getCombatLevel();
            if (localPlayer.getWorldLocation() != null) {
                this.playerWorldX = localPlayer.getWorldLocation().getX();
                this.playerWorldY = localPlayer.getWorldLocation().getY();
                this.playerPlane = localPlayer.getWorldLocation().getPlane();
            }
            this.playerAnimation = localPlayer.getAnimation();
            this.playerHealthRatio = localPlayer.getHealthRatio();
        }
        
        // Calculate state hash for comparison
        this.stateHash = calculateStateHash();
    }
    
    /**
     * Calculate a hash of the essential state for comparison
     * @return State hash for delta calculation
     */
    private long calculateStateHash()
    {
        long hash = 1;
        hash = hash * 31 + (gameState != null ? gameState.hashCode() : 0);
        hash = hash * 31 + (playerWorldX != null ? playerWorldX.hashCode() : 0);
        hash = hash * 31 + (playerWorldY != null ? playerWorldY.hashCode() : 0);
        hash = hash * 31 + (playerPlane != null ? playerPlane.hashCode() : 0);
        hash = hash * 31 + (playerAnimation != null ? playerAnimation.hashCode() : 0);
        hash = hash * 31 + (worldPlane != null ? worldPlane.hashCode() : 0);
        return hash;
    }
    
    /**
     * Get the number of data points in this snapshot
     * @return Data point count
     */
    public int getDataPointCount()
    {
        int count = 0;
        
        // Count non-null fields
        count += (timestamp != null ? 1 : 0);
        count += (tickCount != null ? 1 : 0);
        count += (gameState != null ? 1 : 0);
        count += (playerName != null ? 1 : 0);
        count += (playerCombatLevel != null ? 1 : 0);
        count += (playerWorldX != null ? 1 : 0);
        count += (playerWorldY != null ? 1 : 0);
        count += (playerPlane != null ? 1 : 0);
        count += (playerAnimation != null ? 1 : 0);
        count += (playerHealthRatio != null ? 1 : 0);
        count += (playerPrayerPoints != null ? 1 : 0);
        count += (playerEnergy != null ? 1 : 0);
        count += (playerWeight != null ? 1 : 0);
        count += (worldPlane != null ? 1 : 0);
        count += (baseX != null ? 1 : 0);
        count += (baseY != null ? 1 : 0);
        count += (mapRegions != null ? mapRegions.length : 0);
        count += (cameraX != null ? 1 : 0);
        count += (cameraY != null ? 1 : 0);
        count += (cameraZ != null ? 1 : 0);
        count += (cameraPitch != null ? 1 : 0);
        count += (cameraYaw != null ? 1 : 0);
        count += (minimapZoom != null ? 1 : 0);
        count += (menuOpen != null ? 1 : 0);
        count += (menuEntryCount != null ? 1 : 0);
        count += (currentRegion != null ? 1 : 0);
        count += (currentActivity != null ? 1 : 0);
        count += (stateHash != null ? 1 : 0);
        
        return count;
    }
    
    /**
     * Get estimated memory size of this snapshot
     * @return Estimated memory size in bytes
     */
    public long getEstimatedSize()
    {
        long size = 64; // Base object overhead
        
        // Add size of primitive fields (most are boxed integers = 16 bytes each)
        size += (timestamp != null ? 16 : 0);
        size += (tickCount != null ? 16 : 0);
        size += (playerCombatLevel != null ? 16 : 0);
        size += (playerWorldX != null ? 16 : 0);
        size += (playerWorldY != null ? 16 : 0);
        size += (playerPlane != null ? 16 : 0);
        size += (playerAnimation != null ? 16 : 0);
        size += (playerHealthRatio != null ? 16 : 0);
        size += (playerPrayerPoints != null ? 16 : 0);
        size += (playerEnergy != null ? 16 : 0);
        size += (playerWeight != null ? 16 : 0);
        size += (worldPlane != null ? 16 : 0);
        size += (baseX != null ? 16 : 0);
        size += (baseY != null ? 16 : 0);
        size += (cameraX != null ? 16 : 0);
        size += (cameraY != null ? 16 : 0);
        size += (cameraZ != null ? 16 : 0);
        size += (cameraPitch != null ? 16 : 0);
        size += (cameraYaw != null ? 16 : 0);
        size += (minimapZoom != null ? 16 : 0);
        size += (menuEntryCount != null ? 16 : 0);
        size += (stateHash != null ? 16 : 0);
        
        // Add size of boolean fields
        size += (menuOpen != null ? 8 : 0);
        
        // Add size of string fields
        size += (playerName != null ? playerName.length() * 2 + 32 : 0);
        size += (currentRegion != null ? currentRegion.length() * 2 + 32 : 0);
        size += (currentActivity != null ? currentActivity.length() * 2 + 32 : 0);
        
        // Add size of arrays
        size += (mapRegions != null ? mapRegions.length * 4 + 24 : 0);
        
        return size;
    }
    
    /**
     * Check if this snapshot represents a valid game state
     * @return True if valid, false otherwise
     */
    public boolean isValid()
    {
        return timestamp != null && 
               tickCount != null && 
               gameState != null &&
               timestamp > 0 &&
               tickCount >= 0;
    }
    
    /**
     * Get a compact string representation of this snapshot
     * @return Compact string representation
     */
    public String toCompactString()
    {
        return String.format("GameStateSnapshot{tick=%d, state=%s, player=%s, pos=(%d,%d,%d)}", 
                            tickCount, gameState, playerName, playerWorldX, playerWorldY, playerPlane);
    }
}