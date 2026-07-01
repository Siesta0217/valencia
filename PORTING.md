# PORTING.md — MC 版本移植查核表

目標:把「移植到新 MC 版本」從考古工作變成照表操課。每一項都列出**誰依賴它**,
移植時逐一用 `javap` 對新版 loom jar 確認簽名,炸掉的照表修。

## 怎麼驗證簽名

```
# loom 解混淆 jar(路徑中版本號隨移植目標調整)
~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/<ver>-loom.mappings.../....jar

javap -p -classpath <解壓目錄> net.minecraft.client.player.LocalPlayer | grep sendPosition
```

本機 JDK 25 沒設 JAVA_HOME 時用絕對路徑:
`C:\Program Files\Eclipse Adoptium\jdk-25...\bin\javap.exe`。
jar 解不開就用 .NET `[System.IO.Compression.ZipFile]` 列 entry。

> 本 build 是 **layered 混合 mapping**:類結構走 Mojang,但部分成員名走 Yarn
> (`Identifier`、`getSelectedSlot`)。**別假設純 Mojmap 或純 Yarn,一律 javap。**

## 1. Mixin 注入點(無法 facade,改名 = 直接炸;`defaultRequire=1` 會在啟動報錯)

### 高風險集中點

| 注入目標 | 依賴的 mixin | 備註 |
|---|---|---|
| `LocalPlayer.sendPosition` | **NoFall / KillAura / MaceAura / SpearAura**(各 HEAD+RETURN 一對) | 最大單點風險:改名同時炸 4 個模組。silent-aim 的「送封包前改旋轉、送完還原」全靠它 |
| `ClientPacketListener.handleSetEntityMotion` | VelocityPacket(@Redirect 取 packet 值) | raw packet handler,零容錯 |
| `ClientPacketListener.handleExplode` → `ClientboundExplodePacket.playerKnockback()` | VelocityExplosion(@Redirect) | 同上;另依賴 packet record 的 accessor 名 |
| `LocalPlayer.aiStep` | BHop、Fly(皆 LivingEntity.class 目標但 method 在子類鏈上) | movement 核心 |
| `LocalPlayer.modifyInput` / `aiStep` / `canStartSprinting` | NoSlow(3 × @Redirect) | @Redirect 對 target 內部呼叫簽名敏感,比 @Inject 更易碎 |

### 其餘注入點

| 注入目標 | mixin |
|---|---|
| `Minecraft.tick` | TickMixin(HEAD)、TimerMixin |
| `Minecraft.rightClickDelay`(@Shadow) | TickMixin(FastPlace/Scaffold reset 用) |
| `LocalPlayer.tick` | ScaffoldMixin(TAIL)、StepMixin |
| `Entity.turn` | FreecamTurn(HEAD, cancellable;×0.15 縮放假設) |
| `Entity.isCurrentlyGlowing` | GlowMixin |
| `Entity.getBoundingBox()Lnet/minecraft/world/phys/AABB;` | HitboxMixin(顯式描述子) |
| `Camera.setup` + @Shadow `setPosition`/`setRotation` | FreecamCamera |
| `MultiPlayerGameMode.attack` | CritMixin |
| `MultiPlayerGameMode.continueDestroyBlock` | FastBreakMixin |
| `Gui.render` | HudMixin(TAIL;所有 HUD 的總入口) |
| `EntityRenderer.shouldShowName` | NameTagVisibility |
| `ChatScreen.handleChatInput` | ChatMixin(`.nf` 指令;`require = 0` 故意軟依賴) |
| `BlockBehaviour.BlockStateBase.getRenderShape` / `canOcclude` | XRayBlock(render 內部,最易被重構) |
| `DebugRenderer.emitGizmos` | ESPGizmo(gizmos 是 1.21.9+ 新 API,仍可能變動) |
| `ClientInput.tick` | ClientInputMixin(Freecam 凍結輸入) |

## 2. 已驗證的 API 簽名雷區(改版時重驗)

- `ResourceLocation` 在此 build 叫 **`net.minecraft.resources.Identifier`**(Yarn 名)
- `GuiGraphics.blit` 9-arg = **角座標 + 0..1 normalize UV**,不是 (x,y,w,h)+pixel UV;傳錯 silent fail
- `Inventory.getSelectedSlot()/setSelectedSlot(int)` 是 public(AutoTool/Scaffold/Nuker/ElytraGoto 用)
- `Entity.fallDistance` 是 **public double**(NoFall/Crit/Fly mixin 讀寫)
- `Entity.maxUpStep` 不存在 → `Attributes.STEP_HEIGHT`(StepMod)
- `Options.sensitivity()` 回 `OptionInstance<Double>`,`.get()` 0..1(GCD 公式)
- `DynamicTexture` 需 `(Supplier<String>, NativeImage)` 建構子
- `Screen.mouseClicked/Released/Dragged` 用 `MouseButtonEvent`;`keyPressed` 用 `KeyEvent`
- 敵對判定用 **`Enemy` interface**,不是 `Monster`(Phantom/Slime)
- `InventoryScreen.renderEntityInInventoryFollowsMouse(GuiGraphics,int,int,int,int,int,float,float,float,LivingEntity)`(TargetHUD,已 try/catch)
- `Gizmos.cuboid(AABB, GizmoStyle)` → `GizmoProperties`,穿牆要 `.setAlwaysOnTop()`
- `Camera.setRotation(float,float)`/`setPosition(double,double,double)` 是 protected(@Shadow)
- `Entity.turn(double yawΔ, double pitchΔ)`:×0.15,pitch clamp ±90(FreecamMod.applyTurn 複製此常數)
- `EnchantmentHelper.getItemEnchantmentLevel(Holder<Enchantment>, ItemStack)`;
  `Enchantments.EFFICIENCY` 是 `ResourceKey` → 經 `level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(...)` 取 Holder(AutoTool)
- `LocalPlayer.input` 型別 `ClientInput`,無 `forwardImpulse`/`leftImpulse`
- Mace/Spear/Kill aura 的 reach 用眼睛→AABB 最近點(`KillAuraMod.reachDistSq`),伺服器行為若變需重驗

## 3. 移植流程

1. `gradle.properties`:改 `minecraft_version` / `yarn_mappings` / `loader_version` / `fabric_version`
   (**不要動 build.gradle / settings.gradle / wrapper** — 本機 JDK 25 雷)
2. `./gradlew.bat assemble` → 編譯錯誤 = 表 2 的 API 改名,逐一 javap 修
3. 編譯過 ≠ 能跑:mixin 注入點(表 1)在**啟動時**才炸(`defaultRequire=1`),看 log 的
   `Critical injection failure` 逐一對表修
4. 啟動過 ≠ 正確:`GuiGraphics.blit`/gizmos 這類 silent-fail 要進遊戲逐模組目測
5. 高風險先測:sendPosition 四模組(silent aim + NoFall)、Velocity 兩個 packet redirect、XRay

## 4. 已知外部 gate(2026-06 記錄)

- 「26.2」目標的 Fabric/Yarn/loom mappings 與 Lunar 支援尚未到位;
  Lunar profile 目錄已見 `profiles/26/mods/fabric-26.1.2` 與 `fabric-26.2-snapshot-4`,
  等 mappings 齊了照本表執行。
