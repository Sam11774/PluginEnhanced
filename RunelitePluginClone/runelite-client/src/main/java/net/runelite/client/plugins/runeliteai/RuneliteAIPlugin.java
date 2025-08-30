/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import net.runelite.client.input.MouseListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;

/**
 * RuneLiteAI Plugin - Advanced OSRS Data Collection System
 * 
 * Main plugin orchestrator that coordinates comprehensive data collection
 * across 680+ data points per game tick for AI and machine learning applications.
 * 
 * Features:
 * - Real-time data collection with <2ms processing per tick
 * - PostgreSQL integration with session-based organization
 * - Security analytics and behavioral pattern detection
 * - Performance monitoring and quality validation
 * - Multi-threaded architecture with parallel processing
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
@PluginDescriptor(
    name = "RuneLite AI",
    description = "Advanced OSRS Data Collection System for AI/ML Training - 680+ data points per tick",
    tags = {"ai", "data", "collection", "machine learning", "analytics", "performance", "automation"}
)
public class RuneliteAIPlugin extends Plugin
{
    // Core dependencies
    @Inject private Client client;
    @Inject private ConfigManager configManager;
    @Inject private ItemManager itemManager;
    @Inject private KeyManager keyManager;
    @Inject private MouseManager mouseManager;
    @Inject private ClientToolbar clientToolbar;
    @Inject private OverlayManager overlayManager;
    
    // Plugin configuration
    @Inject private RuneliteAIConfig config;
    
    // Overlay
    @Inject private RuneliteAIOverlay overlay;
    
    // Core managers
    private DataCollectionManager dataCollectionManager;
    private DatabaseManager databaseManager;
    private SecurityAnalyticsManager securityAnalyticsManager;
    private BehavioralAnalysisManager behavioralAnalysisManager;
    private QualityValidator qualityValidator;
    private PerformanceMonitor performanceMonitor;
    
    // Enhanced intelligence services (placeholder for future implementation)
    // @Inject private ClickIntelligenceService clickIntelligenceService;
    
    // Enhanced keyboard tracking
    private final AtomicInteger keyPressCountSinceTick = new AtomicInteger(0);
    private final ConcurrentHashMap<Integer, Long> recentKeyPresses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Boolean> currentlyHeldKeys = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> keyPressTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<DataStructures.KeyPressData> recentKeyPressDetails = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<DataStructures.KeyCombinationData> recentKeyCombinations = new ConcurrentLinkedQueue<>();
    
    // Enhanced mouse tracking
    private final ConcurrentHashMap<Integer, Long> mouseButtonPressTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Boolean> currentlyHeldMouseButtons = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<DataStructures.MouseButtonData> recentMouseButtonDetails = new ConcurrentLinkedQueue<>();
    private volatile boolean middleMouseDragging = false;
    private volatile int middleMouseStartPitch = 0;
    private volatile int middleMouseStartYaw = 0;
    private final KeyListener keyListener = new KeyListener()
    {
        @Override
        public void keyPressed(KeyEvent e)
        {
            keyPressCountSinceTick.incrementAndGet();
            long pressTime = System.currentTimeMillis();
            recentKeyPresses.put(e.getKeyCode(), pressTime);
            currentlyHeldKeys.put(e.getKeyCode(), true);
            keyPressTimestamps.put(e.getKeyCode(), pressTime);
            
            // Create detailed key press data
            recordDetailedKeyPress(e, pressTime);
            
            // Check for key combinations
            checkForKeyCombinations(e, pressTime);
        }
        
        @Override
        public void keyReleased(KeyEvent e)
        {
            currentlyHeldKeys.remove(e.getKeyCode());
            Long pressTime = keyPressTimestamps.remove(e.getKeyCode());
            
            if (pressTime != null) {
                long releaseTime = System.currentTimeMillis();
                int duration = (int) (releaseTime - pressTime);
                
                // Update the key press data with release information
                updateKeyPressRelease(e.getKeyCode(), releaseTime, duration);
            }
        }
        
        @Override
        public void keyTyped(KeyEvent e) 
        {
            // Track typed characters if needed
        }
    };
    
    // Enhanced mouse tracking
    private final MouseListener mouseListener = new MouseListener()
    {
        @Override
        public MouseEvent mousePressed(MouseEvent e)
        {
            long pressTime = System.currentTimeMillis();
            mouseButtonPressTimestamps.put(e.getButton(), pressTime);
            currentlyHeldMouseButtons.put(e.getButton(), true);
            
            // Record detailed mouse button press
            recordDetailedMousePress(e, pressTime);
            
            // Special handling for middle mouse (camera rotation)
            if (e.getButton() == MouseEvent.BUTTON2) {
                handleMiddleMousePress(e, pressTime);
            }
            
            return e; // RuneLite MouseListener returns MouseEvent
        }
        
        @Override
        public MouseEvent mouseReleased(MouseEvent e)
        {
            currentlyHeldMouseButtons.remove(e.getButton());
            Long pressTime = mouseButtonPressTimestamps.remove(e.getButton());
            
            if (pressTime != null) {
                long releaseTime = System.currentTimeMillis();
                int duration = (int) (releaseTime - pressTime);
                
                // Update mouse button data with release information
                updateMouseButtonRelease(e.getButton(), e.getX(), e.getY(), releaseTime, duration);
                
                // Special handling for middle mouse release
                if (e.getButton() == MouseEvent.BUTTON2) {
                    handleMiddleMouseRelease(e, releaseTime, duration);
                }
            }
            
            return e; // RuneLite MouseListener returns MouseEvent
        }
        
        @Override
        public MouseEvent mouseClicked(MouseEvent e)
        {
            return e; // Pass through
        }
        
        @Override
        public MouseEvent mouseEntered(MouseEvent e)
        {
            return e; // Pass through
        }
        
        @Override
        public MouseEvent mouseExited(MouseEvent e)
        {
            return e; // Pass through
        }
        
        @Override
        public MouseEvent mouseMoved(MouseEvent e)
        {
            return e; // Pass through
        }
        
        @Override
        public MouseEvent mouseDragged(MouseEvent e)
        {
            // Track dragging for middle mouse camera rotation
            if (middleMouseDragging && e.getButton() == MouseEvent.BUTTON2) {
                // Update real-time camera rotation tracking
            }
            return e; // Pass through
        }
    };
    
    // Session management
    private Integer currentSessionId;
    private boolean isPluginActive = false;
    private long startupTimestamp;
    private int tickCounter = 0;
    
    // Threading
    private ScheduledExecutorService executor;
    
    // Performance tracking
    private long totalProcessingTimeNanos = 0;
    private int processedTicks = 0;
    private long lastPerformanceReport = 0;
    
    @Override
    protected void startUp() throws Exception
    {
        log.info("Starting RuneLiteAI Plugin v3.1.0...");
        startupTimestamp = System.currentTimeMillis();
        
        // Initialize threading
        executor = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "RuneliteAI-Core");
            t.setDaemon(true);
            return t;
        });
        
        // Initialize managers
        initializeManagers();
        
        // Note: Database session will be initialized when player logs in
        // This prevents duplicate sessions from being created
        
        isPluginActive = true;
        tickCounter = 0;
        
        // Register overlay
        overlayManager.add(overlay);
        
        // Register input listeners
        keyManager.registerKeyListener(keyListener);
        mouseManager.registerMouseListener(mouseListener);
        
        log.info("RuneLiteAI Plugin startup completed in {}ms - waiting for login to initialize database session", 
                System.currentTimeMillis() - startupTimestamp);
    }
    
    @Override
    protected void shutDown() throws Exception
    {
        log.info("Shutting down RuneLiteAI Plugin...");
        isPluginActive = false;
        
        // Remove overlay
        overlayManager.remove(overlay);
        
        // Unregister input listeners
        keyManager.unregisterKeyListener(keyListener);
        mouseManager.unregisterMouseListener(mouseListener);
        
        // Shutdown managers
        if (dataCollectionManager != null) {
            dataCollectionManager.shutdown();
        }
        
        if (databaseManager != null) {
            try {
                databaseManager.finalizeSession(currentSessionId);
                databaseManager.shutdown();
            } catch (Exception e) {
                log.warn("Error during database shutdown", e);
            }
        }
        
        if (securityAnalyticsManager != null) {
            securityAnalyticsManager.shutdown();
        }
        
        if (behavioralAnalysisManager != null) {
            behavioralAnalysisManager.shutdown();
        }
        
        // Shutdown executor
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("RuneLiteAI Plugin shutdown completed. Total ticks processed: {}", processedTicks);
    }
    
    @Provides
    RuneliteAIConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(RuneliteAIConfig.class);
    }
    
    /**
     * Initialize all manager components
     */
    private void initializeManagers()
    {
        try {
            // Core data collection
            dataCollectionManager = new DataCollectionManager(client, itemManager, configManager, this);
            
            // Database management
            if (config.enableDatabaseLogging()) {
                try {
                    System.out.println("[PLUGIN-INIT] Attempting to create DatabaseManager...");
                    log.info("[PLUGIN-INIT] Creating DatabaseManager with database logging enabled");
                    databaseManager = new DatabaseManager(client, itemManager);
                    System.out.println("[PLUGIN-INIT] DatabaseManager created successfully!");
                    log.info("[PLUGIN-INIT] DatabaseManager initialized successfully");
                } catch (Exception e) {
                    System.out.println("[PLUGIN-INIT] DatabaseManager creation FAILED: " + e.getMessage());
                    log.error("[PLUGIN-INIT] Failed to initialize DatabaseManager - database logging will be disabled", e);
                    databaseManager = null;
                }
            }
            
            // Security and behavioral analysis
            if (config.enableSecurityAnalytics()) {
                securityAnalyticsManager = new SecurityAnalyticsManager(client, configManager);
                behavioralAnalysisManager = new BehavioralAnalysisManager(client, configManager);
            }
            
            // Quality and performance monitoring
            if (config.enableQualityValidation()) {
                qualityValidator = new QualityValidator();
            }
            
            if (config.enablePerformanceMonitoring()) {
                performanceMonitor = new PerformanceMonitor();
            }
            
            log.info("All managers initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize managers", e);
            throw new RuntimeException("Manager initialization failed", e);
        }
    }
    
    /**
     * Main game tick event handler - core data collection orchestrator
     */
    @Subscribe
    public void onGameTick(GameTick event)
    {
        System.out.println("[GAME-TICK-DEBUG] onGameTick called - isPluginActive: " + isPluginActive + ", gameState: " + (client != null ? client.getGameState() : "null"));
        log.debug("[GAME-TICK-DEBUG] onGameTick called - isPluginActive: {}, gameState: {}", isPluginActive, client != null ? client.getGameState() : "null");
        
        if (!isPluginActive || client.getGameState() != GameState.LOGGED_IN) {
            System.out.println("[GAME-TICK-DEBUG] Skipping tick - conditions not met");
            log.debug("[GAME-TICK-DEBUG] Skipping tick - isPluginActive: {}, gameState: {}", isPluginActive, client != null ? client.getGameState() : "null");
            return;
        }
        
        long tickStartTime = System.nanoTime();
        tickCounter++;
        
        try {
            // Create game state snapshots
            GameStateSnapshot currentSnapshot = createGameStateSnapshot();
            GameStateDelta delta = calculateStateDelta(currentSnapshot);
            
            // Collect all data points
            log.debug("[TICK-{}] Collecting data with sessionId {}", tickCounter, currentSessionId);
            TickDataCollection tickData = dataCollectionManager.collectAllData(
                currentSessionId, tickCounter, currentSnapshot, delta);
            log.debug("[TICK-{}] Data collection result - tickData: {}, valid: {}", 
                tickCounter,
                tickData != null ? "created" : "null", 
                tickData != null ? tickData.isValid() : "N/A");
            
            // Quality validation
            if (qualityValidator != null && config.enableQualityValidation()) {
                AnalysisResults.QualityScore qualityScore = qualityValidator.validateData(tickData);
                tickData.setQualityScore(qualityScore);
                
                if (qualityScore.getOverallScore() < config.minimumQualityThreshold()) {
                    log.warn("Low quality data detected: {}", qualityScore.getOverallScore());
                }
            }
            
            // Security analysis
            if (securityAnalyticsManager != null && config.enableSecurityAnalytics()) {
                AnalysisResults.SecurityAnalysisResult securityResult = securityAnalyticsManager.analyzeCurrentState(tickData);
                tickData.setSecurityAnalysis(securityResult);
                
                if (securityResult.getRiskLevel().equals("HIGH")) {
                    log.warn("High automation risk detected: {}", securityResult.getAutomationScore());
                }
            }
            
            // Behavioral analysis
            if (behavioralAnalysisManager != null && config.enableBehavioralAnalysis()) {
                AnalysisResults.BehavioralAnalysisResult behavioralResult = behavioralAnalysisManager.analyzePattern(tickData);
                tickData.setBehavioralAnalysis(behavioralResult);
            }
            
            // Database storage
            if (databaseManager != null && config.enableDatabaseLogging()) {
                log.debug("[TICK-{}] Submitting data to database", tickCounter);
                executor.submit(() -> {
                    try {
                        log.debug("[TICK-{}] Attempting to store data", tickCounter);
                        log.debug("[TICK-{}] About to call databaseManager.storeTickData", tickCounter);
                        databaseManager.storeTickData(tickData);
                        log.debug("[TICK-{}] Database storage completed (after storeTickData call)", tickCounter);
                        log.debug("DEBUG: Successfully submitted tick {} to database batch", tickCounter);
                    } catch (Exception e) {
                        log.error("Database storage failed for tick {} - Exception: {}", tickCounter, e.getMessage(), e);
                    }
                });
            } else {
                log.debug("DEBUG: Database storage skipped - databaseManager: {}, config.enableDatabaseLogging(): {}", 
                    databaseManager != null ? "available" : "null", 
                    databaseManager != null ? config.enableDatabaseLogging() : "N/A");
            }
            
            // Performance tracking
            long processingTime = System.nanoTime() - tickStartTime;
            updatePerformanceMetrics(processingTime);
            
            // Performance warnings
            long processingTimeMs = processingTime / 1_000_000;
            if (processingTimeMs > 2) {
                log.warn("Slow tick processing: {}ms (target <2ms)", processingTimeMs);
            }
            
            // Batch processing handled automatically by DatabaseManager
            
        } catch (Exception e) {
            log.error("Error during game tick processing", e);
        }
    }
    
    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (!isPluginActive) return;
        
        GameState newState = event.getGameState();
        log.debug("Game state changed to: {}", newState);
        
        if (newState == GameState.LOGGED_IN) {
            // Player logged in - initialize database session now
            if (currentSessionId == null && config.enableDatabaseLogging()) {
                try {
                    currentSessionId = databaseManager.initializeSession();
                    log.info("Database session initialized on login: {}", currentSessionId);
                } catch (Exception e) {
                    log.error("Failed to initialize database session on login", e);
                    if (config.requireDatabaseConnection()) {
                        log.error("Database connection required but failed - plugin may not function correctly");
                    }
                }
            }
        } else if (newState == GameState.LOGIN_SCREEN || newState == GameState.CONNECTION_LOST) {
            // Player logged out or disconnected
            if (currentSessionId != null && config.enableDatabaseLogging()) {
                try {
                    databaseManager.finalizeSession(currentSessionId);
                    currentSessionId = null;
                    log.info("Session finalized due to logout/disconnect");
                } catch (Exception e) {
                    log.error("Failed to finalize session", e);
                }
            }
        }
    }
    
    @Subscribe
    public void onPlayerSpawned(PlayerSpawned event)
    {
        if (!isPluginActive) return;
        
        Player player = event.getPlayer();
        if (player != null && dataCollectionManager != null) {
            dataCollectionManager.registerPlayer(player);
        }
    }
    
    @Subscribe
    public void onPlayerDespawned(PlayerDespawned event)
    {
        if (!isPluginActive) return;
        
        Player player = event.getPlayer();
        if (player != null && dataCollectionManager != null) {
            dataCollectionManager.unregisterPlayer(player);
        }
    }
    
    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        if (!isPluginActive) return;
        
        NPC npc = event.getNpc();
        if (npc != null && dataCollectionManager != null) {
            dataCollectionManager.registerNPC(npc);
        }
    }
    
    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        if (!isPluginActive) return;
        
        NPC npc = event.getNpc();
        if (npc != null && dataCollectionManager != null) {
            dataCollectionManager.unregisterNPC(npc);
        }
    }
    
    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (!isPluginActive || dataCollectionManager == null) return;
        
        dataCollectionManager.recordChatMessage(event);
        
        // Security analysis for chat patterns
        if (securityAnalyticsManager != null && config.enableSecurityAnalytics()) {
            securityAnalyticsManager.analyzeChatPattern(event);
        }
    }
    
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (!isPluginActive || dataCollectionManager == null) return;
        
        dataCollectionManager.recordItemContainerChange(event);
    }
    
    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (!isPluginActive || dataCollectionManager == null) return;
        
        dataCollectionManager.recordStatChange(event);
    }
    
    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        if (!isPluginActive || dataCollectionManager == null) return;
        
        dataCollectionManager.recordHitsplat(event);
    }
    
    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        if (!isPluginActive || dataCollectionManager == null) return;
        
        dataCollectionManager.recordAnimationChange(event);
    }
    
    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        if (!isPluginActive || dataCollectionManager == null) return;
        
        dataCollectionManager.recordInteractionChange(event);
    }
    
    @Subscribe
    public void onProjectileMoved(ProjectileMoved event)
    {
        if (!isPluginActive || dataCollectionManager == null) return;
        
        dataCollectionManager.recordProjectile(event);
    }
    
    @Subscribe
    public void onGroundObjectSpawned(GroundObjectSpawned event)
    {
        if (!isPluginActive || dataCollectionManager == null) return;
        
        dataCollectionManager.recordGroundObject(event.getGroundObject());
    }
    
    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        if (!isPluginActive || dataCollectionManager == null) return;
        
        dataCollectionManager.recordGameObject(event.getGameObject());
    }
    
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (!isPluginActive || dataCollectionManager == null) return;
        
        try {
            // Record basic click context (existing functionality)
            dataCollectionManager.recordClickContext(event);
            
            // Enhanced click analytics could be added here in the future
            // For now, the existing click context system provides comprehensive data
            
        } catch (Exception e) {
            log.error("Failed to process MenuOptionClicked event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Create a comprehensive snapshot of the current game state
     */
    private GameStateSnapshot createGameStateSnapshot()
    {
        Player localPlayer = client.getLocalPlayer();
        log.debug("DEBUG: Creating GameStateSnapshot - localPlayer: {}, gameState: {}, tickCount: {}",
            localPlayer != null ? "present" : "null", client.getGameState(), client.getTickCount());
            
        return new GameStateSnapshot(
            System.currentTimeMillis(),
            client.getTickCount(),
            client.getGameState(),
            localPlayer,
            client.getPlane(),
            client.getBaseX(),
            client.getBaseY()
        );
    }
    
    /**
     * Calculate the delta between current and previous game states
     */
    private GameStateDelta calculateStateDelta(GameStateSnapshot current)
    {
        // Implementation for calculating state changes
        return new GameStateDelta(current);
    }
    
    /**
     * Update performance metrics and reporting
     */
    private void updatePerformanceMetrics(long processingTimeNanos)
    {
        totalProcessingTimeNanos += processingTimeNanos;
        processedTicks++;
        
        if (performanceMonitor != null && config.enablePerformanceMonitoring()) {
            performanceMonitor.recordTickProcessingTime(processingTimeNanos);
        }
        
        // Performance reporting every 100 ticks
        if (processedTicks % 100 == 0) {
            long avgProcessingTimeMs = (totalProcessingTimeNanos / processedTicks) / 1_000_000;
            
            if (System.currentTimeMillis() - lastPerformanceReport > 60000) {
                log.info("Performance Report - Avg processing time: {}ms, Ticks processed: {}", 
                        avgProcessingTimeMs, processedTicks);
                lastPerformanceReport = System.currentTimeMillis();
            }
        }
    }
    
    /**
     * Get current plugin status information
     */
    public AnalysisResults.PluginStatus getStatus()
    {
        return AnalysisResults.PluginStatus.builder()
            .active(isPluginActive)
            .sessionId(currentSessionId)
            .ticksProcessed(processedTicks)
            .averageProcessingTimeMs(processedTicks > 0 ? 
                (totalProcessingTimeNanos / processedTicks) / 1_000_000 : 0)
            .databaseConnected(databaseManager != null && databaseManager.isConnected())
            .build();
    }
    
    /**
     * Get keyboard activity since last call and reset counter
     */
    public int getAndResetKeyPressCount()
    {
        return keyPressCountSinceTick.getAndSet(0);
    }
    
    /**
     * Get recent key presses (within last 5 seconds) and clean up old entries
     */
    public ConcurrentHashMap<Integer, Long> getAndCleanRecentKeyPresses()
    {
        long now = System.currentTimeMillis();
        ConcurrentHashMap<Integer, Long> result = new ConcurrentHashMap<>(recentKeyPresses);
        
        // Clean up old entries (older than 5 seconds)
        recentKeyPresses.entrySet().removeIf(entry -> (now - entry.getValue()) > 5000);
        
        return result;
    }
    
    /**
     * Get count of currently held keys
     */
    public int getCurrentlyHeldKeysCount()
    {
        return currentlyHeldKeys.size();
    }
    
    /**
     * Get map of currently held keys
     */
    public Map<Integer, Boolean> getCurrentlyHeldKeys()
    {
        return new HashMap<>(currentlyHeldKeys);
    }
    
    /**
     * Get plugin startup timestamp
     */
    public long getStartupTimestamp()
    {
        return startupTimestamp;
    }
    
    /**
     * Estimate data points per tick based on current environment
     */
    public int getEstimatedDataPointsPerTick()
    {
        if (!isPluginActive || client.getGameState() != GameState.LOGGED_IN)
        {
            return 0;
        }
        
        // Base data points (player state, world info, etc.)
        int basePoints = 159; // Minimum baseline
        
        // Add points based on environment
        if (client.getLocalPlayer() != null)
        {
            // NPCs in area
            if (client.getNpcs() != null)
            {
                basePoints += Math.min(client.getNpcs().size() * 8, 200);
            }
            
            // Other players in area
            if (client.getPlayers() != null)
            {
                basePoints += Math.min(client.getPlayers().size() * 12, 150);
            }
            
            // Ground items and objects
            basePoints += 50; // Estimated for objects and ground items
            
            // Combat state
            if (client.getLocalPlayer().getInteracting() != null)
            {
                basePoints += 75; // Combat data points
            }
            
            // Chat and social interactions
            basePoints += 25; // Social data points
        }
        
        return Math.min(basePoints, 741); // Cap at known maximum
    }
    
    // =================================================================================
    // ENHANCED INPUT TRACKING METHODS
    // =================================================================================
    
    /**
     * Record detailed key press information
     */
    private void recordDetailedKeyPress(KeyEvent e, long pressTime)
    {
        try {
            String keyName = getKeyName(e.getKeyCode());
            String keyChar = e.getKeyChar() != KeyEvent.CHAR_UNDEFINED ? String.valueOf(e.getKeyChar()) : null;
            
            DataStructures.KeyPressData keyPress = DataStructures.KeyPressData.builder()
                .keyCode(e.getKeyCode())
                .keyName(keyName)
                .keyChar(keyChar)
                .pressTimestamp(pressTime)
                .releaseTimestamp(null)
                .durationMs(null)
                .isFunctionKey(isFunctionKey(e.getKeyCode()))
                .isModifierKey(isModifierKey(e.getKeyCode()))
                .isMovementKey(isMovementKey(e.getKeyCode()))
                .isActionKey(isActionKey(e.getKeyCode()))
                .ctrlHeld(e.isControlDown())
                .altHeld(e.isAltDown())
                .shiftHeld(e.isShiftDown())
                .build();
                
            recentKeyPressDetails.offer(keyPress);
            
            // Clean up old entries (keep last 100)
            while (recentKeyPressDetails.size() > 100) {
                recentKeyPressDetails.poll();
            }
        } catch (Exception ex) {
            log.warn("Error recording key press details", ex);
        }
    }
    
    /**
     * Update key press with release information
     */
    private void updateKeyPressRelease(int keyCode, long releaseTime, int duration)
    {
        try {
            // Find the most recent key press for this key code
            DataStructures.KeyPressData keyPress = recentKeyPressDetails.stream()
                .filter(kp -> kp.getKeyCode() != null && kp.getKeyCode().equals(keyCode) && kp.getReleaseTimestamp() == null)
                .reduce((first, second) -> second) // Get the last one
                .orElse(null);
                
            if (keyPress != null) {
                keyPress.setReleaseTimestamp(releaseTime);
                keyPress.setDurationMs(duration);
            }
        } catch (Exception ex) {
            log.warn("Error updating key release", ex);
        }
    }
    
    /**
     * Check for key combinations and record them
     */
    private void checkForKeyCombinations(KeyEvent e, long pressTime)
    {
        try {
            List<Integer> modifiers = new ArrayList<>();
            StringBuilder combination = new StringBuilder();
            
            // Check for modifiers
            if (e.isControlDown()) {
                modifiers.add(KeyEvent.VK_CONTROL);
                combination.append("Ctrl+");
            }
            if (e.isAltDown()) {
                modifiers.add(KeyEvent.VK_ALT);
                combination.append("Alt+");
            }
            if (e.isShiftDown()) {
                modifiers.add(KeyEvent.VK_SHIFT);
                combination.append("Shift+");
            }
            
            // Add main key
            String keyName = getKeyName(e.getKeyCode());
            combination.append(keyName);
            
            // FIXED: Enhanced key combination detection with debugging
            log.debug("[KEY-COMBO-DEBUG] Key pressed: {} ({}), Modifiers detected: {}, Is combination: {}", 
                keyName, e.getKeyCode(), modifiers.size(), !modifiers.isEmpty());
            
            // Only record if it's actually a combination (modifier + non-modifier key)
            // Also exclude if the key itself is a modifier key to avoid double-recording
            boolean isModifierKey = (e.getKeyCode() == KeyEvent.VK_CONTROL || 
                                   e.getKeyCode() == KeyEvent.VK_ALT || 
                                   e.getKeyCode() == KeyEvent.VK_SHIFT ||
                                   e.getKeyCode() == KeyEvent.VK_META);
            
            if (!modifiers.isEmpty() && !isModifierKey) {
                log.debug("[KEY-COMBO-DEBUG] Recording combination: '{}' with {} modifiers", 
                    combination.toString(), modifiers.size());
                    
                DataStructures.KeyCombinationData keyCombination = DataStructures.KeyCombinationData.builder()
                    .keyCombination(combination.toString())
                    .primaryKeyCode(e.getKeyCode())
                    .modifierKeys(modifiers)
                    .combinationTimestamp(pressTime)
                    .durationMs(null)
                    .combinationType(getCombinatioType(e.getKeyCode(), modifiers))
                    .isGameHotkey(isFunctionKey(e.getKeyCode()))
                    .isSystemShortcut(isSystemShortcut(e.getKeyCode(), modifiers))
                    .build();
                    
                recentKeyCombinations.offer(keyCombination);
                log.debug("[KEY-COMBO-DEBUG] Successfully added key combination to queue. Queue size: {}", 
                    recentKeyCombinations.size());
                
                // Clean up old entries
                while (recentKeyCombinations.size() > 50) {
                    recentKeyCombinations.poll();
                }
            } else {
                log.debug("[KEY-COMBO-DEBUG] Not recording - isModifierKey: {}, modifiersEmpty: {}", 
                    isModifierKey, modifiers.isEmpty());
            }
        } catch (Exception ex) {
            log.warn("Error checking key combinations", ex);
        }
    }
    
    /**
     * Record detailed mouse button press
     */
    private void recordDetailedMousePress(MouseEvent e, long pressTime)
    {
        try {
            String buttonType = getButtonTypeName(e.getButton());
            
            DataStructures.MouseButtonData mouseButton = DataStructures.MouseButtonData.builder()
                .buttonType(buttonType)
                .buttonCode(e.getButton())
                .pressTimestamp(pressTime)
                .releaseTimestamp(null)
                .durationMs(null)
                .pressX(e.getX())
                .pressY(e.getY())
                .releaseX(null)
                .releaseY(null)
                .isClick(null) // Will be determined on release
                .isDrag(false)
                .isCameraRotation(e.getButton() == MouseEvent.BUTTON2)
                .cameraStartPitch(null)
                .cameraStartYaw(null)
                .cameraEndPitch(null)
                .cameraEndYaw(null)
                .rotationDistance(null)
                .build();
                
            recentMouseButtonDetails.offer(mouseButton);
            
            // Clean up old entries
            while (recentMouseButtonDetails.size() > 100) {
                recentMouseButtonDetails.poll();
            }
        } catch (Exception ex) {
            log.warn("Error recording mouse button press", ex);
        }
    }
    
    /**
     * Update mouse button with release information
     */
    private void updateMouseButtonRelease(int button, int releaseX, int releaseY, long releaseTime, int duration)
    {
        try {
            DataStructures.MouseButtonData mouseButton = recentMouseButtonDetails.stream()
                .filter(mb -> mb.getButtonCode() != null && mb.getButtonCode().equals(button) && mb.getReleaseTimestamp() == null)
                .reduce((first, second) -> second)
                .orElse(null);
                
            if (mouseButton != null) {
                mouseButton.setReleaseTimestamp(releaseTime);
                mouseButton.setDurationMs(duration);
                mouseButton.setReleaseX(releaseX);
                mouseButton.setReleaseY(releaseY);
                mouseButton.setIsClick(duration < 500); // Click vs hold
                
                // Check for drag
                if (mouseButton.getPressX() != null && mouseButton.getPressY() != null) {
                    int dragDistance = (int) Math.sqrt(Math.pow(releaseX - mouseButton.getPressX(), 2) + 
                                                     Math.pow(releaseY - mouseButton.getPressY(), 2));
                    mouseButton.setIsDrag(dragDistance > 5);
                }
            }
        } catch (Exception ex) {
            log.warn("Error updating mouse button release", ex);
        }
    }
    
    /**
     * Handle middle mouse press for camera rotation
     */
    private void handleMiddleMousePress(MouseEvent e, long pressTime)
    {
        try {
            middleMouseDragging = true;
            if (client != null) {
                middleMouseStartPitch = client.getCameraPitch();
                middleMouseStartYaw = client.getCameraYaw();
            }
        } catch (Exception ex) {
            log.warn("Error handling middle mouse press", ex);
        }
    }
    
    /**
     * Handle middle mouse release for camera rotation
     */
    private void handleMiddleMouseRelease(MouseEvent e, long releaseTime, int duration)
    {
        try {
            if (middleMouseDragging && client != null) {
                int endPitch = client.getCameraPitch();
                int endYaw = client.getCameraYaw();
                
                double rotationDistance = Math.sqrt(Math.pow(endPitch - middleMouseStartPitch, 2) + 
                                                  Math.pow(endYaw - middleMouseStartYaw, 2));
                
                // Update the mouse button data with camera info
                DataStructures.MouseButtonData mouseButton = recentMouseButtonDetails.stream()
                    .filter(mb -> mb.getButtonCode() != null && mb.getButtonCode().equals(MouseEvent.BUTTON2) && 
                                mb.getReleaseTimestamp() == null)
                    .reduce((first, second) -> second)
                    .orElse(null);
                    
                if (mouseButton != null) {
                    mouseButton.setCameraStartPitch(middleMouseStartPitch);
                    mouseButton.setCameraStartYaw(middleMouseStartYaw);
                    mouseButton.setCameraEndPitch(endPitch);
                    mouseButton.setCameraEndYaw(endYaw);
                    mouseButton.setRotationDistance(rotationDistance);
                    mouseButton.setIsCameraRotation(rotationDistance > 10); // Threshold for rotation
                }
            }
            
            middleMouseDragging = false;
        } catch (Exception ex) {
            log.warn("Error handling middle mouse release", ex);
        }
    }
    
    // =================================================================================
    // HELPER METHODS FOR INPUT CLASSIFICATION
    // =================================================================================
    
    private String getKeyName(int keyCode)
    {
        return KeyEvent.getKeyText(keyCode);
    }
    
    private boolean isFunctionKey(int keyCode)
    {
        return keyCode >= KeyEvent.VK_F1 && keyCode <= KeyEvent.VK_F12;
    }
    
    private boolean isModifierKey(int keyCode)
    {
        return keyCode == KeyEvent.VK_CONTROL || keyCode == KeyEvent.VK_ALT || 
               keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_META;
    }
    
    private boolean isMovementKey(int keyCode)
    {
        return keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_A || 
               keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_D ||
               keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN ||
               keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT;
    }
    
    private boolean isActionKey(int keyCode)
    {
        return keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ENTER ||
               keyCode == KeyEvent.VK_TAB || keyCode == KeyEvent.VK_ESCAPE;
    }
    
    private String getCombinatioType(int keyCode, List<Integer> modifiers)
    {
        if (isFunctionKey(keyCode)) return "FUNCTION";
        if (isMovementKey(keyCode)) return "MOVEMENT";
        if (modifiers.contains(KeyEvent.VK_ALT)) return "SHORTCUT";
        return "HOTKEY";
    }
    
    private boolean isSystemShortcut(int keyCode, List<Integer> modifiers)
    {
        return modifiers.contains(KeyEvent.VK_ALT) && keyCode == KeyEvent.VK_TAB;
    }
    
    private String getButtonTypeName(int button)
    {
        switch (button) {
            case MouseEvent.BUTTON1: return "LEFT";
            case MouseEvent.BUTTON2: return "MIDDLE";
            case MouseEvent.BUTTON3: return "RIGHT";
            default: return "UNKNOWN";
        }
    }
    
    /**
     * Get enhanced input data for current tick
     */
    public DataStructures.EnhancedInputData getEnhancedInputData()
    {
        try {
            List<DataStructures.KeyPressData> currentKeyPresses = new ArrayList<>(recentKeyPressDetails);
            List<DataStructures.MouseButtonData> currentMouseButtons = new ArrayList<>(recentMouseButtonDetails);
            List<DataStructures.KeyCombinationData> currentKeyCombinations = new ArrayList<>(recentKeyCombinations);
            
            // Clear the queues for next tick
            recentKeyPressDetails.clear();
            recentMouseButtonDetails.clear();
            recentKeyCombinations.clear();
            
            return DataStructures.EnhancedInputData.builder()
                .totalKeyPresses(currentKeyPresses.size())
                .totalMouseClicks(currentMouseButtons.size())
                .totalKeyCombinations(currentKeyCombinations.size())
                .activeKeys(currentlyHeldKeys.size())
                .activeMouseButtons(currentlyHeldMouseButtons.size())
                .keyPresses(currentKeyPresses)
                .mouseButtons(currentMouseButtons)
                .keyCombinations(currentKeyCombinations)
                .cameraRotationActive(middleMouseDragging)
                .cameraRotationAmount(calculateCameraRotationAmount())
                .build();
        } catch (Exception ex) {
            log.warn("Error getting enhanced input data", ex);
            return DataStructures.EnhancedInputData.builder()
                .totalKeyPresses(0)
                .totalMouseClicks(0)
                .totalKeyCombinations(0)
                .activeKeys(currentlyHeldKeys.size())
                .activeMouseButtons(currentlyHeldMouseButtons.size())
                .keyPresses(new ArrayList<>())
                .mouseButtons(new ArrayList<>())
                .keyCombinations(new ArrayList<>())
                .cameraRotationActive(false)
                .cameraRotationAmount(0.0)
                .build();
        }
    }
    
    private Double calculateCameraRotationAmount()
    {
        if (middleMouseDragging && client != null) {
            int currentPitch = client.getCameraPitch();
            int currentYaw = client.getCameraYaw();
            return Math.sqrt(Math.pow(currentPitch - middleMouseStartPitch, 2) + 
                           Math.pow(currentYaw - middleMouseStartYaw, 2));
        }
        return 0.0;
    }
}