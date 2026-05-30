---
type: skill-draft
status: draft
target: claude-code
created: 2026-05-30
---

# design-system Skill Draft

This can later become `.claude/skills/design-system/SKILL.md`.

```md
---
name: design-system
description: Use when designing or polishing Valencia UI, ClickGUI, HUD overlays, module controls, and visual states.
---

# Valencia Design System

Design for a utility-heavy Minecraft client UI: compact, readable, fast to scan, and stable during gameplay.

## Principles

- Prefer dense but organized controls over marketing-style layouts.
- Keep HUD text legible over noisy game backgrounds.
- Avoid oversized cards, decorative blobs, and generic purple gradients.
- Do not place cards inside cards.
- Use stable dimensions so labels, sliders, toggles, and dynamic values do not shift layout.
- Preserve clear enabled/disabled states for modules.
- Color should communicate state, category, and risk without overwhelming the screen.

## UI Surfaces

- ClickGUI: compact module panels, clear sectioning, predictable controls.
- HUD: readable at a glance, no overlap, good contrast.
- ESP/NameTag/TargetHUD: configurable visibility and distance-aware clarity.

## QA Checklist

- Text does not overlap controls.
- Long labels fit or wrap intentionally.
- Enabled/disabled states are distinguishable.
- Sliders and keybind controls have stable hit areas.
- Colors remain readable against bright and dark game scenes.
- Animation, if any, is subtle and does not hurt gameplay readability.
```

