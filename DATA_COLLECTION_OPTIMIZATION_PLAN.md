# RuneLiteAI Data Collection Optimization Plan
## Converting from Tick-Based to Hybrid Collection Strategy

**Document Version**: 1.0  
**Date**: September 5, 2025  
**Status**: PLANNING  
**Impact**: HIGH - Reduces database load by ~84%, improves timing precision by 600x

---

## 📋 Executive Summary

The current RuneLiteAI plugin collects data from all 31 tables every game tick (600ms), resulting in:
- **310,000 records/minute** with 80%+ redundancy
- **Lost event precision** (600ms granularity instead of <1ms)
- **Massive database bloat** (~1.1GB/hour)
- **Poor ML training data** due to timing inaccuracy

This plan outlines the conversion to a **hybrid collection strategy** that will:
- Reduce database size by **84%**
- Improve event timing precision by **600x**
- Enhance ML model training quality significantly
- Reduce CPU and network overhead

---

## 🎯 Collection Strategy Categories

### 1. TICK-BASED (Snapshot Every Tick)
Core game state that changes continuously or needs regular snapshots.

### 2. EVENT-DRIVEN (Capture on Event)
Data that should only be stored when specific events occur.

### 3. STATE-TRANSITION (Capture on Change)
Data that should only be stored when state changes.

### 4. HYBRID (Event with Tick Association)
Events that need precise timing but also tick context.

---

## 📊 Table Classification and Conversion Plan

### Category 1: REMAIN TICK-BASED (5 tables)
No changes needed - these require continuous monitoring:

| Table | Current | Target | Priority |
|-------|---------|--------|----------|
| `game_ticks` | Every tick | Every tick | N/A |
| `player_vitals` | Every tick | Every tick | N/A |
| `player_location` | Every tick | Every tick | N/A |
| `nearby_players_data` | Every tick | Every tick | N/A |
| `nearby_npcs_data` | Every tick | Every tick | N/A |

### Category 2: CONVERT TO STATE-TRANSITION (8 tables)
Only store when data actually changes:

| Table | Current | Target | Detection Method | Priority |
|-------|---------|--------|------------------|----------|
| `player_stats` | Every tick | On XP gain | Compare XP values | MEDIUM |
| `player_equipment` | Every tick | On equip change | Hash comparison | HIGH |
| `player_inventory` | Every tick | On item change | Hash comparison | HIGH |
| `player_prayers` | Every tick | On prayer change | Bitwise comparison | MEDIUM |
| `player_spells` | Every tick | On spell change | Direct comparison | LOW |
| `world_environment` | Every tick | On significant change | State comparison | LOW |
| `interface_data` | Every tick | On interface change | Widget tracking | MEDIUM |
| `game_objects_data` | Every tick | On spawn/despawn | Object tracking | LOW |

### Category 3: CONVERT TO EVENT-DRIVEN (10 tables)
Only store when events occur:

| Table | Current | Target | Trigger Event | Priority |
|-------|---------|--------|---------------|----------|
| `bank_data` | Every tick | Bank open/close | BankOpened/Closed | HIGH |
| `bank_items` | Every tick | Bank change | ItemContainerChanged | HIGH |
| `bank_actions` | Every tick | On action | MenuOptionClicked | HIGH |
| `combat_data` | Every tick | Combat state | InteractingChanged | HIGH |
| `projectiles_data` | Every tick | Projectile exists | ProjectileMoved | MEDIUM |
| `ground_items_data` | Every tick | Item spawn/despawn | ItemSpawned/Despawned | MEDIUM |
| `hitsplats_data` | Every tick | On hitsplat | HitsplatApplied | HIGH |
| `animations_data` | Every tick | On animation | AnimationChanged | MEDIUM |
| `interactions_data` | Every tick | On interaction | InteractingChanged | MEDIUM |
| `chat_messages` | Every tick | On message | ChatMessage | LOW |

### Category 4: CONVERT TO HYBRID (8 tables)
Capture events with precise timing AND tick association:

