/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.runeliteai.TickDataCollection.TickDataCollectionBuilder;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import static net.runelite.client.plugins.runeliteai.DataStructures.*;

/**
 * Dedicated collector for interface and UI interaction data
 * 
 * Responsible for:
 * - Widget and interface state tracking
 * - Dialogue system interaction analysis  
 * - Shop interface monitoring
 * - Banking operations and item tracking
 * - Interface type detection and tab tracking
 * - UI interaction counting and correlation
 * 
 * Migrated from DataCollectionManager lines 3869-7075
 * 
 * @author RuneLiteAI Team
 * @version 3.1.0
 */
@Slf4j
public class InterfaceDataCollector
{
    // Core dependencies
    private final Client client;
    private final ItemManager itemManager;
    
    // Interface interaction tracking
    private int currentTickInterfaceInteractions = 0;
    
    // Banking interaction tracking
    private final Queue<BankingClickEvent> recentBankingClicks = new ConcurrentLinkedQueue<>();
    private final java.util.Map<String, String> lastBankingMethods = new java.util.concurrent.ConcurrentHashMap<>();
    private BankingClickEvent lastBankingClickEvent;
    
    public InterfaceDataCollector(Client client, ItemManager itemManager)
    {
        this.client = client;
        this.itemManager = itemManager;
        log.debug("InterfaceDataCollector initialized");
    }
    
    /**
     * Collect all interface interaction data
     */
    public void collectInterfaceData(TickDataCollection.TickDataCollectionBuilder builder)
    {
        InterfaceData interfaceData = collectRealInterfaceData();
        builder.interfaceData(interfaceData);
        
        DialogueData dialogueData = collectRealDialogueData();
        builder.dialogueData(dialogueData);
        
        ShopData shopData = collectRealShopData();
        builder.shopData(shopData);
        
        BankData bankData = collectRealBankData();
        builder.bankData(bankData);
    }
    
    /**
     * Collect real interface data using widget API
     */
    private InterfaceData collectRealInterfaceData()
    {
        try {
            List<String> openInterfaces = new ArrayList<>();
            List<Integer> visibleWidgets = new ArrayList<>();
            int totalOpenInterfaces = 0;
            String primaryInterface = null;
            
            // Scan all root widgets to find open interfaces
            for (Widget rootWidget : client.getWidgetRoots()) {
                if (rootWidget != null && !rootWidget.isHidden()) {
                    int widgetId = rootWidget.getId();
                    visibleWidgets.add(widgetId);
                    
                    // Determine interface type based on widget ID
                    String interfaceType = getInterfaceType(widgetId);
                    if (interfaceType != null && !interfaceType.equals("unknown")) {
                        openInterfaces.add(interfaceType);
                        totalOpenInterfaces++;
                        
                        if (primaryInterface == null) {
                            primaryInterface = interfaceType;
                        }
                    }
                }
            }
            
            // Check additional interfaces using detection methods
            boolean bankOpen = isBankInterface();
            boolean shopOpen = isShopInterface();  
            boolean tradeOpen = isTradeInterface();
            boolean geOpen = isGrandExchangeInterface();
            boolean equipmentOpen = isEquipmentInterface();
            
            // Add detected interfaces to the lists
            if (bankOpen && !openInterfaces.contains("bank")) {
                openInterfaces.add("bank");
                totalOpenInterfaces++;
                if (primaryInterface == null) primaryInterface = "bank";
            }
            if (shopOpen && !openInterfaces.contains("shop")) {
                openInterfaces.add("shop");
                totalOpenInterfaces++;
                if (primaryInterface == null) primaryInterface = "shop";
            }
            if (tradeOpen && !openInterfaces.contains("trade")) {
                openInterfaces.add("trade");
                totalOpenInterfaces++;
                if (primaryInterface == null) primaryInterface = "trade";
            }
            if (geOpen && !openInterfaces.contains("grandexchange")) {
                openInterfaces.add("grandexchange");
                totalOpenInterfaces++;
                if (primaryInterface == null) primaryInterface = "grandexchange";
            }
            if (equipmentOpen && !openInterfaces.contains("equipment")) {
                openInterfaces.add("equipment");
                totalOpenInterfaces++;
                if (primaryInterface == null) primaryInterface = "equipment";
            }
            
            // Get interface interaction count for this tick and reset counter
            int interfaceInteractionCount = getAndResetInterfaceInteractionCount();
            
            return InterfaceData.builder()
                .openInterfaces(openInterfaces)
                .visibleWidgets(visibleWidgets)
                .totalOpenInterfaces(totalOpenInterfaces)
                .primaryInterface(primaryInterface)
                .currentInterfaceTab(detectCurrentInterfaceTab(primaryInterface))
                .interfaceInteractionCount(interfaceInteractionCount)
                .interfaceClickCorrelation(buildInterfaceClickCorrelation(interfaceInteractionCount, primaryInterface))
                .chatboxOpen(isChatboxInterface())
                .inventoryOpen(isInventoryInterface())
                .skillsInterfaceOpen(isSkillsInterface())
                .questInterfaceOpen(isQuestInterface())
                .settingsInterfaceOpen(isSettingsInterface())
                .build();
                
        } catch (Exception e) {
            log.warn("Error collecting interface data", e);
            return InterfaceData.builder().build();
        }
    }
    
