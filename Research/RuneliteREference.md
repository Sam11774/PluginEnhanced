# RuneLite plugin development documentation

Based on extensive research of the RuneLite ecosystem, I've created two extremely comprehensive markdown documentation files that go far beyond the existing documentation. These files compile information from the official RuneLite repository, API documentation, community resources, and plugin examples.

## MD File 1: Developer Reference

```markdown
# RuneLite Plugin Development - Complete Developer Reference

## Table of Contents
1. [Plugin Architecture and Lifecycle](#plugin-architecture-and-lifecycle)
2. [Dependency Injection and Google Guice](#dependency-injection-and-google-guice)
3. [Thread Management and Concurrency](#thread-management-and-concurrency)
4. [Overlay System](#overlay-system)
5. [Event System and Hooks](#event-system-and-hooks)
6. [Menu Manipulation](#menu-manipulation)
7. [Plugin Communication](#plugin-communication)
8. [Resource Management](#resource-management)
9. [Debugging and Profiling](#debugging-and-profiling)
10. [Configuration System](#configuration-system)
11. [Custom UI Components](#custom-ui-components)
12. [Utility Classes](#utility-classes)

## Plugin Architecture and Lifecycle

### Complete Plugin Lifecycle Flow

The RuneLite plugin system follows a structured lifecycle managed by the PluginManager:

```java
@PluginDescriptor(
    name = "Example Plugin",
    description = "Comprehensive example plugin",
    tags = {"example", "tutorial"},
    enabledByDefault = false,
    conflicts = "conflicting-plugin"
)
public class ExamplePlugin extends Plugin {
    
    @Override
    protected void startUp() throws Exception {
        // Plugin initialization
        // Register overlays, subscribe to events, setup resources
        log.info("Plugin starting up");
    }
    
    @Override
    protected void shutDown() throws Exception {
        // Cleanup code - CRITICAL for memory management
        // Unregister overlays, clean up resources, cancel timers
        log.info("Plugin shutting down");
    }
}
```

**Lifecycle Stages:**
1. **Instantiation**: Plugins instantiated by PluginManager using Guice
2. **Dependency Injection**: All @Inject fields populated
3. **startUp()**: Called when plugin enabled
4. **Active State**: Plugin responds to events
5. **shutDown()**: Called when plugin disabled
6. **Disposal**: Guice handles instance cleanup

## Dependency Injection and Google Guice

### Complete Injection Patterns

```java
public class AdvancedPlugin extends Plugin {
    // Field injection
    @Inject private Client client;
    @Inject private OverlayManager overlayManager;
    @Inject private ConfigManager configManager;
    @Inject private ClientThread clientThread;
    @Inject private EventBus eventBus;
    @Inject private ItemManager itemManager;
    @Inject private ChatMessageManager chatMessageManager;
    
    // Constructor injection (preferred for immutable dependencies)
    private final EventBus eventBus;
    
    @Inject
    public AdvancedPlugin(EventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    // Provider pattern for configuration
    @Provides
    ExampleConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(ExampleConfig.class);
    }
}
```

### All Available Injection Types

- **@Inject**: Marks fields/constructors for injection
- **@Singleton**: Ensures single instance
- **@Provides**: Methods that provide instances
- **@Named**: Named bindings for disambiguation

## Thread Management and Concurrency

### ClientThread Usage Patterns

```java
public class ThreadSafePlugin extends Plugin {
    @Inject private ClientThread clientThread;
    
    // Immediate execution if on client thread
    public void immediateExecution() {
        clientThread.invoke(() -> {
            // Returns boolean for retry logic
            Widget widget = client.getWidget(WidgetInfo.BANK_CONTAINER);
            if (widget == null) {
                return false; // Retry later
            }
            processWidget(widget);
            return true; // Complete
        });
    }
    
    // Delayed execution
    public void delayedExecution() {
        clientThread.invokeLater(() -> {
            // Executed on next client tick
            client.refreshChat();
        });
    }
    
    // End of tick execution
    public void tickEndExecution() {
        clientThread.invokeAtTickEnd(() -> {
            // Executed at end of current game tick
            performTickEndOperations();
        });
    }
}
```

### Thread Safety Rules

1. **ClientThread**: All game state access must occur here
2. **AWT Event Thread**: UI operations, Swing components
3. **Background threads**: HTTP requests, file I/O, heavy computations
4. Use `ConcurrentLinkedQueue` for thread-safe data structures
5. Never block the client thread

## Overlay System

### Complete Overlay Implementation

```java
public class AdvancedOverlay extends Overlay {
    private final Client client;
    private final Plugin plugin;
    
    @Inject
    private AdvancedOverlay(Client client, Plugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
        setMovable(true);
        setSnappable(true);
    }
    
    @Override
    public Dimension render(Graphics2D graphics) {
        // Set rendering hints for quality
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                 RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Dynamic content based on game state
        if (client.getGameState() != GameState.LOGGED_IN) {
            return null;
        }
        
        // Render content
        return new Dimension(width, height);
    }
}
```

### All Overlay Layers

- **ALWAYS_ON_TOP**: Above all game elements
- **ABOVE_WIDGETS**: Above widgets, under menus
- **UNDER_WIDGETS**: Under all interfaces (default)
- **ABOVE_SCENE**: Above 3D scene, under UI
- **MANUAL**: Manual layer control

### Overlay Positions

- **DYNAMIC**: Automatic positioning
- **TOP_LEFT**, **TOP_CENTER**, **TOP_RIGHT**
- **BOTTOM_LEFT**, **BOTTOM_CENTER**, **BOTTOM_RIGHT**
- **ABOVE_CHATBOX_RIGHT**: Special chat position
- **DETACHED**: Independent positioning

## Event System and Hooks

### Complete Event Handling

```java
public class EventHandlingPlugin extends Plugin {
    
