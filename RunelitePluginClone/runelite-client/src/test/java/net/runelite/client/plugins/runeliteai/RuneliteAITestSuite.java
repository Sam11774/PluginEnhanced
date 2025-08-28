/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Comprehensive test suite for RuneliteAI Plugin
 * Runs all unit tests for the plugin and its managers
 */
@RunWith(Suite.class)
@SuiteClasses({
    // Core functionality tests
    SimplePluginTest.class,
    RuneliteAIPluginTest.class,
    
    // Manager tests - Fixed and working
    DatabaseManagerTest.class,
    SecurityAnalyticsManagerTest.class,
    
    // Comprehensive tests - API signatures fixed and working
    DataCollectionManagerTest.class,
    QualityValidatorTest.class,
    TimerManagerTest.class,
    
    // End-to-end integration tests
    EndToEndIntegrationTest.class
})
public class RuneliteAITestSuite {
    
    /**
     * Test suite configuration and setup
     */
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("        RuneliteAI Test Suite v2.0");
        System.out.println("===========================================");
        System.out.println("Testing comprehensive plugin functionality:");
        System.out.println("- Plugin lifecycle and event handling");
        System.out.println("- Core data collection pipeline (741+ data points)");
        System.out.println("- Database operations and connectivity");
        System.out.println("- Security analytics and automation detection");
        System.out.println("- Timer management and status effects");
        System.out.println("- Performance monitoring and optimization");
        System.out.println("- Data quality validation and metrics");
        System.out.println("- Error handling and logging functionality");
        System.out.println("- Thread safety and concurrent access");
        System.out.println("- Memory efficiency and bounded collections");
        System.out.println("===========================================");
        
        // Run the test suite
        org.junit.runner.JUnitCore.main(RuneliteAITestSuite.class.getName());
    }
}