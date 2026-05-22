# Valencia

Fabric client mod for **Lunar Client 1.21** — utility / combat features.

Latest: **v1.4.0** — [Download JAR](https://github.com/Siesta0217/valencia/releases/latest)

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
| **ClickGUI** | `右 Ctrl` | 可拖曳面板，展開每個模組的詳細設定 |

### ClickGUI 設定項

- **KillAura**：範圍、攻擊範圍、攻擊延遲、Single 模式、目標篩選、Raycast、Skip Invisible、平滑旋轉、Body Lock
- **MaceAura**：偵測範圍、攻擊範圍、Hostile / Animals / Players 目標篩選
- **Step**：步高滑桿（1.0×–3.0×）
- **BHop**：速度倍率（0.5×–2.5×）、Low Hop + Jump Height（0.1–1.0）、Boost（1.0–1.5，**每 air tick 複合**，封頂 2.5）、KB Boost（受擊把擊退轉為前進）
- **Timer**：玩家 tick 倍速滑桿（1.0×–3.0×）
- **ElytraGoto**：用 `.nf goto <x> [y] <z>` 設目標、`.nf goto stop` 停止；動作列顯示距離 / ETA / 主世界↔下界座標對照
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
# JAR → build/libs/valencia-1.4.0.jar
```

> **注意**：不要使用 `gradlew build`（test task 在此環境下會壞）。
> `settings.gradle` 的 `rootProject.name` 請勿修改（JDK 25 + Groovy ASM 限制）。

---

## Changelog

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
