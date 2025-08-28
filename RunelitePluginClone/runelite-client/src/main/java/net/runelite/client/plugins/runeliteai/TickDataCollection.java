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
 * Comprehensive data structure representing all data collected for a single game tick
 * 
 * Contains 680+ data points across multiple categories:
 * - Player state and vitals
 * - World environment data
 * - Combat mechanics
 * - Social interactions
 * - Input tracking
 * - Performance metrics
 * - Quality validation results
 * - Security analysis results
 * 
 * This is the primary data container that gets stored in the database
 * and used for AI/ML training data.
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TickDataCollection
{
    // ===== CORE IDENTIFIERS =====
    private Integer sessionId;
    private Integer tickNumber;
    private Long timestamp;
    private Long processingTimeNanos;
    
    // ===== GAME STATE DATA =====
    private GameStateSnapshot gameState;
    private GameStateDelta delta;
    
    // ===== PLAYER DATA =====
    private DataStructures.PlayerData playerData;
    private DataStructures.PlayerVitals playerVitals;
    private DataStructures.PlayerLocation playerLocation;
    private DataStructures.PlayerStats playerStats;
    private DataStructures.PlayerEquipment playerEquipment;
    private DataStructures.PlayerInventory playerInventory;
    private DataStructures.PlayerActivePrayers playerPrayers;
    private DataStructures.PlayerActiveSpells playerSpells;
    
    // ===== WORLD DATA =====
    private DataStructures.WorldEnvironmentData worldData;
    private DataStructures.NearbyPlayersData nearbyPlayers;
    private DataStructures.NearbyNPCsData nearbyNPCs;
    private DataStructures.GameObjectsData gameObjects;
    private DataStructures.GroundItemsData groundItems;
    private DataStructures.ProjectilesData projectiles;
    
    // ===== INPUT DATA =====
    private DataStructures.MouseInputData mouseInput;
    private DataStructures.KeyboardInputData keyboardInput;
    private DataStructures.CameraData cameraData;
    private DataStructures.MenuInteractionData menuData;
    
    // Movement analytics
    private Double movementDistance;
    private Double movementSpeed;
    
    // Click context data
    private DataStructures.ClickContextData clickContext;
    
    // ===== COMBAT DATA =====
    private DataStructures.CombatData combatData;
    private DataStructures.HitsplatData hitsplatData;
    private DataStructures.AnimationData animationData;
    private DataStructures.InteractionData interactionData;
    
    // ===== SOCIAL DATA =====
    private DataStructures.ChatData chatData;
    private DataStructures.FriendsData friendsData;
    private DataStructures.ClanData clanData;
    private DataStructures.TradeData tradeData;
    
    // ===== INTERFACE DATA =====
    private DataStructures.InterfaceData interfaceData;
    private DataStructures.DialogueData dialogueData;
    private DataStructures.ShopData shopData;
    private DataStructures.BankData bankData;
    
    // ===== ANALYSIS RESULTS =====
    private AnalysisResults.QualityScore qualityScore;
    private AnalysisResults.SecurityAnalysisResult securityAnalysis;
    private AnalysisResults.BehavioralAnalysisResult behavioralAnalysis;
    private AnalysisResults.PerformanceMetrics performanceMetrics;
    
    // ===== SYSTEM DATA =====
    private DataStructures.SystemMetrics systemMetrics;
    private DataStructures.ErrorData errorData;
    private DataStructures.TimingBreakdown timingBreakdown;
    
    /**
     * Get the total number of data points collected in this tick
     * @return Total data point count
     */
    public int getTotalDataPoints()
    {
        int count = 0;
        
        // Count base fields
        count += (sessionId != null ? 1 : 0);
        count += (tickNumber != null ? 1 : 0);
        count += (timestamp != null ? 1 : 0);
        count += (processingTimeNanos != null ? 1 : 0);
        
        // Count complex object data points
        count += (gameState != null ? gameState.getDataPointCount() : 0);
        count += (delta != null ? delta.getDataPointCount() : 0);
        count += (playerData != null ? playerData.getDataPointCount() : 0);
        count += (playerVitals != null ? playerVitals.getDataPointCount() : 0);
        count += (playerLocation != null ? playerLocation.getDataPointCount() : 0);
        count += (playerStats != null ? playerStats.getDataPointCount() : 0);
        count += (playerEquipment != null ? playerEquipment.getDataPointCount() : 0);
        count += (playerInventory != null ? playerInventory.getDataPointCount() : 0);
        count += (playerPrayers != null ? playerPrayers.getDataPointCount() : 0);
        count += (playerSpells != null ? playerSpells.getDataPointCount() : 0);
        count += (worldData != null ? worldData.getDataPointCount() : 0);
        count += (nearbyPlayers != null ? nearbyPlayers.getDataPointCount() : 0);
        count += (nearbyNPCs != null ? nearbyNPCs.getDataPointCount() : 0);
        count += (gameObjects != null ? gameObjects.getDataPointCount() : 0);
        count += (groundItems != null ? groundItems.getDataPointCount() : 0);
        count += (projectiles != null ? projectiles.getDataPointCount() : 0);
        count += (mouseInput != null ? mouseInput.getDataPointCount() : 0);
        count += (keyboardInput != null ? keyboardInput.getDataPointCount() : 0);
        count += (cameraData != null ? cameraData.getDataPointCount() : 0);
        count += (menuData != null ? menuData.getDataPointCount() : 0);
        count += (movementDistance != null ? 1 : 0);
        count += (movementSpeed != null ? 1 : 0);
        count += (clickContext != null ? clickContext.getDataPointCount() : 0);
        count += (combatData != null ? combatData.getDataPointCount() : 0);
        count += (hitsplatData != null ? hitsplatData.getDataPointCount() : 0);
        count += (animationData != null ? animationData.getDataPointCount() : 0);
        count += (interactionData != null ? interactionData.getDataPointCount() : 0);
        count += (chatData != null ? chatData.getDataPointCount() : 0);
        count += (friendsData != null ? friendsData.getDataPointCount() : 0);
        count += (clanData != null ? clanData.getDataPointCount() : 0);
        count += (tradeData != null ? tradeData.getDataPointCount() : 0);
        count += (interfaceData != null ? interfaceData.getDataPointCount() : 0);
        count += (dialogueData != null ? dialogueData.getDataPointCount() : 0);
        count += (shopData != null ? shopData.getDataPointCount() : 0);
        count += (bankData != null ? bankData.getDataPointCount() : 0);
        count += (systemMetrics != null ? systemMetrics.getDataPointCount() : 0);
        count += (errorData != null ? errorData.getDataPointCount() : 0);
        count += (timingBreakdown != null ? timingBreakdown.getDataPointCount() : 0);
        
        return count;
    }
    
    /**
     * Get a summary of the data collection
     * @return Data collection summary
     */
    public AnalysisResults.DataCollectionSummary getSummary()
    {
        return AnalysisResults.DataCollectionSummary.builder()
            .sessionId(sessionId)
            .tickNumber(tickNumber)
            .timestamp(timestamp)
            .totalDataPoints(getTotalDataPoints())
            .processingTimeMs(processingTimeNanos != null ? processingTimeNanos / 1_000_000 : 0)
            .qualityScore(qualityScore != null ? qualityScore.getOverallScore() : 0.0)
            .hasSecurityAnalysis(securityAnalysis != null)
            .hasBehavioralAnalysis(behavioralAnalysis != null)
            .build();
    }
    
    /**
     * Validate that this tick data collection is complete and valid
     * @return True if valid, false otherwise
     */
    public boolean isValid()
    {
        return sessionId != null && 
               tickNumber != null && 
               timestamp != null && 
               gameState != null &&
               processingTimeNanos != null &&
               processingTimeNanos > 0;
    }
    
    /**
     * Get the memory footprint estimate of this data collection
     * @return Estimated memory usage in bytes
     */
    public long getEstimatedMemoryUsage()
    {
        // Base object overhead + primitive fields
        long baseSize = 64 + (8 * 4); // Object header + 4 reference fields
        
        // Add estimated sizes of complex objects
        baseSize += (gameState != null ? gameState.getEstimatedSize() : 0);
        baseSize += (delta != null ? delta.getEstimatedSize() : 0);
        baseSize += (playerData != null ? playerData.getEstimatedSize() : 0);
        baseSize += (worldData != null ? worldData.getEstimatedSize() : 0);
        baseSize += (mouseInput != null ? mouseInput.getEstimatedSize() : 0);
        baseSize += (combatData != null ? combatData.getEstimatedSize() : 0);
        baseSize += (chatData != null ? chatData.getEstimatedSize() : 0);
        baseSize += (interfaceData != null ? interfaceData.getEstimatedSize() : 0);
        baseSize += (systemMetrics != null ? systemMetrics.getEstimatedSize() : 0);
        
        return baseSize;
    }
}