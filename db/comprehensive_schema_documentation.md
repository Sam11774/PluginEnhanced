# OSRS AI Database Schema Documentation

**Analysis Date:** August 26, 2025  
**Database:** runelite_ai  
**Total Tables:** 41  
**Data Collection Period:** Ticks 1-8,659 (SwampHunter activity)

---

## Executive Summary

The OSRS AI database contains a comprehensive schema designed to capture detailed RuneScape behavioral data for machine learning training. However, **66% of tables are currently empty**, and the collected data shows severe behavioral diversity limitations that explain the training overfitting issues.

### Key Findings
- **14 tables with data** out of 41 total (34% utilization)
- **245,108 total data rows** across all populated tables
- **Perfect tick coverage** on core behavioral tables (8,659 ticks)
- **Critical diversity limitations** in the SwampHunter dataset

---

## Database Architecture Overview

### Core Data Collection Tables (Populated)

#### 1. **game_ticks** - Master Tick Registry
- **Purpose**: Central timing reference and enhanced action tracking
- **Rows**: 8,659 (100% coverage)
- **Key Columns**: 56 columns with 25 always null
- **Status**: ✅ **ACTIVE** - Perfect tick coverage
- **Data Quality**: High temporal consistency, many unused enhanced features

```sql
-- Core columns with data:
tick_id, session_id, tick_number, timestamp, player_name, world_id,
tick_coverage_percent, data_points_collected, current_interface_tab,
client_focused, available_actions, environmental_context

-- Always null columns (unused features):
enhanced_action_id, target_object_*, predicted_next_action, 
interface_screen_*, action_category, weather_conditions, etc.
```

#### 2. **player_location** - Movement Tracking
- **Purpose**: Player position and environmental context
- **Rows**: 8,659 (100% coverage)  
- **Status**: ✅ **ACTIVE** - Complete spatial data
- **Critical Issue**: **Confined to 7x5 tile area** (35 total tiles)

```sql
-- Active columns:
world_x (3549-3556), world_y (3449-3453), region_id (14652), plane (0)
music_track_id, danger_level, expected_ambient_sounds

-- Always null columns:
chunk_x, chunk_y, local_x, local_y, area_name, location_type
```

**Spatial Coverage Analysis:**
- **X Range**: 7 tiles (3549-3556)
- **Y Range**: 5 tiles (3449-3453)  
- **Total Area**: 35 tiles (~0.001% of game world)
- **Region**: Single region (14652)
- **Plane**: Ground level only

#### 3. **mouse_state** - Mouse Behavior Tracking
- **Purpose**: Mouse movement and human-likeness analysis
- **Rows**: 8,659 (100% coverage)
- **Status**: ⚠️ **CRITICAL ISSUE** - Zero mouse movement

```sql
-- Problematic static values:
movement_speed: 0.0 (all 8659 ticks)
human_likeness_score: 1.0 (all 8659 ticks)  
bot_detection_score: 0.0 (all 8659 ticks)
movement_distance: 0.0 (all 8659 ticks)

-- Variable columns:
mouse_x, mouse_y, idle_time, click_accuracy
```

**Mouse Behavior Analysis:**
- **Movement**: Completely static (0.0 speed/distance)
- **Detection Scores**: Unnaturally perfect (1.0 human, 0.0 bot)
- **Implication**: AFK or automated behavior

#### 4. **camera_state** - Camera Control Tracking  
- **Purpose**: Camera positioning and viewing behavior
- **Rows**: 8,659 (100% coverage)
- **Status**: ✅ **MODERATE** - Some camera movement

```sql
-- Variable columns:
yaw (62 unique values: 0-1877)
pitch (35 unique values: 0-383)
x, y, z (camera world position)

-- Static columns:
zoom_level: 512 (fixed zoom)
camera_smooth: true (all ticks)
```

#### 5. **player_vitals** - Health and Status Tracking
- **Purpose**: Health, prayer, energy, and combat status
- **Rows**: 8,659 (100% coverage)  
- **Status**: ⚠️ **STATIC** - No resource changes

```sql
-- Static values (no variation):
current_health: 42 (all ticks)
max_health: 42 (all ticks)
current_prayer: 33 (all ticks)  
max_prayer: 33 (all ticks)
combat_level: 61 (all ticks)

-- Variable columns:
run_energy (92-100, avg 99.2)
special_attack, health_percent, prayer_percent
```

#### 6. **inventory_items** - Inventory Management
- **Purpose**: Item tracking and inventory interactions
- **Rows**: 42,497 (96.9% tick coverage - missing ticks 7587-7853)
- **Status**: ⚠️ **LIMITED DIVERSITY** - Only 3 items

```sql
-- Item diversity:
item_name: 'Rope' (16,296), 'Small fishing net' (16,104), 'Swamp lizard' (10,097)
item_id: 954, 303, 10149
quantity: 1 (all items - no stacking)
slot_id: 0-27 (uses 13 of 28 slots)

-- Always null:
item_category (missing categorization)
```

#### 7. **nearby_npcs** - NPC Environment Tracking
- **Purpose**: Surrounding NPCs and potential interactions
- **Rows**: 159,294 (100% coverage - highest data volume)
- **Status**: ✅ **GOOD** - Rich environmental data

```sql
-- NPC diversity (12 types):
'Young tree' (63,744), 'Tree' (39,654), 'Swamp lizard' (38,880),
'Oak tree' (12,870), 'Willow tree' (4,146)

-- Combat levels:
Level 0: 120,420 (trees/objects)
Level 1: 38,874 (lizards)

-- Always null:
npc_type, target_name, last_transformation_timestamp
```

