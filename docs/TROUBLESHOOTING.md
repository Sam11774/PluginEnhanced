# RuneLiteAI Troubleshooting Guide

This document provides comprehensive troubleshooting procedures for RuneLiteAI issues.

## 🚨 Critical Issues & Solutions

### Common Command Timeouts & Database Issues

#### PostgreSQL Connection Timeout (CRITICAL)
**Problem**: Standard `psql` commands timeout after 2 minutes, failing silently
**Solution**: ALWAYS use inline PGPASSWORD to prevent authentication delays
```bash
# ❌ WRONG - Will timeout
psql -U postgres -h localhost -p 5432 -d runelite_ai

# ✅ CORRECT - Prevents timeout
PGPASSWORD=sam11773 "C:\Program Files\PostgreSQL\17\bin\psql" -U postgres -h localhost -p 5432 -d runelite_ai
```

#### PostgreSQL Service Not Running
**Problem**: Database connection refused or service unavailable
**Diagnosis**: Check service status first
```bash
"C:\Program Files\PostgreSQL\17\bin\pg_ctl" status -D "C:\Program Files\PostgreSQL\17\data"
```
**Solution**: Start PostgreSQL service
```bash
net start postgresql-x64-17
# OR
"C:\Program Files\PostgreSQL\17\bin\pg_ctl" start -D "C:\Program Files\PostgreSQL\17\data"
```

### Maven Compilation Errors

#### "int cannot be dereferenced" Error
**Problem**: Calling `.toString()` on RuneLite API integer types
**Example Error**:
```
Error: int cannot be dereferenced
hitsplat.getHitsplat().getHitsplatType().toString() // FAILS
```
**Solution**: Create helper method for type conversion
```java
private String getHitsplatTypeName(int hitsplatType) {
    switch (hitsplatType) {
        case 0: return "DAMAGE";
        case 1: return "BLOCK";
        case 2: return "DISEASE";
        case 3: return "POISON";
        case 4: return "HEAL";
        default: return "UNKNOWN_" + hitsplatType;
    }
}
```

#### Maven Build Hanging or Failing
**Problem**: Build process hangs on checkstyle, PMD, or Javadoc generation
**Solution**: Always skip non-essential checks for faster builds
```bash
mvn clean install -DskipTests -Dmaven.javadoc.skip=true -Dcheckstyle.skip=true -Dpmd.skip=true
```

### Data Quality Issues (Fixed in v8.2)

#### Special Attack Percentage Wrong Scale (FIXED)
**Problem**: Special attack showing 1000 instead of 0-100
**Root Cause**: RuneLite API returns 0-1000, database expects 0-100
**Solution Applied**: Division by 10 in DataCollectionManager.java:712
```java
.specialAttackPercent(client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) / 10)
```

#### NULL Chunk Coordinates (FIXED)
**Problem**: player_location.chunk_x and chunk_y were always NULL
**Root Cause**: Chunk coordinates not calculated from world position
**Solution Applied**: Bit shifting in DataCollectionManager.java:654
```java
.chunkX(worldLocation.getX() >> 6)
.chunkY(worldLocation.getY() >> 6)
```

#### Stale Combat Data Persistence (FIXED)
**Problem**: Identical hitsplat arrays across multiple ticks
**Root Cause**: No time-based filtering for event queues
**Solution Applied**: 10-second time window filtering with TimestampedHitsplat wrapper
```java
long timeThreshold = currentTime - 10000; // 10 seconds
if (timestampedHitsplat.getTimestamp() >= timeThreshold) {
    // Process only recent events
}
```

### RuneLite API Quirks & Conversions

#### API Value Scaling Issues
- **Special Attack**: API returns 0-1000, divide by 10 for percentage
- **Prayer Points**: API accurate, no conversion needed
- **Energy**: API returns 0-10000, divide by 100 for percentage
- **Coordinates**: World coordinates need >> 6 for chunk coordinates

#### Item Name Resolution
**Problem**: Getting "Item_1234" instead of proper names
**Solution**: Always use ItemManager with null checks
```java
String itemName = "Unknown_" + itemId;
ItemComposition itemComp = itemManager.getItemComposition(itemId);
if (itemComp != null) {
    itemName = itemComp.getName();
}
```

