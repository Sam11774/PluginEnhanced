# DatabaseManager.java Structure Analysis
**File Size**: 3,414 lines  
**Current Status**: Monolithic database class requiring modularization

## Core File Structure

### Package and Imports (Lines 1-26)
```java
package net.runelite.client.plugins.runeliteai;

// Core imports:
- lombok.extern.slf4j.Slf4j
- net.runelite.api.Client, Player, ItemComposition
- net.runelite.client.game.ItemManager
- DataStructures.BankData, BankItemData, BankActionData
- java.sql.* (all JDBC classes)
- java.util.concurrent.* (concurrency utilities)
- javax.sql.DataSource
- com.zaxxer.hikari.HikariConfig, HikariDataSource (connection pooling)
- java.io.IOException, InputStream, Properties
```

### Main Class Declaration (Lines 50-109)
```java
@Slf4j  
public class DatabaseManager
{
    // Core dependencies (lines 52-54)
    private final Client client;
    private final ItemManager itemManager;
    
    // Database configuration (lines 56-63)
    private final Properties dbConfig;
    private final String databaseUrl, databaseUser, databasePassword;
    private final int batchSize, maxConnections, connectionTimeout;
    
    // Connection management (lines 65-68)
    private HikariDataSource dataSource;
    private final AtomicBoolean connected, shutdown;
    
    // Batch processing (lines 70-74)
    private final ScheduledExecutorService batchExecutor;
    private final Queue<TickDataCollection> pendingBatch;
    private final AtomicLong totalRecordsInserted, totalBatchesProcessed;
    
    // Performance tracking & session management (lines 76-83)
}
```

## Method Structure Analysis

### Core Infrastructure (Lines 85-615)
| Method | Lines | Description |
|--------|-------|-------------|
| `constructor` | 85-109 | Initialize dependencies, load config, start services |
| `loadDatabaseConfig()` | 114-138 | Load properties file and environment variables |
| `initializeDatabase()` | 143-183 | Setup HikariCP connection pool with optimizations |
| `verifyDatabaseSchema()` | 188-203 | Verify required tables exist |
| `initializeSession()` | 209-248 | Create new session record in database |
| `storeTickData()` | 254-318 | **MAIN ENTRY POINT** - Queue tick data for batch processing |
| `startBatchProcessor()` | 325-359 | Initialize scheduled batch processing |
| `triggerBatchProcessing()` | 364-369 | Trigger immediate batch processing |
| `processBatch()` | 385-615 | Core batch processing logic with transaction management |

### Database Table Operations Module (Lines 616-2591)
This section contains 23+ specialized batch insert methods organized by domain:

#### Core Game Data (Lines 616-815)
| Method | Lines | Description |
|--------|-------|-------------|
| `insertGameTicksBatch()` | 616-685 | Insert core tick records and return generated IDs |
| `insertPlayerDataBatch()` | 686-754 | Player vitals (HP, prayer, energy, special attack) |
| `insertPlayerLocationBatch()` | 755-814 | Player position, region, area information |

#### Player State Data (Lines 815-1543)
| Method | Lines | Description |
|--------|-------|-------------|
| `insertPlayerStatsBatch()` | 815-953 | All 23 skills levels and experience |
| `insertPlayerEquipmentBatch()` | 954-1073 | Equipment slots with item names and IDs |
| `insertPlayerInventoryBatch()` | 1074-1409 | **COMPLEX** - Inventory items with JSON serialization |
| `insertPlayerPrayersBatch()` | 1410-1497 | Active prayers state tracking |
| `insertPlayerSpellsBatch()` | 1498-1543 | Active spells and magic state |

#### World Environment Data (Lines 1544-2086)
| Method | Lines | Description |
|--------|-------|-------------|
| `insertWorldDataBatch()` | 1544-1598 | World environment and weather data |
| `insertCombatDataBatch()` | 1599-1649 | Combat events and damage tracking |
| `insertHitsplatsDataBatch()` | 1650-1695 | Damage splat details and analysis |
| `insertAnimationsDataBatch()` | 1696-1740 | Animation state tracking |
| `insertInteractionsDataBatch()` | 1741-1786 | Player-NPC/Player interactions |
| `insertNearbyPlayersDataBatch()` | 1787-1829 | Nearby player detection and analysis |
| `insertNearbyNPCsDataBatch()` | 1830-2086 | **COMPLEX** - NPC tracking with detailed analysis |

