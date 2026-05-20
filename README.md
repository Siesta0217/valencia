# Valencia

Fabric mod for **Lunar Client 1.21** — NoFall / XRay / MaceAura / NoSlow

---

## Features

| Feature | Key | Description |
|---------|-----|-------------|
| **NoFall** | `N` | 不受墜落傷害 |
| **XRay** | `X` | 透視礦石（煤、鐵、金、鑽石、綠寶石、青金石、紅石、下界石英、下界金礦、遠古殘骸、紫水晶） |
| **MaceAura** | `Z` | 手持重錘時自動鎖定並攻擊最近的玩家 / Mob（Silent Aim，不移動視角） |
| **NoSlow** | `G` | 吃東西 / 使用物品時移動速度不減慢 |
| **ClickGUI** | `右 Ctrl` | 開啟模組管理介面（可拖曳，Raven 風格）|

---

## Installation

1. 安裝 **Fabric Loader**（不需要 Fabric API）
2. 下載 JAR → [**releases/valencia-1.0.0.jar**](releases/valencia-1.0.0.jar)
3. 放入 mods 資料夾：
   - **Lunar Client**：`.lunarclient/profiles/lunar/1.21/mods/fabric-1.21.11/`
   - **一般 Fabric**：`.minecraft/mods/`

---

## Keybinds

預設鍵位如下，修改請直接編輯 `.minecraft/config/nofall.json`：

```json
{
  "nofallKey": 78,
  "xrayKey": 88,
  "maceAuraKey": 90,
  "fastFoodKey": 71,
  "guiKey": 345
}
```

常用 GLFW key codes：`G=71  H=72  J=74  N=78  X=88  Z=90  F5=296  RIGHT_CONTROL=345  RIGHT_SHIFT=344`

---

## Requirements

- Minecraft **1.21.11**（Lunar Client 1.21）
- Fabric Loader **0.19.2+**
- Java **21**

---

## Build

```bash
git clone https://github.com/Siesta0217/nofall-mod.git
cd nofall-mod
JAVA_HOME=/path/to/jdk21 ./gradlew build
# JAR → build/libs/valencia-1.0.0.jar
```
