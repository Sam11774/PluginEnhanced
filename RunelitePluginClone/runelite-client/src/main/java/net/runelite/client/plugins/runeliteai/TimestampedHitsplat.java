/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import net.runelite.api.events.HitsplatApplied;

/**
 * Simple wrapper class to store hitsplats with timestamps
 * 
 * Used by CombatDataCollector to track damage events
 * with precise timing information.
 */
public class TimestampedHitsplat
{
    private final HitsplatApplied hitsplat;
    private final long timestamp;
    
    public TimestampedHitsplat(HitsplatApplied hitsplat, long timestamp) 
    {
        this.hitsplat = hitsplat;
        this.timestamp = timestamp;
    }
    
    public HitsplatApplied getHitsplat() { return hitsplat; }
    public long getTimestamp() { return timestamp; }
}