| Table | Current | Target | Schema Change | Priority |
|-------|---------|--------|---------------|----------|
| `click_context` | Tick-batched | Event + tick | Add event_timestamp | CRITICAL |
| `key_presses` | Tick-batched | Event + tick | Add press/release times | CRITICAL |
| `key_combinations` | Tick-batched | Event + tick | Add combo_timestamp | CRITICAL |
| `mouse_buttons` | Tick-batched | Event + tick | Add click_timestamp | CRITICAL |
| `input_data` | Aggregated | Raw events | Restructure completely | HIGH |
| `system_metrics` | Averaged | Continuous | Add sampling_timestamp | LOW |

---

## 🏗️ Implementation Phases

### PHASE 1: Database Schema Updates (Week 1)
**Goal**: Add necessary columns for event timing without breaking existing functionality.

#### Step 1.1: Add Event Timing Columns
```sql
-- Add precise timing to input tables
ALTER TABLE click_context ADD COLUMN event_timestamp BIGINT;
ALTER TABLE key_presses ADD COLUMN press_timestamp BIGINT;
ALTER TABLE key_presses ADD COLUMN release_timestamp BIGINT;
ALTER TABLE key_combinations ADD COLUMN combo_timestamp BIGINT;
ALTER TABLE mouse_buttons ADD COLUMN click_timestamp BIGINT;
ALTER TABLE mouse_buttons ADD COLUMN release_timestamp BIGINT;

-- Add state tracking columns
ALTER TABLE player_equipment ADD COLUMN equipment_hash VARCHAR(64);
ALTER TABLE player_inventory ADD COLUMN inventory_hash VARCHAR(64);
ALTER TABLE player_stats ADD COLUMN total_xp BIGINT;
ALTER TABLE player_stats ADD COLUMN xp_gained INTEGER DEFAULT 0;
```

#### Step 1.2: Create State Tracking Table
```sql
CREATE TABLE collection_state_tracking (
    session_id INTEGER NOT NULL,
    table_name VARCHAR(50) NOT NULL,
    last_hash VARCHAR(64),
    last_update_tick BIGINT,
    last_update_timestamp TIMESTAMP,
    update_count INTEGER DEFAULT 0,
    PRIMARY KEY (session_id, table_name),
    FOREIGN KEY (session_id) REFERENCES sessions(session_id)
);
```

#### Step 1.3: Create Event Buffer Tables
```sql
CREATE TABLE event_buffer (
    event_id BIGSERIAL PRIMARY KEY,
    session_id INTEGER NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB NOT NULL,
    event_timestamp BIGINT NOT NULL,
    associated_tick BIGINT,
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### PHASE 2: Core Infrastructure Changes (Week 2)
**Goal**: Implement the foundation for hybrid collection without disrupting current functionality.

#### Step 2.1: Create Event Collection Framework
```java
// New EventCollectionManager.java
public class EventCollectionManager {
    private final ConcurrentLinkedQueue<GameEvent> eventQueue;
    private final StateTracker stateTracker;
    private final Map<String, Long> lastUpdateTicks;
    
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        captureEventWithTiming(event, "CLICK");
    }
    
    private void captureEventWithTiming(Object event, String type) {
        GameEvent gameEvent = new GameEvent();
        gameEvent.timestamp = System.nanoTime();
        gameEvent.tickAssociation = currentTickId;
        gameEvent.type = type;
        gameEvent.data = event;
        eventQueue.offer(gameEvent);
    }
}
```

#### Step 2.2: Implement State Change Detection
```java
// New StateChangeDetector.java
public class StateChangeDetector {
    private final Map<String, String> previousStates = new HashMap<>();
    
    public boolean hasEquipmentChanged(PlayerEquipment current) {
        String currentHash = generateHash(current);
        String previousHash = previousStates.get("equipment");
        
        if (!currentHash.equals(previousHash)) {
            previousStates.put("equipment", currentHash);
            return true;
        }
        return false;
    }
    
