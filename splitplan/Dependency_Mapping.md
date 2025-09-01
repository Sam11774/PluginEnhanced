# Dependency Mapping and Migration Blueprint

## Inter-Class Relationships

### Current Monolithic Architecture
```
RuneliteAIPlugin
├── DataCollectionManager (7,397 lines)
│   ├── Depends on: Client, ItemManager, ConfigManager, RuneliteAIPlugin
│   ├── Uses: DataStructures.*, AnalysisResults.*
│   ├── Integrates: OSRSWikiService, DistanceAnalyticsManager, GroundObjectTracker
│   └── Produces: TickDataCollection objects
│
└── DatabaseManager (3,414 lines)
    ├── Depends on: Client, ItemManager
    ├── Uses: DataStructures.* (via TickDataCollection)
    ├── Integrates: HikariDataSource, ScheduledExecutorService
    └── Consumes: TickDataCollection objects
```

### Target Modular Architecture
```
RuneliteAIPlugin
├── DataCollectionManager (Orchestrator: ~200 lines)
│   ├── PlayerDataCollector (~800 lines)
│   ├── WorldDataCollector (~700 lines)
│   ├── InputDataCollector (~900 lines)
│   ├── CombatDataCollector (~800 lines)
│   ├── SocialDataCollector (~200 lines)
│   ├── InterfaceDataCollector (~1000 lines)
│   ├── SystemMetricsCollector (~300 lines)
│   └── DataCollectionOrchestrator (~400 lines)
│
└── DatabaseManager (Orchestrator: ~200 lines)
    ├── DatabaseConnectionManager (~400 lines)
    ├── DatabaseTableOperations (~2000 lines)
    ├── DatabaseUtilities (~600 lines)
    └── DatabasePerformanceMonitor (~200 lines)
```

## Critical Dependencies Analysis

### External RuneLite API Dependencies
| API Class | Used By | Purpose | Migration Impact |
|-----------|---------|---------|------------------|
| `Client` | All collectors | Core game state access | Must inject into all modules |
| `ItemManager` | Player, Database | Item name resolution | Shared dependency, needs injection |
| `ConfigManager` | DataCollection | Plugin configuration | Keep in orchestrator |
| `Widget`, `WidgetInfo` | Interface | UI state tracking | Interface module only |
| `WorldPoint`, `LocalPoint` | Player, World | Coordinate tracking | Player/World modules |

### Internal Plugin Dependencies
| Class | Current Usage | New Module Assignment |
|-------|---------------|----------------------|
| `DataStructures.*` | All collectors | Shared across all modules |
| `AnalysisResults.*` | Analysis logic | SystemMetrics and Orchestrator |
| `OSRSWikiService` | Item lookups | DatabaseUtilities module |
| `DistanceAnalyticsManager` | Movement calc | InputDataCollector module |
| `GroundObjectTracker` | Object tracking | WorldDataCollector module |

### Database Schema Dependencies
| Table Group | Insert Methods | New Module |
|-------------|---------------|------------|
| Core Tables | game_ticks, sessions | DatabaseConnectionManager |
| Player Tables | player_*, prayers, spells | DatabaseTableOperations |
| World Tables | world_*, nearby_*, objects | DatabaseTableOperations |
| Input Tables | input_*, click_*, key_* | DatabaseTableOperations |
| Social/Interface | social_*, interface_* | DatabaseTableOperations |

## Migration Blueprint

### Phase 1: Create Module Skeletons

#### DataCollectionManager Modules
1. **PlayerDataCollector.java**
   ```java
   public class PlayerDataCollector {
       private final Client client;
       private final ItemManager itemManager;
       // TODO: Migrate lines 324-1199 from DataCollectionManager
       public PlayerData collectPlayerData() { /* placeholder */ }
       public PlayerVitals collectPlayerVitals() { /* placeholder */ }
       // ... other methods
   }
   ```

2. **WorldDataCollector.java**
   ```java
   public class WorldDataCollector {
       private final Client client;
       private final GroundObjectTracker groundObjectTracker;
       // TODO: Migrate lines 1200-1943 from DataCollectionManager
       public WorldEnvironmentData collectWorldData() { /* placeholder */ }
       // ... other methods
   }
   ```

3. **InputDataCollector.java**
   ```java
   public class InputDataCollector {
       private final Client client;
       private final DistanceAnalyticsManager distanceAnalytics;
       // TODO: Migrate lines 1944-2872 from DataCollectionManager
       public MouseInputData collectMouseInput() { /* placeholder */ }
       // ... other methods
   }
   ```

4. **CombatDataCollector.java**
   ```java  
   public class CombatDataCollector {
       private final Client client;
       // TODO: Migrate lines 2873-3664 from DataCollectionManager
       public CombatData collectCombatData() { /* placeholder */ }
       // ... other methods
   }
   ```

5. **SocialDataCollector.java**
   ```java
   public class SocialDataCollector {
       private final Client client;
       // TODO: Migrate lines 3665-3868 from DataCollectionManager
       public ChatData collectChatData() { /* placeholder */ }
       // ... other methods
   }
   ```

