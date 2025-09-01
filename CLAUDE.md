# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 📖 Documentation Structure

This documentation has been split into focused supplementary files for better maintainability. The main sections are organized as follows:

- **[TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)**: Complete troubleshooting procedures, error recovery, and diagnostic commands
- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)**: Plugin architecture, modular design patterns, and API integration
- **[DATABASE.md](docs/DATABASE.md)**: Database schema, operations, validation, and management procedures  
- **[DEVELOPMENT.md](docs/DEVELOPMENT.md)**: Development environment setup, workflows, and best practices

## Project Overview

RuneLiteAI is an advanced data collection plugin for RuneLite (OSRS client) designed to capture comprehensive gameplay data for AI/ML training. The plugin collects **3,100+ data points per game tick** and stores them in a PostgreSQL database for analysis and model training.

**MAJOR ARCHITECTURE REFACTORING COMPLETED (2025-08-31)**: The codebase has been successfully modularized using a delegation pattern, splitting large monolithic files into focused, maintainable modules while preserving 100% backward compatibility.

## 🏗️ Current Architecture (Post-Refactoring)

### Modular Structure Overview

After the successful refactoring completed on 2025-08-31, the project now features a clean modular architecture:

```
RuneliteAIPlugin (Main Orchestrator)
│
├── DataCollectionManager (333KB → Modularized with 8 collectors)
│   ├── PlayerDataCollector (Player state: vitals, location, stats)
│   ├── InputDataCollector (Input analytics & user interactions)  
│   ├── InterfaceDataCollector (Interface state & widget tracking)
│   ├── CombatDataCollector (Combat events & damage tracking)
│   ├── SocialDataCollector (Chat & social interactions)
│   ├── SystemMetricsCollector (System performance metrics)
│   ├── WorldDataCollector (World environment & objects)
│   └── DataCollectionOrchestrator (Coordination layer)
│
├── DatabaseManager (195KB → Modularized with 4 modules)
│   ├── DatabaseConnectionManager (Connection & schema management)
│   ├── DatabaseTableOperations (Batch processing & table operations)
│   ├── DatabaseUtilities (JSON conversion & ItemManager integration)
│   └── DatabasePerformanceMonitor (Performance metrics & monitoring)
│
└── DataStructures (70KB → Kept as monolith)
    └── Contains 45+ data classes (PlayerData, CombatData, etc.)
```

### Refactoring Success Metrics

- **✅ Build Success**: 100% compilation success with zero functionality changes
- **✅ Database Integrity**: All test records preserved with perfect quality scores  
- **✅ Performance**: No performance degradation (15-30ms processing times maintained)
- **✅ Backward Compatibility**: All existing APIs and usage patterns preserved
- **✅ Code Maintainability**: Large files split into focused, readable modules
- **✅ Delegation Pattern**: Clean architecture using delegation instead of complex inheritance

## Key Architecture Components

### Plugin Structure (RunelitePluginClone/)
- **Main Plugin**: `RuneliteAIPlugin.java` - Core plugin orchestrator
- **Data Collection**: `DataCollectionManager.java` - Core collection logic (modularized)
- **Data Collectors**: 8 focused collector modules for specific domains
- **Database**: `DatabaseManager.java` - Database operations (modularized) 
- **Database Modules**: 4 focused modules for connections, operations, utilities, and monitoring
- **Security**: `SecurityAnalyticsManager.java` - Automation pattern detection
- **Performance**: `PerformanceMonitor.java` - System health monitoring

### Database Schema
PostgreSQL database named `runelite_ai` with **34 production tables** tracking comprehensive gameplay data:

**Core Tables**: sessions, game_ticks, player_location, player_vitals, world_environment
**Equipment & Inventory**: player_equipment, player_inventory, player_prayers  
**Combat & Actions**: combat_data, hitsplats_data, animations_data, interactions_data
**Input Analytics**: click_context, key_presses, mouse_buttons, key_combinations, input_data
**Social & Environmental**: nearby_players_data, nearby_npcs_data, chat_messages, ground_items_data
**Banking & Economics**: bank_actions, player_spells
**Skills & Progression**: player_stats (all 23 OSRS skills)
**System & Analytics**: interface_data, game_objects_data, session_analysis, schema_version_tracking

## Quick Start Commands