    private String generateHash(Object data) {
        return DigestUtils.sha256Hex(gson.toJson(data));
    }
}
```

#### Step 2.3: Create Conditional Collection Logic
```java
// Modified DataCollectionOrchestrator.java
public TickDataCollection orchestrateCollection() {
    TickDataCollection.Builder builder = TickDataCollection.builder();
    
    // Always collect core tick data
    builder.gameTickData(collectGameTick());
    builder.playerVitals(collectPlayerVitals());
    builder.playerLocation(collectPlayerLocation());
    
    // Conditional collection based on state changes
    if (stateDetector.hasEquipmentChanged()) {
        builder.playerEquipment(collectPlayerEquipment());
    }
    
    if (client.getItemContainer(InventoryID.BANK) != null) {
        builder.bankData(collectBankData());
    }
    
    // Process event queue
    processEventQueue(builder);
    
    return builder.build();
}
```

### PHASE 3: Table-by-Table Migration (Weeks 3-4)
**Goal**: Systematically convert each table category to its optimal collection strategy.

#### Step 3.1: Critical Input Tables (CRITICAL PRIORITY)
```java
// Week 3, Days 1-2: Convert click_context
@Subscribe
public void onMenuOptionClicked(MenuOptionClicked event) {
    ClickData click = new ClickData();
    click.eventTimestamp = System.nanoTime();
    click.tickId = currentTickId;
    click.clickType = determineClickType(event);
    click.targetName = event.getMenuTarget();
    click.worldLocation = client.getLocalPlayer().getWorldLocation();
    
    // Store immediately, don't wait for tick
    eventCollector.storeClickEvent(click);
}

// Week 3, Days 3-4: Convert key_presses and key_combinations
@Override
public void keyPressed(KeyEvent e) {
    KeyPressData keyPress = new KeyPressData();
    keyPress.pressTimestamp = System.nanoTime();
    keyPress.keyCode = e.getKeyCode();
    keyPress.tickId = currentTickId;
    
    activeKeyPresses.put(e.getKeyCode(), keyPress);
    detectKeyCombination(e);
}

@Override
public void keyReleased(KeyEvent e) {
    KeyPressData keyPress = activeKeyPresses.remove(e.getKeyCode());
    if (keyPress != null) {
        keyPress.releaseTimestamp = System.nanoTime();
        keyPress.duration = keyPress.releaseTimestamp - keyPress.pressTimestamp;
        eventCollector.storeKeyPress(keyPress);
    }
}
```

#### Step 3.2: Banking System (HIGH PRIORITY)
```java
// Week 3, Days 5-6: Convert banking tables
@Subscribe
public void onWidgetLoaded(WidgetLoaded event) {
    if (event.getGroupId() == WidgetID.BANK_GROUP_ID) {
        startBankingSession();
    }
}

@Subscribe
public void onWidgetClosed(WidgetClosed event) {
    if (event.getGroupId() == WidgetID.BANK_GROUP_ID) {
        endBankingSession();
        storeBankSnapshot();
    }
}

private void storeBankSnapshot() {
    // Only store bank data when bank closes
    BankData snapshot = collectBankData();
    databaseManager.storeBankSnapshot(snapshot);
}
```

#### Step 3.3: State-Change Tables (MEDIUM PRIORITY)
```java
// Week 4, Days 1-3: Implement change detection for equipment/inventory
public void onGameTick(GameTick tick) {
    // Check equipment changes
    PlayerEquipment currentEquipment = collectEquipment();
    if (!currentEquipment.equals(lastEquipment)) {
        storeEquipmentChange(currentEquipment);
        lastEquipment = currentEquipment;
    }
    
    // Check inventory changes
    PlayerInventory currentInventory = collectInventory();
    String invHash = generateHash(currentInventory);
    if (!invHash.equals(lastInventoryHash)) {
        storeInventoryChange(currentInventory);
        lastInventoryHash = invHash;
    }
}
```

#### Step 3.4: Event-Driven Tables (MEDIUM PRIORITY)
```java
// Week 4, Days 4-6: Convert combat and animation tables
@Subscribe
public void onInteractingChanged(InteractingChanged event) {
    if (event.getSource() == client.getLocalPlayer()) {
        Actor target = event.getTarget();
        updateCombatState(target);
        
        if (target != null && !inCombat) {
            startCombatSession();
        } else if (target == null && inCombat) {
            endCombatSession();
        }
    }
}

