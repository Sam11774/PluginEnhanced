/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

/**
 * Session information holder class
 * 
 * Stores basic session data including ID and start time.
 * Used by DatabaseManager for tracking active sessions.
 */
public class SessionInfo
{
    final int sessionId;
    final long startTime;
    
    public SessionInfo(int sessionId, long startTime)
    {
        this.sessionId = sessionId;
        this.startTime = startTime;
    }
    
    public int getSessionId() { return sessionId; }
    public long getStartTime() { return startTime; }
}