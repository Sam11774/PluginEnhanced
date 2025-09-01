# RuneLiteAI Development Guide

This document provides comprehensive development procedures, workflows, and best practices for RuneLiteAI.

## Development Environment Setup

### Prerequisites & System Requirements

#### Operating System
- **Windows 10/11**: Primary development environment
- **64-bit architecture**: Required for PostgreSQL and Maven
- **Administrator privileges**: Needed for service management and installations

#### Java Development Environment
```bash
# Verify Java 17 installation
java -version
# Should output: openjdk version "17.0.x"

# Set JAVA_HOME if not configured
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.x
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.x"
```

#### Maven Configuration
```bash
# Verify Maven 3.9.9 installation at correct location
set MAVEN_HOME=C:\tools\apache-maven-3.9.9
setx MAVEN_HOME "C:\tools\apache-maven-3.9.9"
setx PATH "%PATH%;%MAVEN_HOME%\bin"

# Configure Maven options for optimal performance
set MAVEN_OPTS=-Xmx2g -XX:MaxPermSize=512m
setx MAVEN_OPTS "-Xmx2g -XX:MaxPermSize=512m"
```

### Development Commands

#### Build Commands
```bash
# Full build with AI plugin
.\Bat_Files\RUNELITE_AI_MASTER.bat
# Select option 7 to build RuneLite

# Manual Maven build (from RunelitePluginClone directory)
mvn clean install -DskipTests -Dmaven.javadoc.skip=true -Dcheckstyle.skip=true -Dpmd.skip=true

# Build specific modules in order
mvn -pl cache clean install -DskipTests
mvn -pl runelite-maven-plugin clean compile plugin:descriptor install -DskipTests
mvn -pl runelite-api clean install -DskipTests
mvn -pl runelite-client clean compile package -DskipTests
```

#### Run Commands
```bash
# Start RuneLite with plugin (after building)
.\Bat_Files\RUNELITE_AI_MASTER.bat
# Select option 8 to start RuneLite

# Manual start (from RunelitePluginClone directory)
mvn -pl runelite-client exec:java
```

#### Test Commands
```bash
# Run comprehensive test suite
.\Bat_Files\RUN_ALL_TESTS_SIMPLE.bat

# Run specific test class
mvn -pl runelite-client test -Dtest=QualityValidatorTest
mvn -pl runelite-client test -Dtest=TimerManagerTest
mvn -pl runelite-client test -Dtest=RuneliteAIPluginTest

# Run all tests
mvn -pl runelite-client test -Dtest=QualityValidatorTest,TimerManagerTest,RuneliteAIPluginTest
```

## 🔄 Development Workflow & Best Practices

### Git Workflow Strategy

#### Branch Management
```bash
# Main branch structure
master                    # Production-ready code, stable releases
├── development          # Integration branch for features
│   ├── feature/input-analytics
│   ├── feature/combat-tracking  
│   └── hotfix/database-timeout
└── release/v8.2         # Release preparation branch
```

#### Feature Development Process
```bash
# 1. Create feature branch from development
git checkout development
git pull origin development
git checkout -b feature/enhanced-analytics

# 2. Development cycle
git add .
git commit -m "📊 Add enhanced player analytics collection

Implemented comprehensive player state tracking including:
- Enhanced inventory change detection
- Real-time skill progression monitoring  
- Combat style analysis and optimization

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

#### Commit Message Standards
```
# Format: <emoji> <type>: <description>
# 
# Body: Detailed explanation
# 
# Footer: Claude Code attribution

🎯 feat: Ultimate Input Analytics implementation

Implemented comprehensive input tracking system:
- MenuOptionClicked event correlation with target classification
- Keyboard timing analytics with function key detection
- Mouse button tracking for all three buttons with press/release timing
- Camera rotation detection with middle mouse button analysis

Tables added: click_context, key_presses, mouse_buttons, key_combinations
Data points increased: 3,000+ to 3,100+ per tick
Performance impact: <1ms additional processing time

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

### Code Review & Quality Assurance

#### Pre-Commit Checklist
```bash
# 1. Code compilation
mvn clean compile -DskipTests

# 2. Run specific tests
mvn -pl runelite-client test -Dtest=QualityValidatorTest,DataCollectionManagerTest

# 3. Database validation
PGPASSWORD=sam11773 "C:\Program Files\PostgreSQL\17\bin\psql" -U postgres -h localhost -p 5432 -d runelite_ai -c "SELECT COUNT(*) FROM game_ticks WHERE quality_validation_score < 75;"

# 4. Performance validation
# Start RuneLite, collect 20+ ticks, verify overlay shows <20ms processing
```

### Common Development Tasks

#### Adding New Data Points
1. Update `DataCollectionManager.collectTickData()` method
2. Add corresponding database columns if needed
3. Update `QualityValidator` for new data validation
4. Add test coverage in `DataCollectionManagerTest`

#### Debugging Database Issues
1. Check logs in `Logs/database/database-operations-current.log`
2. Verify PostgreSQL is running: `pg_ctl status`
3. Test connection: `.\Bat_Files\check_database_content.bat`
4. Review schema: `Bat_Files\Database\SQL\RUNELITE_AI_PRODUCTION_SCHEMA.sql`

#### Performance Optimization
1. Monitor with `PerformanceMonitor` metrics
2. Check tick processing times in overlay display
3. Review logs in `Logs/performance/performance-metrics-current.log`
4. Use async operations for non-critical paths

### Debugging Specialized Systems

