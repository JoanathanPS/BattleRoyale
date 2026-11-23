---
name: unslop
description: Remove generic AI slop from UI, copy, code comments, accessibility, and mobile layout. Entry point for the antislop skill pack. Use when the user says "unslop", "anti-slop", "clean up AI-looking output", "make this sound human", or asks to audit or polish generated UI, text, or code.
allowed-tools: Read, Write, Edit, Glob, Grep
---
# unslop

Entry point for the [antislop](https://github.com/miqdadbadjuber/anti-slop) skill pack (MIT, vendored in `.claude/skills/`). This skill only routes. The rules live in the skills below.

## Steps

1. Read `../antislop/SKILL.md` first. It is the core filter, it resolves the usage mode (DURING work or AFTER as an audit), and it is always required.
2. Read the extra skills that match the task:

| Task | Skill |
|---|---|
| Building or editing any interface (HUD, menus, web page): color, layout, components, motion | `../antislop-ui/SKILL.md` |
| Prose: headlines, button labels, README, commit and PR text | `../antislop-copywriting/SKILL.md` |
| Code comments: strip generic AI comments, keep useful ones, never touch code | `../antislop-code/SKILL.md` |
| Contrast, keyboard, focus, states | `../antislop-human/SKILL.md` |
| Layouts that must reflow from phone to desktop | `../antislop-layoutmobile/SKILL.md` |

3. If the user named a target, work on it. If not, work on the current `git diff`.
4. Follow the core skill's PASS/FAIL report format when it asks for one.

## Project notes (BattleRoyale)

- `web/`, `html/`, and `index.html` are the browser game and its UI. `antislop-ui`, `antislop-human`, and `antislop-layoutmobile` apply there.
- `core/` and `desktop/` are the LibGDX Java code. Only `antislop-code` (comments) applies. Do not change gameplay behavior.
- The core skill expects a `DESIGN.md` for UI direction. This repo has none, so label UI results as a draft without direction rather than inventing one.

## Updating the pack

The folders were copied from upstream release 3.2.32 (see `../antislop/VERSION`). To update, re-copy `skills/*` from the upstream repo and keep `../antislop/LICENSE`.