    // Event priority control
    @Subscribe(priority = 100.0f) // Higher = first
    public void onHighPriorityGameTick(GameTick event) {
        // Executes before lower priority subscribers
    }
    
    // All major event types
    @Subscribe public void onGameTick(GameTick event) {} // ~0.6s intervals
    @Subscribe public void onClientTick(ClientTick event) {} // ~20ms intervals
    @Subscribe public void onGameStateChanged(GameStateChanged event) {}
    @Subscribe public void onVarbitChanged(VarbitChanged event) {}
    @Subscribe public void onStatChanged(StatChanged event) {}
    @Subscribe public void onAnimationChanged(AnimationChanged event) {}
    @Subscribe public void onHitsplatApplied(HitsplatApplied event) {}
    @Subscribe public void onNpcSpawned(NpcSpawned event) {}
    @Subscribe public void onNpcDespawned(NpcDespawned event) {}
    @Subscribe public void onGameObjectSpawned(GameObjectSpawned event) {}
    @Subscribe public void onItemContainerChanged(ItemContainerChanged event) {}
    @Subscribe public void onMenuEntryAdded(MenuEntryAdded event) {}
    @Subscribe public void onChatMessage(ChatMessage event) {}
    @Subscribe public void onWidgetLoaded(WidgetLoaded event) {}
    @Subscribe public void onScriptCallbackEvent(ScriptCallbackEvent event) {}
}
```

### Hook System

The Callbacks interface provides low-level hooks:

```java
public interface Callbacks {
    void post(Object event);
    void postDeferred(Object event);
    void tick();
    void tickEnd();
    void frame();
    void drawScene();
    void drawAboveOverheads();
    void keyPressed(KeyEvent keyEvent);
    void mousePressed(MouseEvent mouseEvent);
    boolean draw(Renderable renderable, boolean drawingUi);
}
```

## Menu Manipulation

### Complete Menu System

```java
@Inject private MenuManager menuManager;

@Subscribe
public void onMenuEntryAdded(MenuEntryAdded event) {
    // Add custom menu entries
    if (event.getOption().equals("Examine")) {
        MenuEntry customEntry = new MenuEntry();
        customEntry.setOption("Custom Action");
        customEntry.setTarget(event.getTarget());
        customEntry.setType(MenuAction.RUNELITE.getId());
        customEntry.setParam0(event.getActionParam0());
        customEntry.setParam1(event.getActionParam1());
        
        MenuEntry[] entries = client.getMenuEntries();
        entries = Arrays.copyOf(entries, entries.length + 1);
        entries[entries.length - 1] = customEntry;
        client.setMenuEntries(entries);
    }
}

// Menu priority system
@Subscribe
public void onMenuOpened(MenuOpened event) {
    MenuEntry[] entries = event.getMenuEntries();
    // Reorder entries for priority
    Arrays.sort(entries, menuComparator);
    event.setMenuEntries(entries);
}
```

## Plugin Communication

### Inter-Plugin Messaging

```java
// Define custom event
@Value
public class CustomPluginEvent {
    String data;
    long timestamp;
}

// Publish event
public void publishEvent() {
    eventBus.post(new CustomPluginEvent("data", System.currentTimeMillis()));
}

// Subscribe to custom events
@Subscribe
public void onCustomEvent(CustomPluginEvent event) {
    processCustomEvent(event.getData());
}
```

## Resource Management

### Memory Optimization

```java
public class OptimizedPlugin extends Plugin {
    // Use weak references for large data
    private final Map<Integer, WeakReference<LargeData>> cache = new ConcurrentHashMap<>();
    
    // Scheduled cleanup
    private ScheduledFuture<?> cleanupTask;
    
    @Override
    protected void startUp() {
        cleanupTask = executor.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }
    
    @Override
    protected void shutDown() {
        // Critical cleanup
        if (cleanupTask != null) {
            cleanupTask.cancel(true);
        }
        cache.clear();
        // Unregister all listeners
        eventBus.unregister(this);
    }
}
```

### Memory Limits
- RuneLite JVM: 512MB heap limit
- Plugin hub plugins share ~150MB
- Use weak references for large caches
- Clean up in shutDown()

## Debugging and Profiling

### Developer Mode Setup

```bash
# IntelliJ VM Options
-ea                    # Enable assertions
-Xmx1536M             # Increase heap for development
-Drunelite.pluginhub.version=1.0.0

# Program arguments
--debug               # Enable debug logging
--developer-mode      # Enable developer tools
```

### Logging Best Practices

```java
@Slf4j
public class DebugPlugin extends Plugin {
    @Override
    protected void startUp() {
        log.info("Plugin started");
        log.debug("Debug information"); // Only with --debug
        
        if (log.isTraceEnabled()) {
            log.trace("Detailed trace: {}", getDetailedState());
        }
    }
    
    // Conditional expensive logging
    if (log.isDebugEnabled()) {
        log.debug("Expensive operation result: {}", performExpensiveOperation());
    }
}
```

## Configuration System

### Complete Configuration Example

```java
@ConfigGroup("advancedexample")
public interface AdvancedConfig extends Config {
    