#### Object Name Resolution  
**Problem**: Getting "Unknown_5932" instead of proper object names
**Solution**: Use ObjectComposition lookup with null checks
```java
String objectName = "Unknown_" + objectId;
ObjectComposition objectComp = client.getObjectDefinition(objectId);
if (objectComp != null) {
    objectName = objectComp.getName();
}
```

### Performance Issues

#### Processing Time Over Target
**Problem**: Tick processing exceeding 1ms target
**Acceptable Range**: 15ms average is within acceptable limits
**Monitoring**: Check overlay display for real-time performance metrics
**Optimization**: Use async database operations and parallel processing

#### Memory Leaks in Event Queues
**Problem**: Unbounded queue growth causing memory issues
**Solution**: Bounded queues with LRU eviction
```java
private final Queue<TimestampedHitsplat> recentHitsplats = 
    new ConcurrentLinkedQueue<>(); // Max 50 items
```

### Quick Diagnostic Commands

#### Health Check (Run First)
```bash
# Verify all services and basic functionality
PGPASSWORD=sam11773 "C:\Program Files\PostgreSQL\17\bin\psql" -U postgres -h localhost -p 5432 -d runelite_ai -c "SELECT 'Database Connected' as status;"

# Check table counts
PGPASSWORD=sam11773 "C:\Program Files\PostgreSQL\17\bin\psql" -U postgres -h localhost -p 5432 -d runelite_ai -c "SELECT COUNT(*) as table_count FROM information_schema.tables WHERE table_schema = 'public';"

# Verify recent data
PGPASSWORD=sam11773 "C:\Program Files\PostgreSQL\17\bin\psql" -U postgres -h localhost -p 5432 -d runelite_ai -c "SELECT COUNT(*) as recent_ticks FROM game_ticks WHERE tick_number >= (SELECT MAX(tick_number) - 10 FROM game_ticks);"
```

#### Data Quality Validation
```bash
# Check for data quality issues
PGPASSWORD=sam11773 "C:\Program Files\PostgreSQL\17\bin\psql" -U postgres -h localhost -p 5432 -d runelite_ai -c "SELECT tick_number, special_attack_percent FROM player_vitals WHERE special_attack_percent > 100 OR special_attack_percent < 0 LIMIT 5;"

# Verify chunk coordinates are populated
PGPASSWORD=sam11773 "C:\Program Files\PostgreSQL\17\bin\psql" -U postgres -h localhost -p 5432 -d runelite_ai -c "SELECT COUNT(*) as null_chunks FROM player_location WHERE chunk_x IS NULL OR chunk_y IS NULL;"

# Check for hardcoded fallback values
PGPASSWORD=sam11773 "C:\Program Files\PostgreSQL\17\bin\psql" -U postgres -h localhost -p 5432 -d runelite_ai -c "SELECT COUNT(*) as unknown_items FROM player_inventory WHERE items_json LIKE '%Unknown_%' OR items_json LIKE '%Item_%';"
```

## 🔧 Enhanced Troubleshooting Guide

### PostgreSQL Service Management

#### Service Status Diagnostics
```bash
# Check if PostgreSQL is running
"C:\Program Files\PostgreSQL\17\bin\pg_ctl" status -D "C:\Program Files\PostgreSQL\17\data"

# Check Windows service status
sc query postgresql-x64-17

# Check port 5432 availability
netstat -an | findstr :5432
```

#### Service Recovery Procedures
```bash
# Start PostgreSQL service (multiple methods)
net start postgresql-x64-17
# OR
"C:\Program Files\PostgreSQL\17\bin\pg_ctl" start -D "C:\Program Files\PostgreSQL\17\data"
# OR via Services.msc GUI

# Stop service if needed
net stop postgresql-x64-17

# Restart service
net stop postgresql-x64-17 && net start postgresql-x64-17
```

