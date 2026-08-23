# Changelog

## [1.0.5] — 2026-08-23

### 新增

- **传送触发方式三选一**：玩家可在 GUI 个人设置中切换 走近传送（默认）/ 右键点击传送 / 左键点击传送。左键模式下潜行=原版操作（开门/破坏不受影响）；手持门之钥时优先配对流程。`teleport.default-mode` 定义新玩家默认值。
- **门对详情页**（GUI 列表右键门对）：两端完整信息（世界/坐标/朝向/群系/各自穿越次数）、创建时间、累计穿越；支持一键 **启用/停用**（停用后暂停传送，不删门）、**重命名**（聊天栏输入）、**解除配对**、**传送到 A/B 端**。
- **交互记录**：传送/配对/解绑/破坏全量入库（SQLite/MySQL），GUI 门对详情内可查——谁、何时、在哪个门、做了什么，分页展示。仅门主与管理员可见。`logs.retention-days` 自动过期清理（0 = 永久）。
- **详细报错**：传送失败默认报出具体原因——哪个坐标的什么方块挡住了落点/头部/悬空、对侧门在哪个世界哪个坐标被破坏、目标世界名。玩家可在设置中开启**简化信息显示**降噪（GUI 门对信息与报错同步精简）。
- **门之钥配方自动解锁**：玩家进服即写入配方书，无需手动触发。
- **/ddoor stats 升级**：个人统计对全部玩家开放（门对/上限/累计穿越/传送方式/信息档位）；管理员追加全服统计。

### 数据库

- `ddoor_doors` 新增 `enabled` 列（自动迁移，无需手工改库）
- 新表 `ddoor_player_settings`（玩家偏好）、`ddoor_logs`（交互记录）

## [1.0.2] — 2026-08-23

### 新增

- **MC 1.20.1 – 1.20.6 支持**：新增 `paper-120` 构建 profile（`ddoor-1.0.2-paper-1.20.jar`，`api-version: 1.20`），以 paper-api 1.20.1 为最低基线编译。连同 1.21.x 主包，版本支持范围覆盖 MC 1.20.1 – 1.21.x。
- **GUI 门对管理菜单**（`/ddoor gui`）：54 格箱子界面，分页展示自己的门对列表；左键点击门对直接传送，Shift+右键解除配对；集成个人统计（门对数/上限/累计穿越）与门之钥合成配方提示。拖拽防护、越权校验齐全。
- **Spigot / CraftBukkit 支持**：新增 `TextAdapter` 平台抽象层，同一套业务代码编译为多个发行包——
  - `ddoor-1.0.2-paper.jar`：Paper / Leaves / Purpur 1.21.x，原生 Adventure 与 `teleportAsync`；
  - `ddoor-1.0.2-paper-1.20.jar`：Paper / Leaves / Purpur 1.20.1–1.20.6；
  - `ddoor-1.0.2-spigot.jar`：Spigot / CraftBukkit 1.21+，shade 并重定位 Adventure（`top.midream.ddoor.libs.kyori`），Legacy §x 十六进制色渲染。
- **GPL-3.0 开源**：添加 LICENSE 全文与全部源码文件头。

### 修复

- **旧版语言文件升级兼容**：从 v1.0.0 升级时，服务器上已存在的 `lang/*.yml` 缺失新增文案键（如 `cmd.usage-gui`、`gui.*`）会直接显示原始键名。现在以包内语言文件作为 defaults 合并解析，本地自定义文案仍优先。
- **药水效果枚举跨版本兼容**：抗性效果在 1.20.5 由 `DAMAGE_RESISTANCE` 改名为 `RESISTANCE`，改为运行时按名称解析，1.20.1 与 1.21.x 均可用。
- 落点安全校验改用 `Material#isSolid()`，兼容 Spigot API（原 `Block#isSolid()` 为 Paper 专属）。

### 构建

- Maven 三 profile（`-P paper` / `-P paper-120` / `-P spigot`），`api-version` 按档位自动过滤，build-helper 分平台源码目录 + shade 重定位。
- 排除 VaultAPI 传递的旧版 `org.bukkit:bukkit` 依赖，避免遮蔽 paper-api。

## [1.0.0] — 2026-08-23

首个正式版本。

- 配对系统：门之钥右键两扇门完成配对，双向对称传送
- 六环检测链传送引擎：进门框即传送，落点安全校验（含左右探测）、抗性防摔、载具拦截
- 存储：SQLite（WAL）/ MySQL 双后端 + 异步写队列
- 视觉：粒子三态渲染、音效反馈、审计对账任务
- 命令树：`/ddoor list|tp|rename|unlink|link|delete|key|stats|reload`
- 门之钥合成配方（紫晶×4 + 铁锭×4 + 末影之眼×1 → 2 把）
- Vault 经济（可选）、PlaceholderAPI 变量（可选）软依赖
- 中英双语文案（MiniMessage）
