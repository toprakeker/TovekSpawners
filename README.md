# 🐷 TovekSpawners

<div align="center">

[![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur-F78C40?style=flat-square&logo=minecraft&logoColor=white)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.21.x-blue?style=flat-square)](https://papermc.io)

**Lag-free Silk Touch spawner harvesting and stacking engine for Paper & Purpur 1.21+.**

[📦 Download Latest Release](https://github.com/toprakeker/TovekSpawners/releases/latest) • [🌐 Developer Portfolio](https://toprakeker.com)

</div>

---

## Features

- **Silk Touch Harvesting:** Mine any monster spawner using a Silk Touch pickaxe to collect it as a placeable item.
- **Spawner Stacking:** Place spawners of the same mob type against each other to stack up to 50x in a single block.
- **Server TPS Optimization:** Multiple spawners operate within a single block coordinate, drastically cutting entity tick overhead and keeping server TPS locked at 20.
- **Smart Mining:**
  - *Standard break:* Mines 1 spawner from the stack.
  - *Sneak + break:* Mines the entire stack at once.
- **Persistent Data Container (PDC):** Mob types are stored directly in Minecraft NBT/PDC tags; spawner types persist across server restarts, reloads, and world transfers.

---

## Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/spawners give <player> <type> [amt]` | `spawners.admin` | Give specific spawner items directly |
| `/spawners reload` | `spawners.admin` | Reload configuration settings |

---

## Building from Source

```bash
git clone https://github.com/toprakeker/TovekSpawners.git
cd TovekSpawners
./gradlew build
```

Compiled jar will be located in `build/libs/`.

---

## License

MIT License. Free for public server networks and modification.
