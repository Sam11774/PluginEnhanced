/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;

/**
 * Collection of analysis result classes for RuneLiteAI Plugin
 * 
 * Contains result structures for:
 * - Quality validation scoring
 * - Security analytics and automation detection  
 * - Behavioral pattern analysis
 * - Performance monitoring
 * - Data collection summaries
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
public class AnalysisResults
{
    /**
     * Quality validation scoring for collected data
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QualityScore
    {
        private Double overallScore; // 0.0 - 1.0
        private Double completenessScore;
        private Double accuracyScore;
        private Double consistencyScore;
        private Double timelinessScore;
        private Map<String, Double> categoryScores;
        private List<String> qualityIssues;
        private Integer dataPointsValidated;
        private Integer dataPointsFailed;
        private Long validationTime;
        
        public int getDataPointCount() { 
            int count = 8; // primitive fields
            count += (categoryScores != null ? categoryScores.size() : 0);
            count += (qualityIssues != null ? qualityIssues.size() : 0);
            return count;
        }
        
        public long getEstimatedSize() { 
            long size = 64 + (16 * 6) + (16 * 2); // base + doubles + integers
            size += (categoryScores != null ? categoryScores.size() * 32 : 0);
            if (qualityIssues != null) {
                for (String issue : qualityIssues) {
                    size += (issue != null ? issue.length() * 2 + 32 : 16);
                }
            }
            return size;
        }
    }
    
    /**
     * Security analytics result for automation detection
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SecurityAnalysisResult
    {
        private Double automationScore; // 0.0 - 1.0 (higher = more likely automation)
        private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
        private Integer totalActions;
        private Integer suspiciousActions;
        private Map<String, Double> patternScores;
        private List<String> detectedPatterns;
        private List<String> riskFactors;
        private Long analysisTime;
        private Boolean flaggedForReview;
        private String confidence; // LOW, MEDIUM, HIGH
        
        public int getDataPointCount() { 
            int count = 7; // primitive fields
            count += (patternScores != null ? patternScores.size() : 0);
            count += (detectedPatterns != null ? detectedPatterns.size() : 0);
            count += (riskFactors != null ? riskFactors.size() : 0);
            return count;
        }
        
        public long getEstimatedSize() { 
            long size = 64 + (16 * 4) + (8 * 1); // base + numbers + boolean
            size += (riskLevel != null ? riskLevel.length() * 2 + 32 : 0);
            size += (confidence != null ? confidence.length() * 2 + 32 : 0);
            size += (patternScores != null ? patternScores.size() * 32 : 0);
            
            if (detectedPatterns != null) {
                for (String pattern : detectedPatterns) {
                    size += (pattern != null ? pattern.length() * 2 + 32 : 16);
                }
            }
            
            if (riskFactors != null) {
                for (String factor : riskFactors) {
                    size += (factor != null ? factor.length() * 2 + 32 : 16);
                }
            }
            
            return size;
        }
    }
    
    /**
     * Behavioral pattern analysis result
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BehavioralAnalysisResult
    {
        private String primaryActivity; // combat, skilling, questing, etc.
        private String activityConfidence; // LOW, MEDIUM, HIGH
        private Double focusScore; // 0.0 - 1.0 (higher = more focused)
        private Double efficiencyScore; // 0.0 - 1.0
        private Double consistencyScore; // 0.0 - 1.0
        private Map<String, Double> skillUsagePattern;
        private Map<String, Integer> actionFrequencies;
        private List<String> behavioralTrends;
        private String playerType; // casual, focused, efficiency, social, etc.
        private Integer sessionLength; // minutes
        private String playStyle; // aggressive, defensive, balanced, etc.
        private Long analysisWindowSize; // ticks analyzed
        
        public int getDataPointCount() { 
            int count = 8; // primitive/string fields
            count += (skillUsagePattern != null ? skillUsagePattern.size() : 0);
            count += (actionFrequencies != null ? actionFrequencies.size() : 0);
            count += (behavioralTrends != null ? behavioralTrends.size() : 0);
            return count;
        }
        
        public long getEstimatedSize() { 
            long size = 64 + (16 * 5); // base + numbers
            size += (primaryActivity != null ? primaryActivity.length() * 2 + 32 : 0);
            size += (activityConfidence != null ? activityConfidence.length() * 2 + 32 : 0);
            size += (playerType != null ? playerType.length() * 2 + 32 : 0);
            size += (playStyle != null ? playStyle.length() * 2 + 32 : 0);
            size += (skillUsagePattern != null ? skillUsagePattern.size() * 32 : 0);
            size += (actionFrequencies != null ? actionFrequencies.size() * 32 : 0);
            
            if (behavioralTrends != null) {
                for (String trend : behavioralTrends) {
                    size += (trend != null ? trend.length() * 2 + 32 : 16);
                }
            }
            
            return size;
        }
    }
    
    /**
     * Performance monitoring metrics
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PerformanceMetrics
    {
        private Long processingTimeNanos;
        private Long memoryUsed;
        private Long maxMemoryAvailable;
        private Double cpuUsagePercent;
        private Integer activeThreads;
        private Long databaseResponseTime;
        private Integer cacheHitRate; // percentage
        private Integer gcCount;
        private Long gcTime;
        private Map<String, Long> componentTimings;
        private List<String> performanceWarnings;
        private Boolean withinPerformanceTargets;
        
        public int getDataPointCount() { 
            int count = 10; // primitive fields
            count += (componentTimings != null ? componentTimings.size() : 0);
            count += (performanceWarnings != null ? performanceWarnings.size() : 0);
            return count;
        }
        
        public long getEstimatedSize() { 
            long size = 64 + (16 * 8) + (8 * 1); // base + longs/doubles/ints + boolean
            size += (componentTimings != null ? componentTimings.size() * 32 : 0);
            
            if (performanceWarnings != null) {
                for (String warning : performanceWarnings) {
                    size += (warning != null ? warning.length() * 2 + 32 : 16);
                }
            }
            
            return size;
        }
    }
    
    /**
     * Data collection summary information
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataCollectionSummary
    {
        private Integer sessionId;
        private Integer tickNumber;
        private Long timestamp;
        private Integer totalDataPoints;
        private Long processingTimeMs;
        private Double qualityScore;
        private Boolean hasSecurityAnalysis;
        private Boolean hasBehavioralAnalysis;
        private String collectionStatus; // SUCCESS, PARTIAL, FAILED
        private List<String> collectionErrors;
        private Map<String, Integer> categoryDataCounts;
        
        public int getDataPointCount() { 
            int count = 9; // primitive fields
            count += (collectionErrors != null ? collectionErrors.size() : 0);
            count += (categoryDataCounts != null ? categoryDataCounts.size() : 0);
            return count;
        }
        
        public long getEstimatedSize() { 
            long size = 64 + (16 * 6) + (8 * 2); // base + numbers + booleans
            size += (collectionStatus != null ? collectionStatus.length() * 2 + 32 : 0);
            size += (categoryDataCounts != null ? categoryDataCounts.size() * 32 : 0);
            
            if (collectionErrors != null) {
                for (String error : collectionErrors) {
                    size += (error != null ? error.length() * 2 + 32 : 16);
                }
            }
            
            return size;
        }
    }
    
    /**
     * Plugin status information
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PluginStatus
    {
        private Boolean active;
        private Integer sessionId;
        private Integer ticksProcessed;
        private Long averageProcessingTimeMs;
        private Boolean databaseConnected;
        private String currentActivity;
        private Long uptimeMs;
        private Integer errorsEncountered;
        private String healthStatus; // HEALTHY, WARNING, CRITICAL
        private Long lastUpdateTime;
        
        public int getDataPointCount() { return 10; }
        
        public long getEstimatedSize() { 
            return 64 + (16 * 6) + (8 * 1) + 
                   (currentActivity != null ? currentActivity.length() * 2 + 32 : 0) +
                   (healthStatus != null ? healthStatus.length() * 2 + 32 : 0);
        }
    }
    
    /**
     * Timer-related data for tracking game timers
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimerData
    {
        private Boolean staminaActive;
        private Long staminaRemainingMs;
        private Boolean antifireActive;
        private Long antifireRemainingMs;
        private Boolean superAntifireActive;
        private Long superAntifireRemainingMs;
        private Boolean vengeanceActive;
        private Long vengeanceRemainingMs;
        private Map<String, Long> customTimers;
        private Long lastTimerUpdate;
        
        public int getDataPointCount() { 
            int count = 9; // primitive fields
            count += (customTimers != null ? customTimers.size() : 0);
            return count;
        }
        
        public long getEstimatedSize() { 
            long size = 64 + (16 * 6) + (8 * 4); // base + longs + booleans
            size += (customTimers != null ? customTimers.size() * 32 : 0);
            return size;
        }
    }
}

// Import static inner classes to make them accessible as top-level
// This allows the test files to reference them directly
class QualityScore extends AnalysisResults.QualityScore {}
class SecurityAnalysisResult extends AnalysisResults.SecurityAnalysisResult {}
class BehavioralAnalysisResult extends AnalysisResults.BehavioralAnalysisResult {}
class PerformanceMetrics extends AnalysisResults.PerformanceMetrics {}
class DataCollectionSummary extends AnalysisResults.DataCollectionSummary {}
class PluginStatus extends AnalysisResults.PluginStatus {}
class TimerData extends AnalysisResults.TimerData {}