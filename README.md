# StreamsReflowing · 河流

**顺着溪流走过山谷，看水道汇合、落差与湖泊。**

`Minecraft 1.21.1` · `Fabric` · `Java 21+`

[安装](#安装) · [模组联动](#模组联动) · [版本更新](CHANGELOG.md) · [开发指南](HANDOFF.md)

| 功能 | 游戏里的体验 |
| --- | --- |
| 地形水系 | 水道沿地形组织成溪流、河流与湖泊，形成河床、河岸和落差。 |
| 水面细节 | 水边带有声效与粒子效果。 |
| 开局预热 | 新世界先准备出生区附近的水系，探索时继续准备周边区域。 |

## 安装

本机适配使用 **Fabric Loader 0.19.3** 验证。游戏需安装 Fabric API 和[云游四海世界生成 API](https://github.com/My-World-Traveling-the-World/PlainCraft-Worldgen-API/releases)。Mod Menu 可选。

## 模组联动

河流可以独立生成。合装兼容的长城或道路时，河流先发布就绪信号，供下游安排预热、选线和跨水路段。三者合装的顺序为 **河流 → 长城 → 道路**，前置只装一份。

原模组作者为 **nicejohn**，上游保留全部权利。本机适配的来源和分发边界见[开发指南](HANDOFF.md)。
