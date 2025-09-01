/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;

/**
 * Timer management for performance tracking and timing analytics
 * 
 * This is a stub class created to support existing tests after refactoring.
 * The actual timing functionality has been integrated into SystemMetricsCollector.
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
public class TimerManager
{
    public TimerManager()
    {
        log.debug("TimerManager initialized (stub implementation)");
    }
    
    /**
     * Get timer data for analysis
     * @return Timer data object
     */
    public AnalysisResults.TimerData getTimerData()
    {
        return AnalysisResults.TimerData.builder()
            .staminaActive(false)
            .staminaRemainingMs(0L)
            .antifireActive(false)
            .antifireRemainingMs(0L)
            .superAntifireActive(false)
            .superAntifireRemainingMs(0L)
            .vengeanceActive(false)
            .vengeanceRemainingMs(0L)
            .lastTimerUpdate(System.currentTimeMillis())
            .build();
    }
}