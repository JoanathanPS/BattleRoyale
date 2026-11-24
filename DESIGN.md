# DESIGN.md: Battle Royale Lite portal

Scope: the web portal in `web/index.html`, `web/css/`, `web/js/`, `web/img/`, `web/fonts/`. The game (`web/game.html`, `web/battlearena/`, `web/assets/`) is out of scope and must not change.

Dial: ENERGY 3 / RHYTHM 3 / MOTION 2

Design Read: game launch portal for people who want to try a browser game in one click, flat daylight island style taken from the game's own art.

## Identity motif
The shrinking safe-zone ring. The header minimap shrinks its ring one step per section (4 steps, like the game's 4 shrinks). The hero shows the island with the red outside-the-zone shading. Nothing else uses red.

## Decisions, one line each
- **Name and wordmark**: "Battle Royale Lite", the title on the game's own logo and menu. The header wordmark is live text (Barlow Condensed italic, ink on paper, "Lite" in a red block). The game's logo PNG is white-filled and disappears on a light page, so it is not used.
- **Light theme**: the game is a bright flat daylight island (teal sea, lime land). A dark page would be a different product.
- **Palette**: ocean `#13ae93` (read from `minimap.png`), land `#b9f27a`, ink `#10202a`, paper `#f7f4e8`, one accent zone red `#c62b22`. Red is the danger zone only.
- **Type**: Barlow Condensed (800 italic for headings, 700 for HUD labels) because the game's logo and HUD are condensed italic caps; Barlow 400/600 for reading. Both OFL, self-hosted. BOMBARD (the game's font) is not used because no license was found.
- **Square corners, white tag buttons with a thick left bar**: copied from the game's menu buttons. Reason: the portal should feel like the same product.
- **Layout rhythm**: five sections with different compositions: split hero over the map, dark full-width stage, class-columns armory with a key diagram, results table, two-block download. No repeated card grid.
- **Map grid**: it is the game's real minimap grid, not a decorative pattern.
- **Real screenshot** (`img/game-menu.jpg`) as the stage poster: a capture of the actual game menu, no illustration.
- **Motion**: ring step transitions only (state-driven from the section in view). Off under `prefers-reduced-motion`. Nothing loops.
- **No fabricated content**: numbers come from the game source (weapon stats, bot counts, zone rules) or live from Supabase. No testimonials, no stats counters, no FAQ.
- **Static HTML/CSS/JS, no framework, no CDN**: the game is prebuilt static files on Vercel; nothing here needs a server or a build step.

## Source of facts
Weapon, armor, bot and zone numbers were read from `core/src/com/joanathanps/battlearena/` (weapon classes, `WorldBuilder`, `Difficulty`, `SafeZoneController`, `InputTracker`, `GameOver`). Re-check them there if the game changes.
