# Valencia

Fabric client mod for **Lunar Client 1.21** — utility / combat features.

Latest: **v1.7.31** — [Download JAR](https://github.com/Siesta0217/valencia/releases/latest)

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
| **Fly** | `V` | Motion-based 飛行（伺服器可用）：每 tick 直接設速度，WASD 平移 / SPACE 升 / SHIFT 降，H/V Speed 可調。原版伺服器搭 NoFall AntiKick 不被踢 |
| **Freecam** | `P` | 鏡頭脫離身體自由飛（穿牆偵察）。純 client、零封包、任何伺服器偵測不到；身體原地凍結，強制第三人稱。Speed 可調 |
| **SpearAura** | `U` | 1.21.11 Spear 武器自動鎖定 + silent aim，Jab / Charge / Auto 三模式，自動 step-back 避免太近戳不到 |
| **NoCrash** | — | 鞘翅飛行中前方 raycast 偵測到牆自動減速到 0.4 b/t，server 看到的是自然減速不觸發 wall damage |
| **Hitbox** | — | 其他實體 bounding box 放大讓邊緣攻擊也命中，Players / Hostile / Animals 三開關 |
| **ElytraGoto** | — | 設定 XYZ 目標座標，自動轉向 + 自動發射煙火，遠距離飛行自動駕駛 |
| **DimCoord** | — | 左上角 HUD 永遠顯示當前 XYZ + 另一維度對應座標（主世界↔地獄 1:8 換算） |
| **ESP** | — | 透視玩家 / 怪物 / 動物 / 掉落物：Hitbox（vanilla F3+B 樣式，含 eye-forward 藍線）/ Corners / Outline / Filled 四種框型 + Name / HP / Distance / Tracer 標籤，自動距離 cull |
| **TargetHUD** | — | 上方中央 HUD 面板，顯示當前 KillAura / MaceAura / SpearAura 鎖定目標的名字 / 血條 / 距離 |
| **NameTag** | `Y` | 玩家 / 怪物頭上顯示名字 + 血條 + 護甲值 + 六格裝備（頭/胸/腿/腳/主手/副手）含耐久條，距離自動縮放 |
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

.nf goto <x> [y] <z>           # 當前維度座標（Y 可省略，預設 64）
.nf goto ow <x> [y] <z>        # 主世界座標，在地獄會自動 ÷8 換算
.nf goto nether <x> [y] <z>    # 地獄座標，在主世界會自動 ×8 換算
.nf goto stop                  # 取消目標 + 關掉自動駕駛

.nf bind spearaura <鍵名>      # 重綁 SpearAura toggle key
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
# JAR → build/libs/valencia-1.7.31.jar
```

> **注意**：不要使用 `gradlew build`（test task 在此環境下會壞）。
> `settings.gradle` 的 `rootProject.name` 請勿修改（JDK 25 + Groovy ASM 限制）。

---

## Changelog

### v1.7.31 — Aurora 視覺打磨（回饋：太醜）
- **漸層 1px 條帶**：原 3px 條帶在遊戲內有明顯色階紋，全部改 1px 平滑取樣
- **調色盤降飽和 ~20%**：原霓虹青/紫/粉在遊戲裡像廉價 RGB 燈效，換柔和粉彩調（`4FC3E8`/`9D7BE8`/`E87BB8`）
- **標題列重做**：整條高亮漸層 bar 砍掉——深玻璃底 + 白字品牌，極光只活在標題下一條 1px 細線
- **啟用列 wash 減半**（0x3C→0x26）：開很多模組時不再整面漸層牆，狀態靠左條 + pill + 白字表達
- **掃光變柔**：3 條硬邊（像一根怪線）→ 42px 寬五段漸變光帶，週期放慢
- **邊框收斂**：邊緣 alpha 0xFF→0xD8（染色玻璃感而非電線）、外光暈 0x38→0x28、玻璃底加深（內容對比更好）
- 改動都在共用 `Aurora.java` + `LayoutAurora.java`，**TargetHUD 的 Aurora 風格同步受益**

### v1.7.30 — 清完剩餘的 raw-key 互動 bug
- **BHop**：跟 Fly 同病——讀原始 GLFW WASD，繞過 Freecam 的輸入凍結與開啟中的畫面。修掉兩個情境：(1) freecam 中按 WASD 飛鏡頭，凍結的身體會自動起跳亂跳；(2) **聊天框打字打到 W/A/S/D，身體會原地自動跳**（之前就存在的 bug，跟 Freecam 無關）。KB Boost 不依賴按鍵、保持運作
- **Scaffold**：Tower 讀 `keyJump`，freecam 中按 SPACE 升鏡頭會讓凍結的身體自己疊塔上升。Freecam 作用中 Scaffold 整個暫停（換槽位會自動還原）
- 全 codebase grep 過 raw-key 讀取點：Fly（v1.7.29 已修）、BHop、Scaffold、Freecam 本身（有 screen 檢查）、Keybinds（中央表，本來就該全域生效）——已全數處理

### v1.7.29 — 修 Fly + Freecam 互動 bug
- **Fly 在 Freecam 作用中改為原地懸停**：Freecam 靠 ClientInputMixin 凍結身體輸入，但 Fly 讀的是原始 GLFW 按鍵、繞過了凍結——兩個同開時按 WASD 鏡頭在飛、身體也跟著飛走。現在 Freecam 開著時 Fly 視同 GUI 開啟（速度歸零懸停），身體乖乖留在原地

### v1.7.28 — 補洞：Mace/Spear GCD + Velocity 爆炸擊退
- **MaceAura / SpearAura 補 GCD 旋轉量化**（各自 ClickGUI `GCD` 開關，預設開）：先前只有 KillAura 會把 silent-aim 旋轉 delta 對齊滑鼠靈敏度 GCD 網格，Mace/Spear 送出的任意 float 旋轉正是 anti-cheat GCD 檢查會抓的破綻。現在三個 aura 一致（複用 `KillAuraMod.snapGcd`）
- **Velocity 補爆炸擊退**：TNT / 水晶 / 苦力怕的擊退不走 `SetEntityMotionPacket`，而是包在 `ClientboundExplodePacket.playerKnockback` 裡，先前 Velocity 完全攔不到。新增 `VelocityExplosionMixin` redirect 該 getter 按 Horiz/Vert 縮放——設 0 即完全免疫水晶/TNT 擊退（簽名已 javap 驗證）
- 清死碼：`ClickGuiScreen.gradVert`（從未被呼叫）

### v1.7.27 — TargetHUD Aurora 風格
- **TargetHUD 新增 `4 Aurora` Style**（ClickGUI → Render → TargetHUD → `Style` 改為 0–4）：跟 Layout 3 同一套液態玻璃——玻璃底 + 頂部高光、掃過面板的光帶、流動極光血條（圓角軌）、極光發光邊框；血量靠血條長度 + 紅黃綠染色的 HP 數字判讀
- **抽出共用 `Aurora.java` 繪圖工具**（調色盤取樣、條帶漸層、圓角、玻璃面板、掃光、發光邊框），ClickGUI Aurora 版面改為薄委派，零邏輯複製、行為不變

### v1.7.26 — Aurora Glass 版面（iOS 液態玻璃 + 流動極光）
- **新增 `3 Aurora` Layout**（ClickGUI → Client → Theme → `Layout` 改為 0–3）：
  - **液態玻璃**：低透明度玻璃底（遊戲世界透出來）、頂部 specular 高光遞減、底部內陰影，加一條**緩慢掃過整個視窗的光帶**（液態反光感，低 alpha 蓋在內容上）。GuiGraphics 沒有真模糊，這是逼近版
  - **流動極光漸層**（青→紫→粉循環調色盤，3px 條帶取樣 + 時間相位，整體 8 秒一輪）：跑在標題列、active 分頁底線、啟用列底色、pill 開關、滑桿填色與整圈視窗邊框（外加 1px 低 alpha 外光暈）
  - 版面：漸層標題列（深色字）+ 橫向分類分頁 + 圓角模組卡片（就地展開設定）+ 動畫 pill + 圓鈕滑桿，scissor 裁切捲動
  - 配色固定走極光調色盤（不吃 accent），與 `GUI Style` 無關
- Layout slider 上限 2→3；幾何/命中判定獨立，Panels / Sidebar / Tenacity 不受影響

### v1.7.25 — Tenacity 版面（完整重做，不只配色）
- **新增 `2 Tenacity` Layout**（ClickGUI → Client → Theme → `Layout` 改為 0–2）：一個獨立幾何的單一視窗，不是換色——
  - 圓角視窗（2px 削角）+ accent 外框、accent 標題分隔線、可拖標題列、右上 `x` 關閉
  - 左側分類側欄（選取有 accent 左條 + 染色底）
  - 右側**模組卡片**：圓角卡、啟用有 accent 左條、右邊 **pill 開關**（滑塊會滑動的動畫）、有設定的顯示 `+/-`
  - 點卡片本體**就地展開**（accordion）顯示設定，用重做的 widget：**滑桿帶可拖白色圓鈕** + 數值、設定列 pill 開關、按鍵做成圓角 chip
  - 卡片清單 `enableScissor` 裁切捲動、獨立捲軸
  - 幾何/命中判定完全自帶，但顏色沿用當前 GuiSkin（配 Dark/Tenacity 配色最對味）
- 跟 `1 Sidebar` 一樣是獨立 render 路徑，與 `GUI Style` 配色正交

### v1.7.24 — Tenacity ClickGUI 配色
- **新增 `3 Tenacity` GUI Style**（ClickGUI → Client → Theme → `GUI Style` 改為 0–3）：現代 ghost-client 風格——近黑冷色底，accent 穿過每個邊緣（header 底線、面板外框、widget 邊框、捲軸全部 accent 染色），整體像有一圈 accent 外光暈（Tenacity 招牌）；品牌字改純 accent（非彩虹）。accent 仍吃使用者設定，配 teal/cyan 最對味
- 只動 `GuiSkin.java`（加 `tenacity()` factory + `case 3`）與 GUI Style slider 上限 2→3；Panels / Sidebar 兩種版面都套得上

### v1.7.23 — Freecam 模組（純 client 鏡頭脫離）
- **新模組 Freecam**（Render，預設鍵 `P`）：鏡頭脫離身體自由飛、穿牆偵察。**完全 client-side、不送任何封包 → 任何伺服器/AC 都偵測不到**
  - `FreecamCameraMixin`（`Camera.setup` TAIL）：覆寫相機位置；旋轉沿用玩家視角，所以滑鼠照樣瞄 freecam（javap 確認 `setup(Level, Entity, boolean, boolean, float)` / `setPosition(double,double,double)`）
  - `ClientInputMixin`（`ClientInput.tick` TAIL）：啟用時清空移動輸入（`keyPresses=Input.EMPTY`、`moveVector=Vec2.ZERO`），身體原地凍結、WASD 只開鏡頭
  - `FreecamMod.tick()`（TickMixin）依看向 yaw 用原始 WASD/SPACE/SHIFT 飛鏡頭；啟用時強制 `THIRD_PERSON_BACK` 看得到自己身體、不會有浮空的手
  - `Speed` 滑桿（0.2–5.0 b/t）+ 可綁鍵；接進 Keybinds（toggle/panic）與 ArrayList。**啟用狀態刻意不持久化**（不想一進遊戲就在脫離鏡頭）。已知限制：相機只能看到身體周圍已載入的區塊

### v1.7.22 — Fly 模組（motion-based，伺服器可用）
- **新模組 Fly**（Movement，預設鍵 `V`）：每 tick 直接 `setDeltaMovement`，看向方向 + 輸入控制（WASD 平移、SPACE 升、左 SHIFT 降），注入點同 BHop 的 `aiStep` HEAD
  - 為什麼不用 `Abilities.flying` 旗標：survival 伺服器的 abilities 由 server 掌控、會被 resync 蓋掉，所以走 velocity
  - 中性垂直不會下沉：moveRelative 不動垂直分量、重力只改下一 tick 存量而我們每 tick 覆寫；fallDistance 每 tick 歸零
  - `H Speed` / `V Speed` 滑桿（0.2–5.0 b/t）+ 可綁鍵；接進 Keybinds（toggle/save/panic）與 ArrayList
  - **原版伺服器**（`allow-flight=false`）請搭 NoFall（AntiKick 或 Always），不然持續飛會被飛行踢

### v1.7.21 — NoFall AntiKick（閃原版飛行踢出）
- **NoFall 新增 `AntiKick`**（Movement → NoFall → `AntiKick`，預設開）：原版伺服器（`allow-flight=false`）持續離地 ~80 tick 會踢 `Flying is not enabled`。NoFall 現在追蹤連續離地 tick，在 Smart 模式下每 60 tick 強制補一次 `onGround=true` 把伺服器計數器歸零，持續飛/懸停不會被踢。Always 模式本來每 tick 補就免疫；fall-flying（鞘翅）本身豁免不動
- 這不是 anti-cheat bypass——原版沒 AC 可關，只是不去觸發內建的飛行 sanity 檢查

### v1.7.20 — KillAura 低偵測：旋轉 GCD 吸附 + CPS 抖動
- **GCD**（KillAura，預設開）：把 silent-aim 旋轉吸附到玩家**真實滑鼠網格**。vanilla 轉視角是 `rawDelta × f³ × 8.0 × 0.15`（`f = 靈敏度×0.6+0.2`），所以真人每次旋轉 delta 都是 `f³ × 1.2` 的整數倍。`mouseGcd()` 讀玩家當前靈敏度（javap 確認 `Options.sensitivity()`），`snapGcd()` 把瞄準 delta 對齊到該網格，伺服器收到的旋轉量跟真人滑鼠無法區分——破解現代 AC 的 GCD / improbable-rotation 檢查
- **CPS Jit**（0–5，預設 0）：攻擊延遲加 0..n 隨機 tick，避免攻速完美週期（constant-CPS flag）

### v1.7.19 — NoFall 智慧 spoof 模式
- **NoFall 新增 `Mode`**（ClickGUI → Movement → NoFall → `Mode` 0–1）：
  - `0 Always`：每次 `sendPosition` 都把 onGround spoof 成 true（原本行為）
  - `1 Smart`：只在 `Entity.fallDistance > 2.0`（真的會摔到傷害）時才 spoof，走路/小跳照實送 onGround，把「半空卻 onGround=true」這個現代 anti-cheat 最愛抓的窗口縮到最小
- `fallDistance` 是 Entity 的 public double（javap 對 1.21.11 loom jar 確認）

### v1.7.18 — TargetHUD 環形改放真．實體模型
- **Style 3 Ring 中央改成渲染目標實體模型**（取代原本的 HP% 數字），用 vanilla `InventoryScreen.renderEntityInInventoryFollowsMouse`（簽名以 javap 對 1.21.11 loom jar 確認），「滑鼠」釘在環中心讓實體正面朝前
- 包 try/catch：萬一這支 API 在 HUD pass 丟例外，靜默 fallback 回 HP% 文字，HUD 不會 crash。環半徑 16→20 讓頭像看得清

### v1.7.17 — 真．不同版面（ClickGUI 側欄 + TargetHUD 環形）
- 這次是**整套版面重寫**，不是換色。原版面都保留為 0 號且仍是預設，不切就完全不變
- **ClickGUI 版面**新增獨立維度 `Layout`（ClickGUI → Client → Theme → `Layout`），與配色 `GUI Style` 正交：
  - `0 Panels`：原本的 Raven 散落可拖曳面板（不變）
  - `1 Sidebar`：單一置中視窗——左側分類分頁、中間模組清單（**綠點=開關、列其餘=選取**）、右側該模組設定。標題列可拖曳、右上 `x` 關閉、清單與設定各自獨立捲動。幾何與命中判定完全獨立，但沿用同一份模組資料 / GuiSkin 配色 / slider・toggle・keybind widget，所以 Light / Glass 配色一樣套得上
  - `2 Tenacity`：圓角視窗 + 分類側欄 + 模組卡片 + pill 開關（滑動動畫）+ 就地展開設定（滑桿帶圓鈕）。完整重做的幾何，非換色（v1.7.25）
  - `3 Aurora`：iOS 液態玻璃 + 流動極光漸層（掃光、漸層標題列、橫向分頁、極光 pill/滑桿、發光邊框）（v1.7.26）
- **TargetHUD** 新增 `Style 3 Ring`（`Style` 範圍變 0–3）：環形血量圈、中央顯示 HP%、右側名字 / 血量 / 距離
- rebind 狀態統一成單一 `rebindTarget`，兩種版面共用按鍵捕捉

### v1.7.16 — TargetHUD / ClickGUI 可切換風格
- **TargetHUD Style 選擇器**（ClickGUI → TargetHUD → `Style` 0–2，預設 0）：
  - `0 Classic`：原本的方框（完全沒變）
  - `1 Compact`：單行精簡（名字 / 血量 / 距離）＋ 底部 2px 血條
  - `2 Gradient`：較大面板、切角假圓角、紅→綠漸層血條
  - 三種風格共用同一套目標選擇（aura 鎖定 → 準星 → 最後攻擊者），只差繪製
- **ClickGUI 換膚系統**（ClickGUI → Client → Theme → `GUI Style` 0–3，預設 0）：
  - `0 Dark`：原本的 Raven 深色配色（完全還原，無變化）
  - `1 Light`：淺色面板 + 深色文字，啟用列改平鋪 accent 色
  - `2 Glass`：高透明 accent 染色玻璃風
  - `3 Tenacity`：近黑冷色底 + 全域 accent 描邊外光（v1.7.24 加）
  - 新增 `GuiSkin.java` 集中所有顏色；ClickGuiScreen 每幀依 `guiStyle` 解析，**開著 GUI 也能即時切換**
- 兩個 Style 都預設 0，舊使用者更新後外觀完全不變

### v1.7.15 — ArrayList HUD 改吃 Keybinds 中央表（消除平行清單）
- **重構（無行為變化）**：ArrayList 原本手抄一份 20 個模組的清單，新模組得記得同步。改成直接讀 `Keybinds.TOGGLE_ENTRIES`（所有有 keybind 的模組）＋ 一份只剩 5 項的 `EXTRAS`（無 keybind 的 Hitbox / NoCrash / AutoFish / ElytraGoto / ESP）。往 `Keybinds.TOGGLES` 加一行模組就會自動出現在 ArrayList，兩邊不再會漂移。顯示的模組集合與渲染完全不變

### v1.7.14 — ArrayList HUD
- **ArrayList（Render）**：右上角顯示目前開啟的模組清單，依字寬遞減排成階梯狀。可選 **Rainbow**（垂直彩虹，預設開）或主題 accent 色、可選 **BG** 背景底（預設開）。掛在 `Gui.render` TAIL，狀態跨 session 保存。純 HUD 讀數（TargetHUD / DimCoord）與 ArrayList 自己不列入清單

### v1.7.13 — 程式碼整理（無行為變化）
- **VelocityMixin 加說明註解**：`LivingEntity.knockback` 這條只在 SP / client-side 怪攻擊時觸發；多人伺服器走 velocity packet（`VelocityPacketMixin`，v1.7.5 加的）才是實際路徑。保留此 mixin 供 SP 用，註明別誤刪
- **NoSlowMod 補 `isActive()`**：與其他模組形狀一致，三個 redirect 改用 `isActive()`（觸發時 player/level 必非 null，等價於 `isEnabled()`，無行為變化）
- **AutoFish 吞錯改 log 一次**：`useItem` 真的壞掉時印一次 stderr 警告（之前 `catch(Throwable ignored)` 完全靜默）

### v1.7.12 — Aura 一致化 + NameTag 獨立色 + TickMixin 資料驅動
- **MaceAura / SpearAura 補齊 Raycast / Skip Inv / Smooth / Max Turn**：先前只有 KillAura 有穿牆檢查、隱形過濾、平滑轉頭，現在三個 aura 行為一致（複用 `KillAuraMod.canSee` / `smoothRotation`，可在 ClickGUI 個別開關）。Spear 想吃滿傷可關 Smooth
- **TargetHUD 最後攻擊者 fallback**：沒 aura、準星也沒指人時，改顯示最後打你的怪，整場戰鬥面板不消失
- **NameTag 獨立顏色**：新增 `Theme Col` 開關（預設開＝跟隨 ClickGUI 主題色）＋ 關掉後可用獨立 R/G/B，NameTag 與 ClickGUI 可不同色
- **重構（無行為變化）**：TickMixin 15 個手寫 `prevX` toggle 樣板抽成資料驅動的 `Keybinds` 表（加模組只要一行）；三 aura 重複的陣營過濾抽成 `AuraTargeting.factionAllowed`

### v1.7.11 — AutoTotem + Panic key + TargetHUD 準星 fallback
- **AutoTotem（Combat，預設鍵 `O`）**：自動把不死圖騰補進副手，圖騰爆掉後下一 tick 立即補回。只在沒開任何容器介面時動作，走 vanilla 同一條 `handleInventoryMouseClick` PICKUP 鏈（用 `InventoryMenu.SHIELD_SLOT`），伺服器端看到的是正常的拿取動作。狀態跨 session 保存
- **Panic key（Client，預設鍵 `DELETE`）**：一鍵關閉所有會影響玩法的模組（KillAura/Mace/Spear/Crit/Scaffold/Timer/BHop/Step/Velocity/FastPlace/NoSlow/AutoTotem/ElytraGoto/AutoFish/NoCrash/NoFall），瞬間切回 legit
- **TargetHUD 準星 fallback**：沒有任何 aura 在運作時，改顯示準星指向的生物，HUD 單獨使用也有意義
- 兩個新鍵都可在 ClickGUI 重綁（Combat → AutoTotem、Client → Panic）

### v1.7.10 — ESP 盒子穿牆修正
- **ESP Hitbox 盒子改為 always-on-top（穿牆）**：v1.7.6 把盒子搬到 vanilla gizmo 管線後，盒子變成有深度測試 → 被地形擋住看不到（標籤是 2D HUD 仍穿牆，所以出現「有名牌沒盒子」）。`Gizmos.cuboid(...)` 接上 `setAlwaysOnTop()` 關閉深度測試，恢復標準 ESP 穿牆行為

### v1.7.9 — ESP / NameTag 檢查整理
- **ESP 改為 Hitbox-only，清死碼**：先前 `Corners/Outline/Filled` 三種樣式選不到（啟動時 `espStyle` 被硬寫成 Hitbox、ClickGUI 也沒選擇器），等同死碼。承認 v1.7.6 gizmo 方向，砍掉 `drawFilled/drawOutline/drawCorners`、死欄位 `espStyle`、`ESPMod.style` 概念與啟動的強制覆寫
- **ESP 揭露隱形實體**：移除 ESPGizmoMixin 的 `isInvisible` 過濾。先前隱形怪「沒盒子卻有浮空名牌」的不一致已消除，現在盒子+標籤都會顯示隱形/潛行目標
- **ESP Glow 獨立開關（預設關）**：先前只要 ESP 開著，所有目標都吃 vanilla 發光描邊且無法關。改成 ClickGUI 的 `Glow` 開關，預設關，盒子與發光解耦
- **NameTag 距離上限**：新增 `MaxDist` 滑桿（預設 64m，16–128 可調），比照 ESP 剔除遠處名牌，人多場景不再無上限渲染
- **小整理**：`ESPMod.colorFor(Entity)` 沒用到的參數移除 → `color()`

### v1.7.8 — Audit batch 1：反射清除 + Mace/Spear reach 修正 + GLFW 快取
- **MaceAura / SpearAura reach 修正**：原本用 `player.distanceTo()`（中心對中心），高個 / 在空中的怪會 ghost swing。改用 KillAura 既有的 `reachDistSq`（眼睛 → hitbox 最近點），跟 server 的攻擊距離判定一致。Mace 走平方比較，Spear 因有 sweet-spot 內插改取 `sqrt(reachDistSq)`
- **Scaffold 移除反射**：`Inventory.selected` 反射欄位改用本 build 確認存在的 public `getSelectedSlot()` / `setSelectedSlot(int)`；`onDisable` 改 `Minecraft.rightClickDelay` 的反射也拿掉，改設旗標讓已 `@Shadow` 該欄位的 `TickMixin` 重置
- **GLFW 反射快取**：`ModConfig.keyName` 原本每次（ClickGUI 每幀每個 KeyS widget）都掃一遍 `GLFW.class.getFields()`。改成一次性建好 code↔name 雙向表查表；`ChatMixin.resolveKey` 也改走 `ModConfig.keyCode`
- **NoFallMixin**：修掉一行解碼錯的亂碼注釋

### v1.7.7 — NameTag 美化重做
- **Panel**：四角各 shave 1 px 做假圓角；頂部一條 accent 漸層 stripe，從中央滿 alpha 線性 fade 到兩側 45% — 不用 alpha-blend 對角線就有「軟高光」感
- **底部 1 px 深色帶**：增加深度錯覺
- **Header**：name 加 drop shadow；右側顯示距離（`5.3m`）灰字
- **Stat row**：HP text（依血量比例調色）+ 護甲值（`⛨ 8`，淡藍色），左右分佈
- **HP bar**：4 px 高，紅 → 橘 → 綠**漸層**色（不是離散 3 段），頂部 1 px highlight stripe 加形狀感
- **Equipment row**：armor 4 格 + hands 2 格中間加細隔線；每格本身也加 top highlight + bottom shadow inset
- **最小寬度** 64 px，短名字面板不會擠
- 排版用累積式 cursorY，每段獨立決定 row gap，整體間距更鬆但不浪費

### v1.7.6 — ESP Hitbox 跑到 vanilla 的 Gizmos 管線（完美 smooth）
- **Bug**：v1.7.5 的 Hitbox style 還是 2D HUD 投影畫的，跟 vanilla F3+B 看起來像但不會「黏」在實體上 — 因為：
  - HUD pass 在 world pass 之後跑，camera snapshot 不一樣
  - `projectPointToScreen` 內部用 `getFov(cam, 0f, true)` → partialTick=0
  - 實體位置用 current partialTick
  - 三個誤差疊起來變成可見的浮動 / swim
- **修法**：新增 `ESPGizmoMixin`，`@Inject` `DebugRenderer.emitGizmos` 的 TAIL，把 ESP 目標的 render-interpolated AABB 用 `Gizmos.cuboid(aabb, GizmoStyle.stroke(color))` 丟進 vanilla 的 `collectedGizmos` collector
  - LevelRenderer 包了 `Gizmos.withCollector(...)` scope，所以我們的 cuboid 跟 F3+B hitbox 落在**同一個 collector**，**同一個 world render pass**，**同一組矩陣**
  - 結果：ESP box 跟 vanilla F3+B 完全同步，frame-perfect smooth
- ESPRenderer 的 HITBOX case 改成 no-op（box 由 gizmo 畫），但 Name / HP / Distance / Tracer 標籤還是走 HUD pass
- **撤掉 v1.7.5 的 eye-forward 藍線**（使用者要求）
- Filled / Outline / Corners 三個 style 維持 HUD-based 不動（gizmo 適合 wireframe，不適合 filled overlay）

### v1.7.5 — Velocity packet fix + TargetHUD + ESP vanilla F3+B look
- **Velocity bug**：開 0 / 0 還是會被擊退
  - **根因**：mp 上 KB 是 server 跑 `LivingEntity.knockback()`，然後送 `ClientboundSetEntityMotionPacket` 給 client，client 直接 `entity.lerpMotion(packet.getMovement())` 套用。**完全繞過 `LivingEntity.knockback()`** → 舊 VelocityMixin 永遠不會被觸發
  - **修法**：新增 `VelocityPacketMixin`，`@Redirect` `handleSetEntityMotion` 裡的 `lerpMotion(Vec3)` 呼叫，目標是 local player 時把 motion 用 horizontal / vertical 比例縮放後再套
  - 舊 VelocityMixin 保留（攻擊本機 mob 的 server-side 路徑還是會用到）
- **ESP 改成 vanilla F3+B 風格**
  - 預設色改成白色（255/255/255），跟 vanilla F3+B 一致
  - Hitbox style 加上 **eye-forward 藍線**：從目標 eye position 沿 viewVector 拉 2 格的 0xFF55AAFF 線，跟 vanilla F3+B 那條藍線一模一樣
  - Style 預設仍是 HITBOX（NoFallMod 強制鎖定）
- **新模組 TargetHUD**（分類 RENDER）
  - 上方中央面板，顯示當前 KillAura / MaceAura / SpearAura 鎖定目標的：名稱 + 距離（同行左右）/ HP 條 / HP 文字
  - 三個模組任一啟用且有 `glowTarget` 就會顯示，沒有就隱藏
  - 配色吃 Theme accent + bgAlpha
- ClickGUI 加上 TargetHUD toggle、`targetHudEnabled` 持久化到 config

### v1.7.4 — Projection 改吃 vanilla `projectPointToScreen`
- **Bug**：v1.7.3 的手算投影（camera-relative + invRot.conjugate + perspective matrix m00/m11）數學上看似對，實際在這 Lunar 1.21.11 build 完全顯示不出來。猜測是 camera rotation 或 projection matrix 的 conventions 跟 upstream Yarn 1.21 不完全一致
- **修法**：放棄自己算矩陣，直接呼叫 `mc.gameRenderer.projectPointToScreen(Vec3)` — 這是 vanilla 自己給 `TrackedWaypoint.Projector` 用的 API，跟 world render 用的是同一條矩陣，conventions 一定對得上
- 回傳 NDC `(x ∈ [-1,1], y ∈ [-1,1], z = depth)` → 自己 map 到 GUI-scaled 像素：`sx = (ndc.x+1) * 0.5 * viewW`，`sy = (1-ndc.y) * 0.5 * viewH`
- **背面偵測**：NDC 自己看不出來「在相機後面」（perspective divide 會把符號翻過來給出貌似合理的 screen coord）→ 改用 `(target - camPos) · camera.forwardVector()` 算前進距離，<0.05 格就 reject
- **AABB 近平面 clipping** 改在 world space 做：8 角各自存 forward-distance，跨面的邊用 lerp `t = (fFront - NEAR) / (fFront - fBack)` 算 world-space 切點，再投影
- ESPRenderer / NameTagRenderer 完全沒改 — public API（`Frame / projectPoint / projectAabb / ScreenAabb`）一致

### v1.7.3 — ESP / NameTag 從頭重寫，共用投影管線
- **新增 `Projection.java`**：world→screen 投影集中到一個工具類，Frame snapshot + projectPoint + projectAabb 三個入口；AABB 投影內建 12 邊 near-plane clipping，回傳 `ScreenAabb`（2D bounding rect + per-edge 線段 + valid mask），ESP / NameTag 共用
- **ESPRenderer 重寫**：
  - 投影 / clipping 完全外包給 `Projection`，render loop 只做「filter → projectAabb → reject → 畫框 → 畫附件」
  - 四個 style 各自獨立 method（`drawHitbox / drawFilled / drawOutline / drawCorners`），不再共用一條 fall-through 邏輯
  - **Tracer 起點改成準星**（畫面中央）而不是畫面底部中央，原本拉線到頭都歪掉
  - 拆掉舊的 `OUT_AX/OUT_AY/OUT_BX/OUT_BY` static globals
- **NameTagRenderer 重寫**：
  - 同樣外包投影給 `Projection.projectPoint`，不再自己手算 `1/-z`
  - **距離縮放新公式** `clamp(10/max(5,dist), 0.5, 1.2)`：近距離（≤8 格）不再爆大，遠距離（20+ 格）保留 0.5× 可讀性
  - Layout 拆成 `drawTag / drawBorder / drawSlot` 三個 method，header / hp bar / icon row 各自獨立 cursor
  - 移除 NameTagRenderer 自己的 `NEAR_Z` 重複定義，統一吃 `Projection.NEAR_Z`
- 視覺等價（除了 Tracer 起點改變），純架構整理

### v1.7.2 — NameTag 補上 near-plane 防護
- NameTag 跟 ESP 同個病：`projectPoint()` 用 `rel.z >= -0.05` 當門檻 → 近距離 `1/-z` 爆炸，tag 飛到天邊或不顯示
- NameTag 改用 `NEAR_Z = -0.5`（比 ESP 嚴格，nametag 在 0.5 格內顯示也沒意義）
- 投影輸出加 ±100000 clamp

### v1.7.1 — ESP 砍掉重寫：near-plane clipping，修全螢幕綠線 bug
- **Bug**：站在實體附近 / 實體 AABB 跨越相機平面時，部分角的 `rel.z` 接近 0，投影 `1/-z` 爆炸 → 螢幕被巨大綠線塞滿
- **修法**：對每條邊單獨做 near-plane clipping
  - 兩端都在前 → 直接投影
  - 兩端都在後 → 跳過整條邊
  - 一前一後 → 算邊跟 `z = NEAR_Z (-0.1)` 平面的交點，用交點當端點
- **2D 樣式的 bounding rect** 改成 clip 後 12 邊端點的 min/max（不是角點的 min/max），確保跨越相機平面的盒子也有合理的 2D 框
- **projectX/Y 加 ±100000 clamp**：即便 `-rel.z = 0.1` 還是會放大 10×，clamp 防止整數溢位
- 新增 `e.isAlive()` 過濾，跳過死掉的實體
- 移除過時的 `int[] XS/YS + boolean[] VIS` scratch（不再需要每角獨立可見性）
- 邊處理單一 loop：clip → project → 更新 bounds → 順便畫 hitbox edge，省一次遍歷

### v1.7.0 — ESP 設定面重寫
- 設定面四個分區：**Targets / Box / Labels / Color**
- **去掉** Red / Green / Blue 三個獨立滑桿 + Chroma boolean → 改成單一 **Hue** 滑桿（0–360°，HSV saturation=1, value=1）+ **ColorMd** 選擇器
- **ColorMd**（0–2）：
  - `0 Single`：所有實體用 Hue 滑桿的顏色
  - `1 Category`：玩家紅 / 敵對橘 / 動物綠 / 物品黃，固定 hue
  - `2 Chroma`：色相按時間循環，速度看 ChrSpd
- `ESPMod.colorFor(Entity)` 統一出口，render loop 每實體現算
- 舊 config 欄位 `espBoxR/G/B/espChroma` 移除，Gson 自動忽略殘留 JSON

### v1.6.30 — ESP Chroma 跑馬燈色
- 新增 **Chroma** 開關（ClickGUI ESP）：開啟後 box / name / hp 顏色按 hue 自動循環，現有 Red/Green/Blue 滑桿被忽略
- **ChrSpd** 滑桿：色相循環速度（0.1–3.0 圈/秒），預設 0.5
- 實作：`ESPMod.boxColor()` 在 chroma 模式下用 `System.currentTimeMillis()` × speed 算 hue，丟給 `java.awt.Color.HSBtoRGB`

### v1.6.29 — ESP / NameTag 修 zoom mod 投影跑掉
- 改從 `GameRenderer.getProjectionMatrix(partialTick)` 抽 `m00 / m11`，等於 `1/(tanHalfFov*aspect)` 跟 `1/tanHalfFov`
- 以前讀 `mc.options.fov()`（玩家設定的 base FOV），zoom mod 是修改渲染時的 effective FOV，沒抓到 → ESP 框 + NameTag 都會偏移 / 大小不對
- 改用渲染矩陣後，任何走 `getProjectionMatrix` 的 FOV 改動（OptiFine zoom、Lunar 內建 zoom、Sodium 系 zoom）都自動吃到
- `partialTick` 從 `mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)` 拿，跟世界渲染同步
- NameTagRenderer 同步修一樣的 bug

### v1.6.28 — ESP 效能優化
- **線繪製改用旋轉 quad**：一條對角線從 N 個 `g.fill`（每像素一個 quad）改成 1 個 fill — 用 `Matrix3x2fStack.translate + rotate` 把長度 × thick 的軸對齊長方形旋轉到目標角度。Hitbox 樣式重度受益（12 邊 × 多實體 × N 像素 → 12 個 fill / 實體）
- **軸對齊 fast path**：dx=0 或 dy=0 的線直接用單一 `g.fill`，跳過 atan2 / 矩陣
- **scratch buffer**：每實體 8 個 `new int[2]` + 1 個 bounding rect `new int[]` → 改成 `static int[8] XS/YS + boolean[8] VIS + int[4] BOUNDS_OUT`，render 是 single-thread 安全
- **重用 Vector3f**：投影 8 個角從 8 個 Vector3f allocation → 1 個 static REL.set()
- **去掉 `String.format`**：距離文字從 `String.format("%.0fm", dist)` → `(int) dist + "m"`，少一次 formatter 機制
- **HpBar 邊界 clamp**：實體靠近螢幕左邊時 `barX1 < 0` 直接跳過，不畫到螢幕外
- 視覺零變化，純效能

### v1.6.27 — ESP Hitbox 樣式（3D 線框）
- Style slider 新增 `3 = Hitbox`：實體 AABB 完整 12 條邊線框，跟 vanilla F3+B 一樣的視覺
- 新預設值改為 Hitbox
- 投影流程改成「8 個角各自存 (sx, sy)」，2D 樣式仍從這 8 點算 min/max bounding rect，效能等價
- 12 邊用 Bresenham 畫；endpoint 同側螢幕外 cheap-reject，避免畫到螢幕外
- Tracer 也改用同一條 Bresenham，支援 Thick 滑桿粗細
- 任一 endpoint 在相機後 (`rel.z >= -0.05`) 的 edge 直接跳過（無 near-plane clipping）

### v1.6.26 — ESP 重寫：三種框型 + Name/HP/Distance/Tracer + 距離 cull
- **三種 box style**（Slider 切換）：
  - `0 = Corners`（預設）：四角 L 型括號，依框大小自動縮短長度
  - `1 = Outline`：完整外框
  - `2 = Filled`：半透明填充 + 外框
- **Line Thickness 滑桿**（1–3 px）：外框 / 角線粗細統一控制
- **Name**：實體名稱黑底白字置中顯示在 box 上方
- **HP**：LivingEntity 左側 3px 垂直血量條，紅 / 橘 / 綠依比例
- **Distance**：box 下方顯示 `%.0fm`，灰字
- **Tracer**：從畫面底部中央拉一條 Bresenham 線到 box 底部中央
- **MaxDist** 滑桿（16–200 格）：用平方距離預先 cull，省掉超出範圍實體的 8 點投影
- **新效能 guard**：
  - 投影後框 < 4 px → 跳過（畫不出細節）
  - 整個 box 完全在螢幕外 → 跳過
  - 8 個角至少 2 個在相機前才畫（避免邊緣情況奇怪 box）
- 沿用 ESPRenderer 原本的 `Camera.position()` + `rotation().conjugate()` 投影流程

### v1.6.25 — NameTag 模組（頭頂裝備 + 血量資訊）
- 新增 **NameTag**（預設 `Y`，分類 RENDER）：在目標頭頂繪製資訊面板
  - 第一行：實體名稱 + HP 文字（`14/20`，含吸收 `+4` 顯示）+ 護甲值 `[8]`
  - 第二行：HP 條，紅→橘→綠依比例上色
  - 第三行：六格裝備圖示（頭/胸/腿/腳/主手/副手），含 vanilla 耐久條
- 設定：Players / Hostile / Animals 目標、Armor / Hands / Durability / HP Bar / HP Text 五個顯示開關、Scale 滑桿（60–160%）、Key 綁定
- 距離自動縮放：8 格以內最大，越遠 tag 越小（clamp 0.6–1.4）
- 螢幕背後 / 過近實體自動隱藏（rel.z >= -0.05f reject）
- 用 `Matrix3x2fStack` push/scale/translate 而不是手動算座標，避免在不同 GUI scale 下偏移
- 沿用 ESPRenderer 的 camera 投影流程（`Camera.position()` + `Camera.rotation().conjugate()`）

### v1.6.23 — ClickGUI 重寫 Discord 風格 + ESP 2D overlay（Name / Health / Corner Box）
- **ClickGUI 完全重寫**：從 Faiths client 移植 Discord 風格三面板佈局
  - 左側欄：分類按鈕 + Discord 風格 pill indicator（懸停 / 選中有白條）
  - 中間：模組列表，`#` 前綴 + 綠色側邊條顯示啟用狀態，`›` 箭頭表示有子設定
  - 右側：設定面板，滑桿 / 開關 / 按鍵綁定；模組名稱 + ON/OFF 切換按鈕在頂部
  - 深色 Discord 配色（#1E1F22 / #2B2D31 / #313338）
  - 左鍵選模組顯示設定、右鍵直接切換開關
  - 可拖曳視窗 + 滾輪捲動
