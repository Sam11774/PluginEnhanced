# DataCollectionManager.java Structure Analysis
**File Size**: 7,397 lines (333.4KB)  
**Current Status**: Monolithic class requiring modularization

## Core File Structure

### Package and Imports (Lines 1-30)
```java
package net.runelite.client.plugins.runeliteai;

// Core imports:
- lombok.extern.slf4j.Slf4j
- net.runelite.api.*  
- net.runelite.api.events.*
- net.runelite.api.coords.WorldPoint, LocalPoint
- net.runelite.api.widgets.Widget, WidgetInfo
- net.runelite.client.config.ConfigManager
- net.runelite.client.game.ItemManager
- java.util.concurrent.*
- static imports from DataStructures.*, AnalysisResults.*
```

### Helper Classes (Lines 35-78)
```java
- TimestampedHitsplat (lines 35-46): Wrapper for hitsplats with timestamps
- TimestampedInteractionChanged (lines 51-62): Wrapper for interaction events  
- TimestampedMenuOptionClicked (lines 67-78): Wrapper for menu clicks
```

### Main Class Declaration (Lines 101-189)
```java
@Slf4j
public class DataCollectionManager
{
    // Dependencies (lines 104-111)
    private final Client client;
    private final ItemManager itemManager; 
    private final ConfigManager configManager;
    private final RuneliteAIPlugin plugin;
    private final TimerManager timerManager;
    private final OSRSWikiService wikiService;
    
    // Performance tracking (lines 114-120)
    private final AtomicLong totalProcessingTime;
    private final AtomicLong ticksProcessed;
    
    // Data caches and tracking (lines 122-188)
    // 25+ concurrent data structures for caching and event queuing
}
```

## Method Structure Analysis

### Core Orchestration (Lines 190-323)
| Method | Lines | Description |
|--------|-------|-------------|
| `constructor` | 196-209 | Initialize dependencies and services |
| `collectAllData()` | 221-323 | **MAIN ENTRY POINT** - Orchestrates all data collection |

### Player Data Collection Module (Lines 324-1199) 
| Method | Lines | Description |
|--------|-------|-------------|
| `collectPlayerData()` | 328-367 | Main player data orchestrator |
| `collectBasicPlayerDataOptimized()` | 372-391 | Basic player info with caching |
| `collectPlayerVitalsOptimized()` | 404-422 | HP, prayer, energy, special attack |
| `collectPlayerLocationOptimized()` | 435-457 | World position, region, area type |
| `collectPlayerStats()` | 470-500+ | All 23 skill levels and experience |
| `collectPlayerEquipment()` | ~600-800 | Equipment slots and items |  
| `collectPlayerInventory()` | ~800-1000 | Inventory items and analysis |
| `collectActivePrayers()` | ~1000-1100 | Active prayer tracking |
| `collectActiveSpells()` | ~1100-1199 | Active spell tracking |

### World Data Collection Module (Lines 1200-1943)
| Method | Lines | Description |
|--------|-------|-------------|
| `collectWorldData()` | 1200-1300 | Main world data orchestrator |
| `collectNearbyPlayers()` | ~1300-1500 | Nearby player detection and analysis |
| `collectNearbyNPCs()` | ~1500-1700 | NPC tracking with combat analysis |
| `collectGameObjects()` | ~1700-1800 | Game object interaction tracking |
| `collectGroundItems()` | ~1800-1900 | Ground item detection and ownership |
| `collectProjectiles()` | ~1900-1943 | Projectile tracking and classification |

### Input Data Collection Module (Lines 1944-2872)
| Method | Lines | Description |
|--------|-------|-------------|
| `collectInputData()` | 1944-2000 | Main input data orchestrator |
| `collectMouseInputData()` | ~2000-2200 | Mouse position, clicks, movement analysis |
| `collectKeyboardInputData()` | ~2200-2400 | Keyboard input tracking |
| `collectCameraData()` | ~2400-2500 | Camera angle, zoom, movement |
| `collectMenuInteractionData()` | ~2500-2700 | Menu interactions and click context |
| `calculateMovementAnalytics()` | ~2700-2872 | Movement distance and speed analysis |

### Combat Data Collection Module (Lines 2873-3664)
| Method | Lines | Description |
|--------|-------|-------------|
| `collectCombatData()` | 2873-3000 | Main combat data orchestrator |
| `collectHitsplatData()` | ~3000-3200 | Damage tracking and analysis |
| `collectAnimationData()` | ~3200-3400 | Animation state tracking |
| `collectInteractionData()` | ~3400-3600 | Combat interaction events |
| Helper methods for animation/combat analysis | ~3600-3664 | Type detection, duration estimation |

### Social Data Collection Module (Lines 3665-3868)  
| Method | Lines | Description |
|--------|-------|-------------|
| `collectSocialData()` | 3665-3675 | Main social data orchestrator |
| `collectRealChatData()` | 3680-3764 | Chat message analysis with type classification |
| `collectRealClanData()` | 3769-3804 | Clan membership and activity tracking |
| `collectRealTradeData()` | 3809-3838 | Trade interaction detection |
| Chat helper methods | 3841-3860 | Message processing utilities |

### Interface Data Collection Module (Lines 3869-7398)
| Method | Lines | Description |
|--------|-------|-------------|
| `collectInterfaceData()` | 3869-3882 | Main interface data orchestrator |
| `collectRealInterfaceData()` | 3887-3989 | Widget and interface state tracking |
| `collectRealDialogueData()` | 3994-4200 | Dialogue system interaction |
| `collectRealShopData()` | ~4200-4500 | Shop interface tracking |
| `collectRealBankData()` | ~4500-6000 | Banking operations and item tracking |
| Interface helper methods | ~6000-7000 | Interface type detection, tab tracking |
| Performance optimization methods | 7076-7398 | Object pools, pre-warming, cleanup |

## Key Dependencies and Integration Points

### External Dependencies
- **Client**: Core RuneLite API access
- **ItemManager**: Item name resolution and metadata
- **ConfigManager**: Plugin configuration access
- **RuneliteAIPlugin**: Parent plugin reference
- **DataStructures**: All data structure classes
- **AnalysisResults**: Analysis result structures  

### Internal Integration
- **DistanceAnalyticsManager**: Movement calculations
- **OSRSWikiService**: External item data lookup
- **GroundObjectTracker**: Object ownership tracking
- **TimerManager**: Game timer management

## Modularization Strategy

The file naturally divides into 8 focused modules:
1. **PlayerDataCollector** (lines 324-1199)
2. **WorldDataCollector** (lines 1200-1943)  
3. **InputDataCollector** (lines 1944-2872)
4. **CombatDataCollector** (lines 2873-3664)
5. **SocialDataCollector** (lines 3665-3868)
6. **InterfaceDataCollector** (lines 3869-7075)
7. **SystemMetricsCollector** (lines 7076-7398)
8. **DataCollectionOrchestrator** (main orchestration + core methods)

Each module will maintain the same public API contracts while organizing related functionality into cohesive units.