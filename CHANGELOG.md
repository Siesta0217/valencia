# Valencia Changelog

## [alpha-0.22] - 2026-05-21

### Scaffold — cardinal bridges for diagonal sprint-jump
alpha-0.21 only tried `curFoot.below()` as a bridge — that handles vertical-drop diagonals but not horizontal-only diagonals. Sprint-jumping diagonally (W+A or W+D, or any small heading offset) crosses both x and z boundaries in one tick, so the old column ends up at a diagonal offset and `findPlacement` finds no face-neighbor reference.

Extended bridging to try 4 cardinal horizontal neighbors of curFoot in addition to DOWN. Whichever bridge cell has a face-neighbor reference (typically the old starting block one block laterally) gets placed first; then curFoot uses the bridge as its own face-neighbor.

Two placements per tick when bridging, one otherwise — same as alpha-0.21, just with more bridge candidates.

## [alpha-0.21] - 2026-05-21

### Scaffold — stair-step bridging when towering diagonally
Tower Move + WASD = diagonal motion. When the player crosses both an x/z and a y boundary in one tick, curFoot ends up at a *diagonal* offset from the previous column block — not a face-neighbor — so `findPlacement` returns null and the placement silently fails. Player keeps rising on Tower vy with no column under them.

Fixed by adding a fallback: if curFoot has no face-neighbor reference, try `curFoot.below()`. The bridge block usually does have an old-column face-neighbor (one block laterally + at the same y as old top). Place bridge first, then curFoot uses the bridge as a DOWN reference. Two placements per tick when needed, one otherwise.

Doesn't trigger thick towers — only goes 1 level below, and only when its own findPlacement finds a reference. Isolated lateral drift (no nearby column at all) still skips placement, just like before.

## [alpha-0.20] - 2026-05-21

### Scaffold — fix scattered blocks / fly-away from sprint inertia

Found the real cause of "tower flies up without column". With Tower Move ON, the mod preserved whatever horizontal velocity the player had — including sprint inertia that decays slowly (drag 0.91/tick, ~10 ticks to bleed off). At low Tower Spd, the player drifts sideways 1–2 blocks per vertical block rise. Each scaffold placement then lands at a new (x, z), and `findPlacement` can't find a neighbor (the previous column is at a different x/z). Placement returns null. Tower vy keeps pushing the player up. Blocks end up scattered across the ground instead of forming a column.

Fixed by gating Tower's horizontal-preservation on WASD actually being pressed. Tower Move ON without WASD held now zeros horizontal velocity (same as Tower Move OFF). Sprint momentum no longer leaks into the tower.

- Tower Move OFF → always vertical
- Tower Move ON + WASD held → drift in WASD direction (steerable tower)
- Tower Move ON + no WASD → vertical (same as Tower Move OFF — no sprint drift)

## [alpha-0.19] - 2026-05-21

### Scaffold — fix "tower flies up without column"

Look-ahead was trying to place blocks that the player's hitbox still overlapped (player feet at y=N+0.3, block placement at y=N, hitbox overlaps the block from y=N+0.3 to y=N+1). Vanilla servers reject placements that overlap any entity, so the `useItemOn` packet silently failed. Scaffold thought it placed, but no block ever landed — meanwhile Tower vy kept lifting the player off into the air.

Fixed by removing look-ahead entirely. Scaffold now only places at curFoot when it's actually empty, which means the player has *already* cleared the block above and there's no overlap. Every `useItemOn` is clean, no rejection, column grows reliably at all Tower Spd values.

Removed the `Look Ahead` GUI toggle — wasn't doing useful work, and the toggle was the source of the flying-up bug.

## [alpha-0.18] - 2026-05-21

### Scaffold
- Look-ahead prediction distance bumped from 1.0 → 1.5 ticks. At low Tower Spd, this places the next block one full tick before the player physically crosses the boundary into it. Same placement rate, but each placement happens earlier so the column visibly stays ahead of the feet instead of being placed right at the moment of crossing.

## [alpha-0.17] - 2026-05-21

### Scaffold — fix fat tower from lateral drift
Reverted the 3-deep gap-fill from alpha-0.16. It made the new column thick whenever the player moved sideways while towering: the new (x, z) had no ground below, so scan-downward filled multiple blocks deep at every lateral step. Back to one block per tick (foot, or look-ahead predicted foot). Gravity compensation from 0.16 stays — that alone is enough to keep up.

## [alpha-0.16] - 2026-05-21

### Scaffold — fix "blocks can't keep up" at low Tower Spd

Two underlying bugs:
1. **Gravity ate the slider value.** `setDeltaMovement(*, towerSpeed, *)` was being reduced by gravity (~0.08) and drag every tick, so Tower Spd 0.3 only produced ~0.216 b/tick actual rise. Now stamps `towerSpeed + 0.08` to compensate — the slider value matches the real rise rate.
2. **One-block-per-tick was fragile.** A single missed tick (lag spike, weird velocity, etc.) left a hole the player would fly past. Scaffold now scans the foot block and up to 2 blocks below, and fills any empty cells in a single tick (bottom-up so each placement has a reference below).

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