    @ConfigSection(
        name = "General Settings",
        description = "General plugin settings",
        position = 0
    )
    String generalSection = "general";
    
    @ConfigItem(
        keyName = "enableFeature",
        name = "Enable Feature",
        description = "Enables the main feature",
        section = generalSection,
        position = 1
    )
    default boolean enableFeature() {
        return true;
    }
    
    @Range(min = 1, max = 100)
    @ConfigItem(
        keyName = "threshold",
        name = "Threshold",
        description = "Value threshold (1-100)",
        section = generalSection,
        position = 2
    )
    default int threshold() {
        return 50;
    }
    
    @Alpha
    @ConfigItem(
        keyName = "overlayColor",
        name = "Overlay Color",
        description = "Color for overlay",
        section = generalSection,
        position = 3
    )
    default Color overlayColor() {
        return new Color(255, 0, 0, 128);
    }
    
    @ConfigItem(
        keyName = "hotkey",
        name = "Hotkey",
        description = "Hotkey to toggle feature",
        section = generalSection,
        position = 4
    )
    default Keybind hotkey() {
        return Keybind.NOT_SET;
    }
}
```

### Configuration Annotations

- **@ConfigGroup**: Groups configuration items
- **@ConfigSection**: Creates sections in settings
- **@ConfigItem**: Individual setting
- **@Range**: Numeric limits
- **@Alpha**: Enables alpha channel for colors
- **@Units**: Display units

## Custom UI Components

### Panel Component System

```java
public class CustomPanel extends OverlayPanel {
    private final PanelComponent panelComponent = new PanelComponent();
    
    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().clear();
        
        // Add title
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("Custom Panel")
            .color(Color.GREEN)
            .build());
        
        // Add lines
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Label:")
            .right("Value")
            .build());
        
        // Set layout
        panelComponent.setOrientation(ComponentOrientation.VERTICAL);
        panelComponent.setGap(new Point(5, 5));
        panelComponent.setBorder(new Rectangle(10, 10, 10, 10));
        
        return panelComponent.render(graphics);
    }
}
```

## Utility Classes

### ColorUtil

```java
// Color manipulation
Color withAlpha = ColorUtil.colorWithAlpha(color, 128);
String colorTag = ColorUtil.colorTag(Color.RED);
String wrapped = ColorUtil.wrapWithColorTag("text", Color.GREEN);
Color interpolated = ColorUtil.colorLerp(color1, color2, 0.5);
```

### Experience

```java
// Experience calculations
int xpForLevel = Experience.getXpForLevel(99);
int levelForXp = Experience.getLevelForXp(13034431);
double combatLevel = Experience.getCombatLevelPrecise(att, str, def, hp, magic, range, prayer);
```

### Text

```java
// Text processing
String cleaned = Text.removeTags(taggedString);
String jagexName = Text.toJagexName(displayName);
String sanitized = Text.sanitize(userInput);
```

## Best Practices and Anti-Patterns

### Best Practices
1. Always clean up in shutDown()
2. Use ClientThread for game state access
3. Minimize work in high-frequency events
4. Use appropriate event types
5. Handle null values from API

### Anti-Patterns to Avoid
```java
// BAD: Heavy computation in ClientTick
@Subscribe
public void onClientTick(ClientTick event) {
    performExpensiveCalculation(); // Runs 50 times/second!
}

// GOOD: Use GameTick or cache results
private Object cachedResult;
@Subscribe
public void onGameTick(GameTick event) {
    if (shouldRecalculate()) {
        cachedResult = performExpensiveCalculation();
    }
}

// BAD: Blocking operations in events
@Subscribe
public void onChatMessage(ChatMessage event) {
    String response = httpClient.get(url); // Blocks!
}

// GOOD: Async with callback
@Subscribe
public void onChatMessage(ChatMessage event) {
    executor.submit(() -> {
        String response = httpClient.get(url);
        clientThread.invokeLater(() -> processResponse(response));
    });
}
```
```

## MD File 2: Complete Data Capture Reference

```markdown
# RuneLite API - Complete Data Capture Reference

## Table of Contents
1. [World Data](#world-data)
2. [Player Detailed Data](#player-detailed-data)
3. [Combat Mechanics](#combat-mechanics)
4. [Skilling Data](#skilling-data)
5. [Inventory and Items](#inventory-and-items)
6. [Banking](#banking)
7. [Interfaces](#interfaces)
8. [NPC Data](#npc-data)
9. [Objects](#objects)
10. [Quests](#quests)
11. [Minigames](#minigames)
12. [Clans](#clans)
13. [Grand Exchange](#grand-exchange)
14. [Prayers](#prayers)
15. [Spellbooks](#spellbooks)
16. [Friends/Ignore](#friendsignore)

## World Data

### Complete Tile Data

```java
// Access tile data
Tile[][][] tiles = client.getScene().getTiles();
Tile tile = tiles[plane][x][y];

// Tile properties
GameObjectpublic[] gameObjects = tile.getGameObjects();
DecorativeObject decorativeObject = tile.getDecorativeObject();
WallObject wallObject = tile.getWallObject();
GroundObject groundObject = tile.getGroundObject();
ItemLayer itemLayer = tile.getItemLayer();

// Collision flags
CollisionData[] collisionMaps = client.getCollisionMaps();
int[][] flags = collisionMaps[plane].getFlags();

