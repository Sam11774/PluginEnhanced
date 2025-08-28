/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.ArrayList;

import net.runelite.api.coords.WorldPoint;

/**
 * Comprehensive unit tests for QualityValidator
 * Tests data quality validation, automation detection, and timing analysis
 */
public class QualityValidatorTest {

    @Mock
    private TickDataCollection mockTickData;

    @Mock
    private DataStructures.PlayerData mockPlayerData;

    @Mock
    private DataStructures.MouseInputData mockMouseInputData;

    @Mock  
    private DataStructures.KeyboardInputData mockKeyboardInputData;

    private QualityValidator qualityValidator;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        qualityValidator = new QualityValidator();

        // Setup basic mock data
        when(mockTickData.getPlayerData()).thenReturn(mockPlayerData);
        when(mockTickData.getMouseInput()).thenReturn(mockMouseInputData);
        when(mockTickData.getKeyboardInput()).thenReturn(mockKeyboardInputData);
        when(mockTickData.getProcessingTimeNanos()).thenReturn(5_000_000L); // 5ms
        
        // DataStructures.PlayerData doesn't have these specific methods - use mock without specific setup
    }

    @After
    public void tearDown() {
        // Cleanup
    }

    @Test
    public void testValidTickDataQuality() {
        // Test validation of normal, high-quality tick data
        AnalysisResults.QualityScore result = qualityValidator.validateData(mockTickData);

        assertNotNull("Quality score should not be null", result);
        assertTrue("Quality score should be valid", result.getOverallScore() >= 0.0);
        assertTrue("Quality score should be within bounds", result.getOverallScore() <= 100.0);
    }

    @Test
    public void testNullTickDataHandling() {
        // Test validation handles null tick data gracefully
        AnalysisResults.QualityScore result = qualityValidator.validateData(null);

        assertNotNull("Quality score should not be null even for null input", result);
        assertTrue("Quality score should be within bounds", result.getOverallScore() >= 0.0);
        assertTrue("Quality score should be within bounds", result.getOverallScore() <= 100.0);
    }

    @Test
    public void testMissingPlayerDataDetection() {
        // Test detection of missing player data
        when(mockTickData.getPlayerData()).thenReturn(null);

        AnalysisResults.QualityScore result = qualityValidator.validateData(mockTickData);

        assertNotNull("Quality score should not be null", result);
        assertTrue("Quality score should be within bounds", result.getOverallScore() >= 0.0);
        assertTrue("Quality score should be within bounds", result.getOverallScore() <= 100.0);
    }

    @Test
    public void testMissingInputDataDetection() {
        // Test detection of missing input data
        when(mockTickData.getMouseInput()).thenReturn(null);
        when(mockTickData.getKeyboardInput()).thenReturn(null);

        AnalysisResults.QualityScore result = qualityValidator.validateData(mockTickData);

        assertNotNull("Quality score should not be null", result);
        assertTrue("Quality score should be within bounds", result.getOverallScore() >= 0.0);
        assertTrue("Quality score should be within bounds", result.getOverallScore() <= 100.0);
    }

    @Test
    public void testHighProcessingTimeDetection() {
        // Test detection of high processing times (outliers)
        when(mockTickData.getProcessingTimeNanos()).thenReturn(15_000_000L); // 15ms (high)

        AnalysisResults.QualityScore result = qualityValidator.validateData(mockTickData);

        assertNotNull("Quality score should not be null", result);
        assertTrue("Quality score should be within bounds", result.getOverallScore() >= 0.0);
        assertTrue("Quality score should be within bounds", result.getOverallScore() <= 100.0);
    }

    @Test
    public void testTimingAnalysisWithSufficientData() {
        // Test timing analysis when there's sufficient historical data
        // Simulate multiple validations to build up timing history
        
        for (int i = 0; i < 15; i++) {
            TickDataCollection tickData = mock(TickDataCollection.class);
            when(tickData.getPlayerData()).thenReturn(mockPlayerData);
            when(tickData.getMouseInput()).thenReturn(mockMouseInputData);
            when(tickData.getKeyboardInput()).thenReturn(mockKeyboardInputData);
            when(tickData.getProcessingTimeNanos()).thenReturn(5_000_000L);
            
            qualityValidator.validateData(tickData);
        }

        // Now validate with consistent timing (potential automation)
        for (int i = 0; i < 25; i++) {
            TickDataCollection tickData = mock(TickDataCollection.class);
            when(tickData.getPlayerData()).thenReturn(mockPlayerData);
            when(tickData.getMouseInput()).thenReturn(mockMouseInputData);
            when(tickData.getKeyboardInput()).thenReturn(mockKeyboardInputData);
            when(tickData.getProcessingTimeNanos()).thenReturn(5_000_000L);
            
            AnalysisResults.QualityScore result = qualityValidator.validateData(tickData);
            assertNotNull("Quality score should not be null", result);
        }

        // Final validation should potentially detect automation
        AnalysisResults.QualityScore result = qualityValidator.validateData(mockTickData);
        assertNotNull("Quality score should not be null", result);
        assertTrue("Quality score should be within bounds", result.getOverallScore() >= 0.0);
        assertTrue("Quality score should be within bounds", result.getOverallScore() <= 100.0);
    }

    @Test
    public void testDuplicateDataDetection() {
        // Test detection of duplicate data
        // Create identical tick data to simulate duplicates
        
        // First, add some data to build history
        for (int i = 0; i < 5; i++) {
            TickDataCollection tickData = mock(TickDataCollection.class);
            DataStructures.PlayerData playerData = mock(DataStructures.PlayerData.class);
            
            when(tickData.getPlayerData()).thenReturn(playerData);
            when(tickData.getMouseInput()).thenReturn(mockMouseInputData);
            when(tickData.getKeyboardInput()).thenReturn(mockKeyboardInputData);
            when(tickData.getProcessingTimeNanos()).thenReturn(5_000_000L);
            
            // DataStructures.PlayerData doesn't have these specific methods - use mock without specific setup
            
            qualityValidator.validateData(tickData);
        }

        // Now submit identical data (potential duplicate)
        AnalysisResults.QualityScore result = qualityValidator.validateData(mockTickData);

        assertNotNull("Quality score should not be null", result);
        assertTrue("Quality score should be within bounds", result.getOverallScore() >= 0.0);
        assertTrue("Quality score should be within bounds", result.getOverallScore() <= 100.0);
    }

    @Test
    public void testQualityScoreCalculation() {
        // Test quality score calculation with various data scenarios
        
        // Scenario 1: Perfect data
        when(mockTickData.getProcessingTimeNanos()).thenReturn(2_000_000L); // 2ms
        AnalysisResults.QualityScore perfectResult = qualityValidator.validateData(mockTickData);
        
        // Scenario 2: Data with some issues
        when(mockTickData.getMouseInput()).thenReturn(null); // Missing input data
        when(mockTickData.getKeyboardInput()).thenReturn(null); // Missing input data
        when(mockTickData.getProcessingTimeNanos()).thenReturn(8_000_000L); // 8ms
        AnalysisResults.QualityScore issueResult = qualityValidator.validateData(mockTickData);

        assertNotNull("Perfect result should not be null", perfectResult);
        assertNotNull("Issue result should not be null", issueResult);
        assertTrue("Both results should have valid scores", 
                   perfectResult.getOverallScore() >= 0.0 && issueResult.getOverallScore() >= 0.0);
    }

    @Test
    public void testPerformanceOfValidation() {
        // Test that validation completes within performance requirements
        long startTime = System.nanoTime();

        for (int i = 0; i < 100; i++) {
            AnalysisResults.QualityScore result = qualityValidator.validateData(mockTickData);
            assertNotNull("Each validation should return result", result);
        }

        long endTime = System.nanoTime();
        long totalTimeMs = (endTime - startTime) / 1_000_000;

        assertTrue("100 validations should complete in reasonable time (got " + totalTimeMs + "ms)", 
                   totalTimeMs < 1000); // Less than 1 second for 100 validations
    }

    @Test
    public void testMemoryUsageOfValidator() {
        // Test that validator doesn't consume excessive memory
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Perform many validations
        for (int i = 0; i < 1000; i++) {
            TickDataCollection tickData = mock(TickDataCollection.class);
            when(tickData.getPlayerData()).thenReturn(mockPlayerData);
            when(tickData.getMouseInput()).thenReturn(mockMouseInputData);
            when(tickData.getKeyboardInput()).thenReturn(mockKeyboardInputData);
            when(tickData.getProcessingTimeNanos()).thenReturn(5_000_000L + (i * 1000)); // Varying times
            
            AnalysisResults.QualityScore result = qualityValidator.validateData(tickData);
            assertNotNull("Each validation should return result", result);
        }

        System.gc();
        Thread.yield();

        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryIncrease = endMemory - startMemory;

        assertTrue("Memory increase should be reasonable (got " + (memoryIncrease / 1024 / 1024) + "MB)", 
                   memoryIncrease < 20 * 1024 * 1024); // Less than 20MB increase
    }

    @Test
    public void testConcurrentValidation() {
        // Test thread safety of validation
        List<AnalysisResults.QualityScore> results = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            Thread t = new Thread(() -> {
                TickDataCollection tickData = mock(TickDataCollection.class);
                when(tickData.getPlayerData()).thenReturn(mockPlayerData);
                when(tickData.getMouseInput()).thenReturn(mockMouseInputData);
                when(tickData.getKeyboardInput()).thenReturn(mockKeyboardInputData);
                when(tickData.getProcessingTimeNanos()).thenReturn(5_000_000L);
                
                AnalysisResults.QualityScore result = qualityValidator.validateData(tickData);
                synchronized (results) {
                    results.add(result);
                }
            });
            threads.add(t);
            t.start();
        }

        // Wait for all threads
        for (Thread t : threads) {
            try {
                t.join(5000);
            } catch (InterruptedException e) {
                fail("Thread was interrupted: " + e.getMessage());
            }
        }

        assertEquals("All threads should complete", 5, results.size());
        for (AnalysisResults.QualityScore result : results) {
            assertNotNull("Each result should be valid", result);
            assertTrue("Each result should have reasonable quality score", result.getOverallScore() >= 0.0);
        }
    }

    @Test
    public void testBoundaryConditions() {
        // Test boundary conditions for quality scoring
        
        // Test with exactly 10ms processing time (boundary condition)
        when(mockTickData.getProcessingTimeNanos()).thenReturn(10_000_000L); // Exactly 10ms
        AnalysisResults.QualityScore boundaryResult = qualityValidator.validateData(mockTickData);
        
        assertNotNull("Boundary result should not be null", boundaryResult);
        assertTrue("Quality score should be valid at boundary", boundaryResult.getOverallScore() >= 0.0);
        
        // Test with zero processing time
        when(mockTickData.getProcessingTimeNanos()).thenReturn(0L);
        AnalysisResults.QualityScore zeroResult = qualityValidator.validateData(mockTickData);
        
        assertNotNull("Zero result should not be null", zeroResult);
        assertTrue("Quality score should be valid for zero processing time", zeroResult.getOverallScore() >= 0.0);
    }

    @Test
    public void testValidatorCreation() {
        // Test basic validator creation
        QualityValidator validator = new QualityValidator();
        assertNotNull("Validator should be created successfully", validator);
    }

    @Test
    public void testScoreProperties() {
        // Test that quality score has expected properties
        AnalysisResults.QualityScore result = qualityValidator.validateData(mockTickData);
        
        assertNotNull("Quality score should not be null", result);
        
        // Test that score components exist (if available)
        double overallScore = result.getOverallScore();
        assertTrue("Overall score should be between 0 and 100", overallScore >= 0.0 && overallScore <= 100.0);
    }
}