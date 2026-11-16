# 河流后续开发交接

## 2026-11-16：启动预热结果与就绪发布

### 一、改了什么

- **行为变更**：StreamsPrewarmService 用每世界 RiverStartupReadiness 保存未启动、成功、明确禁用、失败、停服结果。保留 awaitStartup 的公开签名；两个 Fabric 生命周期入口均调用 publishStartupReadiness。
- 配置禁用或半径为零是明确放行，不宣称板块已构建；输出 STREAMS_STARTUP_BYPASSED。上下文缺失、配置/调度异常、异步构建或清理异常不发布 ready，输出 STREAMS_STARTUP_NOT_READY。
- stop 终止本世界的等待，结果门与发布/停止共用锁，旧构建完成不能重新发布。未取消引擎共享构建，也未重写执行器生命周期。
- **测试基建**：新增10项结果门行为测试和2项静态接线守卫；test 零发现改为失败。测试留在河流仓库。
- 未改河流几何、缓存格式、API签名、配置键与 Mixin 优先级。

### 二、我验证了什么

基线8项，完成后20项：tests=20 skipped=0 failures=0 errors=0；test 与 compileJava BUILD SUCCESSFUL。JDK21、外置Gradle9.4.1、--offline --daemon。

本机中文仓库路径下，Gradle UTF-8 参数文件被 Windows JDK 按 GBK 解码，导致已有测试 ClassNotFoundException。独立 java @args 对照：相同 classpath UTF-8 失败、GBK LOAD_OK。本机命令需要 GRADLE_OPTS=-Xmx2G -Dfile.encoding=GBK，并向外置Gradle9.4.1传 test compileJava --offline --daemon --console=plain --dependency-verification=off -Dfile.encoding=GBK；JAVA_HOME 用 jdk-21。源码仍按UTF-8编译。

| 生产变异 | 目标测试 | 结果 |
|---|---|---|
| 异步失败改成功 | asynchronousFailureCannotPublish | 红 |
| 初始化失败改成功 | setupFailureCannotPublishOrBeOverwrittenBySecondHook | 红 |
| 所有结果都调用发布端口 | unstartedCannotPublish | 红 |
| 明确禁用不放行 | disabledIsExplicitBypass | 红 |
| await与publish忽略停止 | stopAfterCompletionStillPreventsBroadcast | 红 |
| 重开入口删除结果门调用 | bothLifecycleHooksUsePublisherGate | 红，静态接线 |
| 服务停止漏停结果门 | serviceRecordsAllFailurePathsAndStopsWorldGate | 红，静态接线 |
| 初始化异常漏记录失败 | serviceRecordsAllFailurePathsAndStopsWorldGate | 红，静态接线 |

每项锚点计数断言，编译成功且目标JUnit失败才记红。finally git checkout -- 精确文件从本轮暂存索引恢复，并核对原字节；正向复验20项通过。脚本/日志在忽略的 build/round33-*。前五项是行为测试，后三项不能替代游戏运行验证。

### 三、我没能验证什么

- 未启动 Minecraft；新建、重开、缺长城以及真实最终 Mixin 执行顺序未验。未打包部署，没有帧时/加载耗时收益结论。
- 真实 ServerLevel 的构建失败注入与广播未运行；目前以生产结果组件行为及入口静态守卫确认，不能称全链路验收。
- API只有就绪布尔值，没有失败状态；河流失败后道路仍会非阻塞等待直到停服，未加重试或失败UI。
- 启动板块构建停止后仍可能清理全局采样模式/进度；未取消共享引擎构建，跨世界清理代次仍待验证。
- 长城订阅、晚订阅与自身完成状态要在长城仓库补守卫。4096工作集、道路子任务预算未在本轮解决。

### 四、遗留半截

本轮结果门与守卫完整，临时变异全部还原。仓库没有配置任何 remote/upstream，git pull --rebase 无法执行同步；无法 push，不能凭空设置远端。

### 五、下一轮先做什么

长城自己的河流订阅/晚订阅/重开隔离/完成失败守卫，然后验证三模组新建与重开顺序；道路执行预算及4096工作集继续在道路仓库推进。

### 六、已解决的坑

await返回包含异常被吞和未创建Future，不能推出河流完成；显式结果必须覆盖同步失败与异步失败。两次生命周期入口不能覆盖第一次失败。停止后即便工作Future已经成功，也须阻止发布。现有源码实际为LF，不因终端显示CRLF就整文件改行尾。