// Collision flag constants
BLOCK_MOVEMENT_NORTH = 0x1
BLOCK_MOVEMENT_EAST = 0x2
BLOCK_MOVEMENT_SOUTH = 0x4
BLOCK_MOVEMENT_WEST = 0x8
BLOCK_MOVEMENT_OBJECT = 0x100
BLOCK_MOVEMENT_FLOOR = 0x200000
BLOCK_MOVEMENT_FULL = 0x20000000
```

### Coordinate Systems

```java
// WorldPoint - Global coordinates
WorldPoint worldPoint = new WorldPoint(x, y, plane);
int regionId = worldPoint.getRegionID();
int regionX = x >> 6; // x / 64
int regionY = y >> 6; // y / 64

// LocalPoint - Scene coordinates (1/128th tile precision)
LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
int sceneX = localPoint.getSceneX(); // localPoint.getX() >> 7
int sceneY = localPoint.getSceneY(); // localPoint.getY() >> 7

// Scene coordinates - 0-103 tile grid
int SCENE_SIZE = 104;

// Region system
int REGION_SIZE = 64; // 64x64 tiles per region
int CHUNK_SIZE = 8;   // 8x8 tiles per chunk
```

### Instance Data

```java
// Instance detection
boolean isInstance = client.isInInstancedRegion();
int[][][] instanceTemplateChunks = client.getInstanceTemplateChunks();

// Process instance chunks
int templateChunk = instanceTemplateChunks[plane][chunkX][chunkY];
int rotation = (templateChunk >> 1) & 0x3;
int templateChunkY = ((templateChunk >> 3) & 0x7FF) * 8;
int templateChunkX = ((templateChunk >> 14) & 0x3FF) * 8;
int templateChunkPlane = (templateChunk >> 24) & 0x3;
```

### Pathfinding and Movement

```java
// Check if can travel in direction
WorldArea area = new WorldArea(worldPoint, width, height);
boolean canMove = area.canTravelInDirection(client, dx, dy);

// Line of sight
boolean hasLOS = area.hasLineOfSightTo(client, targetPoint);

// Movement costs calculated internally
// Access collision flags for custom pathfinding
```

### Environmental Effects

```java
// Graphics objects (spell effects, animations)
GraphicsObject[] graphicsObjects = client.getGraphicsObjects();

// Projectiles
Projectile[] projectiles = client.getProjectiles();
for (Projectile projectile : projectiles) {
    int id = projectile.getId();
    Actor target = projectile.getInteracting();
    int remainingCycles = projectile.getRemainingCycles();
}

// Sound effects
@Subscribe
public void onSoundEffectPlayed(SoundEffectPlayed event) {
    int soundId = event.getSoundId();
}

// Music tracks
@Subscribe
public void onAreaSoundEffectPlayed(AreaSoundEffectPlayed event) {
    int soundId = event.getSoundId();
}
```

### Map and Minimap Data

```java
// Camera system
int cameraX = client.getCameraX();
int cameraY = client.getCameraY();
int cameraZ = client.getCameraZ();
int cameraYaw = client.getCameraYaw();
int cameraPitch = client.getCameraPitch();

// Minimap
int mapAngle = client.getMapAngle();
Point minimapLocation = localPoint.getMinimapLocation();

// World map
WorldMapManager mapManager = client.getWorldMapManager();
Point worldMapPosition = client.getWorldMapPosition();
```

## Player Detailed Data

### Complete Player Stats

```java
Player localPlayer = client.getLocalPlayer();

// All 23 skills
for (Skill skill : Skill.values()) {
    int realLevel = client.getRealSkillLevel(skill);
    int boostedLevel = client.getBoostedSkillLevel(skill);
    int experience = client.getSkillExperience(skill);
}

// Combat and total levels
int combatLevel = localPlayer.getCombatLevel();
int totalLevel = client.getTotalLevel();
long totalXp = client.getOverallExperience();

// Experience calculations
int levelForXp = Experience.getLevelForXp(xp);
int xpForLevel = Experience.getXpForLevel(level);
double preciseCombat = Experience.getCombatLevelPrecise(att, str, def, hp, magic, range, prayer);
```

### Player Model and Appearance

```java
// Visual composition
PlayerComposition composition = localPlayer.getPlayerComposition();
int[] equipmentIds = composition.getEquipmentIds();
int[] colors = composition.getColors();

// Model data
Model model = localPlayer.getModel();
int modelHeight = localPlayer.getModelHeight();
int logicalHeight = localPlayer.getLogicalHeight();
Polygon[] polygons = localPlayer.getPolygons();
Shape convexHull = localPlayer.getConvexHull();
```

### Status Effects and Timers

```java
// VarPlayer values
int poisonState = client.getVarpValue(VarPlayer.IS_POISONED);
// 0 = not poisoned, 1-49 = poisoned, >=1000000 = venomed

int skullTimer = client.getVarpValue(VarPlayer.SKULL_TIMER);
int teleblockTimer = client.getVarpValue(VarPlayer.TELEBLOCK_TIMER);

// VarBit values
int staminaEffect = client.getVarbitValue(Varbits.STAMINA_EFFECT);
int antifireTimer = client.getVarbitValue(Varbits.ANTIFIRE);
int superAntifireTimer = client.getVarbitValue(Varbits.SUPER_ANTIFIRE);
int vengeanceActive = client.getVarbitValue(Varbits.VENGEANCE_ACTIVE);
int vengeanceCooldown = client.getVarbitValue(Varbits.VENGEANCE_COOLDOWN);
```

### Account Type and Restrictions

```java
// Account type detection
int accountType = client.getVarbitValue(Varbits.ACCOUNT_TYPE);
// 0=Normal, 1=Ironman, 2=HCIM, 3=UIM, 4=GIM

