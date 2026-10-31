# OpenCode Prompts — V7 Fix/Feature Batch

Run these ONE AT A TIME, in this order. Confirm each is verified working
before starting the next. Do not paste all of these into one session.

---

## 1. Remove Help option from main menu

```
Task: remove the "HELP" button from the main menu.

Step 1 — Find it
Locate the main menu screen class and find where the HELP button is defined
and wired up (its click listener / screen transition). Report what you find
before changing anything.

Step 2 — Remove
Remove the HELP button and its listener. Adjust the remaining button layout
(PLAY, LEADERBOARD, SETTINGS, difficulty selector, QUIT) so there's no gap
left where HELP used to be — reflow/reposition the other buttons to close
the space.

Step 3 — Verify
Run the game, confirm HELP no longer appears and the remaining menu buttons
are evenly spaced with no leftover gap.
```

---

## 2. Fix the Settings menu (currently non-functional)

```
Task: diagnose and fix the Settings menu, which currently does nothing when
interacted with.

Step 1 — Audit
Open the Settings screen/button code. Report: does clicking SETTINGS
navigate anywhere at all? If it opens a screen, what's on it, and which
specific controls (if any) don't respond? Don't fix anything yet — just
report what's actually wired up versus broken.

Step 2 — Confirm scope with me
Tell me what Settings currently is supposed to contain (resolution, window
mode, language, volume, etc. — check PlayerSettings.java referenced in the
README) versus what's actually implemented and reachable from the menu.

Step 3 — Fix
Once confirmed, wire up the Settings screen so it's actually reachable and
its controls actually apply and persist (at minimum: whatever's already
defined in PlayerSettings.java should be editable from this screen).

Step 4 — Verify
Open Settings, change a setting, back out, confirm the change actually took
effect (e.g. resolution/window mode visibly changes, or reopen Settings and
confirm the changed value persisted).
```

---

## 3. Fix "C'est la vie!" game-over text

```
Task: replace the game-over screen's "C'EST LA VIE!" heading with clearer
death-state messaging.

Step 1 — Find it
Locate the game-over/results screen and find where "C'EST LA VIE!" is set.
Check whether this screen is shown identically for both a win and a loss, or
if there's already a branch for different outcomes.

Step 2 — Confirm approach
The heading should reflect the actual outcome: something like "YOU DIED" or
"ELIMINATED" for a loss, and a distinct win message (e.g. "VICTORY" or
"WINNER WINNER") for a win, if the game currently has no win-state text at
all. Tell me what you find and confirm this split before implementing.

Step 3 — Implement
Add the outcome-based heading text. Keep RANK/KILLS/MATCH DURATION exactly
as they are — only the heading changes.

Step 4 — Verify
Play one match to a loss and confirm the new death text shows. If a win
condition is reachable in testing, verify that heading too; if not,
describe what you were unable to test.
```

---

## 4. Fix the Supabase leaderboard connection

```
Task: diagnose why the leaderboard shows "Could not reach the Supabase
leaderboard" and fix it.

Step 1 — Audit, don't guess
Find SupabaseMatchResultsRepository. Add temporary verbose logging (or check
if logging already exists) that prints: the exact URL being requested, the
HTTP status code returned, and the raw response body, on both the insert and
the leaderboard GET request. Do not change any logic yet.

Step 2 — Reproduce and report
Run the game, trigger a leaderboard fetch (open Leaderboard from main menu),
and report back the exact logged URL/status/response body. This tells us
whether it's: (a) a malformed request URL, (b) an auth/key problem, (c) a
Row Level Security policy blocking the anon key, (d) a network/connectivity
issue, or (e) something else. Do not fix anything until you show me this.

Step 3 — Fix based on what Step 2 shows
Once we know the actual cause, fix it. Common causes to check specifically:
  - supabase.properties has the correct URL/key with no typos or stray
    whitespace
  - the request URL is exactly {SUPABASE_URL}/rest/v1/match_results with
    correct query params for the GET
  - headers "apikey" and "Authorization: Bearer {key}" are both present on
    every request, not just insert
  - the match_results table's RLS policies actually allow anon SELECT
    (confirm this is a code problem, not a Supabase dashboard config problem,
    before changing code)

Step 4 — Verify
Reproduce the same leaderboard fetch, confirm it now succeeds and displays
real data (play a match first if the table is currently empty).
```

---

## 5. Reduce time-to-kill (player and bots are too tanky)

```
Task: reduce time-to-kill across the board — matches currently take too long
because entities have too much effective health relative to weapon damage.

Step 1 — Audit current numbers
Report the current health values for Player and Bot, and damage values for
each weapon tier. Show me the numbers before changing anything.

Step 2 — Confirm the adjustment
Propose new numbers that meaningfully reduce time-to-kill — either lower
max health across the board, or raise weapon damage, not both at once so we
can isolate what actually changes the feel. Suggest specific new values and
wait for me to confirm before implementing.

Step 3 — Implement
Apply the confirmed values.

Step 4 — Verify
Play a match, report roughly how many hits of the starting/common weapon
it now takes to kill a bot, and whether it feels meaningfully faster than
before.
```

