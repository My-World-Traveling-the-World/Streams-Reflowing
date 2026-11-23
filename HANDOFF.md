# 河流开发指南与交接

[项目介绍](README.md) · [版本更新](CHANGELOG.md)

## 来源与分发

上游是 **nicejohn** 的 StreamsReflowing 2.12.8（Minecraft 1.21.1 Fabric），元数据声明 **All Rights Reserved**。本仓库用于本机开发，派生源码和成品的分发须获原作者授权。原始输入 `StreamsReflowing-1.21.1-fabric-2.12.8.jar` 的 SHA-256 为 `55916c7e334fe9776fa262bc7ca167b9f9417e5982f0cb61d5fa55955bdbc1cb`，恢复记录见 `git show 1e2df19`。

## 构建和验证

先在同级 `plaincraft-worldgen-api` 运行 `:api:contractJar :api-fabric:devModJar`。按 `build.gradle` 将 Mojang 映射的 DH、Sable、SableBeyond 与 companion 编译件准备在 `_recover/libs/named/`。使用 JDK 21 与外置 Gradle 9.4.1。

```powershell
& $gradle test remapJar planContractJar --daemon --console=plain
```

正式包在 `build/libs/`。`build/contract/streams-plan-api.jar` 是道路和长城的编译输入。生产源码在 `src/main`，回归在 `src/test`。合法空水系、终态失败与旧会话回调分别处理，继续拆分施工前用固定样本记录区块方块、含水和流体调度结果。

## 当前交接 · 2026-11-23

1. **改动**：合并来源、构建与交接，单独整理版本历史。
2. **验证**：重跑 99 项单测，零跳过和失败。`remapJar` 与 `planContractJar` 验证成功。
3. **未验证**：整合包实机水流、开局联动与施工尾延迟。
4. **负控**：本轮沿用现有单测，未新增生成守卫。
5. **下一步**：用新档核对预热就绪、合法重试与区块边界，再根据实测调整施工。
6. **历史与回退**：清理前文档见 `git show 9b1bdd7:HANDOFF.md` 和 `git show 9b1bdd7:RECOVERY.md`，本轮提交保留在本机。
