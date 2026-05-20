# Valencia Changelog

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