### Build & Run
```bash
# Full build and start
.\Bat_Files\RUNELITE_AI_MASTER.bat
# Select option 7 (Build RuneLite) then option 8 (Start RuneLite)

# Fast build (skip checks)
mvn clean install -DskipTests -Dmaven.javadoc.skip=true -Dcheckstyle.skip=true -Dpmd.skip=true
```

### Database Operations
```bash
# Database connection (CRITICAL: Use inline PGPASSWORD to avoid 2-minute timeout)
PGPASSWORD=sam11773 "C:\Program Files\PostgreSQL\17\bin\psql" -U postgres -h localhost -p 5432 -d runelite_ai

# Quick health check
PGPASSWORD=sam11773 "C:\Program Files\PostgreSQL\17\bin\psql" -U postgres -h localhost -p 5432 -d runelite_ai -c "SELECT COUNT(*) FROM sessions; SELECT COUNT(*) FROM game_ticks;"
```

### Testing
```bash
# Run test suite
.\Bat_Files\RUN_ALL_TESTS_SIMPLE.bat

# Specific tests
mvn -pl runelite-client test -Dtest=QualityValidatorTest,DataCollectionManagerTest
```

## Current System Status (Updated 2025-08-31)

### Production Ready Features
- **✅ Data Collection**: 3,100+ data points per tick across 34 production tables
- **✅ Ultimate Input Analytics**: Complete click context, keyboard timing, mouse button tracking
- **✅ Combat Analytics**: Damage tracking, animation states, interaction events
- **✅ Environmental Data**: Nearby players, NPCs, object interactions
- **✅ Skills & XP Tracking**: All 23 OSRS skills with current/real levels and experience
- **✅ Performance**: 15ms average processing time with async database operations
- **✅ Data Quality**: Zero hardcoded values, complete friendly name resolution
- **✅ Modular Architecture**: Clean, maintainable code structure with focused modules

### Key Configuration
- **Java 17**: Required for compilation and runtime
- **Maven 3.9.9**: Build system configured at `C:\tools\apache-maven-3.9.9`
- **PostgreSQL 17**: Database with credentials postgres/sam11773
- **Database**: runelite_ai with 34 production tables

## Important Development Notes

### Following Conventions
When making changes to files:
- **Check existing patterns**: Look at neighboring files and existing components
- **Verify dependencies**: Never assume libraries are available - check package.json/pom.xml
- **Maintain security**: Never introduce code that exposes secrets or credentials
- **Use existing frameworks**: Follow established patterns for RuneLite API integration

### Code Quality Standards
- **IMPORTANT**: DO NOT ADD ***ANY*** COMMENTS unless asked
- Focus on "why" rather than "what" in documentation
- Use descriptive names, avoid abbreviations
- Single responsibility for methods and classes
- Graceful error handling with informative logging

### Common Tasks
- **Adding Data Points**: Update DataCollectionManager → database columns → QualityValidator → tests
- **Database Issues**: Check logs → verify PostgreSQL service → test connection → review schema
- **Performance**: Monitor overlay metrics → review logs → use async operations
- **Input Analytics**: Check debug logs with `[CLICK-CONTEXT]`, `[KEY-DEBUG]`, `[MOUSE-DEBUG]` prefixes

## Documentation Navigation

For detailed information on specific topics, refer to the supplementary documentation:

- **🚨 Having issues?** → [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)
- **🏗️ Understanding the code?** → [ARCHITECTURE.md](docs/ARCHITECTURE.md)  
- **🗄️ Database questions?** → [DATABASE.md](docs/DATABASE.md)
- **⚡ Development setup?** → [DEVELOPMENT.md](docs/DEVELOPMENT.md)

## Auto-Approval Rules

The project includes comprehensive auto-approval rules in `.claude/auto-approval-rules.json` that enable unrestricted command execution when Claude Code is in auto-approve mode. This covers:

- **All Bash Commands**: Complete unrestricted execution for development workflows
- **All File Operations**: Read, Write, Edit, MultiEdit for any file path  
- **All Database Operations**: PostgreSQL access with credentials for schema and data management
- **All Development Tools**: Maven, Java, Git, testing frameworks, batch file automation

When auto-approve mode is enabled, Claude Code will execute all commands without prompting, enabling seamless development workflow for RuneLiteAI project tasks.

---

*Last Updated: 2025-08-31 - Major Architecture Refactoring & Documentation Restructure*