#### 8. **sessions** - Session Management
- **Purpose**: Session metadata and activity classification
- **Rows**: 9 sessions
- **Status**: ⚠️ **SEVERE LIMITATION** - Single activity type

```sql
-- Critical limitations:
activity_type: 'traveling' (all 9 sessions)
player_name: 'R4ngethis2' (single player)
tags: ['swampHunter'] (single activity)

-- World variation:
world_id: 325(1), 509(2), 510(6) - mostly world 510

-- Always null:
end_time, activity_subtype, location_description
```

---

### Empty Tables (No Data Collected)

#### Combat & Damage Analysis (Critical Missing)
- **`damage_analytics`** (23 columns) - Combat damage tracking
- **`combat_achievement_tasks`** (23 columns) - Combat achievements
- **`boss_statistics`** (23 columns) - Boss encounter metrics
- **`enhanced_timer_tracking`** (18 columns) - Buff/debuff timers

#### Social & Multiplayer (High Value Missing)
- **`nearby_players`** (20 columns) - Player interactions
- **`friends_activity`** (22 columns) - Social network analysis  
- **`clan_detailed`** (28 columns) - Clan dynamics
- **`overhead_text_events`** (13 columns) - Chat/communication

#### Advanced Behavioral Analysis (Critical Missing)
- **`behavioral_analysis_comprehensive`** (45 columns) - Advanced behavioral metrics
- **`automation_risk_assessment`** (14 columns) - Bot detection analysis
- **`animation_completion_tracking`** (12 columns) - Animation patterns

#### Economic & Trading (Moderate Value)
- **`ground_item_events`** (18 columns) - Loot interactions
- **`shop_tracking`** (16 columns) - NPC trading
- **`item_pricing_timeseries`** (18 columns) - Economic data

#### UI & Interaction (High Value)
- **`widget_parsing_data`** (12 columns) - Interface interactions
- **`collision_analysis`** (16 columns) - Pathfinding analysis
- **`post_animation_events`** (13 columns) - Action sequences

#### System & Performance (Utility)
- **`performance_metrics`** (39 columns) - System monitoring
- **`security_analytics`** (18 columns) - Threat detection
- **`plugin_performance_monitoring`** (15 columns) - Data quality

---

## Data Quality Assessment

### ✅ Excellent Coverage
- **Core tick-based tables**: 100% coverage (8,659 ticks)
- **Temporal consistency**: Perfect tick sequencing
- **NPC environmental data**: Rich contextual information
- **Session metadata**: Complete session tracking

### ⚠️ Major Limitations  
- **Behavioral diversity**: Extremely limited action types (3 total)
- **Spatial coverage**: Confined to 35-tile area
- **Mouse behavior**: Completely static/AFK
- **Activity variety**: Single activity type
- **Player diversity**: Single player dataset

### ❌ Critical Gaps
- **Combat data**: 0% - No damage, healing, or combat mechanics
- **Social interaction**: 0% - No player-to-player behavior
- **Economic activity**: 0% - No trading or market behavior  
- **Advanced UI**: 0% - No complex interface interactions
- **Pathfinding**: 0% - No movement optimization data

---

## Schema Optimization Recommendations

### Immediate Actions
1. **Remove 27 empty tables** to reduce schema complexity
2. **Clean up 38 always-null columns** to improve query performance  
3. **Enable critical missing features**: automation_risk_assessment, behavioral_analysis_comprehensive
4. **Investigate inventory data gap** (ticks 7587-7853)

### Data Collection Priorities
1. **Combat scenarios** - Enable damage_analytics, boss_statistics
2. **Social interactions** - Activate nearby_players, friends_activity
3. **UI interactions** - Enable widget_parsing_data
4. **Movement diversity** - Collect data from multiple regions/activities

### Training Data Requirements
For meaningful AI training, collect data with:
- **10+ unique action types** (currently 3)
- **Multiple activity contexts** (currently 1)  
- **Dynamic mouse behavior** (currently static)
- **Varied spatial coverage** (currently 35 tiles)
- **Resource management scenarios** (currently static vitals)

---

## Technical Implementation Notes

### Database Performance
- **Index Strategy**: Primary keys on tick_id, session_id efficient
- **Storage Efficiency**: 66% unused tables waste storage
- **Query Performance**: Always-null columns add unnecessary overhead

### Data Pipeline Health
- **Collection Reliability**: 96.9-100% coverage on active tables
- **Plugin Integration**: Core features working correctly
- **Error Handling**: Robust null handling for missing features

### Scalability Considerations
- **Current Data Volume**: 245K rows manageable
- **Growth Projection**: Enable missing features will 10x data volume
- **Storage Planning**: Remove empty tables before scaling

---

## Conclusion

The OSRS AI database schema is well-designed and technically sound, with excellent data collection reliability on active features. However, the current SwampHunter dataset represents an **AFK/static behavior pattern** with severe diversity limitations that explain the training overfitting issues.

**Key Insight**: The database structure supports sophisticated behavioral modeling, but the collected data lacks the diversity needed for meaningful AI training. The solution is not technical (the preprocessing pipeline works correctly) but data collection - moving beyond the limited SwampHunter activity to more diverse, active gameplay scenarios.

### Next Steps
1. **Immediate**: Enable 4 critical missing features for behavioral analysis
2. **Short-term**: Collect data from combat, social, and economic activities  
3. **Long-term**: Optimize schema by removing unused tables and columns

The foundation is solid - the database just needs more diverse behavioral data to fulfill its AI training potential.