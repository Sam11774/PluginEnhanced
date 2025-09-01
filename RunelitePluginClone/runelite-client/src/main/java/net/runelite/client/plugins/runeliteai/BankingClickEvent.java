/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

/**
 * Banking click event data for method detection
 * 
 * Stores information about banking interactions including
 * the action type, method used, and item involved.
 * Used by InterfaceDataCollector for banking analytics.
 */
public class BankingClickEvent
{
    public String action;        // withdraw, deposit
    public String method;        // 1, 5, 10, All, X
    public String itemName;      // item being clicked
    public long timestamp;       // when the click occurred
    public boolean isNoted;      // true if this is a noted transaction
    
    public BankingClickEvent() 
    {
        this.timestamp = System.currentTimeMillis();
        this.isNoted = false;
    }
    
    public BankingClickEvent(String action, String method, String itemName) 
    {
        this.action = action;
        this.method = method;
        this.itemName = itemName;
        this.timestamp = System.currentTimeMillis();
        this.isNoted = false;
    }
}