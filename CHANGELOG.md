# Valencia Changelog

## [alpha-0.3] - 2026-05-21

### Removed
- **AutoSprint** module removed (unused)

### FPS Optimizations
- `XRayMod.isXRayBlock` now caches results per Block instance using a `ConcurrentHashMap` — previously did `descriptionId.substring()` + Set lookup for **every block in every chunk on every frame** when XRay was enabled. Now O(1) after first lookup per block type.
- `ClickGuiScreen` waifu rendering: cached the `GuiGraphics.blit` reflection `Method` once at load time instead of re-resolving it every frame.

## [alpha-0.2] - 2026-05-21

### New Modules
- **AutoSprint** — auto-sprint when any WASD key held, key `V`
- **Velocity** — cancels all knockback for local player, key `C`
- **FastPlace** — removes right-click delay (instant block/item use spam), key `F`
- **CritHit** — forces critical hits on every attack, key `R`

### Improvements
- Waifu image support: place `waifu.png` in `.minecraft/config/valencia/` — rendered bottom-left of ClickGUI (requires valid Minecraft ResourceLocation API at runtime; falls back to placeholder text if unavailable)
- ClickGUI now shows AutoSprint, Velocity, FastPlace in Movement column and CritHit in Combat column

---

## [alpha-0.1] - 2026-05-20

### Modules
- **NoFall** — onGround spoofing, no fall damage, default ON
- **XRay** — hide non-ore blocks, gamma 16.0
- **MaceAura** — silent aim + auto-attack with mace, detect 6f / attack 3.5f
- **KillAura** — any-weapon aura, Switch / Single target mode, tick-based attack timer, glow outline on target
- **NoSlow** — no slowdown while using items
- **BHop** — auto-jump on landing (WASD), air steering: velocity direction follows keys + yaw instantly, speed preserved through jumps
- **ClickGUI** — draggable panel, expandable per-module settings (sliders, toggles, inline keybind rebinding)

### Config (`valencia.json`)
- Saves and restores all module on/off states across sessions
- Saves keybinds, KillAura range / delay / mode, MaceAura range, BHop speed multiplier

### Default Keybinds
| Module | Key |
|---|---|
| NoFall | N |
| XRay | X |
| MaceAura | Z |
| KillAura | K |
| NoSlow | G |
| BHop | B |
| Step | H |
| ClickGUI | Right Ctrl |

### Notes
- Step module exists in GUI but is non-functional (Lunar Client 1.21 missing `maxUpStep` field)
- Requires Fabric Loader 0.19.2+, no Fabric API needed
