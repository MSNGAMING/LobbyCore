# Project Plan: Modular Paper/Purpur Lobby Plugin (1.21.4 - 1.26.x)

## 1. Project Overview & Architecture
* **Target Engine**: Paper / Purpur 1.21.4+ (Cross-version compatible up to 1.26.3 using standard modern Paper/Adventure APIs, avoiding deprecated NMS).
* **Language & Runtime**: Java 21 (Corretto / OpenJDK).
* **Build System**: Maven with Lombok, PlaceholderAPI soft-dependency, and Maven Shade Plugin.
* **Text & Styling**: Kyori Adventure MiniMessage with legacy color code and PlaceholderAPI (%...%) interoperability.
* **Concurrency**: Asynchronous tasks for disk I/O, YAML persistence, Tablist, and Scoreboard buffer updates.

---

## 2. Complete Project Directory Structure

```
Lobby/
├── pom.xml
├── plan.md
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── voltic/
        │           └── lobby/
        │               ├── LobbyPlugin.java
        │               ├── chat/
        │               │   ├── ChatManager.java
        │               │   └── ChatListener.java
        │               ├── command/
        │               │   ├── HologramCommand.java
        │               │   ├── LobbyAdminCommand.java
        │               │   ├── LobbyCommand.java
        │               │   ├── NpcCommand.java
        │               │   └── SetLobbyCommand.java
        │               ├── config/
        │               │   ├── ConfigFile.java
        │               │   └── ConfigManager.java
        │               ├── gui/
        │               │   ├── AdminConfigGui.java
        │               │   ├── AdminGuiHolder.java
        │               │   ├── GuiListener.java
        │               │   └── anvil/
        │               │       ├── AnvilInputGui.java
        │               │       └── AnvilListener.java
        │               ├── hologram/
        │               │   ├── Hologram.java
        │               │   └── HologramManager.java
        │               ├── hotbar/
        │               │   ├── HotbarListener.java
        │               │   └── HotbarManager.java
        │               ├── listener/
        │               │   ├── PlayerConnectionListener.java
        │               │   ├── ServerListPingListener.java
        │               │   └── WorldProtectionListener.java
        │               ├── lobby/
        │               │   ├── LobbyData.java
        │               │   └── LobbyManager.java
        │               ├── npc/
        │               │   ├── NpcData.java
        │               │   ├── NpcInteractListener.java
        │               │   └── NpcManager.java
        │               ├── scoreboard/
        │               │   ├── ScoreboardBoard.java
        │               │   └── ScoreboardManager.java
        │               ├── tab/
        │               │   └── TabManager.java
        │               └── util/
        │                   ├── ItemBuilder.java
        │                   ├── LocationUtil.java
        │                   └── TextUtil.java
        └── resources/
            ├── plugin.yml
            ├── config.yml
            ├── holograms.yml
            ├── npcs.yml
            ├── chat.yml
            ├── tab.yml
            ├── scoreboard.yml
            ├── motd.yml
            └── lobbies.yml
```

---

## 3. Module Breakdown & Responsibilities

| Module | Core Classes | Purpose & Functionality |
| :--- | :--- | :--- |
| **Core & Main** | `LobbyPlugin` | Thread-safe singleton lifecycle management, dependency initialization, safe teardown. |
| **Config System** | `ConfigManager`, `ConfigFile` | Multi-file YAML management, async saving, non-blocking reload, defaults preservation. |
| **Utilities** | `TextUtil`, `LocationUtil`, `ItemBuilder` | MiniMessage styling, PlaceholderAPI hook, string/location serialization, fluent PDC item builder. |
| **Multi-Lobby** | `LobbyManager`, `LobbyData`, Commands | Multi-world lobby balancing, fallback spawns, `/lobby`, `/setlobby`. |
| **Lobby Hotbar** | `HotbarManager`, `HotbarListener` | On-join interactive items (Selector Compass, Player Visibility Toggle Dye, Admin Control Star). |
| **Protection** | `WorldProtectionListener` | Per-world game rules (PvP, block break/place, weather, time, hunger, void-fall rescue). |
| **Chat System** | `ChatManager`, `ChatListener` | Paper `AsyncChatEvent` MiniMessage formatting, regex anti-swear masking/blocking, anti-spam delay. |
| **Holograms** | `HologramManager`, `Hologram` | Modern Paper 1.21+ `TextDisplay` entities with billboards and PDC tags. Zero NMS. |
| **NPCs** | `NpcManager`, `NpcData`, `NpcInteractListener` | Paper 1.21+ `Interaction` click hitboxes + visuals, actions (`COMMAND`, `TELEPORT`, `GUI`). |
| **Tab & Scoreboard** | `TabManager`, `ScoreboardManager`, `ScoreboardBoard` | Flicker-free Team/Objective buffer scoreboard and async Tablist header/footer updates. |
| **Server MOTD** | `ServerListPingListener` | Modern Paper `PaperServerListPingEvent` for MiniMessage MOTD, player hover samples & counts. |
| **In-game Admin GUI** | `AdminConfigGui`, `AdminGuiHolder`, `GuiListener` | Interactive 54-slot GUI for admins to toggle rules, MOTD, chat, and trigger live config hot-reloads. |
| **Anvil GUI Editor** | `AnvilInputGui`, `AnvilListener` | Virtual Anvil UI allowing live in-game text editing (MOTD lines, Chat format, Scoreboard title). |