---

## 6. Auto-reload

```
Task: make weapon reload automatic instead of requiring the player to press
R.

Step 1 — Audit
Find the current reload logic (bound to the R key). Report how ammo
tracking currently works per weapon.

Step 2 — Confirm approach
Recommended: when the current weapon's ammo hits 0 and the player attempts
to fire, automatically trigger the existing reload logic/animation instead
of requiring a keypress. Keep the R key working too as a manual "reload
early" option — don't remove it, just add the automatic trigger. Confirm
this approach before implementing.

Step 3 — Implement and verify
Implement, then play-test: fire a weapon empty, confirm it auto-reloads
without pressing R, and confirm manually pressing R before empty still also
works.
```

---

## 7. Melee weapon: unarmed "fists" with animation

```
Task: add a melee/unarmed attack (punching) available to the player and bots
when they have no weapon equipped.

Step 1 — Audit
Report how weapon-equip state currently works — is "no weapon" already a
valid state, or does the game assume a weapon is always equipped? Report
what animation system is already in use for other actions (movement, aim)
so the punch animation follows the same pattern.

Step 2 — Confirm scope
Propose: a short-range punch attack usable when unarmed (or explicitly
selected, if you want fists selectable even while carrying a weapon — tell
me which you want), with a basic swing animation and a modest damage value
comparable to the weakest weapon tier. Confirm damage/range numbers with me
before implementing.

Step 3 — Implement
Add the melee attack (input binding, hit detection at close range, damage
application) and its animation. Bots should also be able to use this when
unarmed, using the same attack logic as their existing weapon-based combat
decision code.

Step 4 — Verify
Play-test: punch a bot as the unarmed player, confirm damage applies and
the animation plays. Confirm an unarmed bot can also melee-attack (watch a
match where you avoid picking up a weapon immediately).
```

---

## 8. Fix crate/pickup looting everything at once

```
Task: fix item pickups so they don't dump the entire crate's contents into
inventory at once — pickup should be per-item, one at a time.

Step 1 — Audit
Describe exactly what currently happens when a player walks over/interacts
with a loot spawn: is it one item per pickup location, or is there still a
"crate" object containing multiple items that all transfer at once on
interaction? Report this before changing anything — this may already be
partially addressed by the earlier no-crates change, so confirm current
actual behavior rather than assuming.

Step 2 — Confirm approach
Based on what you find, propose the fix: each visible pickup on the ground
should represent exactly one item, picked up individually. If there's
still an underlying "crate contains N items" data structure causing the
dump-everything behavior, propose removing it in favor of spawning each
item as its own separate pickup in the world.

Step 3 — Implement and verify
Implement the confirmed approach. Play-test: walk over a loot area, confirm
you pick up one item at a time rather than your entire inventory filling
instantly from one interaction.
```

---

## 9. Scale match size to 50 combatants (49 bots + 1 player)

```
Task: increase match size so a full match has 50 combatants total — 49 bots
plus the player.

Step 1 — Audit
Report the current bot spawn count and where it's configured (a constant,
a config value, difficulty-based scaling, etc). Report whether spawn point
placement is hardcoded to a fixed list or generated/scattered — this matters
because 49 bots needs enough valid spawn locations on the current map.

Step 2 — Confirm before implementing
Confirm with me: should all 49 bots spawn at match start (harder on
performance, more chaotic early game), or should this be handled some other
way? Also flag if the current map size/spawn logic realistically supports
49 distinct starting positions without heavy overlap — tell me if this needs
map changes too, don't just force the number in.

Step 3 — Implement
Update the spawn count to 49 bots + 1 player. If spawn points are a fixed
list shorter than 49, generate additional scattered spawn points rather than
stacking bots on top of each other.

Step 4 — Verify
Run a match, confirm the alive counter starts at 50, and check whether
performance (frame rate) holds up reasonably with the full bot count active
at once. Report what you observed, including any slowdown.
```

---

## 10. Bots should fight each other, not just the player (V6 item, still unresolved)

```
Task: this was flagged before and doesn't appear fixed yet — bots currently
only target the player. Fix bot targeting so bots fight each other too.

Step 1 — Audit
Find the bot AI target-selection code. Report exactly what it currently
does: does it only ever consider the player as a possible target, or does
it consider other bots and just isn't picking them for some specific
reason (e.g. a distance check, a team/faction flag, a bug in the nearest-
target search)? Show me the actual logic before changing it — I want to
understand what's actually happening, not just have it patched blind.

Step 2 — Confirm the fix
Based on what you find, propose the specific change needed so an armed bot
evaluates the nearest reachable combatant — player OR any other bot — as
its target, not just the player. Confirm the approach with me before
implementing, since this is the highest-risk change in this batch.

Step 3 — Implement
Make the confirmed change.

Step 4 — Verify, specifically
Start a match and do not move or engage at all for at least 20-30 seconds.
Confirm the alive count drops from bot-vs-bot combat alone, with zero player
involvement. This is the actual test — report what you observed.
```
