# Valencia

Fabric client mod for **Lunar Client 1.21** — utility / combat features.

Latest: **v1.6.6** — [Download JAR](https://github.com/Siesta0217/valencia/releases/latest)

---

## Features

| Module | Default Key | Description |
|--------|-------------|-------------|
| **NoFall** | `N` | onGround spoofing，不受墜落傷害 |
| **XRay** | `X` | 透視礦石，gamma 16.0 |
| **MaceAura** | `Z` | 手持重錘自動鎖定 + silent aim 攻擊，可選 Hostile / Animals / Players 目標 |
| **KillAura** | `K` | 全武器 aura，switch/single 模式可選，目標範圍 / 種類 / 攻速可調 |
| **NoSlow** | `G` | 使用物品時不減速 |
| **BHop** | `B` | 自動跳躍保持最高速度，可選 LowHop（低跳）/ Boost（每跳複合加速）/ KB Boost（受擊反 velocity） |
| **Step** | `H` | 步高提升（透過 `STEP_HEIGHT` attribute，1.0–3.0 可調） |
| **Velocity** | `C` | 受擊擊退倍率調整（水平 / 垂直 0–200%） |
| **FastPlace** | `F` | 移除右鍵放置冷卻 |
| **CritHit** | `R` | 自動 micro-hop 觸發暴擊 |
| **Scaffold** | `J` | 自動橋方塊 + Tower 模式（按 SPACE 自動疊塔） |
| **Timer** | `T` | 玩家 tick 倍速（1.0–3.0×），等同 movement-only speedhack |
| **ElytraGoto** | — | 設定 XYZ 目標座標，自動轉向 + 自動發射煙火，遠距離飛行自動駕駛 |
| **DimCoord** | — | 左上角 HUD 永遠顯示當前 XYZ + 另一維度對應座標（主世界↔地獄 1:8 換算） |
| **ESP** | — | 透視玩家 / 怪物 / 動物 / 掉落物（牆後也看得到輪廓），用 vanilla glow shader |
| **AutoFish** | — | 自動釣魚：偵測 bobber 下沉收竿 + 自動重丟，純 right-click 動作 |
| **ClickGUI** | `右 Ctrl` | 可拖曳面板，展開每個模組的詳細設定 |

### ClickGUI 設定項

- **KillAura**：範圍、攻擊範圍、攻擊延遲、Single 模式、目標篩選、Raycast、Skip Invisible、平滑旋轉、Body Lock
- **MaceAura**：偵測範圍、攻擊範圍、Hostile / Animals / Players 目標篩選
- **Step**：步高滑桿（1.0×–3.0×）
- **BHop**：速度倍率（0.5×–2.5×）、Low Hop + Jump Height（0.1–1.0）、Boost（1.0–1.5，**每 air tick 複合**，封頂 2.5）、KB Boost（受擊把擊退轉為前進）
- **Timer**：玩家 tick 倍速滑桿（1.0×–3.0×）
- **ElytraGoto**：用 `.nf goto <x> [y] <z>` 設目標、`.nf goto stop` 停止；ClickGUI 有 Safe HP slider 可調保護門檻；動作列顯示距離 / ETA / 主世界↔下界座標對照
- **Scaffold**：Tower / Tower Move / Tower Speed / Fake Hand / Silent Rot / Auto Switch / Switch Back / Place Delay
- **Velocity**：水平 / 垂直擊退倍率
- **Theme Color**：主題色 RGB + 背景透明度
- 所有模組：點擊右側 `[KEY]` 徽章可直接在 GUI 內重新綁鍵
- **Waifu 背景**：放圖片到 `<config>/valencia/waifu.{png,jpg,bmp,gif}` 會顯示在 ClickGUI 左下角

---

## Installation

1. 安裝 **Fabric Loader 0.19.2+**（不需要 Fabric API）
2. 下載最新 JAR → [GitHub Releases](https://github.com/Siesta0217/valencia/releases/latest)
3. 放入 mods 資料夾：
   - **Lunar Client**：`.lunarclient/profiles/lunar/1.21/mods/fabric-1.21.11/`
   - **一般 Fabric**：`.minecraft/mods/`

---

## Keybinds & Config

設定儲存於 `<config>/valencia.json`，可用 `.nf bind` 指令在遊戲內修改：

```
.nf bind <功能> <鍵名>
功能：nofall / xray / maceaura / killaura / noslow / bhop / step
       velocity / fastplace / crit / scaffold / timer / gui
鍵名：N  X  Z  K  G  B  H  C  F  R  J  T  RIGHT_CONTROL  F5  ...
       （GLFW 名稱去掉 GLFW_KEY_ 前綴）

.nf goto <x> [y] <z>    # 設定 ElytraGoto 目標座標並啟動（Y 可省略，預設 64）
.nf goto stop           # 取消目標 + 關掉自動駕駛
```

常用 key codes：`B=66  C=67  F=70  G=71  H=72  J=74  K=75  N=78  R=82  T=84  X=88  Z=90  RIGHT_CONTROL=345`

---

## Requirements

- Minecraft **1.21.11**（Lunar Client 1.21）
- Fabric Loader **0.19.2+**
- Java **21+**
- 相容 Sodium / Iris（純 GuiGraphics 渲染，無 GL11 直接呼叫）

---

## Build

```bash
git clone https://github.com/Siesta0217/valencia.git
cd valencia
.\gradlew.bat assemble
# JAR → build/libs/valencia-1.6.6.jar
```

> **注意**：不要使用 `gradlew build`（test task 在此環境下會壞）。
> `settings.gradle` 的 `rootProject.name` 請勿修改（JDK 25 + Groovy ASM 限制）。

---

## Changelog

### v1.6.6 — ElytraGoto rocket fire 終於修對了
- **真．真．真的修好不發煙火**：之前用 direct `ServerboundUseItemPacket` 但這個 Lunar build 沒有 `suggestedSequence` 方法，sequence 永遠 0 可能造成伺服器忽略
- 改回走 `mc.gameMode.useItem` 但**把 `mc.hitResult` 暫時清成 null**，強制走 `useItemFromInventory` 路徑（不會被「啊你在看方塊」誤判成「在方塊上放置火箭」）
- 展翅成功後 cooldown 設 10 ticks (~500ms)，讓伺服器有時間先把 `isFallFlying` 同步到 server-side，第一發 `useItem` 才不會在 server.isFallFlying=false 時 silent no-op

### v1.6.5 — ElytraGoto 全面 bugfix（6 個問題）
- **A. 真正修好不發煙火**：原本走 `mc.gameMode.useItem`，當 autopilot 把 pitch 拉到 +20° 俯衝時 hitResult 是 BLOCK，火箭被誤判成「放置物品」放到下方方塊上 → 改成直接送 `ServerboundUseItemPacket` 強制走「空中使用」路徑
- **B. 5 條 raycast 改成下傾 10°**：原本 Y 分量 0 純水平掃描，俯衝時看不到斜下方山壁
- **C. groundDanger + ceilingDanger 同時觸發改成 level out**：地獄夾層裡不會再撞天花板
- **D. 高度地板永遠生效**：原本只在 horizDist > 80 才啟用，短程飛行直接撞地
- **E. 展翅失敗偵測**：耐久=0 / 水裡 / levitation 效果造成的展翅失敗，2 秒後動作列會清楚顯示 `elytra 展不開 — 檢查耐久…`
- **F. pitch 表改成 target Y 感知**：用 `atan2(-dy, horizDist)` 算理想角度 + 5° 安全裕度，目標跟你同高度時不會再俯衝撞地

### v1.6.4 — ElytraGoto 拿掉 rocket gate
- 真正的原因找到了：原本 `forwardDanger` 一觸發就 skip rocket，但低空跳下時前方 15m 內必有地形 → 永遠 skip → 慢慢掉
- 而且 forwardDanger 時 pitch 已經被 override 成 -28°/-40°（拉起來逃）— 這時候**就是需要 rocket 推力**才爬得上去
- 拿掉所有 rocket gate（除最後 20m 接近目標時不發以免撞太大力），cooldown 從 60 → 50 tick

### v1.6.3 — ElytraGoto auto-switch to rocket slot
- **真正修好不發煙火**：原本只認主手/副手裡的煙火，現在會掃熱鍵欄 9 格，找到後**自動切到那格**再發射（Inventory.selected 是 private，用反射 + setSelectedHotbarSlot 雙路徑寫入）
- 動作列改顯示：`[Goto] <距離>m  ETA <秒>s  | Y <高度>  | 煙火 <剩餘數>` — 沒煙火時顯示紅字「無煙火!」
- `.nf goto stop` / `setTarget` 時 reset `rocketCooldown = 0`，新航程第一發煙火不會被上次的冷卻擋掉

### v1.6.2 — ElytraGoto bugfixes (no rocket, drop into sea, BHop + elytra loop)
- **修飛海上掉海裡 / 不發煙火**：原本 ground check 用 `Fluid.ANY` 把水也當地面危險，飛海上水永遠在腳下 → rocket 永遠被擋 → 高度地板 -10° 爬不過去就掉水
  - 主世界改成 `Fluid.NONE`（水不算地面），地獄維持 `Fluid.ANY`（岩漿要算）
  - Rocket gating 改成只擋 **forward danger / landing**，ground danger 反而正是要 boost 爬升
  - Altitude floor 改 scale 爬升：每低於 safeY 1 格多 -0.5°，最陡到 -30°
- **修 BHop + 鞘翅死迴圈**：穿鞘翅時 BHop 不再自動跳。原本 BHop 跳 → 空中 + SPACE 按住 → vanilla 自動展翅 → 落地 → 又跳…的循環
- **stop 後加提示**：聊天告訴你「想走路請先脫掉鞘翅」

### v1.6.1 — ElytraGoto bugfix
- 修正視角會在還沒展翅時就被鎖定的問題 — 現在只有 `isFallFlying()` 為 true 才接管 yaw/pitch
- 修正從矮塔跳下展翅不開 — 拿掉 `y < 0` 限制，airborne 第一個 tick 就嘗試 `tryToStartFallFlying()`
- 動作列新增三種狀態提示：`waiting — jump off something` / `deploying elytra…` / 正常飛行
- 註記：vanilla MC 鞘翅展開後**只有觸地才會收回**，這不是模組的問題；`.nf goto stop` 只關掉自動駕駛，不會讓你在空中收翅

### v1.6.0 — AutoFish + ESP
- 新增 **AutoFish**：監聽手中釣竿的 bobber `getDeltaMovement().y`，垂直速度低於門檻（預設 -0.04）視為魚咬鉤 → 右鍵收竿 → 等 12 ticks 後再右鍵重新拋投。可調 Bite Vy / Recast Delay。
- 新增 **ESP**：擴充原本 KillAura 的 `GlowMixin`，全域玩家 / 敵對怪 / 動物 / 掉落物可勾選，被勾的種類會走 vanilla 的 glow outline shader（穿牆顯示輪廓），自己永遠不會發光。

### v1.5.0 — DimCoord HUD
- 新增 **DimCoord** 模組（Visuals 分類，預設 ON）：左上角 HUD 永遠顯示當前座標 + 另一維度的對應座標
  - 主世界：`Overworld 1234, 65, -5678` / `Nether 154, 65, -710`
  - 地獄：`Nether 154, 65, -710` / `Overworld 1232, 65, -5680`
  - F3 debug 畫面或 `mc.options.hideGui` 開啟時自動隱藏

### v1.4.2 — ElytraGoto stronger protection
- **5-ray fan-out forward check** (±25° / ±12° / 0°)，繞角的山 / 樹冠 / 紅石高塔都會被偵測
- **流體偵測**：raycast 改用 `Fluid.ANY`，岩漿池 / 大水面也算障礙會自動拉起
- **Speed-aware lookahead**：速度越快看得越遠（lookahead = max(15, speed × 12 ticks)），boost 中也來得及反應
- **Altitude floor**：主世界 Y < 120 / 地獄 Y < 80 時自動爬升
- **危險時不發煙火**：raycast 命中就停 boost，免得火箭把你噴進牆
- **可調 Safe HP slider** (2–20 HP)：ClickGUI ElytraGoto 展開可調，預設 4 = 2 顆心
- **降落更平滑**：30m 內逐段拉平（20°→8°→-2°），不會 elytra 高速摔死

### v1.4.1 — ElytraGoto safety
- **Dimension lock**：在主世界設目標後跨進地獄會自動暫停（保留目標，回到原 dimension 自動恢復），不會傻傻往錯地方飛
- **障礙閃避**：raycast 偵測前方 14 格 / 下方 6 格 / 地獄天花板 4 格，撞到山或太靠近地面會自動 pull up（pitch -25° ~ -30°）
- **死亡 / 殘血保護**：玩家死亡或 HP ≤ 2 顆心自動關閉，不會繼續發射煙火
- **動作列雙向座標**：在主世界顯示「nether x/8」對照，在地獄顯示「overworld x*8」對照（方便決定要不要 portal）

### v1.4.0
- 新增 **ElytraGoto** 模組：輸入 `.nf goto <x> [y] <z>` 後自動鎖定 yaw 朝目標、依距離調整 pitch、自動發射主手 / 副手的煙火火箭，動作全是 vanilla 合法所以 anti-cheat 抓不到
- 動作列同步顯示：剩餘距離 / ETA / 當前座標 / 主世界↔下界座標換算（除 8）

### v1.3.0
- 新增 **Timer** 模組（預設 `T`）：每 game tick 額外 tick 玩家 N 次，1.0–3.0× 滑桿
- **BHop Boost** 修復：原本只在 jump 那 tick 套用，搭配 LowHop 會被 ground friction (0.546) 吃光；現改成每個 air tick 都複合，並 cap 在 2.5 防止失控
- **KillAura / MaceAura** 修復：原本用 `instanceof Monster` 判斷敵對生物，Phantom（`FlyingMob`）和 Slime（`Mob`）不繼承 Monster 所以被忽略；改用 `Enemy` interface 後三類敵對怪都會被偵測（Slime / Phantom / Ghast / Blaze / 末影龍 etc.）

### v1.2.0
- **BHop** 新增三個子選項：
  - **Low Hop**：自訂跳躍高度倍率（0.1–1.0），讓 bhop 看起來像在地面滑行
  - **Boost**：每次跳躍把水平速度乘上 1.0–1.5（複合，多跳幾次可衝出高速）
  - **KB Boost**：受擊瞬間把擊退向量旋轉成玩家面向方向，把被打變成往前衝

### v1.1.0
- **Step** 修復：改用 `Attributes.STEP_HEIGHT`（取代 1.20.5+ 移除的 `maxUpStep`），新增高度滑桿 1.0–3.0
- **MaceAura** 新增 Hostile / Animals / Players 目標篩選（預設 Hostile + Players）
- **Waifu** 載入修復：原本反射查 `blit` 方法在 Lunar Client classloader 下會失敗，現在以方法名稱掃描取代 `Class` 物件比對；fallback 提示也會顯示實際絕對路徑

### v1.0.0
- 從 NoFall Mod 改名為 Valencia，package 遷移至 `com.valencia`
