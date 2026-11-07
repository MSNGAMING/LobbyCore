# LobbyCore

![Java](https://img.shields.io/badge/Java-21%2B-orange?style=flat-square&logo=openjdk)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur%20%7C%20Folia-blue?style=flat-square)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x%20--%2026.x-green?style=flat-square)
![Build](https://img.shields.io/badge/Build-Maven-C71A36?style=flat-square&logo=apachemaven)

**LobbyCore** is a next-generation, ultra-high performance modular lobby management plugin designed specifically for modern **Paper**, **Purpur**, and **Folia** servers running Minecraft 1.21.x through 26.x.

---

## ⚡ Performance & Architectural Highlights

* **Thread-Safe & Folia Region Compliant**:
  * Offloads asynchronous chunk callbacks onto Paper/Folia's `RegionScheduler`.
  * Executes console commands on `GlobalRegionScheduler` and player interactions on `player.getScheduler()`.
  * Eliminates `ConcurrentModificationException` during YAML file saving with thread-safe in-memory string snapshots.
* **Network Packet Delta Caching**:
  * **Scoreboard Delta Caching**: Tracks previous line states and only dispatches packets across the network when text actually changes, reducing scoreboard bandwidth by up to 90%.
  * **Native Score Numbers**: Employs modern Paper `NumberFormat.blank()` to cleanly hide red scoreboard numbers.
  * **Tablist Delta Caching**: Avoids sending redundant `ClientboundTabListPacket`s when header and footer contents are unchanged.
* **Zero-Allocation Fast-Path Listeners**:
  * `PlayerMoveEvent` features fast-path returns for non-vertical movements and coordinates above the void threshold.
  * $O(1)$ world protection checks using cached sets, skipping repeated YAML tree traversals and string allocations.
  * Zero-leak memory management with lifecycle tracking cleanups upon player disconnection.
* **Transient Entity Lifecycle**:
  * Holograms (`TextDisplay`) and NPCs (`Interaction` hitboxes) are non-persistent (`setPersistent(false)`), preventing duplicate ghost entity stacking in chunk region files across server restarts.
  * Dynamic `ChunkLoadEvent` management instantiates entities cleanly as chunks load.
* **Anti-Swear Single-Pass Filtering**:
  * Compiles blocked words into a single alternation regex pattern `\b(?:\Qword1\E|\Qword2\E)\b`, filtering messages in $O(N)$ single-pass time.
  * Synchronizes filtered messages with downstream plugins (Discord bridges, moderation logs).
* **Pre-Rendered Server Ping (Anti-Ping-Flood)**:
  * MOTD components, max players, version text, and `ListedPlayerInfo` hover samples are pre-compiled at configuration load time.

---

## 🛠️ Feature Overview

1. **Multi-Lobby Balancing**:
   * Automatic player balancing across multiple lobby worlds.
   * Fallback spawns and permission-restricted VIP lobbies.
2. **Interactive Lobby Hotbar**:
   * Game Selector Compass (Slot 0).
   * Player Visibility Toggle Dye (Slot 4 - Green/Gray with instant show/hide).
   * Administrative Control Star (Slot 8).
3. **Modern Display Holograms**:
   * Built on Paper `TextDisplay` entities with billboards, background opacity, and Adventure MiniMessage styling.
   * Zero NMS or fragile packet hacks.
4. **Crisp Interaction NPCs**:
   * Custom entity models paired with modern Paper `Interaction` click hitboxes.
   * Configurable actions: `COMMAND`, `TELEPORT`, `MESSAGE`, and `GUI`.
5. **In-Game Admin GUI & Virtual Anvil**:
   * 54-slot interactive menu (`/lobbyadmin`) for toggling rules live.
   * Virtual Anvil editor powered by modern Paper `MenuType.ANVIL` and `AnvilView` for live configuration text editing without restarts.
6. **World Rules & Protection**:
   * PvP combat toggling, building protection (break/place), hunger depletion lock, weather/time lock, and void fall rescue.

---

## 📋 Commands & Permissions

| Command | Aliases | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/lobby [id]` | `/hub`, `/l`, `/spawn` | Teleport to a balanced or specified lobby | None |
| `/setlobby [id]` | `/sethub` | Set spawn location for a lobby | `lobby.admin.setlobby` |
| `/hologram <create\|delete\|addline\|list\|reload>` | `/holo`, `/hd` | Manage `TextDisplay` holograms | `lobby.admin.hologram` |
| `/npc <create\|delete\|action\|movehere\|list>` | | Manage `Interaction` NPCs | `lobby.admin.npc` |
| `/lobbyadmin [reload\|lobbies\|worlds]` | `/lobbygui`, `/hubadmin`| Open interactive GUI, manage lobby worlds, or hot-reload | `lobby.admin.gui` |

### Key Permissions
* `lobby.admin.*` – Full access to all administrative commands and bypasses.
* `lobby.admin.build` – Bypass block place/break restrictions in lobby worlds.
* `lobby.chat.bypass` – Bypass chat anti-spam and anti-swear filters.
* `lobby.chat.color` – Allows formatting chat with color codes and MiniMessage tags.
* `lobby.admin.bypasshide` – Keeps staff visible even when players have hidden others.

---

## 🔧 Building from Source

### Prerequisites
* **Java 21** or higher.
* **Maven 3.9+**.

### Build Command
```bash
mvn clean package
```
The compiled, shaded JAR will be located at:
```
target/LobbyCore-1.0.0-SNAPSHOT.jar
```

---

## 📄 License
This project is proprietary and maintained for high-performance server networks.