- **ESP 新增 2D overlay**：從 Faiths PlayerESP 移植
  - **Name Tag**：實體名稱置中顯示在頭頂，黑底白字
  - **Health Bar**：左側垂直血量條，紅→綠漸層 + 受傷時顯示數值
  - **Corner Box**：四角框風格（取代完整矩形），帶黑邊描邊
  - 所有 overlay 走 AABB 8 角投影到螢幕座標，跟 Show Box wireframe 共用投影邏輯

### v1.6.22 — ESP Show Box 升級成 3D wireframe（F3+B 風格）
- 從 v1.6.21 的 2D 矩形升級成「投影 8 個角 + 畫 12 條邊」的 3D 線框，外觀跟 vanilla F3+B 一樣
- AABB 8 個角各自做 perspective projection → 螢幕座標，再對 12 條邊 (`EDGES` 表) 跑 Bresenham 1px line
- 角落在 camera 後方的標記 `Integer.MIN_VALUE`，畫線時對應的邊整條 skip — 走進實體裡某些邊會少，但正常觀看距離下完整線框照畫
- 預設顏色從 cyan 改成 lime green (`0xFF80FF40`) 符合 F3+B 配色
- 早退機制：兩端點都在同一螢幕邊外的邊直接 skip；單邊長 > 4000 px hard cap 避免一條失控的邊拖垮整個 frame
- 效能：12 邊 × ~100 px × N entity，typical ~10 entities 在 60fps 沒問題；大量實體場景可能會卡（用 GuiGraphics.fill 而不是 native GL line batch 的代價）