6. **InterfaceDataCollector.java**
   ```java
   public class InterfaceDataCollector {
       private final Client client;
       // TODO: Migrate lines 3869-7075 from DataCollectionManager
       public InterfaceData collectInterfaceData() { /* placeholder */ }
       // ... other methods
   }
   ```

7. **SystemMetricsCollector.java**
   ```java
   public class SystemMetricsCollector {
       private final Client client;
       // TODO: Migrate lines 7076-7398 from DataCollectionManager
       public SystemMetrics collectSystemMetrics() { /* placeholder */ }
       // ... other methods
   }
   ```

8. **DataCollectionOrchestrator.java**
   ```java
   public class DataCollectionOrchestrator {
       private final PlayerDataCollector playerCollector;
       private final WorldDataCollector worldCollector;
       // ... other collectors
       // TODO: Migrate main orchestration logic
       public TickDataCollection collectAllData() { /* placeholder */ }
   }
   ```

#### DatabaseManager Modules

1. **DatabaseConnectionManager.java**
   ```java
   public class DatabaseConnectionManager {
       private HikariDataSource dataSource;
       // TODO: Migrate lines 85-318 from DatabaseManager
       public void initializeDatabase() { /* placeholder */ }
       public Integer initializeSession() { /* placeholder */ }
       // ... other methods
   }
   ```

2. **DatabaseTableOperations.java**
   ```java
   public class DatabaseTableOperations {
       private final Client client;
       private final ItemManager itemManager;
       // TODO: Migrate lines 616-2591 from DatabaseManager
       public void insertGameTicksBatch() { /* placeholder */ }
       public void insertPlayerDataBatch() { /* placeholder */ }
       // ... 20+ other insert methods
   }
   ```

3. **DatabaseUtilities.java**
   ```java
   public class DatabaseUtilities {
       private final ItemManager itemManager;
       // TODO: Migrate lines 2592-3302 from DatabaseManager
       public String convertInventoryItemsToJson() { /* placeholder */ }
       // ... other utility methods
   }
   ```

4. **DatabasePerformanceMonitor.java**
   ```java
   public class DatabasePerformanceMonitor {
       // TODO: Migrate lines 2751-2950 from DatabaseManager
       public void recordDatabaseCall() { /* placeholder */ }
       public boolean isHealthy() { /* placeholder */ }
       // ... other monitoring methods
   }
   ```

### Phase 2: Update Main Classes for Delegation

#### DataCollectionManager.java (Reduced to ~200 lines)
```java
@Slf4j
public class DataCollectionManager {
    private final DataCollectionOrchestrator orchestrator;
    
    public DataCollectionManager(Client client, ItemManager itemManager, 
                               ConfigManager configManager, RuneliteAIPlugin plugin) {
        // Initialize all collector modules
        PlayerDataCollector playerCollector = new PlayerDataCollector(client, itemManager);
        WorldDataCollector worldCollector = new WorldDataCollector(client);
        // ... initialize other collectors
        
        this.orchestrator = new DataCollectionOrchestrator(playerCollector, worldCollector, /*...*/);
    }
    
    public TickDataCollection collectAllData(Integer sessionId, int tickNumber, 
                                           GameStateSnapshot gameStateSnapshot, 
                                           GameStateDelta gameStateDelta) {
        return orchestrator.collectAllData(sessionId, tickNumber, gameStateSnapshot, gameStateDelta);
    }
    
    public void shutdown() {
        orchestrator.shutdown();
    }
}
```

#### DatabaseManager.java (Reduced to ~200 lines)
```java
@Slf4j
public class DatabaseManager {
    private final DatabaseConnectionManager connectionManager;
    private final DatabaseTableOperations tableOperations;
    private final DatabaseUtilities utilities;
    private final DatabasePerformanceMonitor performanceMonitor;
    
    public DatabaseManager(Client client, ItemManager itemManager) {
        this.connectionManager = new DatabaseConnectionManager();
        this.tableOperations = new DatabaseTableOperations(client, itemManager, connectionManager);
        this.utilities = new DatabaseUtilities(itemManager);
        this.performanceMonitor = new DatabasePerformanceMonitor();
    }
    
    public Integer initializeSession() {
        return connectionManager.initializeSession();
    }
    
    public void storeTickData(TickDataCollection tickData) {
        tableOperations.storeTickData(tickData);
    }
    
    public void shutdown() {
        performanceMonitor.shutdown();
        connectionManager.shutdown();
    }
}
```

## Migration Validation Strategy

### Compilation Checkpoints
1. After each module skeleton creation → Verify no compilation errors
2. After main class delegation updates → Verify API compatibility
3. After each content migration phase → Run full compilation

### Runtime Testing Points
1. Database connectivity verification
2. Data collection functional testing
3. Performance regression checking  
4. Integration testing with full plugin

### Rollback Strategy
- Complete backup before starting migration
- Git commit after each successful phase
- Ability to restore monolithic version if needed

## Success Metrics
- ✅ Zero compilation errors maintained throughout migration
- ✅ All public APIs preserved exactly  
- ✅ Database operations continue functioning
- ✅ Performance remains within 10% of baseline
- ✅ Code maintainability dramatically improved