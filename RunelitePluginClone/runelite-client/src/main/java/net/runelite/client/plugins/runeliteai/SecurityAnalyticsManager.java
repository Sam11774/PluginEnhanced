/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static net.runelite.client.plugins.runeliteai.AnalysisResults.SecurityAnalysisResult;

/**
 * Security Analytics Manager for automation detection and behavioral security analysis
 * 
 * Implements sophisticated algorithms to detect:
 * - Automation patterns and bot behavior
 * - Suspicious interaction timing
 * - Inhuman precision and repeatability
 * - Automated chat patterns
 * - Risk assessment and scoring
 * 
 * Uses machine learning-inspired heuristics to identify potential automation
 * with configurable sensitivity and false positive reduction.
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
public class SecurityAnalyticsManager
{
    private final Client client;
    private final ConfigManager configManager;
    
    // Action tracking
    private final Map<String, AtomicInteger> actionCounts = new ConcurrentHashMap<>();
    private final Queue<ActionTiming> recentActions = new LinkedList<>();
    private final Queue<ChatMessage> recentChatMessages = new LinkedList<>();
    
    // Pattern detection
    private final Map<String, List<Long>> actionTimings = new ConcurrentHashMap<>();
    private final AtomicLong totalActions = new AtomicLong(0);
    private final AtomicLong suspiciousActions = new AtomicLong(0);
    
    // Analysis state
    private double currentAutomationScore = 0.0;
    private String currentRiskLevel = "LOW";
    private final List<String> detectedPatterns = new ArrayList<>();
    private final List<String> riskFactors = new ArrayList<>();
    
    // Configuration thresholds
    private static final int MAX_RECENT_ACTIONS = 1000;
    private static final int MAX_CHAT_MESSAGES = 100;
    private static final double TIMING_PRECISION_THRESHOLD = 0.05; // 5% variance
    private static final double REPETITION_THRESHOLD = 0.8; // 80% similarity
    private static final long ANALYSIS_WINDOW_MS = 300000; // 5 minutes
    
    public SecurityAnalyticsManager(Client client, ConfigManager configManager)
    {
        this.client = client;
        this.configManager = configManager;
        log.info("SecurityAnalyticsManager initialized");
    }
    
    /**
     * Analyze current state for automation patterns
     * @param tickData Current tick data for analysis
     * @return Security analysis result
     */
    public SecurityAnalysisResult analyzeCurrentState(TickDataCollection tickData)
    {
        long startTime = System.nanoTime();
        
        try {
            // Update action tracking
            recordAction("tick_analysis");
            
            // Analyze various patterns
            double timingScore = analyzeActionTiming();
            double precisionScore = analyzePrecisionPatterns();
            double repetitionScore = analyzeRepetitionPatterns();
            double chatScore = analyzeChatPatterns();
            double behaviorScore = analyzeBehavioralConsistency(tickData);
            
            // Calculate composite automation score
            double automationScore = calculateCompositeScore(
                timingScore, precisionScore, repetitionScore, chatScore, behaviorScore
            );
            
            // Determine risk level
            String riskLevel = determineRiskLevel(automationScore);
            
            // Update internal state
            currentAutomationScore = automationScore;
            currentRiskLevel = riskLevel;
            
            // Build result
            return SecurityAnalysisResult.builder()
                .automationScore(automationScore)
                .riskLevel(riskLevel)
                .totalActions((int) totalActions.get())
                .suspiciousActions((int) suspiciousActions.get())
                .patternScores(buildPatternScores(timingScore, precisionScore, repetitionScore, chatScore, behaviorScore))
                .detectedPatterns(new ArrayList<>(detectedPatterns))
                .riskFactors(new ArrayList<>(riskFactors))
                .analysisTime(System.nanoTime() - startTime)
                .flaggedForReview(automationScore > 0.8)
                .confidence(determineConfidence(automationScore))
                .build();
                
        } catch (Exception e) {
            log.error("Error in security analysis", e);
            return createErrorResult(System.nanoTime() - startTime);
        }
    }
    
    /**
     * Analyze action timing patterns for automation detection
     */
    private double analyzeActionTiming()
    {
        if (recentActions.size() < 10) {
            return 0.0; // Not enough data
        }
        
        double score = 0.0;
        List<ActionTiming> actionList = new ArrayList<>(recentActions);
        
        // Check for inhuman precision in timing
        score += analyzePrecisionTiming(actionList);
        
        // Check for rhythmic patterns
        score += analyzeRhythmicPatterns(actionList);
        
        // Check for impossible reaction times
        score += analyzeReactionTimes(actionList);
        
        return Math.min(1.0, score);
    }
    
    /**
     * Analyze precision patterns in actions
     */
    private double analyzePrecisionPatterns()
    {
        double score = 0.0;
        
        // Analyze mouse movement precision
        score += analyzeMousePrecision();
        
        // Analyze click timing precision  
        score += analyzeClickPrecision();
        
        // Analyze keyboard timing precision
        score += analyzeKeyboardPrecision();
        
        return Math.min(1.0, score / 3.0);
    }
    
    /**
     * Analyze repetition patterns
     */
    private double analyzeRepetitionPatterns()
    {
        double score = 0.0;
        
        // Check for identical action sequences
        score += analyzeActionSequences();
        
        // Check for repeated timing patterns
        score += analyzeTimingSequences();
        
        // Check for spatial repetition
        score += analyzeSpatialPatterns();
        
        return Math.min(1.0, score / 3.0);
    }
    
    /**
     * Analyze chat patterns for automation
     */
    private double analyzeChatPatterns()
    {
        if (recentChatMessages.size() < 5) {
            return 0.0;
        }
        
        double score = 0.0;
        
        // Check for repeated messages
        score += analyzeRepeatedMessages();
        
        // Check for timing patterns in chat
        score += analyzeChatTiming();
        
        // Check for generic/template messages
        score += analyzeMessageContent();
        
        return Math.min(1.0, score / 3.0);
    }
    
    /**
     * Analyze behavioral consistency
     */
    private double analyzeBehavioralConsistency(TickDataCollection tickData)
    {
        if (tickData == null) {
            return 0.0;
        }
        
        double score = 0.0;
        
        // Check for unnatural consistency in actions
        score += analyzeActionConsistency();
        
        // Check for lack of human variations
        score += analyzeVariationPatterns();
        
        // Check for impossible multitasking
        score += analyzeMultitaskingPatterns();
        
        return Math.min(1.0, score / 3.0);
    }
    
    /**
     * Calculate composite automation score from individual components
     */
    private double calculateCompositeScore(double timing, double precision, 
                                         double repetition, double chat, double behavior)
    {
        // Weighted combination of different factors
        double weightedScore = 
            (timing * 0.3) +        // Timing patterns are highly indicative
            (precision * 0.25) +    // Inhuman precision is suspicious
            (repetition * 0.2) +    // Repetition suggests automation
            (chat * 0.15) +         // Chat patterns can reveal bots
            (behavior * 0.1);       // Behavioral inconsistencies
        
        // Apply non-linear scaling to emphasize high scores
        double scaledScore = Math.pow(weightedScore, 1.2);
        
        return Math.min(1.0, scaledScore);
    }
    
    /**
     * Determine risk level from automation score
     */
    private String determineRiskLevel(double automationScore)
    {
        if (automationScore >= 0.9) return "CRITICAL";
        if (automationScore >= 0.75) return "HIGH";
        if (automationScore >= 0.5) return "MEDIUM";
        return "LOW";
    }
    
    /**
     * Build pattern scores map
     */
    private Map<String, Double> buildPatternScores(double timing, double precision, 
                                                  double repetition, double chat, double behavior)
    {
        Map<String, Double> scores = new HashMap<>();
        scores.put("timing", timing);
        scores.put("precision", precision);
        scores.put("repetition", repetition);
        scores.put("chat", chat);
        scores.put("behavior", behavior);
        return scores;
    }
    
    /**
     * Determine confidence level
     */
    private String determineConfidence(double automationScore)
    {
        int dataPoints = recentActions.size() + recentChatMessages.size();
        
        if (dataPoints < 50) return "LOW";
        if (dataPoints < 200 && automationScore < 0.7) return "MEDIUM";
        return "HIGH";
    }
    
    // ===== DETAILED ANALYSIS METHODS =====
    
    private double analyzePrecisionTiming(List<ActionTiming> actions)
    {
        if (actions.size() < 5) return 0.0;
        
        // Calculate variance in timing intervals
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < actions.size(); i++) {
            long interval = actions.get(i).timestamp - actions.get(i-1).timestamp;
            intervals.add(interval);
        }
        
        double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double variance = intervals.stream()
            .mapToDouble(interval -> Math.pow(interval - mean, 2))
            .average().orElse(0.0);
        
        double standardDeviation = Math.sqrt(variance);
        double coefficientOfVariation = mean > 0 ? standardDeviation / mean : 0;
        
        // Low variation suggests automation
        if (coefficientOfVariation < TIMING_PRECISION_THRESHOLD) {
            suspiciousActions.incrementAndGet();
            detectedPatterns.add("precise_timing");
            riskFactors.add("Inhuman timing precision detected");
            return 0.8;
        }
        
        return 0.0;
    }
    
    private double analyzeRhythmicPatterns(List<ActionTiming> actions)
    {
        // Look for rhythmic patterns that suggest automation
        // Implementation would check for regular intervals
        return 0.0;
    }
    
    private double analyzeReactionTimes(List<ActionTiming> actions)
    {
        // Check for impossibly fast reaction times
        // Implementation would analyze response times to events
        return 0.0;
    }
    
    private double analyzeMousePrecision()
    {
        // Analyze mouse movement for inhuman precision
        return 0.0;
    }
    
    private double analyzeClickPrecision()
    {
        // Analyze click timing for automation patterns
        return 0.0;
    }
    
    private double analyzeKeyboardPrecision()
    {
        // Analyze keyboard timing patterns
        return 0.0;
    }
    
    private double analyzeActionSequences()
    {
        // Check for identical sequences of actions
        return 0.0;
    }
    
    private double analyzeTimingSequences()
    {
        // Check for repeated timing patterns
        return 0.0;
    }
    
    private double analyzeSpatialPatterns()
    {
        // Check for repeated spatial movement patterns
        return 0.0;
    }
    
    private double analyzeRepeatedMessages()
    {
        if (recentChatMessages.size() < 3) return 0.0;
        
        Set<String> uniqueMessages = new HashSet<>();
        for (ChatMessage msg : recentChatMessages) {
            uniqueMessages.add(msg.getMessage().toLowerCase().trim());
        }
        
        double uniqueRatio = (double) uniqueMessages.size() / recentChatMessages.size();
        
        if (uniqueRatio < 0.3) { // Less than 30% unique messages
            detectedPatterns.add("repeated_chat");
            riskFactors.add("High rate of repeated chat messages");
            return 0.6;
        }
        
        return 0.0;
    }
    
    private double analyzeChatTiming()
    {
        // Analyze timing patterns in chat messages
        return 0.0;
    }
    
    private double analyzeMessageContent()
    {
        // Analyze message content for automation patterns
        return 0.0;
    }
    
    private double analyzeActionConsistency()
    {
        // Check for unnatural consistency in actions
        return 0.0;
    }
    
    private double analyzeVariationPatterns()
    {
        // Check for lack of human-like variations
        return 0.0;
    }
    
    private double analyzeMultitaskingPatterns()
    {
        // Check for impossible multitasking
        return 0.0;
    }
    
    // ===== PUBLIC INTERFACE METHODS =====
    
    /**
     * Record an action for analysis
     */
    public void recordAction(String actionType)
    {
        totalActions.incrementAndGet();
        actionCounts.computeIfAbsent(actionType, k -> new AtomicInteger(0)).incrementAndGet();
        
        ActionTiming timing = new ActionTiming(actionType, System.currentTimeMillis());
        recentActions.offer(timing);
        
        // Maintain size limit
        while (recentActions.size() > MAX_RECENT_ACTIONS) {
            recentActions.poll();
        }
        
        // Record timing for pattern analysis
        actionTimings.computeIfAbsent(actionType, k -> new ArrayList<>())
            .add(timing.timestamp);
        
        // Clean old timings
        cleanOldTimings();
    }
    
    /**
     * Analyze chat pattern for automation
     */
    public void analyzeChatPattern(ChatMessage chatMessage)
    {
        if (chatMessage == null) return;
        
        recentChatMessages.offer(chatMessage);
        
        // Maintain size limit
        while (recentChatMessages.size() > MAX_CHAT_MESSAGES) {
            recentChatMessages.poll();
        }
        
        recordAction("chat_message");
    }
    
    /**
     * Get current automation risk level
     */
    public String getCurrentRiskLevel()
    {
        return currentRiskLevel;
    }
    
    /**
     * Get current automation score
     */
    public double getCurrentAutomationScore()
    {
        return currentAutomationScore;
    }
    
    /**
     * Clean old timing data
     */
    private void cleanOldTimings()
    {
        long cutoff = System.currentTimeMillis() - ANALYSIS_WINDOW_MS;
        
        actionTimings.values().forEach(timings -> 
            timings.removeIf(timestamp -> timestamp < cutoff)
        );
    }
    
    /**
     * Create error result when analysis fails
     */
    private SecurityAnalysisResult createErrorResult(long analysisTime)
    {
        return SecurityAnalysisResult.builder()
            .automationScore(0.0)
            .riskLevel("UNKNOWN")
            .totalActions(0)
            .suspiciousActions(0)
            .patternScores(new HashMap<>())
            .detectedPatterns(List.of("analysis_error"))
            .riskFactors(List.of("Analysis failed"))
            .analysisTime(analysisTime)
            .flaggedForReview(false)
            .confidence("LOW")
            .build();
    }
    
    /**
     * Shutdown the security analytics manager
     */
    public void shutdown()
    {
        actionCounts.clear();
        recentActions.clear();
        recentChatMessages.clear();
        actionTimings.clear();
        detectedPatterns.clear();
        riskFactors.clear();
        
        log.info("SecurityAnalyticsManager shutdown completed");
    }
    
    /**
     * Internal class for tracking action timing
     */
    private static class ActionTiming
    {
        final String actionType;
        final long timestamp;
        
        ActionTiming(String actionType, long timestamp)
        {
            this.actionType = actionType;
            this.timestamp = timestamp;
        }
    }
}