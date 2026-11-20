# 河流后续开发交接

## 2026-11-20：三模组复查，进度归属与缓存冲突复现

### 一、改了什么

行为等价/审查基建：只新增`tools/review/startup_cache_probe.py`及`docs/evidence/startup-cache-probe-20261120.json`，没有改生产Java。完整联合复查在相邻`../roadweaver-next/docs/THREE-MOD-REVIEW-20261120.md`。探针直接编译原ChunkPlanCache、ColumnPlan及StreamsStartupProgress，只为空RiverEngine类型提供替身，不启动Minecraft。

### 二、我验证了什么

开工HEAD 82cc818干净；无remote，未虚构pull成功。外置Gradle9.4.1/JDK21/GBK执行test --rerun compileJava，BUILD SUCCESSFUL，XML合计20项，失败/错误/跳过0。相邻道路实际1753/0/0/0，长城实际982/0/0/0。原缓存同线程插入32个对角线区块只保留1个，横向保留32；A开始→清理→B开始20块→A旧更新让B显示20/20，A旧finish使B inactive。源码SHA256在证据中。生产回调没有owner检查，结果门已修的ready保护仍有效。

### 三、我没能验证什么

没有Minecraft退出重开时序、FPS/MSPT、预热总时长或4096工作集。没有完整ServerLevel运行期构建故障注入；源码确认异步失败/撤回后仅移除pending，玩家中心记录仍能跳过原地重试，但不声称世界最终丢河。没有动态复现采样模式跨会话泄漏。细湖掩码按触及单元清理尚未实现或计时。没有新守卫或生产变异，本轮探针不计入JUnit数量。

### 四、遗留半截

本轮只审查和复现，三个生产修复仍未做：启动进度/模式归属、失败后有界重试、ChunkPlanCache冲突替换。探针固定当前缺陷现象，修复时必须转换为正式正反测试，不能当作成功验收套件。河流仍无remote，不能push；没有部署成品。

### 五、下一轮先做什么

先在河流仓库迁出StartupSession和真实worker端口，钉住A停/B开/A回调、重复释放与取消，再修原地失败重试；随后量缓存冲突并对拍小型相联缓存。NetworkTracer仍4001非空非注释行，按洪泛/路由、细湖、河链和跨板阶段拆，保留浮点与同权次序。

### 六、踩到的坑

旧TASKS/OPTIMIZATION多项已实现，不能重新计算收益或照搬线程数倍数。Java源解析应先识别字符串再去注释，避免中文日志内容误判。探针首次沙箱TemporaryDirectory写入被拒，按权限流程升级后原脚本运行成功；没有把沙箱失败算作组件故障。缓存完整键/engine比较正确，此处是命中容量问题，不是错命中。

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