### v1.6.21 — Hitbox 拆三開關（KillAura 風格）+ ESP 新增 2D hitbox 框顯示
- **Hitbox 三開關**：`Players` / `Hostile` / `Animals`（取代原本的 `Players Only` 反向開關），跟 KillAura / ESP 同一套介面。預設全開
- **ESP 新增 Show Box**：開啟後在每個 ESP 目標的螢幕投影位置畫 2D 矩形外框
  - 投影邏輯：取 `entity.getBoundingBox()` 8 個角，用 camera 反向四元數 + perspective projection 換算到 GUI 座標，找 min/max 圍成矩形
  - **自動跟著 HitboxMod 大小變化** — 因為 `getBoundingBox()` 就是 HitboxMixin mixin 的回傳，所以 HitboxMod 拉大 expand 滑桿時 ESP 框跟著變大
  - 跟現有 glow ESP 獨立 — 兩個都可同時開、或只開一個
  - 用 2D 而不是 3D wireframe 是因為 1.21.11 新 framegraph render pipeline 比較難 mixin；2D overlay 走 HudMixin 已驗證的渲染路徑
- 顏色預設 cyan (`0xFF00E5FF`)，可在 `<config>/valencia.json` 改 `espBoxColor`

### v1.6.20 — Hitbox 預設改放大全部實體（PvE 友善）
- `hitboxPlayersOnly` 預設 `true → false`：新安裝直接放大玩家 + mob + 動物，伺服器 PvE 也受益
- 舊安裝者要更新行為：ClickGUI → Hitbox → 把 Players Only 那欄關掉（或刪掉 `<config>/valencia.json` 重置）
- 純 PvP 想最小化 mob AI 副作用的話手動把 Players Only 打開即可

