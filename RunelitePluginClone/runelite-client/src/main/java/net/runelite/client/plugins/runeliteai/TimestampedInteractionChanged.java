/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import net.runelite.api.events.InteractingChanged;

/**
 * Simple wrapper class to store interaction changes with timestamps
 * 
 * Used by CombatDataCollector to track when entities start/stop
 * interacting with each other, with precise timing information.
 */
public class TimestampedInteractionChanged
{
    private final InteractingChanged interaction;
    private final long timestamp;
    
    public TimestampedInteractionChanged(InteractingChanged interaction, long timestamp) 
    {
        this.interaction = interaction;
        this.timestamp = timestamp;
    }
    
    public InteractingChanged getInteraction() { return interaction; }
    public long getTimestamp() { return timestamp; }
}