/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
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
    private DataCollectionManager dataCollectionManager; // Reference to get recent item changes
    
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
     * Set reference to DataCollectionManager for accessing recent item changes
     */
    public void setDataCollectionManager(DataCollectionManager dataCollectionManager) {
        this.dataCollectionManager = dataCollectionManager;
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
     * Collect real bank data with comprehensive item and action tracking
     */
    private BankData collectRealBankData()
    {
        try {
            ItemContainer bankContainer = client.getItemContainer(InventoryID.BANK);
            boolean bankOpen = bankContainer != null;
            
            if (!bankOpen) {
                return BankData.builder()
                    .bankOpen(false)
                    .currentTab(0)
                    .searchActive(false)
                    .bankInterfaceType("none")
                    .bankOrganizationScore(0.0f)
                    .tabSwitchCount(0)
                    .timeSpentInBank(0L)
                    .build();
            }
            
            // Process bank container data
            Item[] bankItems = bankContainer.getItems();
            
            // Get real bank capacity from widgets instead of hardcoded value
            int maxBankSlots = getBankCapacityFromWidget();
            int totalUniqueItems = 0;
            int usedSlots = 0;
            long totalBankValue = 0;
            
            // Process individual bank items with comprehensive metadata
            List<DataStructures.BankItemData> bankItemsList = new ArrayList<>();
            
            for (int slot = 0; slot < bankItems.length; slot++) {
                Item item = bankItems[slot];
                
                // Only process items with valid ID and quantity > 0 (skip placeholders)
                if (item != null && item.getId() > 0 && item.getQuantity() > 0) {
                    totalUniqueItems++;
                    usedSlots++;
                    
                    // Get item name and properties
                    String itemName = "Unknown";
                    long itemValue = 0;
                    
                    try {
                        if (itemManager != null) {
                            itemName = itemManager.getItemComposition(item.getId()).getName();
                            itemValue = (long) item.getQuantity() * itemManager.getItemPrice(item.getId());
                            totalBankValue += itemValue;
                        }
                    } catch (Exception e) {
                        log.debug("[BANK-ITEM-DEBUG] Error getting item details for ID {}: {}", item.getId(), e.getMessage());
                        itemName = "Item_" + item.getId();
                    }
                    
                    // Bank items are never noted - noted items only exist in inventory/transactions
                    boolean isNoted = false;
                    boolean isStackable = isItemStackable(item.getId());
                    
                    DataStructures.BankItemData bankItemData = DataStructures.BankItemData.builder()
                        .itemId(item.getId())
                        .itemName(itemName)
                        .quantity(item.getQuantity())
                        .itemValue(itemValue)
                        .slotPosition(slot)
                        .tabNumber(getCurrentBankTab())
                        .coordinateX(slot % 8) // Bank slots are in 8-column grid
                        .coordinateY(slot / 8)
                        .isNoted(isNoted)
                        .isStackable(isStackable)
                        .category(getItemCategory(item.getId()))
                        .gePrice(getGrandExchangePrice(item.getId()).intValue())
                        .build();
                    
                    bankItemsList.add(bankItemData);
                    
                    // Debug logging for first few items
                    if (totalUniqueItems <= 5) {
                        log.debug("[BANK-DEBUG] Bank item: {} x{} '{}' in slot {} (tab {})", 
                            item.getId(), item.getQuantity(), itemName, slot, getCurrentBankTab());
                    }
                }
            }
            
            // Track recent banking activity with detailed action analysis
            List<DataStructures.BankActionData> recentActionsList = new ArrayList<>();
            int recentDeposits = 0;
            int recentWithdrawals = 0;
            
            if (dataCollectionManager != null) {
                Queue<ItemContainerChanged> itemChanges = dataCollectionManager.getRecentItemChanges();
                
                for (ItemContainerChanged change : itemChanges) {
                    if (change != null) {
                        DataStructures.BankActionData actionData = null;
                        
                        // Extract item details from the container change
                        Item[] changedItems = change.getItemContainer().getItems();
                        if (changedItems != null && changedItems.length > 0) {
                            // Find the most significant item change (highest value/quantity change)
                            Item mostSignificantItem = null;
                            int maxQuantity = 0;
                            
                            for (Item item : changedItems) {
                                if (item != null && item.getId() > 0 && item.getQuantity() > maxQuantity) {
                                    mostSignificantItem = item;
                                    maxQuantity = item.getQuantity();
                                }
                            }
                            
                            // Extract item details if found
                            Integer itemId = null;
                            String itemName = null;
                            Integer quantity = null;
                            
                            if (mostSignificantItem != null) {
                                itemId = mostSignificantItem.getId();
                                quantity = mostSignificantItem.getQuantity();
                                
                                try {
                                    if (itemManager != null) {
                                        itemName = itemManager.getItemComposition(itemId).getName();
                                    } else {
                                        itemName = "Item_" + itemId;
                                    }
                                } catch (Exception e) {
                                    itemName = "Item_" + itemId;
                                    log.debug("[BANK-ACTION-DEBUG] Error getting item name for ID {}: {}", itemId, e.getMessage());
                                }
                            }
                            
                            if (change.getContainerId() == InventoryID.BANK.getId()) {
                                recentDeposits++;
                                actionData = DataStructures.BankActionData.builder()
                                    .actionType("deposit")
                                    .itemId(itemId)
                                    .itemName(itemName)
                                    .quantity(quantity)
                                    .actionTimestamp(System.currentTimeMillis())
                                    .methodUsed("1") // Default method
                                    .fromTab(getCurrentBankTab())
                                    .isNoted(false)
                                    .durationMs(0)
                                    .build();
                            } else if (change.getContainerId() == InventoryID.INVENTORY.getId()) {
                                recentWithdrawals++;
                                actionData = DataStructures.BankActionData.builder()
                                    .actionType("withdrawal")
                                    .itemId(itemId)
                                    .itemName(itemName)
                                    .quantity(quantity)
                                    .actionTimestamp(System.currentTimeMillis())
                                    .methodUsed("1") // Default method
                                    .fromTab(getCurrentBankTab())
                                    .isNoted(false)
                                    .durationMs(0)
                                    .build();
                            }
                            
                            if (actionData != null) {
                                recentActionsList.add(actionData);
                                log.debug("[BANK-ACTION-DEBUG] Added banking action: {} for item {} (qty: {})", 
                                    actionData.getActionType(), itemName, quantity);
                            }
                        }
                    }
                }
            }
            
            return DataStructures.BankData.builder()
                .bankOpen(true)
                .totalUniqueItems(totalUniqueItems)
                .usedBankSlots(usedSlots)
                .maxBankSlots(maxBankSlots)
                .totalBankValue(totalBankValue)
                .currentTab(getCurrentBankTab())
                .searchQuery(getBankSearchQuery())
                .bankInterfaceType(getBankInterfaceType())
                .searchActive(isBankSearchActive())
                .bankOrganizationScore(0.0f) // Placeholder
                .tabSwitchCount(0) // Placeholder
                .timeSpentInBank(0L) // Placeholder
                .bankItems(bankItemsList)
                .recentActions(recentActionsList)
                .recentDeposits(recentDeposits)
                .recentWithdrawals(recentWithdrawals)
                .notedItemsCount(0) // Bank items are never noted
                .build();
                
        } catch (Exception e) {
            log.warn("[BANK-DEBUG] Error collecting bank data: {}", e.getMessage());
            return DataStructures.BankData.builder()
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
    
    // Banking helper methods
    
    private boolean isItemStackable(int itemId) {
        try {
            if (itemManager != null) {
                return itemManager.getItemComposition(itemId).isStackable();
            }
        } catch (Exception e) {
            log.debug("Error checking if item {} is stackable: {}", itemId, e.getMessage());
        }
        return false;
    }
    
    private int getCurrentBankTab() {
        // Default to tab 0 - could be enhanced with widget detection
        return 0;
    }
    
    private String getBankSearchQuery() {
        // Try to get search query from bank interface - simplified for now
        return null;
    }
    
    private String getBankInterfaceType() {
        // Detect bank interface type - simplified implementation
        return "bank_booth";
    }
    
    private boolean isBankSearchActive() {
        // Check if bank search is active - simplified
        return false;
    }
    
    private String getItemCategory(int itemId) {
        // Simple item categorization based on item ID ranges
        if (itemId >= 1 && itemId <= 100) return "weapons";
        if (itemId >= 101 && itemId <= 200) return "armor";
        if (itemId >= 201 && itemId <= 300) return "food";
        if (itemId >= 301 && itemId <= 400) return "potions";
        if (itemId >= 401 && itemId <= 500) return "runes";
        return "misc";
    }
    
    private Long getGrandExchangePrice(int itemId) {
        try {
            if (itemManager != null) {
                return (long) itemManager.getItemPrice(itemId);
            }
        } catch (Exception e) {
            log.debug("Error getting GE price for item {}: {}", itemId, e.getMessage());
        }
        return 0L;
    }
    
    /**
     * Get real bank capacity from widget instead of hardcoded value
     */
    private int getBankCapacityFromWidget() {
        try {
            // Try to get bank capacity from the capacity widget
            Widget capacityWidget = client.getWidget(WidgetInfo.BANK_ITEM_COUNT_BOTTOM);
            if (capacityWidget != null && !capacityWidget.isHidden()) {
                String text = capacityWidget.getText();
                if (text != null && text.contains("/")) {
                    // Format is typically "used/capacity" like "123/416"
                    String[] parts = text.split("/");
                    if (parts.length == 2) {
                        try {
                            int capacity = Integer.parseInt(parts[1].trim());
                            log.debug("[BANK-CAPACITY] Found bank capacity from widget: {}", capacity);
                            return capacity;
                        } catch (NumberFormatException e) {
                            log.debug("[BANK-CAPACITY] Failed to parse capacity from widget text: {}", text);
                        }
                    }
                }
            }
            
            // Fallback: try to get from occupied slots widget
            Widget occupiedWidget = client.getWidget(WidgetInfo.BANK_ITEM_COUNT_TOP);
            if (occupiedWidget != null && !occupiedWidget.isHidden()) {
                String text = occupiedWidget.getText();
                if (text != null && text.contains("/")) {
                    String[] parts = text.split("/");
                    if (parts.length == 2) {
                        try {
                            int capacity = Integer.parseInt(parts[1].trim());
                            log.debug("[BANK-CAPACITY] Found bank capacity from occupied widget: {}", capacity);
                            return capacity;
                        } catch (NumberFormatException e) {
                            log.debug("[BANK-CAPACITY] Failed to parse capacity from occupied widget text: {}", text);
                        }
                    }
                }
            }
            
            log.debug("[BANK-CAPACITY] Could not find bank capacity widget, using fallback");
            
        } catch (Exception e) {
            log.debug("[BANK-CAPACITY] Error getting bank capacity from widget: {}", e.getMessage());
        }
        
        // Fallback to reasonable default if we can't get the real capacity
        // Most accounts have 400+ slots, f2p has ~70 slots
        return 416; // This is still a fallback but better than before since we tried to get real value
    }
}