### v1.6.19 — 新模組 Hitbox：放大其他實體 bounding box 讓邊緣攻擊也命中
- vanilla 近戰 pick 是 `ProjectileUtil.getEntityHitResult(..., entity.getBoundingBox().inflate(pickRadius), ...)` — 命中體積等於 entity bounding box + pickRadius，放大 bbox 等於放大命中區
- mixin 進 `Entity.getBoundingBox()` 的 RETURN，把回傳的 AABB `.inflate(expand)`，預設 0.3（每側 +0.3 格 ≈ 玩家箱子變兩倍寬）
- 跳過本機玩家自己（不然會搞壞自身碰撞 / 鏡頭）
- 預設「Players Only」=true，只放大玩家（PvP 用），mob 不動避免影響 AI 尋路 / 渲染
- **server 端注意**：1.20+ vanilla server 會驗證玩家視角是否真的對到「真實」hitbox。放大太多（≥ 0.5）會被當作 hitbox cheat flag 掉，建議 0.2–0.4
- **對 SpearAura 影響有限**：silent-aim 直接送 `attack(target)` 不靠視線 raycast；這個主要幫**手動瞄準近戰**邊緣擦到也算命中

### v1.6.18 — SpearAura 擴大判定 + 新模組 NoCrash 解鞘翅撞牆動能傷害
- **SpearAura Max Reach 滑桿上限 8 → 12**：單機 / 朋友自架（無 anti-cheat）可以拉到 vanilla server 接受的上限以上試。主流伺服器（Hypixel 等）attack distance 是 server 驗證的，超過 spear 原生 reach 一樣會被 reject
- **新模組 NoCrash**：解 vanilla 鞘翅 `flyIntoWall` 動能傷害
  - vanilla 算法：`change = previousSpeed - currentSpeed`，`change > 0.2` 時送 `(int)(change * 10)` 半心傷害，**server-authoritative**，純改 client 血量無效
  - 策略：飛行中每 tick 朝速度方向 raycast `lookahead` 格（預設 4），偵測到 solid block 就把 `setDeltaMovement` 的水平分量 clamp 到 `maxSpeed`（預設 0.4 b/t）
  - 因為**減速發生在 collision tick 之前**，server 看到的是平滑減速不是高速撞擊，`change` 始終 < 0.2 就不觸發 damage code path
  - ClickGUI 兩個滑桿：Look Ahead（2–10 格）、Max Speed（0.1–1.0 b/t）。對手動飛 + ElytraGoto 自動駕駛都生效