@Subscribe
public void onAnimationChanged(AnimationChanged event) {
    if (event.getActor() == client.getLocalPlayer()) {
        AnimationData anim = new AnimationData();
        anim.animationId = event.getActor().getAnimation();
        anim.timestamp = System.nanoTime();
        anim.tickId = currentTickId;
        storeAnimationEvent(anim);
    }
}
```

### PHASE 4: Database Operations Optimization (Week 5)
**Goal**: Modify database operations to handle the new collection patterns efficiently.

#### Step 4.1: Batch Processing Optimization
```java
// Modified DatabaseTableOperations.java
public void processMixedBatch(MixedDataBatch batch) {
    try (Connection conn = getConnection()) {
        conn.setAutoCommit(false);
        
        // Process tick-based data
        if (!batch.getTickData().isEmpty()) {
            insertTickBasedData(conn, batch.getTickData());
        }
        
        // Process events
        if (!batch.getEvents().isEmpty()) {
            insertEventData(conn, batch.getEvents());
        }
        
        // Process state changes
        if (!batch.getStateChanges().isEmpty()) {
            insertStateChanges(conn, batch.getStateChanges());
        }
        
        conn.commit();
    } catch (SQLException e) {
        log.error("Batch processing failed", e);
        rollback(conn);
    }
}
```

#### Step 4.2: Implement Selective Insert Methods
```java
private void insertSelectiveData(Connection conn, TickDataCollection data) {
    // Only insert non-null data that has changed
    if (data.getPlayerEquipment() != null) {
        insertEquipmentData(conn, data.getPlayerEquipment());
    }
    
    if (data.getBankData() != null) {
        insertBankData(conn, data.getBankData());
    }
    
    // Always insert core tick data
    insertCoreTickData(conn, data);
}
```

### PHASE 5: Testing and Validation (Week 6)
**Goal**: Ensure the new system maintains data integrity while achieving performance goals.

#### Step 5.1: Unit Tests for State Detection
```java
@Test
public void testEquipmentChangeDetection() {
    StateChangeDetector detector = new StateChangeDetector();
    PlayerEquipment equip1 = createEquipment("helm", "chest", "legs");
    PlayerEquipment equip2 = createEquipment("helm", "chest", "legs");
    PlayerEquipment equip3 = createEquipment("hat", "chest", "legs");
    
    assertTrue(detector.hasChanged("equipment", equip1)); // First time
    assertFalse(detector.hasChanged("equipment", equip2)); // Same equipment
    assertTrue(detector.hasChanged("equipment", equip3)); // Different helm
}
```

#### Step 5.2: Performance Benchmarks
```java
@Test
public void benchmarkDataReduction() {
    // Run old collection method for 1000 ticks
    long oldRecordCount = runOldCollection(1000);
    long oldDbSize = measureDatabaseSize();
    
    // Run new collection method for 1000 ticks
    long newRecordCount = runNewCollection(1000);
    long newDbSize = measureDatabaseSize();
    
    // Assert 80%+ reduction
    assertTrue(newRecordCount < oldRecordCount * 0.2);
    assertTrue(newDbSize < oldDbSize * 0.2);
}
```

#### Step 5.3: Data Quality Validation
```java
@Test
public void validateEventTimingPrecision() {
    // Capture 100 click events
    List<ClickEvent> clicks = captureClicks(100);
    
    // Verify microsecond precision
    for (ClickEvent click : clicks) {
        assertNotNull(click.getEventTimestamp());
        assertTrue(click.getEventTimestamp() % 1000 != 0); // Not rounded to ms
    }
    
    // Verify proper tick association
    for (ClickEvent click : clicks) {
        assertNotNull(click.getAssociatedTick());
        assertTrue(click.getAssociatedTick() > 0);
    }
}
```

### PHASE 6: Rollout and Migration (Week 7)
**Goal**: Deploy the optimized system with minimal disruption.

#### Step 6.1: Feature Flags Implementation
```java
public class CollectionConfig {
    // Feature flags for gradual rollout
    public static boolean USE_EVENT_DRIVEN_CLICKS = false;
    public static boolean USE_STATE_CHANGE_EQUIPMENT = false;
    public static boolean USE_EVENT_DRIVEN_BANKING = false;
    
