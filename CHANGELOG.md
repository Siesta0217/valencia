# Valencia Changelog

## [alpha-0.7] - 2026-05-21

### KillAura
- **Vis Body** toggle added — sets `yBodyRot` + `yHeadRot` to target yaw while leaving `yRot` (camera) restored to user input. Visible in 3rd person and to other players; first-person view unaffected. Independent from Body Lock; combine as needed.

## [alpha-0.6] - 2026-05-21

### KillAura
- **Body Lock** toggle added — when ON, the local view physically snaps to the target (visible rotation / snap-aim) instead of restoring rotation post-send (silent aim). Combine with `Smooth Rot` OFF for instant snap or `Smooth Rot` ON for FPS-style aimlock tracking.

## [alpha-0.5] - 2026-05-21

### KillAura — Major Overhaul

**Fixed:**
- **Ghost swings on airborne mobs.** Attack range was using `Entity.distanceTo` (center-to-center), but the vanilla server reach check uses player-eye-to-hitbox-nearest-point. A mob floating at Y+4 had center-distance ~4.0 (above the 3.0 reach gate, so we skipped) but hitbox-edge distance ~2.4 (well within reach). Now uses `reachDistSq` to match server behavior.

**New:**
- **Raycast (line-of-sight)** — won't attack targets behind walls. Toggleable.
- **Skip Invisible** — ignores invisible players/mobs. Toggleable.
- **Wait Cooldown** — only attacks when 1.21 attack-strength meter is fully charged (max damage every hit). Toggleable.
- **Smooth Rotation** — lerps rotation toward target with `Max Turn` deg/tick cap (default 60°). Much less detectable than instant snap. Toggleable.

**Internal:**
- Attack delay now keyed on `tickCount` not a countdown — robust across sleeping/dead/riding states.
- `distanceTo` → `distanceToSqr` throughout `findTarget`; raycast deferred until cheaper checks pass.
- Bounds-checked `attackDelay` to `max(1, …)`.

## [alpha-0.4] - 2026-05-21

### Improvements
- Waifu image now supports **PNG, JPG/JPEG, BMP, GIF**. Place any of `waifu.png`, `waifu.jpg`, `waifu.jpeg`, `waifu.bmp`, `waifu.gif` in `.minecraft/config/valencia/`. Non-PNG formats are decoded via Java `ImageIO` and re-encoded to PNG internally for `NativeImage`.

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