### v1.6.17 — SpearAura：1.21.11 新武器 Spear 的 Aura 模組
- **新模組 SpearAura**（預設 `U`）— 為 Mounts of Mayhem 更新加入的 Spear 武器設計
- **三種攻擊模式**（ClickGUI 滑桿切換）：
  - **Jab**：短按攻擊，silent aim 對準身體中心後送一發 `mc.gameMode.attack()`，等同 MaceAura 但用 Spear 的傷害公式
  - **Charge**：自動 `keyAttack.setDown(true)` 蓄力 N ticks 後 release，期間每 tick 重新 silent aim 跟住會跑的目標
  - **Auto**：騎馬 / 移動速度 > 0.15 blocks/tick 自動切 Charge，否則 Jab。讓 PvE 圈怪 + PvP 騎馬衝擊用同一個鍵
- **Spear 專屬機制處理**：
  - 使用 `ItemTags.SPEARS` 判斷手持，七種材質（wooden..netherite）都認得
  - 目標篩選照 sweet spot 距離（`(MIN+MAX)/2`）打分，太近的目標扣 100 分，不會鎖一個壓在臉上根本戳不到的怪
  - **Min Reach**（預設 1.6）：Spear 在這距離內傷害是 0，模組偵測到目標太近就不送 attack 浪費 swing
  - **Auto Step Back**：太近時自動按 S 拉開距離回到 sweet spot，可在 ClickGUI 關掉