// Membership
EnumSet<WorldType> worldTypes = client.getWorldType();
boolean isMembers = worldTypes.contains(WorldType.MEMBERS);

// League/seasonal
boolean isLeague = worldTypes.contains(WorldType.LEAGUE);
boolean isSeasonal = worldTypes.contains(WorldType.SEASONAL);
```

### Player Interactions

```java
// Overhead elements
String overheadText = localPlayer.getOverheadText();
int overheadCycle = localPlayer.getOverheadCycle();
HeadIcon overheadIcon = localPlayer.getOverheadIcon();
int skullIcon = localPlayer.getSkullIcon();

// Social status
boolean isFriend = localPlayer.isFriend();
boolean isClanMember = localPlayer.isClanMember();
boolean isFriendsChatMember = localPlayer.isFriendsChatMember();
int team = localPlayer.getTeam();
```

## Combat Mechanics

### Damage Calculations

```java
// Hitsplat tracking
@Subscribe
public void onHitsplatApplied(HitsplatApplied event) {
    Actor actor = event.getActor();
    Hitsplat hitsplat = event.getHitsplat();
    
    int damage = hitsplat.getAmount();
    int hitsplatType = hitsplat.getHitsplatType();
    // Types: DAMAGE_ME, DAMAGE_OTHER, BLOCK_ME, etc.
}

// Health tracking
int healthRatio = actor.getHealthRatio(); // 0-30 typically
int healthScale = actor.getHealthScale(); // Max scale
double healthPercent = (double)healthRatio / healthScale;
```

### Attack Speed and Delays

```java
// Animation-based attack speed detection
@Subscribe
public void onAnimationChanged(AnimationChanged event) {
    int animationId = event.getActor().getAnimation();
    // Map animation IDs to attack speeds
    // Not directly exposed - requires manual tracking
}
```

### Combat Triangle

```java
// Protection prayers
boolean protectMelee = client.isPrayerActive(Prayer.PROTECT_FROM_MELEE);
boolean protectMagic = client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC);
boolean protectMissiles = client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES);

// Combat styles accessed through varbits
int attackStyle = client.getVarpValue(VarPlayer.ATTACK_STYLE);
```

### Special Attack

```java
// Special attack energy (0-100)
int specialAttack = client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT);

// Special attack activation requires widget interaction
```

### Death Mechanics

```java
// Death detection
@Subscribe
public void onActorDeath(ActorDeath event) {
    Actor actor = event.getActor();
    if (actor == client.getLocalPlayer()) {
        // Player died
    }
}

// Item retrieval services tracked via varbits
```

## Skilling Data

### All Skilling Action IDs

```java
// Woodcutting animations by axe type
WOODCUTTING_BRONZE = 879
WOODCUTTING_IRON = 877
WOODCUTTING_STEEL = 875
WOODCUTTING_BLACK = 873
WOODCUTTING_MITHRIL = 871
WOODCUTTING_ADAMANT = 869
WOODCUTTING_RUNE = 867
WOODCUTTING_DRAGON = 2846
WOODCUTTING_INFERNAL = 2117
WOODCUTTING_3A_AXE = 7264
WOODCUTTING_CRYSTAL = 8324

// Other skills
MINING_BRONZE_PICKAXE = 625
FISHING_OILY_ROD = 622
COOKING_FIRE = 897
FIREMAKING = 733
SMITHING_ANVIL = 898
HERBLORE_PESTLE_AND_MORTAR = 364
```

### Resource Timers

```java
// Track resource depletion
@Subscribe
public void onGameObjectDespawned(GameObjectDespawned event) {
    GameObject object = event.getGameObject();
    // Track when trees/rocks deplete
}

// Respawn timers (not directly exposed)
// Must track manually with timestamps
```

### Boost Timers and Decay

```java
// Boost detection
int realLevel = client.getRealSkillLevel(skill);
int boostedLevel = client.getBoostedSkillLevel(skill);
int boost = boostedLevel - realLevel;

// Boost decay happens every minute
// Decreases by 1 toward base level
```

### Farming System

```java
// Farming timers via varbits
// Each patch type has specific varbits for:
// - Growth stage (0-max stages)
// - Disease state
// - Compost type applied
// - Protection payment status

// Example: Herb patches
int herbPatchStage = client.getVarbitValue(Varbits.FARMING_4771);
```

### Hunter Mechanics

```java
// Trap states tracked through game objects
@Subscribe
public void onGameObjectSpawned(GameObjectSpawned event) {
    GameObject trap = event.getGameObject();
    // Check trap IDs for different states
    // Empty, set, caught, failed
}
```

### Construction

```java
// House data
boolean inHouse = client.getVarbitValue(Varbits.IN_HOUSE) == 1;
boolean isBuildingMode = client.getVarbitValue(Varbits.BUILDING_MODE) == 1;

// Hotspot data requires widget inspection
// Room layouts tracked via varbits
```

## Inventory and Items

### Complete Item Metadata

```java
ItemComposition comp = itemManager.getItemComposition(itemId);

// All properties
String name = comp.getName();
int storePrice = comp.getPrice();
int haPrice = Math.round(storePrice * 0.6f);
boolean stackable = comp.isStackable();
boolean tradeable = comp.isTradeable();
boolean members = comp.isMembers();
int weight = comp.getWeight();

