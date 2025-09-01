/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

/**
 * Central orchestrator for all data collection activities
 * 
 * Coordinates the collection of data across all specialized collectors:
 * - PlayerDataCollector: Player vitals, stats, location, equipment
 * - WorldDataCollector: Environment, NPCs, objects, projectiles  
 * - InputDataCollector: Mouse, keyboard, camera tracking
 * - CombatDataCollector: Combat events, animations, damage
 * - SocialDataCollector: Chat, clan, trade interactions
 * - InterfaceDataCollector: UI interactions, banking, widgets
 * - SystemMetricsCollector: Performance metrics, optimization
 * 
 * Migrated from DataCollectionManager main orchestration logic
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
public class DataCollectionOrchestrator
{
    // Specialized collectors
    private final PlayerDataCollector playerCollector;
    private final WorldDataCollector worldCollector;
    private final InputDataCollector inputCollector;
    private final CombatDataCollector combatCollector;
    private final SocialDataCollector socialCollector;
    private final InterfaceDataCollector interfaceCollector;
    private final SystemMetricsCollector systemCollector;
    
    // State tracking
    private GameStateSnapshot lastSnapshot;
    private volatile boolean isShutdown = false;
    
    public DataCollectionOrchestrator(
        PlayerDataCollector playerCollector,
        WorldDataCollector worldCollector,
        InputDataCollector inputCollector,
        CombatDataCollector combatCollector,
        SocialDataCollector socialCollector,
        InterfaceDataCollector interfaceCollector,
        SystemMetricsCollector systemCollector)
    {
        this.playerCollector = playerCollector;
        this.worldCollector = worldCollector;
        this.inputCollector = inputCollector;
        this.combatCollector = combatCollector;
        this.socialCollector = socialCollector;
        this.interfaceCollector = interfaceCollector;
        this.systemCollector = systemCollector;
        
        log.debug("DataCollectionOrchestrator initialized with all collectors");
    }
    
    /**
     * Collect all data for a single game tick - MAIN ORCHESTRATION METHOD
     */
    public TickDataCollection collectAllData(Integer sessionId, int tickNumber, 
                                           GameStateSnapshot gameStateSnapshot, 
                                           GameStateDelta gameStateDelta)
    {
        log.debug("DEBUG: collectAllData called - sessionId: {}, tickNumber: {}, shutdown: {}", 
            sessionId, tickNumber, isShutdown);
            
        if (isShutdown) {
            log.warn("DataCollectionOrchestrator is shutdown, skipping data collection");
            return null;
        }
        
        long startTime = System.nanoTime();
        
        // First-tick optimization
        systemCollector.handleFirstTick();
        
        try {
            TickDataCollection.TickDataCollectionBuilder builder = TickDataCollection.builder()
                .sessionId(sessionId)
                .tickNumber(tickNumber)
                .timestamp(System.currentTimeMillis())
                .gameState(gameStateSnapshot)
                .delta(gameStateDelta);
            
            log.debug("DEBUG: Builder created - sessionId: {}, tickNumber: {}, gameState: {}", 
                sessionId, tickNumber, gameStateSnapshot != null ? "present" : "null");
            
            // Orchestrate data collection across all collectors
            long componentStart = System.nanoTime();
            try {
                log.debug("DEBUG: Calling playerCollector.collectPlayerData");
                playerCollector.collectPlayerData(builder);
                log.debug("DEBUG: playerCollector.collectPlayerData completed successfully");
            } catch (Exception e) {
                log.error("ERROR: Player data collection failed", e);
                throw e; // Re-throw to identify if this is the source of the issue
            } finally {
                systemCollector.recordComponentTiming("player_data", System.nanoTime() - componentStart);
            }
            
            componentStart = System.nanoTime();
            try {
                worldCollector.collectWorldData(builder);
            } catch (Exception e) {
                log.warn("Error collecting world data", e);
            } finally {
                systemCollector.recordComponentTiming("world_data", System.nanoTime() - componentStart);
            }
            
            componentStart = System.nanoTime();
            try {
                inputCollector.collectInputData(builder);
                inputCollector.calculateMovementAnalytics(builder);
            } catch (Exception e) {
                log.warn("Error collecting input data", e);
            } finally {
                systemCollector.recordComponentTiming("input_data", System.nanoTime() - componentStart);
            }
            
            componentStart = System.nanoTime();
            try {
                combatCollector.collectCombatData(builder);
            } catch (Exception e) {
                log.warn("Error collecting combat data", e);
            } finally {
                systemCollector.recordComponentTiming("combat_data", System.nanoTime() - componentStart);
            }
            
            // Collect additional data
            socialCollector.collectSocialData(builder);
            interfaceCollector.collectInterfaceData(builder);
            systemCollector.collectSystemMetrics(builder);
            
            // Build final result
            long processingTime = System.nanoTime() - startTime;
            TickDataCollection result = builder
                .processingTimeNanos(processingTime)
                .build();
                
            log.info("[ORCHESTRATOR-DEBUG] TickDataCollection built - sessionId: {}, tickNumber: {}, isValid: {}", 
                result.getSessionId(), result.getTickNumber(), result.isValid());
                
            // Debug validation details if invalid
            if (!result.isValid()) {
                log.error("VALIDATION FAILED - Details: sessionId={}, tickNumber={}, timestamp={}, gameState={}, processingTimeNanos={}",
                    result.getSessionId(), result.getTickNumber(), result.getTimestamp(),
                    result.getGameState() != null ? "present" : "NULL", result.getProcessingTimeNanos());
            }
            
            // Update performance metrics
            systemCollector.updatePerformanceMetrics(processingTime);
            
            // Store snapshot for next delta calculation
            lastSnapshot = gameStateSnapshot;
            
            return result;
            
        } catch (Exception e) {
            log.error("**DATA COLLECTION FAILED** CRITICAL: Error in orchestrated data collection for tick {}", tickNumber, e);
            TickDataCollection errorData = createErrorTickData(sessionId, tickNumber, e);
            log.error("**DATA COLLECTION FAILED** Created error tick data - valid: {}, sessionId: {}, tickNumber: {}", 
                errorData != null ? errorData.isValid() : "null", 
                errorData != null ? errorData.getSessionId() : "null",
                errorData != null ? errorData.getTickNumber() : "null");
            return errorData;
        }
    }
    
    /**
     * Create error tick data for failed collections
     */
    private TickDataCollection createErrorTickData(Integer sessionId, int tickNumber, Exception e)
    {
        
        return TickDataCollection.builder()
            .sessionId(sessionId)
            .tickNumber(tickNumber)
            .timestamp(System.currentTimeMillis())
            .gameState(GameStateSnapshot.builder().build())
            .processingTimeNanos(1L)
            .build();
    }
    
    /**
     * Shutdown the orchestrator and all collectors
     */
    public void shutdown()
    {
        isShutdown = true;
        log.debug("DataCollectionOrchestrator shutdown completed");
    }
    
    /**
     * Get the last game state snapshot
     */
    public GameStateSnapshot getLastSnapshot()
    {
        return lastSnapshot;
    }
    
    /**
     * Check if orchestrator is shutdown
     */
    public boolean isShutdown()
    {
        return isShutdown;
    }
    
    /**
     * Get individual collectors for event forwarding
     */
    public PlayerDataCollector getPlayerCollector() { return playerCollector; }
    public WorldDataCollector getWorldCollector() { return worldCollector; }
    public InputDataCollector getInputCollector() { return inputCollector; }
    public CombatDataCollector getCombatCollector() { return combatCollector; }
    public SocialDataCollector getSocialCollector() { return socialCollector; }
    public InterfaceDataCollector getInterfaceCollector() { return interfaceCollector; }
    public SystemMetricsCollector getSystemCollector() { return systemCollector; }
}