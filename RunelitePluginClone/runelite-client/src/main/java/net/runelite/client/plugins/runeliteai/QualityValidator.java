/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static net.runelite.client.plugins.runeliteai.AnalysisResults.QualityScore;

/**
 * Quality Validator for data collection integrity
 * 
 * Validates collected data for:
 * - Completeness: Are all expected data points present?
 * - Accuracy: Are data values within expected ranges?
 * - Consistency: Are data values internally consistent?
 * - Timeliness: Is data fresh and properly timestamped?
 * 
 * Provides comprehensive scoring and quality assessment
 * to ensure high-quality training data for AI/ML systems.
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
public class QualityValidator
{
    // Quality thresholds
    private static final double MIN_COMPLETENESS_SCORE = 0.8;
    private static final double MIN_ACCURACY_SCORE = 0.9;
    private static final double MIN_CONSISTENCY_SCORE = 0.7;
    private static final double MIN_TIMELINESS_SCORE = 0.95;
    
    // Expected data point ranges
    private static final Map<String, Range> EXPECTED_RANGES = initializeExpectedRanges();
    
    public QualityValidator()
    {
        log.info("QualityValidator initialized");
    }
    
    /**
     * Validate tick data and generate quality score
     * @param tickData Data to validate
     * @return Quality score with detailed breakdown
     */
    public QualityScore validateData(TickDataCollection tickData)
    {
        if (tickData == null) {
            return createErrorScore("Null tick data provided");
        }
        
        try {
            long startTime = System.nanoTime();
            
            // Validate different aspects
            double completenessScore = validateCompleteness(tickData);
            double accuracyScore = validateAccuracy(tickData);
            double consistencyScore = validateConsistency(tickData);
            double timelinessScore = validateTimeliness(tickData);
            
            // Calculate category scores
            Map<String, Double> categoryScores = calculateCategoryScores(tickData);
            
            // Identify quality issues
            List<String> qualityIssues = identifyQualityIssues(
                completenessScore, accuracyScore, consistencyScore, timelinessScore, categoryScores);
            
            // Calculate overall score (weighted average)
            double overallScore = calculateOverallScore(
                completenessScore, accuracyScore, consistencyScore, timelinessScore);
            
            // Count validation statistics
            int totalDataPoints = tickData.getTotalDataPoints();
            int failedDataPoints = countFailedDataPoints(tickData);
            
            return QualityScore.builder()
                .overallScore(overallScore)
                .completenessScore(completenessScore)
                .accuracyScore(accuracyScore)
                .consistencyScore(consistencyScore)
                .timelinessScore(timelinessScore)
                .categoryScores(categoryScores)
                .qualityIssues(qualityIssues)
                .dataPointsValidated(totalDataPoints)
                .dataPointsFailed(failedDataPoints)
                .validationTime(System.nanoTime() - startTime)
                .build();
                
        } catch (Exception e) {
            log.error("Error during data validation", e);
            return createErrorScore("Validation error: " + e.getMessage());
        }
    }
    
    /**
     * Validate data completeness
     */
    private double validateCompleteness(TickDataCollection tickData)
    {
        int expectedDataPoints = calculateExpectedDataPoints();
        int actualDataPoints = tickData.getTotalDataPoints();
        
        if (expectedDataPoints == 0) return 1.0;
        
        double completenessRatio = (double) actualDataPoints / expectedDataPoints;
        return Math.min(1.0, completenessRatio);
    }
    
    /**
     * Validate data accuracy
     */
    private double validateAccuracy(TickDataCollection tickData)
    {
        double totalScore = 0.0;
        int validatedFields = 0;
        
        // Validate basic fields
        totalScore += validateBasicFields(tickData);
        validatedFields += 4; // sessionId, tickNumber, timestamp, processingTime
        
        // Validate player data
        if (tickData.getPlayerData() != null) {
            totalScore += validatePlayerData(tickData.getPlayerData());
            validatedFields++;
        }
        
        // Validate player vitals
        if (tickData.getPlayerVitals() != null) {
            totalScore += validatePlayerVitals(tickData.getPlayerVitals());
            validatedFields++;
        }
        
        // Validate location data
        if (tickData.getPlayerLocation() != null) {
            totalScore += validateLocationData(tickData.getPlayerLocation());
            validatedFields++;
        }
        
        // Validate world data
        if (tickData.getWorldData() != null) {
            totalScore += validateWorldData(tickData.getWorldData());
            validatedFields++;
        }
        
        return validatedFields > 0 ? totalScore / validatedFields : 0.0;
    }
    
    /**
     * Validate data consistency
     */
    private double validateConsistency(TickDataCollection tickData)
    {
        double score = 1.0;
        List<String> inconsistencies = new ArrayList<>();
        
        // Check timestamp consistency
        if (tickData.getTimestamp() != null && tickData.getGameState() != null) {
            Long gameTimestamp = tickData.getGameState().getTimestamp();
            if (gameTimestamp != null) {
                long timeDiff = Math.abs(tickData.getTimestamp() - gameTimestamp);
                if (timeDiff > 1000) { // More than 1 second difference
                    score -= 0.1;
                    inconsistencies.add("Timestamp mismatch between tick data and game state");
                }
            }
        }
        
        // Check location consistency
        if (tickData.getPlayerData() != null && tickData.getPlayerLocation() != null) {
            score += validateLocationConsistency(tickData.getPlayerData(), tickData.getPlayerLocation());
        }
        
        // Check health consistency
        if (tickData.getPlayerData() != null && tickData.getPlayerVitals() != null) {
            score += validateHealthConsistency(tickData.getPlayerData(), tickData.getPlayerVitals());
        }
        
        // Check special attack consistency between player_vitals and combat_data
        if (tickData.getPlayerVitals() != null && tickData.getCombatData() != null) {
            score += validateSpecialAttackConsistency(tickData.getPlayerVitals(), tickData.getCombatData());
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Validate data timeliness
     */
    private double validateTimeliness(TickDataCollection tickData)
    {
        if (tickData.getTimestamp() == null) return 0.0;
        
        long currentTime = System.currentTimeMillis();
        long dataAge = currentTime - tickData.getTimestamp();
        
        // Data should be very recent (within 5 seconds for real-time processing)
        if (dataAge <= 5000) return 1.0;
        if (dataAge <= 30000) return 0.8;
        if (dataAge <= 60000) return 0.6;
        if (dataAge <= 300000) return 0.4;
        
        return 0.2;
    }
    
    /**
     * Calculate category-specific quality scores
     */
    private Map<String, Double> calculateCategoryScores(TickDataCollection tickData)
    {
        Map<String, Double> scores = new HashMap<>();
        
        scores.put("player_data", evaluatePlayerDataQuality(tickData));
        scores.put("world_data", evaluateWorldDataQuality(tickData));
        scores.put("input_data", evaluateInputDataQuality(tickData));
        scores.put("combat_data", evaluateCombatDataQuality(tickData));
        scores.put("social_data", evaluateSocialDataQuality(tickData));
        scores.put("interface_data", evaluateInterfaceDataQuality(tickData));
        scores.put("system_data", evaluateSystemDataQuality(tickData));
        
        return scores;
    }
    
    /**
     * Validate basic fields
     */
    private double validateBasicFields(TickDataCollection tickData)
    {
        double score = 0.0;
        
        // Session ID validation
        if (tickData.getSessionId() != null && tickData.getSessionId() > 0) {
            score += 0.25;
        }
        
        // Tick number validation
        if (tickData.getTickNumber() != null && tickData.getTickNumber() >= 0) {
            score += 0.25;
        }
        
        // Timestamp validation
        if (tickData.getTimestamp() != null && 
            tickData.getTimestamp() > 1600000000000L && // After 2020
            tickData.getTimestamp() <= System.currentTimeMillis()) {
            score += 0.25;
        }
        
        // Processing time validation
        if (tickData.getProcessingTimeNanos() != null && 
            tickData.getProcessingTimeNanos() > 0 &&
            tickData.getProcessingTimeNanos() < 100_000_000_000L) { // Less than 100 seconds
            score += 0.25;
        }
        
        return score;
    }
    
    /**
     * Validate player data
     */
    private double validatePlayerData(DataStructures.PlayerData playerData)
    {
        double score = 0.0;
        int validFields = 0;
        int totalFields = 5; // Key fields to validate
        
        // Combat level validation
        if (playerData.getCombatLevel() != null) {
            if (playerData.getCombatLevel() >= 3 && playerData.getCombatLevel() <= 126) {
                score += 1.0;
            }
            validFields++;
        }
        
        // Health ratio validation
        if (playerData.getHealthRatio() != null) {
            if (playerData.getHealthRatio() >= 0 && playerData.getHealthRatio() <= 30) {
                score += 1.0;
            }
            validFields++;
        }
        
        // Animation validation
        if (playerData.getAnimation() != null) {
            if (playerData.getAnimation() >= -1 && playerData.getAnimation() <= 10000) {
                score += 1.0;
            }
            validFields++;
        }
        
        // Coordinate validation
        if (playerData.getWorldX() != null && playerData.getWorldY() != null) {
            if (isValidWorldCoordinate(playerData.getWorldX(), playerData.getWorldY())) {
                score += 1.0;
            }
            validFields++;
        }
        
        // Plane validation
        if (playerData.getPlane() != null) {
            if (playerData.getPlane() >= 0 && playerData.getPlane() <= 3) {
                score += 1.0;
            }
            validFields++;
        }
        
        return validFields > 0 ? score / validFields : 0.0;
    }
    
    /**
     * Validate player vitals
     */
    private double validatePlayerVitals(DataStructures.PlayerVitals vitals)
    {
        double score = 0.0;
        int validatedFields = 0;
        
        // Hitpoints validation
        if (vitals.getCurrentHitpoints() != null && vitals.getMaxHitpoints() != null) {
            if (vitals.getCurrentHitpoints() <= vitals.getMaxHitpoints() &&
                vitals.getCurrentHitpoints() >= 0 &&
                vitals.getMaxHitpoints() >= 10 && vitals.getMaxHitpoints() <= 99) {
                score += 1.0;
            }
            validatedFields++;
        }
        
        // Prayer validation
        if (vitals.getCurrentPrayer() != null && vitals.getMaxPrayer() != null) {
            if (vitals.getCurrentPrayer() <= vitals.getMaxPrayer() &&
                vitals.getCurrentPrayer() >= 0 &&
                vitals.getMaxPrayer() >= 1 && vitals.getMaxPrayer() <= 99) {
                score += 1.0;
            }
            validatedFields++;
        }
        
        // Energy validation
        if (vitals.getEnergy() != null) {
            if (vitals.getEnergy() >= 0 && vitals.getEnergy() <= 100) {
                score += 1.0;
            }
            validatedFields++;
        }
        
        return validatedFields > 0 ? score / validatedFields : 1.0;
    }
    
    /**
     * Validate location data
     */
    private double validateLocationData(DataStructures.PlayerLocation location)
    {
        double score = 0.0;
        
        // Coordinate validation
        if (location.getWorldX() != null && location.getWorldY() != null) {
            if (isValidWorldCoordinate(location.getWorldX(), location.getWorldY())) {
                score += 0.4;
            }
        }
        
        // Plane validation
        if (location.getPlane() != null && location.getPlane() >= 0 && location.getPlane() <= 3) {
            score += 0.2;
        }
        
        // Region validation
        if (location.getRegionId() != null && location.getRegionId() > 0) {
            score += 0.2;
        }
        
        // Wilderness consistency
        if (location.getInWilderness() != null && location.getWildernessLevel() != null) {
            if (location.getInWilderness() == (location.getWildernessLevel() > 0)) {
                score += 0.2;
            }
        }
        
        return score;
    }
    
    /**
     * Validate world data
     */
    private double validateWorldData(DataStructures.WorldEnvironmentData worldData)
    {
        double score = 0.0;
        
        // Plane validation
        if (worldData.getPlane() != null && worldData.getPlane() >= 0 && worldData.getPlane() <= 3) {
            score += 0.25;
        }
        
        // Player count validation
        if (worldData.getNearbyPlayerCount() != null && worldData.getNearbyPlayerCount() >= 0) {
            score += 0.25;
        }
        
        // NPC count validation
        if (worldData.getNearbyNPCCount() != null && worldData.getNearbyNPCCount() >= 0) {
            score += 0.25;
        }
        
        // World tick validation
        if (worldData.getWorldTick() != null && worldData.getWorldTick() >= 0) {
            score += 0.25;
        }
        
        return score;
    }
    
    /**
     * Validate location consistency between player data and location data
     */
    private double validateLocationConsistency(DataStructures.PlayerData playerData, 
                                             DataStructures.PlayerLocation location)
    {
        double score = 0.0;
        
        // Check coordinate consistency
        if (playerData.getWorldX() != null && location.getWorldX() != null) {
            if (playerData.getWorldX().equals(location.getWorldX())) {
                score += 0.5;
            }
        }
        
        if (playerData.getWorldY() != null && location.getWorldY() != null) {
            if (playerData.getWorldY().equals(location.getWorldY())) {
                score += 0.5;
            }
        }
        
        return score;
    }
    
    /**
     * Validate health consistency
     */
    private double validateHealthConsistency(DataStructures.PlayerData playerData, 
                                           DataStructures.PlayerVitals vitals)
    {
        // Check if health ratio is consistent with actual hitpoints
        if (playerData.getHealthRatio() != null && 
            vitals.getCurrentHitpoints() != null && 
            vitals.getMaxHitpoints() != null &&
            vitals.getMaxHitpoints() > 0) {
            
            int expectedRatio = (vitals.getCurrentHitpoints() * 30) / vitals.getMaxHitpoints();
            int actualRatio = playerData.getHealthRatio();
            
            if (Math.abs(expectedRatio - actualRatio) <= 1) {
                return 0.0; // Consistent
            } else {
                return -0.2; // Inconsistent
            }
        }
        
        return 0.0; // No data to validate
    }
    
    /**
     * Validate special attack consistency between player_vitals and combat_data
     */
    private double validateSpecialAttackConsistency(DataStructures.PlayerVitals vitals, 
                                                   DataStructures.CombatData combatData)
    {
        // Check if special attack percentages match between tables
        if (vitals.getSpecialAttackPercent() != null && combatData.getSpecialAttackPercent() != null) {
            Integer vitalsSpecial = vitals.getSpecialAttackPercent();
            Integer combatSpecial = combatData.getSpecialAttackPercent();
            
            // Allow for small differences due to timing or rounding
            if (Math.abs(vitalsSpecial - combatSpecial) <= 25) {
                return 0.0; // Consistent (within 2.5% tolerance)
            } else {
                log.warn("Special attack inconsistency: player_vitals={}, combat_data={}", 
                        vitalsSpecial, combatSpecial);
                return -0.3; // Major inconsistency detected
            }
        }
        
        return 0.0; // No data to validate
    }
    
    /**
     * Identify quality issues based on scores
     */
    private List<String> identifyQualityIssues(double completeness, double accuracy, 
                                             double consistency, double timeliness, 
                                             Map<String, Double> categoryScores)
    {
        List<String> issues = new ArrayList<>();
        
        if (completeness < MIN_COMPLETENESS_SCORE) {
            issues.add("Low data completeness: " + String.format("%.2f", completeness));
        }
        
        if (accuracy < MIN_ACCURACY_SCORE) {
            issues.add("Low data accuracy: " + String.format("%.2f", accuracy));
        }
        
        if (consistency < MIN_CONSISTENCY_SCORE) {
            issues.add("Low data consistency: " + String.format("%.2f", consistency));
        }
        
        if (timeliness < MIN_TIMELINESS_SCORE) {
            issues.add("Low data timeliness: " + String.format("%.2f", timeliness));
        }
        
        // Check category-specific issues
        for (Map.Entry<String, Double> entry : categoryScores.entrySet()) {
            if (entry.getValue() < 0.6) {
                issues.add("Low quality in " + entry.getKey() + ": " + String.format("%.2f", entry.getValue()));
            }
        }
        
        return issues;
    }
    
    /**
     * Calculate overall quality score
     */
    private double calculateOverallScore(double completeness, double accuracy, 
                                       double consistency, double timeliness)
    {
        // Weighted average with higher weight on accuracy and completeness
        return (completeness * 0.3) + (accuracy * 0.4) + (consistency * 0.2) + (timeliness * 0.1);
    }
    
    /**
     * Count failed data points
     */
    private int countFailedDataPoints(TickDataCollection tickData)
    {
        // This would count specific validation failures
        // Simplified implementation
        int failures = 0;
        
        if (tickData.getSessionId() == null || tickData.getSessionId() <= 0) failures++;
        if (tickData.getTickNumber() == null || tickData.getTickNumber() < 0) failures++;
        if (tickData.getTimestamp() == null) failures++;
        if (tickData.getProcessingTimeNanos() == null || tickData.getProcessingTimeNanos() <= 0) failures++;
        
        return failures;
    }
    
    // ===== EVALUATION METHODS =====
    
    private double evaluatePlayerDataQuality(TickDataCollection tickData) {
        return tickData.getPlayerData() != null ? validatePlayerData(tickData.getPlayerData()) : 0.0;
    }
    
    private double evaluateWorldDataQuality(TickDataCollection tickData) {
        return tickData.getWorldData() != null ? validateWorldData(tickData.getWorldData()) : 0.0;
    }
    
    private double evaluateInputDataQuality(TickDataCollection tickData) {
        return tickData.getMouseInput() != null || tickData.getKeyboardInput() != null ? 0.8 : 0.0;
    }
    
    private double evaluateCombatDataQuality(TickDataCollection tickData) {
        return tickData.getCombatData() != null ? 0.7 : 0.0;
    }
    
    private double evaluateSocialDataQuality(TickDataCollection tickData) {
        return tickData.getChatData() != null || tickData.getFriendsData() != null ? 0.6 : 0.0;
    }
    
    private double evaluateInterfaceDataQuality(TickDataCollection tickData) {
        return tickData.getInterfaceData() != null ? 0.5 : 0.0;
    }
    
    private double evaluateSystemDataQuality(TickDataCollection tickData) {
        return tickData.getSystemMetrics() != null ? 0.9 : 0.0;
    }
    
    // ===== UTILITY METHODS =====
    
    /**
     * Check if world coordinates are valid
     */
    private boolean isValidWorldCoordinate(int x, int y)
    {
        // OSRS world coordinates are roughly 1024-4096 for both X and Y
        return x >= 1024 && x <= 4096 && y >= 1024 && y <= 4096;
    }
    
    /**
     * Calculate expected number of data points
     */
    private int calculateExpectedDataPoints()
    {
        // This would be based on configuration and expected data structure
        return 680; // Target data points per tick
    }
    
    /**
     * Initialize expected ranges for data validation
     */
    private static Map<String, Range> initializeExpectedRanges()
    {
        Map<String, Range> ranges = new HashMap<>();
        ranges.put("combat_level", new Range(3, 126));
        ranges.put("health_ratio", new Range(0, 30));
        ranges.put("plane", new Range(0, 3));
        ranges.put("energy", new Range(0, 100));
        ranges.put("prayer", new Range(0, 99));
        return ranges;
    }
    
    /**
     * Create error quality score
     */
    private QualityScore createErrorScore(String errorMessage)
    {
        return QualityScore.builder()
            .overallScore(0.0)
            .completenessScore(0.0)
            .accuracyScore(0.0)
            .consistencyScore(0.0)
            .timelinessScore(0.0)
            .categoryScores(new HashMap<>())
            .qualityIssues(List.of(errorMessage))
            .dataPointsValidated(0)
            .dataPointsFailed(0)
            .validationTime(0L)
            .build();
    }
    
    /**
     * Simple range class for validation
     */
    private static class Range
    {
        final double min;
        final double max;
        
        Range(double min, double max)
        {
            this.min = min;
            this.max = max;
        }
        
        boolean contains(double value)
        {
            return value >= min && value <= max;
        }
    }
}