#### Input and Interface Data (Lines 2087-2591)
| Method | Lines | Description |
|--------|-------|-------------|
| `insertInputDataBatch()` | 2087-2181 | Mouse and keyboard input tracking |
| `insertClickContextBatch()` | 2182-2245 | Click context and intelligence data |
| `insertKeyPressDataBatch()` | 2246-2300 | Detailed key press analytics |
| `insertMouseButtonDataBatch()` | 2301-2360 | Mouse button interaction tracking |
| `insertSocialDataBatch()` | 2361-2400 | Chat and social interaction data |
| `insertInterfaceDataBatch()` | 2401-2591 | **COMPLEX** - Interface and widget state tracking |

#### System and Performance Data (Lines 2592-2684)
| Method | Lines | Description |
|--------|-------|-------------|
| `insertSystemMetricsBatch()` | 2592-2637 | Performance metrics and system health |
| `insertWorldObjectsBatch()` | 2638-2684 | Game object interaction tracking |

### Database Utilities Module (Lines 2685-3415)
#### Core Utility Methods (Lines 2685-2800)
| Method | Lines | Description |
|--------|-------|-------------|
| `getCurrentPlayerName()` | 2685-2695 | Get current player name with fallback |
| `updatePlayerNameIfNeeded()` | 2696-2750 | Update session if player name changes |
| `recordDatabaseCall()` | 2751-2760 | Performance tracking for database operations |
| `reportPerformanceMetrics()` | 2761-2800 | Periodic performance reporting |

#### Session and Health Management (Lines 2801-2950)
| Method | Lines | Description |
|--------|-------|-------------|
| `getActiveSessions()` | 2801-2810 | Get active session information |
| `isHealthy()` | 2811-2820 | Database health check |
| `getDatabaseStats()` | 2821-2850 | Database performance statistics |
| `shutdown()` | 2851-2950 | Clean shutdown with resource cleanup |

#### JSON Conversion Utilities (Lines 2951-3302)
| Method | Lines | Description |
|--------|-------|-------------|
| `convertInventoryItemsToJson()` | 2951-3302 | **VERY COMPLEX** - Convert inventory to JSON with ItemManager integration and timeout protection |

#### Item Name Resolution (Lines 3303-3415)
| Method | Lines | Description |
|--------|-------|-------------|
| `getPrayerState()` | 3307-3312 | Extract prayer state from maps |
| `getTotalQuantity()` | 3317-3328 | Calculate item quantities |
| `getUniqueItemTypes()` | 3333-3338 | Count unique items |
| `getMostValuableItem()` | 3343-3359 | Find most valuable inventory item |
| `isProblematicItemId()` | 3368-3389 | Identify items that cause hanging |
| `getKnownItemName()` | 3396-3414 | Fallback names for problematic items |

## Key Dependencies and Integration Points

### External Dependencies
- **Client**: RuneLite API for player data access
- **ItemManager**: Item name resolution with timeout protection
- **HikariDataSource**: Connection pooling and management
- **ScheduledExecutorService**: Batch processing coordination
- **DataStructures**: All data structure classes from main plugin

### Database Schema Dependencies
- **34 Production Tables**: All insert methods depend on specific table schemas
- **Foreign Key Relationships**: Many tables reference game_ticks.tick_id
- **JSON Columns**: Complex JSON serialization for inventory and other data
- **Performance Indexes**: Batch operations depend on proper indexing

## Performance-Critical Sections

### High-Complexity Areas Requiring Special Attention
1. **Inventory JSON Conversion** (lines 2951-3302): Very complex with ItemManager integration
2. **NPC Batch Insert** (lines 1830-2086): Large data volumes with complex analysis
3. **Interface Data Insert** (lines 2401-2591): Complex widget state serialization
4. **Batch Processing** (lines 385-615): Transaction management and error handling

### Performance Optimizations Present
- **Connection Pooling**: HikariCP with optimized settings
- **Prepared Statement Caching**: Reduces SQL parsing overhead  
- **Batch Operations**: All inserts use batch processing for efficiency
- **Item Name Caching**: Timeout protection for problematic items
- **Transaction Management**: Single transaction per batch

## Modularization Strategy

The file naturally divides into 4 focused modules:

1. **DatabaseConnectionManager** (lines 85-318)
   - Configuration loading
   - HikariCP setup and management
   - Session initialization
   - Connection health checks

2. **DatabaseTableOperations** (lines 319-2591)  
   - All 23+ batch insert methods
   - Transaction management
   - Batch processing coordination
   - Table-specific operations

3. **DatabaseUtilities** (lines 2592-3302)
   - JSON conversion utilities
   - Item name resolution
   - Performance metrics
   - Helper methods

4. **DatabasePerformanceMonitor** (lines 2751-2950)
   - Performance tracking
   - Health monitoring  
   - Statistics reporting
   - Shutdown coordination

Each module will maintain clear separation while working together through well-defined interfaces.