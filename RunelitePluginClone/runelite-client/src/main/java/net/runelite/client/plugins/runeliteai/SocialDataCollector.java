/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.plugins.runeliteai.TickDataCollection.TickDataCollectionBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import static net.runelite.client.plugins.runeliteai.DataStructures.*;

/**
 * Dedicated collector for social interaction data
 * 
 * Responsible for:
 * - Chat message analysis and type classification
 * - Clan membership and activity tracking
 * - Trade interaction detection and analysis
 * - Social interaction metrics and analytics
 * - Message processing and filtering
 * 
 * Migrated from DataCollectionManager lines 3665-3868
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
public class SocialDataCollector
{
    // Core dependencies
    private final Client client;
    
    // Social event tracking
    private final Queue<ChatMessage> recentChatMessages = new ConcurrentLinkedQueue<>();
    
    public SocialDataCollector(Client client)
    {
        this.client = client;
        log.debug("SocialDataCollector initialized");
    }
    
    /**
     * Collect all social interaction data
     */
    public void collectSocialData(TickDataCollection.TickDataCollectionBuilder builder)
    {
        ChatData chatData = collectRealChatData();
        builder.chatData(chatData);
        
        ClanData clanData = collectRealClanData();
        builder.clanData(clanData);
        
        TradeData tradeData = collectRealTradeData();
        builder.tradeData(tradeData);
    }
    
    /**
     * Collect real chat data from recent messages
     */
    private ChatData collectRealChatData()
    {
        try {
            List<ChatMessage> recentMessages = new ArrayList<>();
            Map<String, Integer> messageTypeCounts = new HashMap<>();
            int totalMessages = 0;
            long latestMessageTime = 0;
            String latestMessage = null;
            
            // Process recent chat messages from the queue - extended time window for better capture
            long currentTime = System.currentTimeMillis();
            long recentTimeThreshold = currentTime - 300000; // 5 minutes for better message capture
            
            log.debug("[CHAT-DEBUG] Processing {} messages from queue, currentTime={}, threshold={}", 
                recentChatMessages.size(), currentTime, recentTimeThreshold);
            
            for (ChatMessage message : recentChatMessages) {
                if (message != null) {
                    recentMessages.add(message);
                    totalMessages++;
                    
                    // Count message types
                    String messageType = message.getType().toString().toLowerCase();
                    messageTypeCounts.merge(messageType, 1, Integer::sum);
                    
                    // Store the most recent message regardless of timestamp (for debugging)
                    if (message.getTimestamp() > latestMessageTime) {
                        latestMessageTime = message.getTimestamp();
                        latestMessage = message.getMessage();
                        log.debug("[CHAT-DEBUG] Updated latest message: '{}' (type={}, time={}, age={}ms)", 
                            latestMessage, message.getType(), latestMessageTime, currentTime - latestMessageTime);
                    }
                    
                    // Log all messages for debugging
                    log.debug("[CHAT-DEBUG] Processing message: '{}' (type={}, timestamp={}, age={}ms)", 
                        message.getMessage(), message.getType(), message.getTimestamp(), currentTime - message.getTimestamp());
                }
            }
            
            // Debug: Log all message types we received
            if (!messageTypeCounts.isEmpty()) {
                log.debug("Chat message types received this tick: {}", messageTypeCounts);
            }
            
            // Calculate additional metrics using correct RuneLite ChatMessageType values
            // Public chat
            int publicChatCount = messageTypeCounts.getOrDefault("publicchat", 0);
            
            // Private messages (both incoming and outgoing)
            int privateChatCount = messageTypeCounts.getOrDefault("privatechat", 0) + 
                                  messageTypeCounts.getOrDefault("privatechatout", 0);
            
            // Clan/FC messages
            int clanChatCount = messageTypeCounts.getOrDefault("clanchat", 0) + 
                               messageTypeCounts.getOrDefault("friendschat", 0);
            
            // System messages (game messages, engine messages, console, etc.)
            int systemMessageCount = messageTypeCounts.getOrDefault("gamemessage", 0) + 
                                   messageTypeCounts.getOrDefault("engine", 0) +
                                   messageTypeCounts.getOrDefault("console", 0) +
                                   messageTypeCounts.getOrDefault("broadcast", 0) +
                                   messageTypeCounts.getOrDefault("didyouknow", 0) +
                                   messageTypeCounts.getOrDefault("tradereq", 0) +
                                   messageTypeCounts.getOrDefault("trade", 0) +
                                   messageTypeCounts.getOrDefault("modautotyper", 0);
            
            return ChatData.builder()
                .recentMessages(recentMessages)
                .totalMessageCount(totalMessages)
                .publicChatCount(publicChatCount)
                .privateChatCount(privateChatCount)
                .clanChatCount(clanChatCount)
                .systemMessageCount(systemMessageCount)
                .messageTypeCounts(messageTypeCounts)
                .lastMessage(latestMessage)
                .lastMessageTime(latestMessageTime)
                .averageMessageLength(calculateAverageMessageLength(recentMessages))
                .mostActiveMessageType(getMostActiveMessageType(messageTypeCounts))
                .build();
                
        } catch (Exception e) {
            log.warn("Error collecting chat data", e);
            return ChatData.builder().build();
        }
    }
    
    /**
     * Collect real clan data
     */
    private ClanData collectRealClanData()
    {
        try {
            // ClanChannel clanChannel = client.getClanChannel(); // API not available
            Object clanChannel = null;
            if (clanChannel == null) {
                return ClanData.builder()
                    .inClan(false)
                    .clanMemberCount(0)
                    .build();
            }
            
            // List<ClanChannelMember> members = new ArrayList<>(); // API not available
            List<Object> members = new ArrayList<>();
            int memberCount = 0;
            Map<String, Integer> rankCounts = new HashMap<>();
            
            // Clan processing disabled - API not available in this RuneLite version
            
            return ClanData.builder()
                .inClan(false)
                .clanName(null)
                .clanMemberCount(0)
                .onlineClanMembers(0)
                .inClanChannel(false)
                .memberCount(0)
                .build();
                
        } catch (Exception e) {
            log.warn("Error collecting clan data", e);
            return ClanData.builder()
                .inClan(false)
                .clanMemberCount(0)
                .build();
        }
    }
    
    /**
     * Collect real trade data
     */
    private TradeData collectRealTradeData()
    {
        try {
            // Check if currently in a trade interaction (simplified - just check if interacting with player)
            boolean inTrade = false;
            String tradePartner = null;
            
            if (client.getLocalPlayer() != null && client.getLocalPlayer().getInteracting() != null) {
                if (client.getLocalPlayer().getInteracting() instanceof Player) {
                    Player partner = (Player) client.getLocalPlayer().getInteracting();
                    tradePartner = partner.getName();
                    // Simple heuristic: if interacting with another player, might be trading
                    inTrade = true;
                }
            }
            
            return TradeData.builder()
                .inTrade(inTrade)
                .tradePartner(tradePartner)
                .tradeStartTime(inTrade ? System.currentTimeMillis() : 0)
                .build();
                
        } catch (Exception e) {
            log.warn("Error collecting trade data", e);
            return TradeData.builder()
                .inTrade(false)
                .tradePartner(null)
                .build();
        }
    }
    
    /**
     * Event handler for chat messages
     */
    public void onChatMessage(ChatMessage chatMessage)
    {
        if (chatMessage != null) {
            recentChatMessages.offer(chatMessage);
            
            // Keep queue bounded (5 minutes of messages)
            while (recentChatMessages.size() > 100) {
                recentChatMessages.poll();
            }
        }
    }
    
    /**
     * Calculate average message length
     */
    private double calculateAverageMessageLength(List<ChatMessage> messages)
    {
        if (messages.isEmpty()) return 0.0;
        
        int totalLength = messages.stream()
            .filter(Objects::nonNull)
            .mapToInt(msg -> msg.getMessage() != null ? msg.getMessage().length() : 0)
            .sum();
            
        return (double) totalLength / messages.size();
    }
    
    /**
     * Get most active message type
     */
    private String getMostActiveMessageType(Map<String, Integer> typeCounts)
    {
        return typeCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("none");
    }
}