#### Debugging Ultimate Input Analytics
1. **Click Context Issues**: Check logs for `[CLICK-CONTEXT]` debug messages
2. **Keyboard Tracking**: Monitor `[KEY-DEBUG]` messages for key press/release events
3. **Mouse Button Problems**: Look for `[MOUSE-DEBUG]` messages in logs
4. **Movement Analytics**: Check `[INPUT-DEBUG]` messages for movement calculation issues
5. **Database Insertion**: Verify input analytics tables have matching tick counts

#### Debugging Noted Items Banking System
1. **Banking Action Detection**: Check logs for `[BANKING-DEBUG]` messages during withdraw/deposit
2. **MenuOptionClicked Correlation**: Verify banking actions trigger MenuOptionClicked events
3. **Inventory Detection**: Monitor inventory noted_items_count field for real-time counting
4. **ItemComposition API**: Ensure ItemComposition.getNote() returns valid IDs for noted items

### Performance Tuning

#### JVM Tuning for RuneLite
Create/edit `RunelitePluginClone/.mvn/jvm.config`:
```
-Xmx4g
-Xms2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
-Dfile.encoding=UTF-8
-Djava.awt.headless=false
```

#### Performance Monitoring
```bash
# Monitor processing time trends
findstr "Processing time:" Logs\performance\performance-metrics-current.log | tail -20

# Check for performance degradation
findstr "PERFORMANCE-WARNING" Logs\performance\performance-metrics-current.log

# Analyze database operation times
findstr "Database insert time:" Logs\database\database-operations-current.log | tail -10
```

### Testing Approach

#### Unit Test Framework
- Unit tests for individual components (validators, managers)
- Integration tests for database operations
- End-to-end tests for full data collection pipeline
- Mock RuneLite API objects for testing without game client

#### Test Scenarios Validation
The system has been validated against comprehensive gameplay testing:

**✅ Banking & Inventory**: withdraw items, deposit all, change bank tabs, noted items
**✅ Equipment Management**: equip items, unequip items, change weapons
**✅ Combat Systems**: melee combat, magic combat, special attacks
**✅ Prayer System**: change prayer, use quick prayer, restore prayer points
**✅ Magic & Spells**: teleports, high alch, spell casting, autocast
**✅ Movement & Navigation**: walk, run, teleport, click minimap
**✅ Ultimate Input Analytics**: comprehensive clicking, keyboard combinations, mouse button testing

### Code Quality Standards

#### Code Style Guidelines
- **Naming**: Descriptive names, avoid abbreviations
- **Methods**: Single responsibility, max 50 lines
- **Classes**: Focused purpose, proper separation of concerns
- **Error Handling**: Graceful degradation, informative logging
- **Performance**: Async operations, bounded collections, cleanup

#### Security Considerations
- **Data Privacy**: No personally identifiable information logged
- **Database Security**: Parameterized queries only
- **Resource Management**: Proper connection and memory cleanup
- **Input Validation**: All external inputs validated
- **Dependency Management**: Keep dependencies updated and secure

## Configuration Management

### Maven Settings
- Java 11+ required (project uses Java 17)
- Maven 3.9.9 configured at: `C:\tools\apache-maven-3.9.9`
- Skip checkstyle, PMD, and Javadoc for faster builds

### Database Configuration
- PostgreSQL 17 required
- Default credentials: postgres/sam11773
- Database name: runelite_ai
- Connection pooling with HikariCP
- Async operations to prevent game lag

### Plugin Configuration
```java
@ConfigGroup("runeliteai")
public interface RuneliteAIConfig extends Config {
    
    @ConfigItem(
        keyName = "enableDataCollection",
        name = "Enable Data Collection",
        description = "Enable comprehensive gameplay data collection"
    )
    default boolean enableDataCollection() {
        return true;
    }
    
    @ConfigItem(
        keyName = "showPerformanceOverlay",
        name = "Show Performance Overlay",
        description = "Display real-time performance metrics"
    )
    default boolean showPerformanceOverlay() {
        return true;
    }
}
```

## Auto-Approval Rules

The project includes comprehensive auto-approval rules in `.claude/auto-approval-rules.json` that enable unrestricted command execution when Claude Code is in auto-approve mode:

### Enabled Operations
- **All Bash Commands**: Complete unrestricted bash execution (`Bash(*)`)
- **All File Operations**: Read, Write, Edit, MultiEdit for any file path
- **All Search Operations**: Glob, Grep, LS for comprehensive code search
- **All Development Tools**: Maven, Java, Git, Database operations, Testing frameworks
- **All Project Paths**: RuneLiteAI directory structure, Windows system paths, PostgreSQL paths

### Database Operations
- **PostgreSQL Access**: Full psql command access with credentials (postgres/sam11773)
- **Schema Management**: Database creation, migration, backup, restore operations
- **Query Execution**: Unrestricted SQL query execution for data analysis

## Following Conventions

When making changes to files, first understand the file's code conventions:

- **NEVER assume that a given library is available** - Always check if this codebase already uses the given library
- **When you create a new component**, first look at existing components to see how they're written
- **When you edit code**, first look at the code's surrounding context to understand frameworks and libraries
- **Always follow security best practices** - Never introduce code that exposes or logs secrets

## Key Files to Review

- **Plugin Entry**: `RuneliteAIPlugin.java` - Main plugin class
- **Data Collection**: `DataCollectionManager.java` - Core collection logic (modularized)
- **Database**: `DatabaseManager.java` - Database operations (modularized)
- **Collectors**: Various `*Collector.java` files - Focused data collection modules
- **Config**: `RuneliteAIConfig.java` - User configuration options
- **Constants**: `RuneliteAIConstants.java` - Plugin constants and thresholds
- **Master Script**: `Bat_Files\RUNELITE_AI_MASTER.bat` - Main control interface

---

**Navigation**: [← Back to Main CLAUDE.md](../CLAUDE.md)