// Note/placeholder handling
int noteId = comp.getNote();
int linkedNoteId = comp.getLinkedNoteId();
int placeholderId = comp.getPlaceholderId();
int placeholderTemplateId = comp.getPlaceholderTemplateId();

// Actions
String[] inventoryActions = comp.getInventoryActions();
String[] groundActions = comp.getGroundActions();
```

### Item Transformations

```java
// Degradation tracking
// Items change IDs when degrading
// Track through ItemContainerChanged events

// Charge tracking
// Either through quantity or separate IDs
// Examples: Zulrah scales, crystal charges

// Dose tracking for potions
// Different IDs per dose: POTION4, POTION3, etc.
```

### Equipment Requirements

```java
// Equipment stats via ItemStats
ItemStats stats = itemManager.getItemStats(itemId);
if (stats != null) {
    // Combat bonuses
    ItemEquipmentStats equipment = stats.getEquipment();
    int attackStab = equipment.getAstab();
    int attackSlash = equipment.getAslash();
    int attackCrush = equipment.getAcrush();
    int attackMagic = equipment.getAmagic();
    int attackRanged = equipment.getArange();
    
    // Defence bonuses
    int defenceStab = equipment.getDstab();
    int defenceSlash = equipment.getDslash();
    int defenceCrush = equipment.getDcrush();
    int defenceMagic = equipment.getDmagic();
    int defenceRanged = equipment.getDrange();
    
    // Other bonuses
    int strengthBonus = equipment.getStr();
    int rangedStrength = equipment.getRstr();
    int magicDamage = equipment.getMdmg();
    int prayerBonus = equipment.getPrayer();
}
```

## Banking

### Bank Organization

```java
ItemContainer bank = client.getItemContainer(InventoryID.BANK);
if (bank != null) {
    Item[] items = bank.getItems();
    
    // Calculate total value
    long totalValue = 0;
    for (Item item : items) {
        if (item != null && item.getId() > 0) {
            int price = itemManager.getItemPrice(item.getId());
            totalValue += (long)price * item.getQuantity();
        }
    }
}

// Bank tabs (accessed via widgets)
Widget bankTabs = client.getWidget(WidgetInfo.BANK_TAB_CONTAINER);
```

### Bank Tags System

```java
// Tags stored as comma-separated strings
// Managed through ConfigManager
// "tag1,tag2,tag3"

// Tag tabs with custom icons
// Search with tag:tagname prefix
```

### Placeholders

```java
// Placeholder items have different IDs
// Use ItemManager.canonicalize() to get base item
int baseItemId = itemManager.canonicalize(placeholderId);
```

### Seed Vault

```java
ItemContainer seedVault = client.getItemContainer(InventoryID.SEED_VAULT);
if (seedVault != null) {
    Item[] seeds = seedVault.getItems();
    // Process seed storage
}
```

## Interfaces

### All Interface IDs

```java
// Major interfaces
BANK = 12
INVENTORY = 149
EQUIPMENT = 387
PRAYER = 541
SPELLBOOK = 218
COMBAT_OPTIONS = 593
FRIENDS_LIST = 429
CLAN_CHAT = 7
WORLD_MAP = 595
QUEST_TAB = 399
GRAND_EXCHANGE = 465

// Hundreds more interface IDs available
```

### Widget Component Access

```java
// Access widgets
Widget widget = client.getWidget(groupId, childId);
Widget widget = client.getWidget(WidgetInfo.CONSTANT);

// Widget properties
String text = widget.getText();
int itemId = widget.getItemId();
int itemQuantity = widget.getItemQuantity();
boolean hidden = widget.isHidden();
Widget[] children = widget.getChildren();
Rectangle bounds = widget.getBounds();
```

### Dialog Systems

```java
// Dialog tracking
@Subscribe
public void onWidgetLoaded(WidgetLoaded event) {
    if (event.getGroupId() == InterfaceID.DIALOG_NPC) {
        // NPC dialog opened
    } else if (event.getGroupId() == InterfaceID.DIALOG_PLAYER) {
        // Player dialog opened
    }
}
```

### Shop Interfaces

```java
// Shop detection
@Subscribe
public void onWidgetLoaded(WidgetLoaded event) {
    if (event.getGroupId() == InterfaceID.SHOP) {
        // Shop opened
        Widget shopItems = client.getWidget(WidgetInfo.SHOP_ITEMS);
    }
}
```

## NPC Data

### Complete NPC Properties

```java
NPC npc = event.getNpc();
NPCComposition comp = npc.getComposition();

// Basic properties
int id = npc.getId();
String name = comp.getName();
int combatLevel = comp.getCombatLevel();
int size = comp.getSize(); // Tiles wide
String[] actions = comp.getActions(); // Menu options

// Visual properties
int[] modelIds = comp.getModels();
int widthScale = comp.getWidthScale();
int heightScale = comp.getHeightScale();

// Combat stats (limited access)
int[] stats = comp.getStats();
// Index 0-5: Attack, Defence, Strength, Hitpoints, Ranged, Magic
```

### NPC Transformations

```java
// NPCs can transform based on varbits
int[] configs = comp.getConfigs();
NPCComposition transformed = comp.transform();

// Track transformations
@Subscribe
public void onNpcChanged(NpcChanged event) {
    NPC npc = event.getNpc();
    // NPC transformed
}
```

### Boss Mechanics

```java
// Boss phases tracked through animations/graphics
@Subscribe
public void onAnimationChanged(AnimationChanged event) {
    if (event.getActor() instanceof NPC) {
        NPC npc = (NPC)event.getActor();
        // Track boss phase changes
    }
}

