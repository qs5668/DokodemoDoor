# 任意门 DokodemoDoor

配对式跨世界传送门插件，面向 Paper / Leaves / Purpur / Spigot 1.21.8（Java 21）。

放两扇门，手持门之钥依次右键，两门从此配对互通——走进任意一扇，从另一扇门前走出。像原版下界门一样自然，但连接的是任意两个世界的任意两扇门。

## 状态

- [x] 策划案（竞品分析 / 功能需求 / 技术方案 / 开发计划）
- [x] 玩家玩法指南页
- [x] 插件 V1.0（配对系统 / 传送引擎 / 存储 / 粒子音效 / 命令树 / 双语文案）
- [x] V1.0.2（GPL-3.0 开源 / Paper+Spigot 双平台构建 / GUI 门对管理菜单）

## 下载

前往 [Releases](https://github.com/qs5668/DokodemoDoor/releases) 下载最新版本：

| 文件 | 适用服务端 |
|------|-----------|
| `ddoor-x.y.z-paper.jar` | Paper / Leaves / Purpur（原生 Adventure，体积小） |
| `ddoor-x.y.z-spigot.jar` | Spigot / CraftBukkit（内置重定位 Adventure，体积大） |

两个包功能完全一致，按服务端类型选一个即可。

## 文档

- 官网：https://www.midream.top
- 插件文档站：<https://dokodemodoor.plugin.midream.top/>
- [插件策划案](https://dokodemodoor.plugin.midream.top/proposal/) — 完整方案设计
- [玩家玩法指南](https://dokodemodoor.plugin.midream.top/player-guide/) — 面向玩家的上手指南

## 构建

```bash
cd plugin
mvn clean package -P paper    # 产物: target/ddoor-1.0.2-paper.jar
mvn clean package -P spigot   # 产物: target/ddoor-1.0.2-spigot.jar（shade Adventure）
```

要求 JDK 21+。默认 profile 为 paper。依赖（paper-api / spigot-api、VaultAPI、PlaceholderAPI）均为 provided 作用域，运行时由服务器或对应插件提供；Spigot 包将 Adventure 序列化器 shade 并重定位到 `top.midream.ddoor.libs.kyori`。

## 安装

1. 从 [Releases](https://github.com/qs5668/DokodemoDoor/releases) 下载对应平台的 jar，放入服务器 `plugins/` 目录
2. 重启服务器，`plugins/DokodemoDoor/config.yml` 自动生成
3. 默认 SQLite（WAL 模式）零配置可用；群组服可切换 MySQL

## 使用

- **玩家**：合成门之钥（紫晶×4 + 铁锭×4 + 末影之眼×1 → 2 把）→ 放两扇门 → 手持钥匙依次右键 → 走进门框即传送
- **GUI**：`/ddoor gui` 打开门对管理菜单——分页门对列表、左键传送、Shift+右键解绑、个人统计、钥匙配方提示
- **命令**：`/ddoor gui|list|tp|rename|unlink|link|delete|key|stats|reload`
- **权限**：`ddoor.use`（穿越）/ `ddoor.create`（配对）/ `ddoor.gui`（菜单）/ `ddoor.limit.<n>`（门对上限）/ `ddoor.admin`（管理）详见 plugin.yml

## 核心设计

| 特性 | 说明 |
|------|------|
| 零门槛创建 | 放门 + 门之钥右键，无需命令/选区/告示牌 |
| 实体配对模型 | 门对（DoorPair）双向对称，一扇门同一时刻只属于一个门对 |
| 原生化体验 | 走进门框即传送，粒子/音效/渐暗三段反馈 |
| 服务器友好 | PlayerMoveEvent 双重节流、O(1) 内存索引、异步落盘 |
| 跨世界安全 | 落点安全校验（含左右探测）、抗性防摔、载具拦截 |
| 双平台兼容 | TextAdapter 平台抽象层，同一套业务代码跑 Paper 与 Spigot |

## 架构

```
plugin/src/
├── main/java/top/midream/ddoor/
│   ├── DDoorPlugin.java        # 主类装配
│   ├── DDoorConfig.java        # 配置对象（reload 热生效）
│   ├── door/                   # 门记录、O(1) 坐标索引、配对会话、方块识别
│   ├── teleport/               # 六环检测链传送引擎
│   ├── listener/               # 钥匙右键 / 移动节流 / 破坏解绑
│   ├── key/                    # 门之钥物品与配方
│   ├── command/                # /ddoor 命令树
│   ├── gui/                    # GUI 门对管理菜单（分页/传送/解绑/统计）
│   ├── platform/               # TextAdapter 平台抽象接口
│   ├── storage/                # SQLite(WAL)/MySQL + 异步写队列
│   ├── visual/                 # 粒子三态渲染、音效、审计对账
│   └── hook/                   # Vault / PlaceholderAPI 软依赖
├── paper/java/.../platform/    # Paper 实现（原生 Adventure / teleportAsync）
└── spigot/java/.../platform/   # Spigot 实现（Legacy 序列化 / 同步传送）
```

## 目录结构

```
DokodemoDoor/
├── docs/            # GitHub Pages 站点
│   ├── index.html   # 落地页
│   ├── proposal/    # 策划案
│   └── player-guide/ # 玩家指南
└── plugin/          # 插件源码（Maven 工程，双 profile 构建）
```

## 许可

[GPL-3.0](LICENSE) — 本项目基于 GNU General Public License v3.0 开源。
