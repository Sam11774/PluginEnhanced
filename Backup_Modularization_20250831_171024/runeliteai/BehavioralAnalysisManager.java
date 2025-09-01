/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static net.runelite.client.plugins.runeliteai.AnalysisResults.BehavioralAnalysisResult;

/**
 * Behavioral Analysis Manager for advanced pattern analysis
 * 
 * Analyzes player behavior patterns to identify:
 * - Primary activities and focus patterns
 * - Efficiency and consistency metrics
 * - Skill usage patterns and preferences
 * - Play style classification
 * - Behavioral trends over time
 * 
 * Uses sophisticated algorithms to classify player behavior
 * and detect behavioral patterns for AI/ML training data enhancement.
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
public class BehavioralAnalysisManager
{
    private final Client client;
    private final ConfigManager configManager;
    
    // Activity tracking
    private final Map<String, AtomicInteger> activityCounts = new ConcurrentHashMap<>();
    private final Queue<ActivityRecord> recentActivities = new LinkedList<>();
    private final Map<String, Long> activityDurations = new ConcurrentHashMap<>();
    
    // Skill usage tracking
    private final Map<String, AtomicInteger> skillUsage = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> skillExperienceGains = new ConcurrentHashMap<>();
    
    // Efficiency metrics
    private final Map<String, Double> efficiencyScores = new ConcurrentHashMap<>();
    private final AtomicLong sessionStartTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger totalActions = new AtomicInteger(0);
    private final AtomicInteger productiveActions = new AtomicInteger(0);
    
    // Behavioral patterns
    private String primaryActivity = "unknown";
    private String playerType = "casual";
    private String playStyle = "balanced";
    private double focusScore = 0.5;
    private double efficiencyScore = 0.5;
    private double consistencyScore = 0.5;
    
    // Configuration - use centralized constants
    private static final int MAX_ACTIVITY_HISTORY = RuneliteAIConstants.MAX_ACTIVITY_HISTORY;
    private static final int ANALYSIS_WINDOW_SIZE = RuneliteAIConstants.ANALYSIS_WINDOW_SIZE;
    private static final long ACTIVITY_TIMEOUT_MS = 30000; // 30 seconds
    
    public BehavioralAnalysisManager(Client client, ConfigManager configManager)
    {
        this.client = client;
        this.configManager = configManager;
        log.info("BehavioralAnalysisManager initialized");
    }
    
    /**
     * Analyze behavior patterns from tick data
     * @param tickData Current tick data for analysis
     * @return Behavioral analysis result
     */
    public BehavioralAnalysisResult analyzePattern(TickDataCollection tickData)
    {
        try {
            // Update activity tracking
            updateActivityTracking(tickData);
            
            // Analyze current patterns
            analyzePrimaryActivity();
            analyzePlayerType();
            analyzePlayStyle();
            
            // Calculate metrics
            focusScore = calculateFocusScore();
            efficiencyScore = calculateEfficiencyScore();
            consistencyScore = calculateConsistencyScore();
            
            // Build skill usage pattern
            Map<String, Double> skillPattern = buildSkillUsagePattern();
            
            // Build action frequencies
            Map<String, Integer> actionFreqs = buildActionFrequencies();
            
            // Identify behavioral trends
            List<String> trends = identifyBehavioralTrends();
            
            // Calculate session metrics
            int sessionLengthMinutes = (int) ((System.currentTimeMillis() - sessionStartTime.get()) / 60000);
            long analysisWindow = Math.min(recentActivities.size(), ANALYSIS_WINDOW_SIZE);
            
            return BehavioralAnalysisResult.builder()
                .primaryActivity(primaryActivity)
                .activityConfidence(determineActivityConfidence())
                .focusScore(focusScore)
                .efficiencyScore(efficiencyScore)
                .consistencyScore(consistencyScore)
                .skillUsagePattern(skillPattern)
                .actionFrequencies(actionFreqs)
                .behavioralTrends(trends)
                .playerType(playerType)
                .sessionLength(sessionLengthMinutes)
                .playStyle(playStyle)
                .analysisWindowSize(analysisWindow)
                .build();
                
        } catch (Exception e) {
            log.error("Error in behavioral analysis", e);
            return createErrorResult();
        }
    }
    
    /**
     * Update activity tracking from tick data
     */
    private void updateActivityTracking(TickDataCollection tickData)
    {
        if (tickData == null) return;
        
        String currentActivity = determineCurrentActivity(tickData);
        
        if (currentActivity != null && !currentActivity.equals("idle")) {
            recordActivity(currentActivity);
        }
        
        // Update skill usage if experience gained
        updateSkillUsage(tickData);
        
        // Clean old activity records
        cleanOldActivities();
    }
    
    /**
     * Determine current activity from tick data
     */
    private String determineCurrentActivity(TickDataCollection tickData)
    {
        // Analyze player data for activity indicators
        if (tickData.getPlayerData() != null) {
            Integer animation = tickData.getPlayerData().getAnimation();
            
            if (animation != null && animation != -1) {
                return classifyActivityFromAnimation(animation);
            }
        }
        
        // Check combat indicators
        if (tickData.getCombatData() != null) {
            return "combat";
        }
        
        // Check interface interactions
        if (tickData.getInterfaceData() != null) {
            return classifyActivityFromInterface();
        }
        
        // Check location-based activities
        if (tickData.getPlayerLocation() != null) {
            return classifyActivityFromLocation(tickData.getPlayerLocation());
        }
        
        return "idle";
    }
    
    /**
     * Classify activity from animation
     */
    private String classifyActivityFromAnimation(int animationId)
    {
        // Map animation IDs to activities (simplified mapping)
        Map<Integer, String> animationMap = Map.of(
            // Combat animations
            390, "combat",
            386, "combat", 
            // Skilling animations
            879, "woodcutting",
            896, "mining",
            618, "fishing",
            // Cooking
            883, "cooking",
            // Crafting  
            884, "crafting"
        );
        
        return animationMap.getOrDefault(animationId, "unknown");
    }
    
    /**
     * Classify activity from interface
     */
    private String classifyActivityFromInterface()
    {
        // Implementation would check active interfaces
        return "interface_interaction";
    }
    
    /**
     * Classify activity from location
     */
    private String classifyActivityFromLocation(DataStructures.PlayerLocation location)
    {
        if (location.getInWilderness() != null && location.getInWilderness()) {
            return "wilderness";
        }
        
        if (location.getInMultiCombat() != null && location.getInMultiCombat()) {
            return "multi_combat";
        }
        
        // Could add more location-based classifications
        return "exploration";
    }
    
    /**
     * Record an activity
     */
    private void recordActivity(String activity)
    {
        totalActions.incrementAndGet();
        
        // Count productive actions
        if (isProductiveActivity(activity)) {
            productiveActions.incrementAndGet();
        }
        
        activityCounts.computeIfAbsent(activity, k -> new AtomicInteger(0)).incrementAndGet();
        
        ActivityRecord record = new ActivityRecord(activity, System.currentTimeMillis());
        recentActivities.offer(record);
        
        // Maintain size limit
        while (recentActivities.size() > MAX_ACTIVITY_HISTORY) {
            recentActivities.poll();
        }
    }
    
    /**
     * Update skill usage tracking
     */
    private void updateSkillUsage(TickDataCollection tickData)
    {
        if (tickData.getPlayerStats() == null) return;
        
        // This would track experience gains to determine skill usage
        // Implementation would compare current XP with previous XP
    }
    
    /**
     * Analyze primary activity
     */
    private void analyzePrimaryActivity()
    {
        if (activityCounts.isEmpty()) {
            primaryActivity = "unknown";
            return;
        }
        
        // Find most common activity
        String mostCommon = activityCounts.entrySet().stream()
            .max(Map.Entry.<String, AtomicInteger>comparingByValue(
                (a, b) -> Integer.compare(a.get(), b.get())))
            .map(Map.Entry::getKey)
            .orElse("unknown");
        
        primaryActivity = mostCommon;
    }
    
    /**
     * Analyze player type
     */
    private void analyzePlayerType()
    {
        double productivityRatio = totalActions.get() > 0 ? 
            (double) productiveActions.get() / totalActions.get() : 0.0;
        
        if (productivityRatio > 0.8) {
            playerType = "efficiency";
        } else if (productivityRatio > 0.6) {
            playerType = "focused";
        } else if (productivityRatio > 0.3) {
            playerType = "casual";
        } else {
            playerType = "social";
        }
    }
    
    /**
     * Analyze play style
     */
    private void analyzePlayStyle()
    {
        // Analyze combat vs non-combat activities
        int combatActions = activityCounts.getOrDefault("combat", new AtomicInteger(0)).get();
        int totalActivities = activityCounts.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
        
        if (totalActivities == 0) {
            playStyle = "unknown";
            return;
        }
        
        double combatRatio = (double) combatActions / totalActivities;
        
        if (combatRatio > 0.7) {
            playStyle = "aggressive";
        } else if (combatRatio > 0.3) {
            playStyle = "balanced";
        } else {
            playStyle = "peaceful";
        }
    }
    
    /**
     * Calculate focus score
     */
    private double calculateFocusScore()
    {
        if (activityCounts.isEmpty()) return 0.5;
        
        // Calculate activity diversity (lower diversity = higher focus)
        int totalActivities = activityCounts.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
        
        if (totalActivities == 0) return 0.5;
        
        // Calculate entropy (diversity measure)
        double entropy = 0.0;
        for (AtomicInteger count : activityCounts.values()) {
            double probability = (double) count.get() / totalActivities;
            if (probability > 0) {
                entropy -= probability * Math.log(probability) / Math.log(2);
            }
        }
        
        // Convert entropy to focus score (lower entropy = higher focus)
        double maxEntropy = Math.log(activityCounts.size()) / Math.log(2);
        double focusScore = maxEntropy > 0 ? 1.0 - (entropy / maxEntropy) : 0.5;
        
        return Math.max(0.0, Math.min(1.0, focusScore));
    }
    
    /**
     * Calculate efficiency score
     */
    private double calculateEfficiencyScore()
    {
        if (totalActions.get() == 0) return 0.5;
        
        double productivityRatio = (double) productiveActions.get() / totalActions.get();
        return Math.max(0.0, Math.min(1.0, productivityRatio));
    }
    
    /**
     * Calculate consistency score
     */
    private double calculateConsistencyScore()
    {
        if (recentActivities.size() < 10) return 0.5;
        
        // Analyze consistency in activity timing
        List<ActivityRecord> activities = new ArrayList<>(recentActivities);
        Map<String, List<Long>> activityTimings = new HashMap<>();
        
        // Group activities by type
        for (ActivityRecord record : activities) {
            activityTimings.computeIfAbsent(record.activityType, k -> new ArrayList<>())
                .add(record.timestamp);
        }
        
        // Calculate timing consistency for each activity
        double totalConsistency = 0.0;
        int activityTypes = 0;
        
        for (Map.Entry<String, List<Long>> entry : activityTimings.entrySet()) {
            List<Long> timings = entry.getValue();
            if (timings.size() >= 3) {
                double consistency = calculateTimingConsistency(timings);
                totalConsistency += consistency;
                activityTypes++;
            }
        }
        
        return activityTypes > 0 ? totalConsistency / activityTypes : 0.5;
    }
    
    /**
     * Calculate timing consistency for a list of timestamps
     */
    private double calculateTimingConsistency(List<Long> timings)
    {
        if (timings.size() < 3) return 0.5;
        
        // Calculate intervals between activities
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < timings.size(); i++) {
            intervals.add(timings.get(i) - timings.get(i-1));
        }
        
        // Calculate coefficient of variation
        double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
        if (mean == 0) return 0.5;
        
        double variance = intervals.stream()
            .mapToDouble(interval -> Math.pow(interval - mean, 2))
            .average().orElse(0.0);
        
        double standardDeviation = Math.sqrt(variance);
        double coefficientOfVariation = standardDeviation / mean;
        
        // Convert CV to consistency score (lower CV = higher consistency)
        return Math.max(0.0, Math.min(1.0, 1.0 - Math.min(coefficientOfVariation, 1.0)));
    }
    
    /**
     * Build skill usage pattern
     */
    private Map<String, Double> buildSkillUsagePattern()
    {
        Map<String, Double> pattern = new HashMap<>();
        
        if (skillUsage.isEmpty()) {
            return pattern;
        }
        
        int totalSkillActions = skillUsage.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
        
        if (totalSkillActions == 0) return pattern;
        
        for (Map.Entry<String, AtomicInteger> entry : skillUsage.entrySet()) {
            double ratio = (double) entry.getValue().get() / totalSkillActions;
            pattern.put(entry.getKey(), ratio);
        }
        
        return pattern;
    }
    
    /**
     * Build action frequencies
     */
    private Map<String, Integer> buildActionFrequencies()
    {
        Map<String, Integer> frequencies = new HashMap<>();
        
        for (Map.Entry<String, AtomicInteger> entry : activityCounts.entrySet()) {
            frequencies.put(entry.getKey(), entry.getValue().get());
        }
        
        return frequencies;
    }
    
    /**
     * Identify behavioral trends
     */
    private List<String> identifyBehavioralTrends()
    {
        List<String> trends = new ArrayList<>();
        
        // Analyze recent vs older activity patterns
        if (recentActivities.size() >= 50) {
            Map<String, Integer> recentPattern = analyzeRecentActivities(25);
            Map<String, Integer> olderPattern = analyzeOlderActivities(25);
            
            // Compare patterns to identify trends
            for (String activity : recentPattern.keySet()) {
                int recentCount = recentPattern.getOrDefault(activity, 0);
                int olderCount = olderPattern.getOrDefault(activity, 0);
                
                if (recentCount > olderCount * 1.5) {
                    trends.add("increasing_" + activity);
                } else if (recentCount * 1.5 < olderCount) {
                    trends.add("decreasing_" + activity);
                }
            }
        }
        
        // Add general trends
        if (focusScore > 0.8) {
            trends.add("highly_focused");
        }
        
        if (efficiencyScore > 0.8) {
            trends.add("high_efficiency");
        }
        
        if (consistencyScore > 0.8) {
            trends.add("consistent_behavior");
        }
        
        return trends;
    }
    
    /**
     * Analyze recent activities
     */
    private Map<String, Integer> analyzeRecentActivities(int count)
    {
        Map<String, Integer> pattern = new HashMap<>();
        List<ActivityRecord> activities = new ArrayList<>(recentActivities);
        
        int start = Math.max(0, activities.size() - count);
        for (int i = start; i < activities.size(); i++) {
            String activity = activities.get(i).activityType;
            pattern.merge(activity, 1, Integer::sum);
        }
        
        return pattern;
    }
    
    /**
     * Analyze older activities
     */
    private Map<String, Integer> analyzeOlderActivities(int count)
    {
        Map<String, Integer> pattern = new HashMap<>();
        List<ActivityRecord> activities = new ArrayList<>(recentActivities);
        
        if (activities.size() < count * 2) return pattern;
        
        int end = activities.size() - count;
        int start = Math.max(0, end - count);
        
        for (int i = start; i < end; i++) {
            String activity = activities.get(i).activityType;
            pattern.merge(activity, 1, Integer::sum);
        }
        
        return pattern;
    }
    
    /**
     * Determine activity confidence
     */
    private String determineActivityConfidence()
    {
        if (recentActivities.size() < 10) return "LOW";
        if (recentActivities.size() < 50) return "MEDIUM";
        return "HIGH";
    }
    
    /**
     * Check if activity is productive
     */
    private boolean isProductiveActivity(String activity)
    {
        Set<String> productiveActivities = Set.of(
            "combat", "woodcutting", "mining", "fishing", "cooking",
            "crafting", "smithing", "farming", "runecraft", "construction"
        );
        return productiveActivities.contains(activity);
    }
    
    /**
     * Clean old activity records
     */
    private void cleanOldActivities()
    {
        long cutoff = System.currentTimeMillis() - ACTIVITY_TIMEOUT_MS;
        recentActivities.removeIf(record -> record.timestamp < cutoff);
    }
    
    /**
     * Create error result when analysis fails
     */
    private BehavioralAnalysisResult createErrorResult()
    {
        return BehavioralAnalysisResult.builder()
            .primaryActivity("unknown")
            .activityConfidence("LOW")
            .focusScore(0.0)
            .efficiencyScore(0.0)
            .consistencyScore(0.0)
            .skillUsagePattern(new HashMap<>())
            .actionFrequencies(new HashMap<>())
            .behavioralTrends(List.of("analysis_error"))
            .playerType("unknown")
            .sessionLength(0)
            .playStyle("unknown")
            .analysisWindowSize(0L)
            .build();
    }
    
    /**
     * Reset analysis state (useful for new sessions)
     */
    public void resetAnalysis()
    {
        activityCounts.clear();
        recentActivities.clear();
        activityDurations.clear();
        skillUsage.clear();
        skillExperienceGains.clear();
        efficiencyScores.clear();
        
        totalActions.set(0);
        productiveActions.set(0);
        sessionStartTime.set(System.currentTimeMillis());
        
        primaryActivity = "unknown";
        playerType = "casual";
        playStyle = "balanced";
        focusScore = 0.5;
        efficiencyScore = 0.5;
        consistencyScore = 0.5;
        
        log.info("BehavioralAnalysisManager state reset");
    }
    
    /**
     * Shutdown the behavioral analysis manager
     */
    public void shutdown()
    {
        resetAnalysis();
        log.info("BehavioralAnalysisManager shutdown completed");
    }
    
    /**
     * Internal class for activity records
     */
    private static class ActivityRecord
    {
        final String activityType;
        final long timestamp;
        
        ActivityRecord(String activityType, long timestamp)
        {
            this.activityType = activityType;
            this.timestamp = timestamp;
        }
    }
}