# 河流适配源码的来源与构建

上游是 **nicejohn** 的 StreamsReflowing 2.12.8，Minecraft 1.21.1 Fabric，原始元数据为 **All Rights Reserved**。本项目从上游成品恢复并适配，仅供本机开发。没有作者授权时，不分发派生源码或成品。独立的云游四海世界生成 API 按其自身许可发包，不包括河流代码。

原始输入为 `StreamsReflowing-1.21.1-fabric-2.12.8.jar`，SHA-256 `55916c7e334fe9776fa262bc7ca167b9f9417e5982f0cb61d5fa55955bdbc1cb`。生产编辑只改 `src/main`，不使用旧反编译工作树作为编译输入。反编译和初次恢复历史保留在本机提交 `1e2df19`。

## 本机构建

1. 在同级 `plaincraft-worldgen-api` 用 Gradle 8.8 执行 `:api:contractJar :api-fabric:devModJar`。可通过 `-PworldgenApiProjectDir=<路径>` 指定其他路径。
2. 根据 `build.gradle` 准备本机 Mojang 映射的 DH、Sable、SableBeyond 和 companion 编译 JAR，放在 `_recover/libs/named/`，这些第三方本机输入不进 Git 或成品。
3. Java 21、外置 Gradle 9.4.1，执行 `test remapJar planContractJar --daemon --console=plain`。正式成品在 `build/libs/`；`build/contract/streams-plan-api.jar` 只供长城/道路编译，不作为玩家的前置安装。玩家运行使用一份独立 `roadweaver_api`，而非道路主 JAR。

当前验证、限制和后续任务见[交接](HANDOFF.md)。