- **Silent aim 對準身體中心**（`y + bbHeight * 0.5`，不是頭部也不是腳）— wiki 說 view angle 是傷害係數之一，crosshair 對 centroid 時傷害最高
- 不主動觸發 Lunge 附魔（會耗飢餓）；vanilla 自己看玩家有沒有 Lunge 附魔處理

### v1.6.16 — ElytraGoto 支援 ow / nether 跨維度座標前綴
- `.nf goto <x> [y] <z>` 維持原本「當前維度座標」行為
- 新增 `.nf goto ow <x> [y] <z>`：宣告這組是主世界座標，在地獄會自動 ÷8 飛到對應的下界點；蓋傳送門出來就會落在原本指定的主世界座標附近
- 新增 `.nf goto nether <x> [y] <z>`：反向，在主世界自動 ×8。簡寫 `o` / `n` 也支援
- Y **不**換算（vanilla 傳送門 Y 是 1:1），只縮放 XZ
- 換算完才呼叫 `setTarget()`，所以 `inWrongDimension` 暫停邏輯不受影響：過傳送門切維度後仍會暫停（這時繼續飛沒意義）
- 動作列顯示 `(OW→Nether ÷8)` / `(Nether→OW ×8)` 註記讓使用者確認實際飛的座標

### v1.6.15 — CritHit 真的會觸發暴擊 + Scaffold 關閉後不再卡攻擊
- **CritHit**：原本只在 client-side 設 `fallDistance = 1.0f`，server 端完全看不到 → 永遠不會判 crit。從 bytecode 抽 `Player.canCriticalAttack()`：要 `fallDistance > 0 && !onGround && !sprinting && !水/梯子/載具 && target 是 LivingEntity && 攻擊冷卻 > 0.9`。新寫法：攻擊 HEAD 觸發時，地上 + 冷卻滿 + 非 sprint 時送三發 `ServerboundMovePlayerPacket.Pos(onGround=false)` mini-hop，server 端 `fallDistance` 變正、`onGround` 變 false → server-side crit 條件達成。視覺上 client 完全不動。Sprint 中不觸發（hypixel watchdog 對 sprint + crit 會抓），要 crit 就先放 sprint
- **Scaffold tower**：關掉模組後一段時間打不到方塊 — root cause 是 tower mode 每 tick `setDeltaMovement(0, towerSpeed+0.08, 0)`，user disable 時 player 上升慣性還沒消失，繼續飛幾 tick → 期間 hitResult=null（看天空），每次按左鍵 `Minecraft.startAttack` 偵測到後設 `missTime=10`，累積成「破壞不了一段時間」。修法：disable 時殺掉殘留的上升動量（v.y > 0 設成 0），順便 reset `placeTimer` + `Minecraft.rightClickDelay` 防其他狀態 stuck

