# OpenCode Prompts — V8 Fix Batch

Run ONE AT A TIME, in this order, verifying each before the next.

---

## 1. Melee — confirm/restore with cooldown (don't fully remove)

```
Task: melee should stay in the game, not be removed — the earlier removal
request has been reversed. It should behave like Fortnite's harvesting tool:
a punch with a clear cooldown/timeout between swings, no custom character
animation required (the procedural swipe VFX from before is fine to keep,
if it's still in the codebase).

Step 1 — Audit current state
Report exactly what's currently in the codebase: was the melee removal task
from before already executed (punch method, cooldown, and VFX all deleted),
partially executed, or not started? Report the current damage value and
cooldown timing if any melee code still exists.

Step 2 — Confirm target state before changing anything
Target: punch does 5 HP damage (or less, per earlier confirmed balance),
with a ~0.6-0.8 second cooldown between swings that fully blocks spam
(same values as the earlier balance-fix task). Bots should be able to use
this too when unarmed. The procedural swipe + impact-burst VFX can stay if
it still exists — it's acceptable without a full sprite animation. Confirm
this target state with me, and tell me exactly what needs to change to get
there from whatever Step 1 found (restore deleted code, adjust existing
values, or no change needed).

Step 3 — Implement
Make whatever changes are needed to reach the confirmed target state.

Step 4 — Verify
Play-test: confirm punch works, does the confirmed damage, and cannot be
spammed faster than the cooldown allows. Confirm an unarmed bot can also
punch.
```

---

## 2. Fix safe zone — it teleports instead of shrinking

```
Task: the safe zone is broken — it doesn't shrink concentrically, it appears
to relocate/jump from one area to another instead. Diagnose and fix this
before touching shrink speed.

Step 1 — Audit
Report exactly how the zone's current/next circle is calculated each shrink
phase: is there a fixed sequence of circle centers+radii it moves between
(which would explain "changes from one end to another"), or is it supposed
to interpolate the radius down from the same center and something's
overriding the center position incorrectly? Show me the actual shrink
logic — this needs a real explanation, not a guess, before anything is
changed.

Step 2 — Confirm the fix
Based on what Step 1 finds, propose the fix. The correct behavior: each
shrink phase should smoothly reduce the radius of a circle whose center is
either fixed for the whole match, or moves gradually and continuously
toward a new center rather than snapping — "changes from one end to
another" strongly suggests either a hardcoded jump between preset zone
definitions, or a center-recalculation bug. Confirm the specific fix before
implementing.

Step 3 — Implement and verify
Implement the confirmed fix. Play-test a full match (or as much as needed to
observe 2-3 shrink phases) and confirm the zone visibly shrinks smoothly
from its current boundary rather than jumping to a new location.
```

---

## 3. Zone shrink speed — make it faster

```
Task: once the shrink bug (previous task) is fixed and confirmed, increase
the zone's shrink speed so matches move faster.

Step 1 — Report current timing
Report the current shrink phase duration/interval and how much radius is
lost per phase.

Step 2 — Confirm new timing
Propose meaningfully faster values (shorter time between shrink phases,
and/or a larger radius reduction per phase) and confirm with me before
implementing — give me actual numbers to react to, not just "faster."

Step 3 — Implement and verify
Apply confirmed values. Play-test a full match and report how long it took
to reach the final zone state versus before.
```

---

## 4. Bots should actively move into the shrinking zone

```
Task: bots need to path toward the safe zone as it shrinks, not just wander
regardless of zone position.

Step 1 — Audit
Report whether bot movement/AI currently has any awareness of the zone at
all — does it check if the bot is outside the safe area and path back in,
or is zone damage the only mechanism currently keeping bots roughly in
bounds (i.e. they just take damage and wander regardless)?

Step 2 — Confirm the fix
Propose: bots should check their position against the current zone each
decision tick, and if outside (or soon to be outside, given shrink timing),
path toward the zone center/nearest safe edge as a higher priority than
other behavior (looting/combat), similar to how the player has to react to
the zone. Confirm this priority ordering with me — should fleeing the zone
override an active fight, or only override passive looting/wandering?

Step 3 — Implement and verify
Implement the confirmed behavior. Play-test/autodemo and confirm bots
outside a shrinking zone visibly move toward safety rather than continuing
to wander or fight in place while taking zone damage.
```

---

## 5. Bot movement speed — likely an infinite/always-on sprint bug

```
Task: bots appear to move much faster than intended, possibly always
sprinting. Diagnose and fix.

Step 1 — Audit
Report the bot's current movement speed value(s) and compare against the
player's base and sprint speeds. Check specifically whether bot movement
code is applying the sprint multiplier unconditionally (always-on) rather
than only when actively pursuing a target at close range, per the original
sprint-when-chasing design.

Step 2 — Confirm the fix
Based on findings, propose the correct behavior: bots should move at base
speed normally, and only use sprint speed when actively pursuing a visible
target (matching the original intent), not constantly. Confirm specific
speed values with me.

Step 3 — Implement and verify
Apply the fix. Play-test/autodemo and confirm bot movement speed now looks
reasonable relative to the player during normal wandering/looting, with
sprint only kicking in during an active chase.
```

---

## 6. Chests should not contain duplicate weapon types

```
Task: chest loot generation currently allows two of the same weapon in one
chest — fix so each chest's contents have no duplicate weapon types.

Step 1 — Audit
Report how chest contents are currently rolled/generated — random selection
with replacement (which allows duplicates) or some other method.

Step 2 — Confirm fix
Propose switching to selection without replacement for a single chest's
weapon rolls (still allowing duplicates of non-weapon items like medkits if
that's acceptable — confirm whether medkit duplicates are fine or should
also be restricted).

Step 3 — Implement and verify
Implement, then check several freshly-generated chests (via autodemo or
manual play) and confirm no chest contains two of the same weapon.
```

---

## 7. Chest loot UI — click to add to inventory (no drag-and-drop)

```
Task: chest contents are currently barely visible/unclear. Add back a
proper loot UI when interacting with a chest: shows all contents (weapons,
medkits, etc.) clearly, and clicking an item adds it directly to inventory
— no drag-and-drop needed. Note: chests currently auto-equip on pickup,
which is fine to keep for whatever the primary/first item is — this task is
about making the FULL contents visible and individually collectible via
click, not changing the auto-equip behavior.

Step 1 — Audit
Report whether any loot UI/panel already exists in the codebase (possibly
inherited from the original Rendezvous base and disabled/unused), or if
this needs to be built from scratch. Report how chest contents are
currently stored/represented in code.

Step 2 — Confirm the design before implementing
Propose: on interacting with a chest, open a panel listing each item inside
with its icon/name clearly visible. Clicking an item removes it from the
chest's contents and adds it to the player's inventory (respecting whatever
inventory slot limits already exist — flag if this needs slot-limit
handling). Confirm this matches what you want, and confirm whether this UI
should pause gameplay while open or not.

Step 3 — Implement
Build the confirmed UI and click-to-loot interaction.

Step 4 — Verify
Interact with a chest in-game, confirm all contents are clearly visible,
and confirm clicking each item successfully adds it to inventory without
any drag-and-drop required.
```