// Health tracking for phases
int healthRatio = npc.getHealthRatio();
int healthScale = npc.getHealthScale();
```

### Slayer Data

```java
// Slayer task tracking
int slayerPoints = client.getVarbitValue(Varbits.SLAYER_POINTS);
int taskStreak = client.getVarbitValue(Varbits.SLAYER_TASK_STREAK);

// Superior slayer monsters
boolean superiorsEnabled = client.getVarbitValue(Varbits.SUPERIOR_ENABLED) == 1;
```

## Objects

### Object Types and Properties

```java
// GameObject - most common
GameObject object = event.getGameObject();
int id = object.getId();
int sizeX = object.sizeX();
int sizeY = object.sizeY();
int orientation = object.getOrientation();
Shape clickbox = object.getClickbox();

// WallObject - walls/doors
WallObject wall = tile.getWallObject();
int orientationA = wall.getOrientationA();
int orientationB = wall.getOrientationB();

// GroundObject - floor decorations
GroundObject ground = tile.getGroundObject();

// DecorativeObject - environmental details
DecorativeObject decorative = tile.getDecorativeObject();
```

### Object Transformations

```java
// Objects transform based on varbits
// Example: Doors opening/closing
@Subscribe
public void onGameObjectDespawned(GameObjectDespawned event) {
    GameObject oldObject = event.getGameObject();
}

@Subscribe
public void onGameObjectSpawned(GameObjectSpawned event) {
    GameObject newObject = event.getGameObject();
}
```

### Interaction Requirements

```java
// Menu interactions
@Subscribe
public void onMenuEntryAdded(MenuEntryAdded event) {
    if (event.getType() == MenuAction.GAME_OBJECT_FIRST_OPTION) {
        int objectId = event.getIdentifier();
        String option = event.getOption();
        // Check requirements
    }
}
```

## Quests

### Quest Progress Tracking

```java
// Major quest varbits
QUEST_DRAGON_SLAYER_II = 6104
QUEST_MONKEY_MADNESS_II = 5027
QUEST_SONG_OF_THE_ELVES = 9016

// Track quest progress
int questProgress = client.getVarbitValue(Varbits.QUEST_DRAGON_SLAYER_II);
// 0 = not started
// Various values = in progress
// Final value = completed

@Subscribe
public void onVarbitChanged(VarbitChanged event) {
    if (event.getVarbitId() == Varbits.QUEST_DRAGON_SLAYER_II) {
        int progress = event.getValue();
        // Quest state changed
    }
}
```

### Quest Requirements

```java
// Check quest completion
boolean questComplete = client.getVarbitValue(questVarbit) >= completionValue;

// Skill requirements
boolean hasRequirement = client.getRealSkillLevel(skill) >= requiredLevel;

// Item requirements
ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
boolean hasItem = inventory != null && inventory.contains(itemId);
```

## Minigames

### Minigame Varbits

```java
// Barrows
BARROWS_KILLED_AHRIM = 457
BARROWS_KILLED_DHAROK = 458
BARROWS_KILLED_GUTHAN = 459
BARROWS_KILLED_KARIL = 460
BARROWS_KILLED_TORAG = 461
BARROWS_KILLED_VERAC = 462
BARROWS_REWARD_POTENTIAL = 463
BARROWS_NPCS_SLAIN = 464

// Wintertodt
WINTERTODT_WARMTH = 11434

// Combat Achievements
COMBAT_TASK_EASY = 12885
COMBAT_TASK_MEDIUM = 12886
COMBAT_TASK_HARD = 12887
COMBAT_TASK_ELITE = 12888
```

### Score Tracking

```java
// Track minigame scores
int barrowsReward = client.getVarbitValue(Varbits.BARROWS_REWARD_POTENTIAL);
int wintertodtWarmth = client.getVarbitValue(Varbits.WINTERTODT_WARMTH);

// Points and rewards
// Usually tracked through varbits or interfaces
```

## Clans

### Clan Data Access

```java
// Clan channels
ClanChannel clanChannel = client.getClanChannel();
ClanChannel guestChannel = client.getGuestClanChannel();

if (clanChannel != null) {
    String clanName = clanChannel.getName();
    List<ClanChannelMember> members = clanChannel.getMembers();
    
    for (ClanChannelMember member : members) {
        String name = member.getName();
        ClanRank rank = member.getRank();
        int world = member.getWorld();
    }
}

// Clan settings
ClanSettings settings = client.getClanSettings();
```

### Clan Ranks

```java
// ClanRank enum
GUEST, FRIEND, RECRUIT, CORPORAL, SERGEANT,
LIEUTENANT, CAPTAIN, GENERAL, OWNER

// Permission checking through ClanSettings
```

## Grand Exchange

### GE Offer Tracking

```java
// GE offers via events
@Subscribe
public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event) {
    GrandExchangeOffer offer = event.getOffer();
    
    int itemId = offer.getItemId();
    int quantity = offer.getTotalQuantity();
    int quantityTraded = offer.getQuantitySold();
    int price = offer.getPrice();
    GrandExchangeOfferState state = offer.getState();
    // States: EMPTY, BUYING, BOUGHT, SELLING, SOLD, CANCELLED
}
```

### Price Data

```java
// Current GE price
int gePrice = itemManager.getItemPrice(itemId);