### v1.6.14 — waifu 終於顯示了（blit 參數順序搞錯）
- v1.6.13 改對了 class 名稱（Identifier），但 `blit` 9-arg 簽名其實是 `(Identifier, x1, y1, x2, y2, u1, u2, v1, v2)` — 角座標 + normalize UV，不是 `(x, y, w, h, u, v, uW, vH)` + pixel UV
- 從 loom jar 的 bytecode 確認：9-arg blit 內部呼叫 `innerBlit(pipeline, id, x1, x2, y1, y2, ...)`，且 UV 是 0..1 normalize
- 原本傳的等於繪一個零寬度切片到鬼地方所以看不見，也不會 throw

### v1.6.13 — waifu 真的修好了（class 名稱搞錯）
- 從 loom 抽出的 1.21.11 MC jar 確認三個關鍵差異：
  - 在這個 Lunar build 裡 `net.minecraft.resources.ResourceLocation` 實際叫 `Identifier`（Yarn 名稱）
  - `DynamicTexture(NativeImage)` constructor 不存在，要用 `DynamicTexture(Supplier<String>, NativeImage)`
  - `GuiGraphics.blit` 的 9-arg 簽名是 `(Identifier, int, int, int, int, float, float, float, float)` — 跟我假設的 1.21 標準簽名不同
