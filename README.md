# Battle Royale Lite

A top-down battle royale for one player against 5, 12 or 20 bots. You drop onto an island with nothing, loot crates and bodies for guns, armor and medkits, and stay inside a circle that keeps closing until one soldier is left.

Written in Java with libGDX. It runs on the desktop and, compiled with GWT, in the browser.

- Play in the browser: [battleroyale.joanathan.in](https://battleroyale.joanathan.in)
- Desktop: download `BattleRoyaleLite.jar` from the site or the [releases](https://github.com/JoanathanPS/BattleRoyale/releases), then run `java -jar BattleRoyaleLite.jar` (Java 8 or newer)

## Controls

| Key | Action |
|---|---|
| W A S D | Move |
| Shift | Sprint (uses stamina) |
| Mouse | Aim |
| Left click | Shoot, punch with an empty hand, or use a medkit |
| R | Reload |
| 1 to 6, mouse wheel | Pick an inventory slot |
| E, Enter or click | Take the highlighted item from the loot panel |
| Up / Down | Move through the loot panel |
| M | Island map |
| Esc | Pause, or close the map |

Walking over a body takes everything useful from it. Crates and chests open a loot panel so you can choose.

## How a match works

**The zone.** It closes in five phases. In each phase the next circle appears on the minimap and the map (white outline), then the current circle (red) slides and shrinks onto it. Every circle sits inside the previous one and the last one closes to nothing, so every match ends. Standing outside costs health every second, and armor does not help:

| Phase | Wait | Shrink | Circle radius | Damage outside |
|---|---|---|---|---|
| 1 | 35 s | 25 s | 30.5 | 2 HP/s |
| 2 | 30 s | 20 s | 21 | 3 HP/s |
| 3 | 25 s | 15 s | 13 | 5 HP/s |
| 4 | 20 s | 12 s | 6.5 | 8 HP/s |
| 5 | 15 s | 15 s | 0 | 12 HP/s |

**Armor.** A helmet and a vest share each hit until their points run out, and whatever they cannot absorb reaches your health. A piece at zero breaks and drops off.

**Weapons.** Magazines are limited and reserve ammo is not. A second copy of a gun you already carry refills it.

| Weapon | Damage | Time between shots | Magazine | Reload | Range |
|---|---|---|---|---|---|
| G21 | 15 | 0.6 s | 15 | 2 s | 16 |
| P26 | 18 | 0.4 s | 17 | 1 s | 16 |
| STAR | 8 | 0.1 s | 20 | 3 s | 20 |
| W16A | 9 | 0.2 s | 20 | 4 s | 20 |
| M20 | 105 | 3 s | 5 | 4 s | 34 |
| AW3 | 135 | 5 s | 1 | 5 s | 34 |

Medkits heal 50 over 5 seconds, during which you cannot move.

**Bots.** Difficulty sets how many there are, how fast they react, how often their shots go where they aim, and how far away they notice you.

| Difficulty | Bots | Reaction time | Shots on target | Notice range |
|---|---|---|---|---|
| Easy | 5 | 0.5 s | 45 % | 0.7x |
| Medium | 12 | 0.25 s | 70 % | 1x |
| Hard | 20 | 0.08 s | 90 % | 1.35x |

Bots loot, carry the best gun they find, fight anyone they can see (each other included), route around buildings and head for the next circle before it closes.

**Score.** 100 per kill, 500 for a win, 10 per second survived. Finished matches are sent to the online leaderboard under the name you type in the main menu.

## Build from source

You need a JDK (8 or newer). The Gradle wrapper downloads everything else.

```bash
./gradlew desktop:run        # play from source
./gradlew desktop:dist       # desktop/build/libs/BattleRoyaleLite.jar
./gradlew desktop:webDist    # same, copied to web/ for the download link
./gradlew html:webDist       # browser build, copied to web/battlearena and web/assets
```

To try the browser build locally, serve `web/` (browsers block it over `file://`):

```bash
cd web && python3 -m http.server 8000   # then open http://localhost:8000/game.html
```

After `desktop:webDist`, update the JAR's size and SHA-256 shown in `web/index.html`.

### Leaderboard

The game reads Supabase credentials from `core/assets/supabase.properties`, which is not committed. Copy `supabase.properties.example` next to it and fill in the project URL and anon key. Without it the game runs normally and the leaderboard screen says it is offline.

It expects a `match_results` table with `player_name`, `difficulty`, `score`, `kills`, `survival_time_seconds`, `result` and `created_at`, readable and insertable by the anon role.

## Project layout

```
core/      the game, shared by every platform
  scenes/     MainMenu, Match (the game loop)
  entities/   Player, Enemy (bot), Soldier (shared), items, loot containers, bullets
  events/     AI, SafeZoneController, InputTracker, contact listener, waypoint navigation
  gui/        HUD, map, pause, game over, leaderboard, settings
  forge/      builds the physics world from the Tiled map
core/assets/  textures, Tiled maps, fonts, sounds, text strings
desktop/   LWJGL launcher
html/      GWT launcher and the bitmap-font stand-in for FreeType on the web
web/       the deployed site: portal page, compiled browser build, desktop JAR
```

## Credits

The original engine, art and map are by Breno Sousa ([brensio](https://github.com/brensio)), MIT licensed. Extended and maintained by [JoanathanPS](https://github.com/JoanathanPS): bots, difficulty, zone, combat, loot, HUD, leaderboard, browser build and site.