// Wiki price with manipulation check
int wikiPrice = itemManager.getWikiPrice(itemPrice);

// Buy limits
ItemStats stats = itemManager.getItemStats(itemId);
int geLimit = stats != null ? stats.getGeLimit() : 0;
```

## Prayers

### Prayer Activation

```java
// All prayers available in Prayer enum
boolean prayerActive = client.isPrayerActive(Prayer.PIETY);

// Prayer points
int currentPrayer = client.getBoostedSkillLevel(Skill.PRAYER);
int maxPrayer = client.getRealSkillLevel(Skill.PRAYER);

// Prayer bonus from equipment
// Calculate from equipment stats
```

### Quick Prayers

```java
// Quick prayers accessed via widgets
Widget quickPrayers = client.getWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);

// Prayer book (normal/ruinous powers)
// Determined by quest completion
```

## Spellbooks

### Spellbook Detection

```java
// Current spellbook
int spellbook = client.getVarbitValue(Varbits.SPELLBOOK);
// 0 = Standard
// 1 = Ancients
// 2 = Lunars
// 3 = Arceuus

// Autocast spell
int autocastSpell = client.getVarbitValue(Varbits.AUTO_CAST_SPELL);
```

### Spell Requirements

```java
// Rune checking requires inventory inspection
ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
// Check for required runes/staves
```

## Friends/Ignore

### Friend List

```java
// Friend container
FriendContainer friends = client.getFriendContainer();
if (friends != null) {
    for (Friend friend : friends.getMembers()) {
        String name = friend.getName();
        int world = friend.getWorld(); // 0 if offline
    }
}

// Check if player is friend
boolean isFriend = client.isFriend(playerName, false);
```

### Ignore List

```java
// Ignore container
NameableContainer<Ignore> ignores = client.getIgnoreContainer();
if (ignores != null) {
    for (Ignore ignored : ignores.getMembers()) {
        String name = ignored.getName();
    }
}
```

### Private Messages

```java
@Subscribe
public void onChatMessage(ChatMessage event) {
    if (event.getType() == ChatMessageType.PRIVATECHAT ||
        event.getType() == ChatMessageType.PRIVATECHATOUT) {
        String sender = event.getName();
        String message = event.getMessage();
    }
}
```

## Complete Data Access Examples

### Comprehensive Player Data Capture

```java
public class PlayerDataCapture {
    @Inject private Client client;
    @Inject private ItemManager itemManager;
    
    public PlayerData captureCompletePlayerData() {
        Player player = client.getLocalPlayer();
        if (player == null) return null;
        
        PlayerData data = new PlayerData();
        
        // Basic info
        data.name = player.getName();
        data.combatLevel = player.getCombatLevel();
        data.totalLevel = client.getTotalLevel();
        data.worldLocation = player.getWorldLocation();
        
        // All skills
        for (Skill skill : Skill.values()) {
            data.skills.put(skill, new SkillData(
                client.getRealSkillLevel(skill),
                client.getBoostedSkillLevel(skill),
                client.getSkillExperience(skill)
            ));
        }
        
        // Equipment
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment != null) {
            data.equipment = Arrays.asList(equipment.getItems());
        }
        
        // Inventory
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        if (inventory != null) {
            data.inventory = Arrays.asList(inventory.getItems());
        }
        
        // Status effects
        data.poisoned = client.getVarpValue(VarPlayer.IS_POISONED) > 0;
        data.skulled = client.getVarpValue(VarPlayer.SKULL_TIMER) > 0;
        data.specialAttack = client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT);
        
        // Account type
        data.accountType = client.getVarbitValue(Varbits.ACCOUNT_TYPE);
        
        // Quest points
        data.questPoints = client.getVarpValue(VarPlayer.QUEST_POINTS);
        
        return data;
    }
}
```

### Complete NPC Data Extraction

```java
public class NpcDataExtractor {
    public NpcData extractNpcData(NPC npc) {
        if (npc == null) return null;
        
        NPCComposition comp = npc.getComposition();
        if (comp == null) return null;
        
        NpcData data = new NpcData();
        
        // Basic properties
        data.id = npc.getId();
        data.name = comp.getName();
        data.combatLevel = comp.getCombatLevel();
        data.size = comp.getSize();
        
        // Position
        data.worldLocation = npc.getWorldLocation();
        data.orientation = npc.getOrientation();
        
        // Health
        data.healthRatio = npc.getHealthRatio();
        data.healthScale = npc.getHealthScale();
        data.healthPercent = (double)data.healthRatio / data.healthScale;
        
        // Interaction
        data.interacting = npc.getInteracting();
        data.actions = comp.getActions();
        
        // Animation
        data.animation = npc.getAnimation();
        data.graphic = npc.getGraphic();
        
        // Combat stats (if available)
        data.stats = comp.getStats();
        
        return data;
    }
}
```

## API Limitations

### Data Not Available
- Exact NPC health values (only ratios)
- Drop tables (requires external data)
- Respawn timers (must track manually)
- Internal damage formulas
- Exact accuracy calculations
- Some equipment effects

### Workarounds
- Track state changes via events
- Use external databases for drop tables
- Calculate timers manually
- Reverse engineer formulas from observed data
```

These two comprehensive markdown files provide extensive documentation for RuneLite plugin development, covering all available APIs, data access methods, and practical implementation examples. The documentation goes far beyond what's typically available, providing developers with a complete reference for creating advanced RuneLite plugins.