- 整個 waifu 載入改用**直接 import**（不再反射），同時加 `waifuErr` 顯示實際錯誤訊息
- 之前的「不會讀到」就是 `Class.forName("ResourceLocation")` 直接 throw → 被 catch 吞掉 → silent fail

### v1.6.12 — ElytraGoto 找到真正拒收原因：deploy 太早送
- 使用者報告手動完全沒拒收問題（一跳一發就成功），但模組怎麼樣都被拒
- **真因**：vanilla 的展翅檢查包含 `getDeltaMovement().y < 0`（要正在下落才送 deploy），但 v1.6.6 拿掉這個守衛後，模組在玩家**剛跳起來向上飛時就送 START_FALL_FLYING**。server 那邊位置還沒同步到「離地」，看 `onGround=true` 直接拒絕 deploy → 後續所有 useItem 全被拒
- **修法**：恢復 `y < -0.04` 守衛 — 等玩家確實在下落才送 deploy packet。短跳場景用「6 ticks (~300ms) airborne 強制 deploy」作為 fallback 不會卡死

### v1.6.11 — ElytraGoto sync-gap retry 拉到 10 次/秒
- 使用者實測：模組明顯比手動慢，因為 unconfirmed 期間我每 10 ticks (500ms) 才重試 = 2 次/秒，而手動 mash 右鍵約 5 次/秒（甚至開 FastPlace 後可達 20 次/秒）
- 拉到每 **2 ticks (100ms)** 重試 = 10 次/秒，比手動還快，幾乎立刻跨過 sync gap

### v1.6.10 — ElytraGoto 平滑視角 + 自動降落 + 同步 gap 連發
- **平滑旋轉**：yaw 限制 12°/tick、pitch 限制 10°/tick，視角不再瞬間 snap（之前轉得太極限）
- **自動降落**：到達 XZ 8m 內後**不會馬上 stop**，繼續引導下降直到觸地或 vanilla 自動收翅；高於目標 Y 就 pitch +15° 下降，到目標 Y 附近就 level out
- **同步 gap 連發**：使用者實測手動「前幾發拒收，之後正常」確認是 sync gap。第一次成功消耗火箭前每 10 ticks (~500ms) 重試一次；確認成功後切回正常 50-tick 節奏

### v1.6.9 — ElytraGoto 伺服器拒收偵測 + 自動重發 fall-flying
- 截圖確認 v1.6.8 跑得正確（嘗試=3）但煙火 128 完全沒減 → 證實 server 在 silent reject useItem
- 動作列改顯示 `消耗 N/嘗試 M`，**消耗=0 + 嘗試>2 時直接顯示「伺服器拒收」紅字**
- 自動觸發 START_FALL_FLYING 重發（最多 3 次）— 若是 client/server fall-flying 不同步造成的拒收，重發可以救
- 若伺服器是真的禁飛（plugin 或 anti-cheat），請手動拿火箭右鍵測試。手動也不能 boost 就是伺服器限制，模組無解

### v1.6.8 — BHop 不再要求手動按 SPACE
- v1.6.2 加的「穿著鞘翅就 skip 自動跳」守衛太嚴格。實際上前面那行 `if (!moving || jumping) return;` 已經處理掉了「玩家按住 SPACE」的狀況（`jumping` 就是 vanilla 的 input.jumping），所以額外擋 elytra 是多餘的
- 拿掉後 BHop + 穿鞘翅可以正常自動跳了；死亡迴圈只在你「真的按住 SPACE」時才會發生（這是 vanilla 機制，無解 — 不按 SPACE 就沒事）

### v1.6.7 — ElytraGoto client/server fall-flying desync + 診斷
- **加 `!onGround()` 守衛**：截圖看到 client 顯示 flying 狀態但實際在地上跑 6.3 m/s — 表示 `isFallFlying` 卡在 true。這時送的 useItem 包到 server 是 silent no-op，火箭不消耗也不 boost。改成只信「真的在飛」(`isFallFlying() && !p.onGround()`)
- **動作列加診斷計數器**：`煙火 80 /嘗試 12` — 嘗試數會升但煙火數沒降代表 server silent reject（多半是 sync 問題或伺服器禁飛）
- **動作列加版本號 v1.6.7**：方便從截圖判斷你跑的是哪一版（Lunar Client 必須完全重啟才會載入新 jar）

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
