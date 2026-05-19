# NoFall Mod

Fabric mod for **Lunar Client 1.21** — NoFall / XRay / MaceAura / NoSlow

---

## Features

| Feature | Key | Description |
|---------|-----|-------------|
| **NoFall** | `N` | 不受墜落傷害 |
| **XRay** | `X` | 透視礦石（煤、鐵、金、鑽石、綠寶石、青金石、紅石、下界石英、下界金礦、遠古殘骸、紫水晶） |
| **MaceAura** | `Z` | 手持重錘時自動鎖定並攻擊最近的玩家 / Mob（Silent Aim，不移動視角） |
| **NoSlow** | `G` | 吃東西 / 使用物品時移動速度不減慢 |

---

## Installation

1. 安裝 **Fabric Loader**（不需要 Fabric API）
2. 下載 JAR → [**releases/nofall-mod-1.0.0.jar**](releases/nofall-mod-1.0.0.jar)
3. 放入 mods 資料夾：
   - **Lunar Client**：`.lunarclient/profiles/lunar/1.21/mods/fabric-1.21.11/`
   - **一般 Fabric**：`.minecraft/mods/`

---

## Keybinds

預設鍵位，可以在遊戲內用指令修改：

```
.nf bind <nofall|xray|maceaura|noslow> <鍵名>
```

例：`.nf bind xray F5`

設定存在 `.minecraft/config/nofall.json`

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
# JAR → build/libs/nofall-mod-1.0.0.jar
```
