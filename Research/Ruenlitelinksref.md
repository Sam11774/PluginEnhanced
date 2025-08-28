RuneLite Development Resources & API Reference Links
Official RuneLite Resources
Main Sites

RuneLite Homepage: https://runelite.net/
Plugin Hub: https://runelite.net/plugin-hub/
RuneLite Blog (Updates): https://runelite.net/blog/

GitHub Repositories

Main Repository: https://github.com/runelite/runelite
Example Plugin: https://github.com/runelite/example-plugin
Plugin Hub Repository: https://github.com/runelite/plugin-hub

API Documentation

RuneLite API Javadocs: https://static.runelite.net/api/runelite-api/
RuneLite Client Javadocs: https://static.runelite.net/api/runelite-client/
Latest API Docs: https://static.runelite.net/api/

Wiki & Guides

Developer Guide: https://github.com/runelite/runelite/wiki/Developer-Guide
Creating Plugin Config Panels: https://github.com/runelite/runelite/wiki/Creating-plugin-config-panels
Overlay Tutorial: https://github.com/runelite/runelite/wiki/Overlay
Plugin Hub Submission: https://github.com/runelite/runelite/wiki/Building-with-Maven

API Endpoints & External Services
OSRS Price APIs
# Real-time prices (RuneScape Wiki)
https://prices.runescape.wiki/api/v1/osrs/latest
https://prices.runescape.wiki/api/v1/osrs/5m
https://prices.runescape.wiki/api/v1/osrs/1h
https://prices.runescape.wiki/api/v1/osrs/timeseries?id={itemId}&timestep={5m|1h|6h|24h}
https://prices.runescape.wiki/api/v1/osrs/mapping

# Volume data
https://prices.runescape.wiki/api/v1/osrs/volumes
Hiscores Endpoints
# Main game
https://secure.runescape.com/m=hiscore_oldschool/index_lite.ws?player={username}

# Ironman modes
https://secure.runescape.com/m=hiscore_oldschool_ironman/index_lite.ws?player={username}
https://secure.runescape.com/m=hiscore_oldschool_hardcore_ironman/index_lite.ws?player={username}
https://secure.runescape.com/m=hiscore_oldschool_ultimate/index_lite.ws?player={username}
https://secure.runescape.com/m=hiscore_oldschool_tournament/index_lite.ws?player={username}

# Seasonal/Leagues
https://secure.runescape.com/m=hiscore_oldschool_seasonal/index_lite.ws?player={username}
https://secure.runescape.com/m=hiscore_oldschool_fresh_start/index_lite.ws?player={username}
OSRS Wiki API
https://oldschool.runescape.wiki/api.php
https://oldschool.runescape.wiki/api.php?action=query&prop=revisions&rvprop=content
Key Source Files (GitHub)
Core API Classes

Client.java: https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/Client.java
Scene.java: https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/Scene.java
ItemComposition.java: https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/ItemComposition.java
NPCComposition.java: https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/NPCComposition.java
Varbits.java: https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/Varbits.java
AnimationID.java: https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/AnimationID.java

Event System

Events Directory: https://github.com/runelite/runelite/tree/master/runelite-api/src/main/java/net/runelite/api/events
EventBus.java: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/eventbus/EventBus.java

Plugin Infrastructure

PluginManager.java: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/PluginManager.java
ConfigManager.java: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/config/ConfigManager.java
ClientThread.java: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/callback/ClientThread.java
Overlay.java: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/ui/overlay/Overlay.java

Managers

ItemManager.java: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/game/ItemManager.java
ChatMessageManager.java: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/chat/ChatMessageManager.java
MenuManager.java: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/menus/MenuManager.java

Widget System

WidgetInfo.java: https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/widgets/WidgetInfo.java
WidgetID.java: https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/widgets/WidgetID.java
Widget.java: https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/widgets/Widget.java

Popular Plugin Examples
Official Plugins

Loot Tracker: https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/loottracker
XP Tracker: https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/xptracker
GPU Plugin: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/gpu/GpuPlugin.java
Bank Tags: https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/banktags

