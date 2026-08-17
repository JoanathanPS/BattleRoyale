# Battle Royale Arena

A 2D top-down Battle Royale game built in Java with LibGDX. Survive on an island with dozens of bots, competing over resources in shrinking combat zones.

## Features

- Shrinking safe zone mechanic with increasing damage
- AI-controlled bots with combat behavior
- Loot system: weapons (pistols, assault rifles, sniper rifles), armor, helmets, medkits
- HUD with health, armor, ammo, minimap, kill tracker
- Match history and leaderboard system (JDBC/MySQL)

## How to play

Run `run.bat` or execute the Gradle task: `.\gradlew.bat desktop:run`

## Controls

| Action | Key |
|--------|-----|
| Movement | WASD |
| Aim | Mouse |
| Shoot | Left Mouse Button |
| Select item | 1-6 or Mouse Scroll |
| Interact | E |
| Reload | R |
| Map | M |
| Pause | ESC |

## Credits & Attribution

This project is built on [Rendezvous](https://github.com/brensio/rendezvous) by **brensio**, licensed under the [MIT License](LICENSE).

### Original engine contributions (by brensio):
- 2D top-down Battle Royale game engine
- Player, Enemy, and Soldier entity hierarchy
- Weapon system (pistols, assault rifles, sniper rifles)
- Loot and inventory system
- Shrinking safe zone mechanic
- Box2D physics integration
- Tiled map rendering
- HUD and UI system
- AI bot behavior

### Battle Royale Arena additions:
- JDBC/MySQL match history and leaderboard system (`DatabaseManager`, `LeaderboardScreen`)
- Match result persistence (kills, survival time, score)
- Leaderboard screen accessible from main menu and game over
- Database schema for match history storage

## Development

- **IDE**: IntelliJ IDEA
- **Framework**: LibGDX 1.9.8
- **Build**: Gradle 4.6
- **Java**: JDK 8
- **Database**: MySQL 8.0 (for leaderboard feature)

## Configuration

Edit `core/src/com/joanathanps/battlearena/scheme/PlayerSettings.java` to change resolution, window mode, and language settings.