    public static void enablePhase1() {
        USE_EVENT_DRIVEN_CLICKS = true;
    }
    
    public static void enablePhase2() {
        USE_STATE_CHANGE_EQUIPMENT = true;
        USE_EVENT_DRIVEN_BANKING = true;
    }
}
```

#### Step 6.2: Backward Compatibility Layer
```java
public class BackwardCompatibilityAdapter {
    public TickDataCollection adaptToLegacy(MixedDataBatch modern) {
        // Convert modern event-driven data to legacy tick format
        // for compatibility during transition
    }
}
```

#### Step 6.3: Data Migration Scripts
```sql
-- Migrate existing data to new schema
-- Add event timestamps based on tick timestamps
UPDATE click_context 
SET event_timestamp = 
    (SELECT timestamp FROM game_ticks WHERE tick_id = click_context.tick_id)
WHERE event_timestamp IS NULL;

-- Create indexes for new columns
CREATE INDEX idx_click_event_timestamp ON click_context(event_timestamp);
CREATE INDEX idx_key_press_timestamp ON key_presses(press_timestamp);
```

---

## 📊 Success Metrics

### Performance Targets
| Metric | Current | Target | Measurement Method |
|--------|---------|--------|-------------------|
| Database growth rate | 1.1 GB/hour | < 200 MB/hour | Monitor pg_database_size() |
| Records per minute | 310,000 | < 50,000 | COUNT(*) queries |
| Event timing precision | 600ms | < 1ms | Timestamp analysis |
| CPU usage | High | < 10% reduction | System monitoring |
| Network bandwidth | High | < 80% reduction | Network monitoring |

### Data Quality Targets
| Metric | Current | Target | Measurement Method |
|--------|---------|--------|-------------------|
| Input event accuracy | 600ms granularity | Sub-millisecond | Timestamp precision |
| State change detection | 0% (all recorded) | > 95% accurate | Change validation |
| Redundant data | > 80% | < 5% | Duplicate detection |
| ML training quality | Poor timing data | Precise sequences | Model performance |

---

## ⚠️ Risk Assessment and Mitigation

### Risk 1: Data Loss During Transition
**Mitigation**: 
- Implement parallel collection during transition
- Keep legacy system running until validation complete
- Create comprehensive backups before migration

### Risk 2: Performance Degradation from Event Handling
**Mitigation**:
- Use async event processing
- Implement event buffering
- Set maximum queue sizes with overflow handling

### Risk 3: Breaking Existing Analysis Tools
**Mitigation**:
- Provide compatibility layer
- Create data transformation scripts
- Document all schema changes

### Risk 4: Increased Code Complexity
**Mitigation**:
- Maintain clear separation of concerns
- Comprehensive documentation
- Extensive unit testing

---

## 📅 Timeline Summary

| Week | Phase | Deliverables |
|------|-------|-------------|
| 1 | Database Schema | Updated schema with event timing columns |
| 2 | Core Infrastructure | Event collection framework, state detection |
| 3 | Critical Tables | Input event conversion (clicks, keys) |
| 4 | High Priority Tables | Banking, combat, inventory conversions |
| 5 | Database Optimization | Batch processing, selective inserts |
| 6 | Testing & Validation | Performance benchmarks, quality validation |
| 7 | Rollout & Migration | Gradual deployment with feature flags |

**Total Duration**: 7 weeks  
**Expected Benefits**: 84% database size reduction, 600x timing precision improvement

---

## 🚀 Next Steps

1. **Review and Approval**: Get stakeholder approval for this plan
2. **Environment Setup**: Create development/testing database
3. **Begin Phase 1**: Start with database schema updates
4. **Weekly Progress Reviews**: Track against success metrics
5. **Documentation Updates**: Update all technical documentation

---

## 📝 Appendix A: Detailed Table Specifications

[Detailed specifications for each table's conversion would go here]

## 📝 Appendix B: Code Examples

[Additional code examples and patterns would go here]

## 📝 Appendix C: SQL Migration Scripts

[Complete SQL scripts for all schema changes would go here]

---

*This document is a living plan and will be updated as implementation progresses.*