Community Plugins

Quest Helper: https://github.com/Zoinkwiz/quest-helper
Socket Plugin: https://github.com/capslock13/socket-runelite
Bot Detector: https://github.com/Bot-detector/bot-detector

Community Resources
Tutorials & Guides

OSRSBox Plugin Tutorials: https://www.osrsbox.com/blog/tags/RuneLite/
Writing RuneLite Plugins (4-part series): https://www.osrsbox.com/blog/2019/01/17/writing-runelite-plugins-part-1-building/
Sly Automation Guide: https://www.slyautomation.com/blog/creating-your-first-runelite-plugin/

Code Examples

Tabnine Code Examples: https://www.tabnine.com/code/java/methods/net.runelite.api.Client
Program Creek Examples: https://www.programcreek.com/java-api-examples/?api=net.runelite.api

Development Tools
Build Dependencies
xml<!-- Maven Repository -->
<repository>
    <id>runelite</id>
    <url>https://repo.runelite.net</url>
</repository>

<!-- Current Version (as of 2025) -->
<dependency>
    <groupId>net.runelite</groupId>
    <artifactId>client</artifactId>
    <version>latest.release</version>
</dependency>
JitPack Alternative
https://jitpack.io/p/runelite/runelite
Important Constants & IDs
Common Interface IDs
javaBANK_GROUP_ID = 12
INVENTORY_GROUP_ID = 149
EQUIPMENT_GROUP_ID = 387
PRAYER_GROUP_ID = 541
SPELLBOOK_GROUP_ID = 218
COMBAT_OPTIONS_GROUP_ID = 593
FRIENDS_LIST_GROUP_ID = 429
CLAN_CHAT_GROUP_ID = 7
WORLD_MAP_GROUP_ID = 595
QUEST_TAB_GROUP_ID = 399
GRAND_EXCHANGE_GROUP_ID = 465
Common Varbits
javaACCOUNT_TYPE = 1777
SPELLBOOK = 4070
QUEST_POINTS = 3184 (VarPlayer)
SPECIAL_ATTACK_PERCENT = 301 (VarPlayer)
STAMINA_EFFECT = 25
VENGEANCE_COOLDOWN = 2451
Performance & Troubleshooting
Performance Resources

GPU FAQ: https://github.com/runelite/runelite/wiki/GPU-FAQ
Troubleshooting Guide: https://github.com/runelite/runelite/wiki/Troubleshooting-problems-with-the-client
Performance Blog Post: https://runelite.net/blog/show/2022-09-04-recent-performance-regressions-and-tombs-of-amascut/

Memory & JVM Settings
bash# Development VM Options
-Xmx512m                    # Max heap (production)
-Xmx1536m                   # Max heap (development)
-Drunelite.pluginhub.version=1.0.0
--debug                     # Enable debug logging
--developer-mode            # Enable developer tools
Latest Updates (2024-2025)
Varlamore Content

1.10.25 Release Notes: https://runelite.net/blog/show/2024-03-21-1.10.25-Release/
Fortis Colosseum Wiki: https://oldschool.runescape.wiki/w/Fortis_Colosseum

UI Updates

FlatLaf Integration: https://runelite.net/blog/show/2024-02-24-1.10.23-Release/
Extended Draw Distance: https://runelite.net/blog/show/2021-02-13-1.7.0-Release/

File System Locations
Configuration Directories
# Windows
%USERPROFILE%\\.runelite\\

# macOS
~/Library/Application Support/RuneLite/

# Linux
~/.runelite/

# Directory Structure
.runelite/
├── settings.properties
├── profiles/
├── logs/
├── screenshots/
├── cache/
└── jagexcache/
Contact & Support

RuneLite Discord: https://discord.gg/runelite
Issue Tracker: https://github.com/runelite/runelite/issues
Plugin Hub Submissions: https://github.com/runelite/plugin-hub/pulls


Note: URLs are current as of 2025. The RuneLite project is actively maintained, so check the official repository for the most recent updates and API changes.