    /**
     * Collect real dialogue data
     */
    private DialogueData collectRealDialogueData()
    {
        try {
            Widget dialogueWidget = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);
            boolean inDialogue = dialogueWidget != null && !dialogueWidget.isHidden();
            
            if (!inDialogue) {
                return DialogueData.builder()
                    .inDialogue(false)
                    .build();
            }
            
            String npcName = null;
            String dialogueText = null;
            List<String> dialogueOptions = new ArrayList<>();
            
            // Get NPC name
            Widget npcNameWidget = client.getWidget(WidgetInfo.DIALOG_NPC_NAME);
            if (npcNameWidget != null && !npcNameWidget.isHidden()) {
                npcName = npcNameWidget.getText();
            }
            
            // Get dialogue text
            if (dialogueWidget.getText() != null) {
                dialogueText = dialogueWidget.getText();
            }
            
            // Get dialogue options
            Widget optionsWidget = client.getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS);
            if (optionsWidget != null && !optionsWidget.isHidden()) {
                for (Widget child : optionsWidget.getChildren()) {
                    if (child != null && child.getText() != null && !child.getText().isEmpty()) {
                        dialogueOptions.add(child.getText());
                    }
                }
            }
            
            return DialogueData.builder()
                .inDialogue(true)
                .npcName(npcName)
                .dialogueText(dialogueText)
                .dialogueOptions(dialogueOptions)
                .dialogueType(getDialogueType())
                .build();
                
        } catch (Exception e) {
            log.warn("Error collecting dialogue data", e);
            return DialogueData.builder()
                .inDialogue(false)
                .build();
        }
    }
    
    /**
     * Collect real shop data
     */
    private ShopData collectRealShopData()
    {
        try {
            boolean shopOpen = isShopInterface();
            
            return ShopData.builder()
                .shopOpen(shopOpen)
                .totalShopValue(0L)
                .shopName(shopOpen ? getShopName() : null)
                .build();
                
        } catch (Exception e) {
            log.warn("Error collecting shop data", e);
            return ShopData.builder()
                .shopOpen(false)
                .build();
        }
    }
    
    /**
     * Collect real bank data
     */
    private BankData collectRealBankData()
    {
        try {
            boolean bankOpen = isBankInterface();
            
            return BankData.builder()
                .bankOpen(bankOpen)
                .totalBankValue(0L)
                .build();
                
        } catch (Exception e) {
            log.warn("Error collecting bank data", e);
            return BankData.builder()
                .bankOpen(false)
                .totalBankValue(0L)
                .build();
        }
    }
    
    // Helper methods - simplified implementations to avoid API issues
    
    private String getInterfaceType(int widgetId)
    {
        int groupId = widgetId >> 16;
        
        // Map common interface group IDs to interface types
        switch (groupId) {
            case 12: return "chatbox";
            case 149: return "inventory";
            case 320: return "skills";
            case 399: return "quest";
            case 116: return "settings";
            case 213: return "bank";
            case 300: return "shop";
            case 335: return "trade";
            case 465: return "grandexchange";
            case 387: return "equipment";
            case 231: return "dialogue";
            case 219: return "prayer";
            case 218: return "spells";
            default: return "unknown";
        }
    }
    
    private boolean isBankInterface()
    {
        try {
            Widget bankWidget = client.getWidget(WidgetInfo.BANK_CONTAINER);
            return bankWidget != null && !bankWidget.isHidden();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isShopInterface()
    {
        try {
            // Use basic shop widget detection
            Widget shopWidget = client.getWidget(300, 16);
            return shopWidget != null && !shopWidget.isHidden();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isTradeInterface()
    {
        try {
            Widget tradeWidget = client.getWidget(335, 0);
            return tradeWidget != null && !tradeWidget.isHidden();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isGrandExchangeInterface()
    {
        try {
            Widget geWidget = client.getWidget(WidgetInfo.GRAND_EXCHANGE_OFFER_CONTAINER);
            return geWidget != null && !geWidget.isHidden();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isEquipmentInterface()
    {
        try {
            Widget equipWidget = client.getWidget(WidgetInfo.EQUIPMENT_INVENTORY_ITEMS_CONTAINER);
            return equipWidget != null && !equipWidget.isHidden();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isChatboxInterface()
    {
        try {
            Widget chatWidget = client.getWidget(WidgetInfo.CHATBOX_MESSAGES);
            return chatWidget != null && !chatWidget.isHidden();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isInventoryInterface()
    {
        try {
            Widget invWidget = client.getWidget(WidgetInfo.INVENTORY);
            return invWidget != null && !invWidget.isHidden();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isSkillsInterface()
    {
        try {
            Widget skillsWidget = client.getWidget(WidgetInfo.SKILLS_CONTAINER);
            return skillsWidget != null && !skillsWidget.isHidden();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isQuestInterface()
    {
        try {
            Widget questWidget = client.getWidget(399, 0);
            return questWidget != null && !questWidget.isHidden();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isSettingsInterface()
    {
        try {
            Widget settingsWidget = client.getWidget(116, 0);
            return settingsWidget != null && !settingsWidget.isHidden();
        } catch (Exception e) {
            return false;
        }
    }
    
    private String detectCurrentInterfaceTab(String primaryInterface)
    {
        if (primaryInterface == null) return null;
        
        try {
            // Basic tab detection based on interface type
            switch (primaryInterface) {
                case "inventory": return "inventory_tab";
                case "skills": return "skills_tab";
                case "quest": return "quest_tab";
                case "equipment": return "equipment_tab";
                case "prayer": return "prayer_tab";
                case "spells": return "spells_tab";
                case "settings": return "settings_tab";
                default: return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    private int getAndResetInterfaceInteractionCount()
    {
        int count = currentTickInterfaceInteractions;
        currentTickInterfaceInteractions = 0;
        return count;
    }
    
    private String buildInterfaceClickCorrelation(int interactionCount, String primaryInterface)
    {
        if (interactionCount == 0 || primaryInterface == null) {
            return "none";
        }
        
        return String.format("%s:%d", primaryInterface, interactionCount);
    }
    
    public void detectBankingMethod(String menuOption, String menuTarget)
    {
        if (menuOption == null || menuTarget == null) return;
        
        try {
            String method = "unknown";
            
            if (menuOption.contains("Deposit")) {
                if (menuOption.contains("All")) {
                    method = "deposit_all";
                } else if (menuOption.contains("X")) {
                    method = "deposit_x";
                } else {
                    method = "deposit_one";
                }
                lastBankingMethods.put("deposit", method);
            } else if (menuOption.contains("Withdraw")) {
                if (menuOption.contains("All")) {
                    method = "withdraw_all";
                } else if (menuOption.contains("X")) {
                    method = "withdraw_x";
                } else {
                    method = "withdraw_one";
                }
                lastBankingMethods.put("withdraw", method);
            }
            
            log.debug("[BANKING-DEBUG] Detected banking method: {} for action with option: {}", method, menuOption);
        } catch (Exception e) {
            log.debug("Error detecting banking method: {}", e.getMessage());
        }
    }
    
    public String getLastBankingMethod(String action)
    {
        return lastBankingMethods.getOrDefault(action, "unknown");
    }
    
    private String getDialogueType()
    {
        try {
            if (client.getWidget(WidgetInfo.DIALOG_NPC_TEXT) != null && !client.getWidget(WidgetInfo.DIALOG_NPC_TEXT).isHidden()) {
                return "npc_dialogue";
            }
            if (client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT) != null && !client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT).isHidden()) {
                return "player_dialogue";
            }
            if (client.getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS) != null && !client.getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS).isHidden()) {
                return "options_dialogue";
            }
            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private String getShopName()
    {
        try {
            // Simplified shop name detection
            return "Unknown Shop";
        } catch (Exception e) {
            return "Unknown Shop";
        }
    }
}