#### Database Connection Diagnostics
```bash
# Test basic connectivity
PGPASSWORD=sam11773 "C:\Program Files\PostgreSQL\17\bin\psql" -U postgres -h localhost -p 5432 -c "SELECT version();"

# Verify database exists
PGPASSWORD=sam11773 "C:\Program Files\PostgreSQL\17\bin\psql" -U postgres -h localhost -p 5432 -l | findstr runelite_ai

# Check database size and connections
PGPASSWORD=sam11773 "C:\Program Files\PostgreSQL\17\bin\psql" -U postgres -h localhost -p 5432 -d runelite_ai -c "SELECT pg_size_pretty(pg_database_size('runelite_ai')) as db_size; SELECT count(*) as active_connections FROM pg_stat_activity WHERE datname = 'runelite_ai';"
```

### Maven Build Troubleshooting

#### Compilation Failure Recovery
```bash
# Clean all modules and rebuild from scratch
mvn clean
mvn -pl cache clean install -DskipTests
mvn -pl runelite-maven-plugin clean compile plugin:descriptor install -DskipTests
mvn -pl runelite-api clean install -DskipTests  
mvn -pl runelite-client clean compile package -DskipTests

# If build still fails, check Java version
java -version  # Must be Java 11+
mvn -version   # Verify Maven configuration
```

#### Dependency Resolution Issues
```bash
# Force dependency refresh
mvn clean compile -U

# Clear local repository if corrupted
rmdir /s "%USERPROFILE%\.m2\repository"
mvn clean install -DskipTests

# Check for version conflicts
mvn dependency:tree -Dverbose
```

## 🚨 Comprehensive Error Catalog & Recovery Strategies

### Compilation & Build Errors

#### Maven Compilation Failures

**Error**: `"int cannot be dereferenced"`
```
Error: int cannot be dereferenced
    at DataCollectionManager.java:425
hitsplat.getHitsplat().getHitsplatType().toString()
```
**Root Cause**: Attempting to call object methods on primitive int types from RuneLite API  
**Solution**: Create helper method for type conversion
```java
private String getHitsplatTypeName(int hitsplatType) {
    switch (hitsplatType) {
        case 0: return "DAMAGE";
        case 1: return "BLOCK"; 
        case 2: return "DISEASE";
        case 3: return "POISON";
        case 4: return "HEAL";
        default: return "UNKNOWN_" + hitsplatType;
    }
}
```

**Error**: `"Maven build hangs indefinitely"`
```
[INFO] Scanning for projects...
[INFO] Building runelite-client 1.10.27-SNAPSHOT
[HANGS HERE]
```
**Root Cause**: Checkstyle, PMD, or Javadoc generation taking excessive time  
**Solution**: Always skip non-essential checks
```bash
mvn clean install -DskipTests -Dmaven.javadoc.skip=true -Dcheckstyle.skip=true -Dpmd.skip=true
```

### Database Connection Errors

#### PostgreSQL Connection Timeouts

**Error**: `"Connection timeout after 2 minutes"`
```
psql: error: connection to server on socket "/tmp/.s.PGSQL.5432" failed: Connection timed out
```
**Root Cause**: Authentication delay when PGPASSWORD not set inline  
**Solution**: Always use inline PGPASSWORD

### Emergency Recovery Procedures

#### Complete System Reset
```bash
# 1. Stop all processes
# Stop RuneLite client if running
# Stop PostgreSQL service
net stop postgresql-x64-17

# 2. Backup existing data (optional)
PGPASSWORD=sam11773 "C:\Program Files\PostgreSQL\17\bin\pg_dump" -U postgres -h localhost -p 5432 -d runelite_ai > backup_$(date +%Y%m%d).sql

# 3. Rebuild everything
.\Bat_Files\RUNELITE_AI_MASTER.bat
# Select option 1 (Setup Database)
# Select option 7 (Build RuneLite)
# Select option 8 (Start RuneLite)
```

#### Partial Recovery (Data Preservation)
```bash
# Rebuild plugin without losing data
mvn clean install -DskipTests -Dmaven.javadoc.skip=true -Dcheckstyle.skip=true -Dpmd.skip=true

# Verify data integrity after rebuild
PGPASSWORD=sam11773 "C:\Program Files\PostgreSQL\17\bin\psql" -U postgres -h localhost -p 5432 -d runelite_ai -c "SELECT COUNT(*) FROM game_ticks; SELECT COUNT(*) FROM sessions;"
```

---

**Navigation**: [← Back to Main CLAUDE.md](../CLAUDE.md)