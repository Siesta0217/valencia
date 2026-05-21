# Valencia Changelog

## [alpha-0.15] - 2026-05-21

### Fixed
- Slider int-snap heuristic. A range whose bounds were both integers (e.g. Tower Spd 0–1) was being treated as integer-only and collapsing to just `{0, 1}`. Now also requires `max - min >= 2` before treating as integer slider. Tower Spd works at 0.1 granularity again.

## [alpha-0.14] - 2026-05-21

### Scaffold
- Tower Spd slider min changed from 0.42 to 0.0 — useful for fine-tuning slow-tower or effectively disabling rise while keeping the rest of the Tower behavior wired.

## [alpha-0.13] - 2026-05-21

### Scaffold
- Tower is now **hold-to-rise** — checks `mc.options.keyJump.isDown()` instead of stamping every tick unconditionally. Press jump = go up at `Tower Spd`. Release = fall back onto the column.

## [alpha-0.12] - 2026-05-21

### Scaffold
- **Fast Tower** — removed `onGround` gate; Tower now stamps y velocity every tick instead of only after landing. Goes straight from ~1.6 b/s (vanilla jump cooldown) to whatever `Tower Spd` is set to.
- **Tower Spd** slider (0.42 – 1.0). 0.42 = vanilla jump magnitude per tick (~8 b/s). 1.0 = ~20 b/s.
- **Fake Hand** toggle — server-only slot swap via `ServerboundSetCarriedItemPacket`. Client `inv.selected` is untouched, so first-person view keeps showing whatever item you were holding (sword, bow, etc.) while server still processes the placement as if you'd swapped to a block.

### Velocity — Knockback Scaling
Replaced full-cancel with delta-scale:
- **Horiz** (0–200%) — scales the horizontal component of the knockback vanilla adds. 0 = immune. 100 = vanilla. 200 = doubled.
- **Vert** (0–200%) — same for the vertical component.
- Implementation samples velocity in `knockback` HEAD, reads post-knockback velocity in RETURN, scales the delta and re-applies. Doesn't touch the player's pre-existing movement.

## [alpha-0.11] - 2026-05-21

### Scaffold
- **Tower Move** toggle added. ON (default) = keep horizontal velocity while Tower is jumping → WASD steers a slanted tower. OFF = zero out x/z velocity each Tower jump → straight-up column regardless of inputs.

## [alpha-0.10] - 2026-05-21

### Scaffold — Simplified
Trimmed GUI to the three settings that actually matter, plus slot management:
- **Tower**, **Look Ahead**, **Silent Rot**, **Auto Sw**, **Sw Back**, **Delay**, **Key**.

Internal-only (still always on, no longer in GUI):
- Multi-block-per-tick removed — back to one placement per tick (foot, or look-ahead foot if current is solid)
- Air-place is always on (was a toggle; off would just drop you)
- Skip-falling-block and skip-container item filters are always on
- Extend radius removed (was 0 default anyway)
- Sneak / Safe Walk placeholders removed (never implemented)

## [alpha-0.9] - 2026-05-21

### Scaffold — Aggressive Mode

Re-tuned for vanilla / no-anticheat servers:
- **Look Ahead** — predicts next-tick foot position via `player.getDeltaMovement` and pre-places. Catches fast/sprinting movement before the player loses ground.
- **Per Tick (1–5, default 3)** — multiple `useItemOn` calls per tick. Place foot + look-ahead + extend candidates in a single tick.
- **Air Place** (default ON) — places while airborne (jumping, falling, mid-step).
- **Silent Rot** (default ON) — sends a `ServerboundMovePlayerPacket.Rot` aimed at the hit point right before `useItemOn`, then restores. Bypasses vanilla/Paper facing-validation when the local view isn't pointed at the placement.
- **Skip Heavy** — auto-skips FallingBlock items (sand, gravel, anvil, concrete powder).
- **Skip Cont** — auto-skips containers (chest, barrel, ender chest, furnace, crafting table).
- **Extend (0–2)** — places in a 1- or 2-block radius around the foot for wide platforms.
- **Place Delay default = 0** — fire every tick when work is available.
- `findBlockSlot` now picks the slot with the **most blocks**, so long bridges don't fragment hotbar slots.

## [alpha-0.8] - 2026-05-21

### New Module
- **Scaffold** (`J`) — auto-places a block under the player's feet when stepping off solid ground.
  - **Tower** — stamps jump velocity while grounded → tower up by holding the scaffold key + scaffold places blocks under you each tick
  - **Auto Sw** — auto-switches hotbar to a slot containing any BlockItem
  - **Sw Back** — restores the previous hotbar slot after placing
  - **Delay** — ticks between placement attempts (0–10)
  - **Sneak** — placeholder hook for server-side sneak while placing
  - **Safe Walk** — placeholder hook for sneaking at ledges (not yet wired)
  - **Key** — toggle key

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
