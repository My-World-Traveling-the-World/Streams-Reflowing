# 河流后续开发交接

## 2026-11-20：第38轮，稀疏触及掩码（第四项）

### 一、改了什么

行为等价/性能：每次NetworkTracer.build独享TouchedCellMask；候选数≤网格1/64时只记录并清触及索引，重复索引只记录一次，索引表按需增长；密集候选批量清零。FineLakeRefiner使用try-with-resources，提前拒绝、采样异常和prepare部分失败都清干净。未改变湖形、随机或洪泛次序。

### 二、我验证了什么（附变异）

完整test/compileJava成功52项、失败/错误/跳过0。保留四项冻结湖形对拍，新增五项稀疏/密集切换、重复/增长、空掩码、部分标记异常、真实细湖拒绝及采样故障后再成功。负控：不清稀疏3失败、不清密集2、prepare异常不清3、精修不归还1，均完整5项、errors/skipped0；精确checkout和字节恢复，证据fine-mask-mutations-20261120.json。

真实生产掩码与旧完整fill在独立JVM测262144格、16/1024/4096/8192/半图/满图；每样本100轮、7样本，原始纳秒见touched-mask-benchmark-20261120.json和可复跑tools/benchmarks/touched_mask_probe.py。纯触及方案密集输入明显退化，因此最终使用混合策略；最终样本保留JIT波动，不给游戏加速倍数。

### 三、我没能验证什么

未采集真实每板块候选密度、整体精修耗时或峰值内存。索引表稀疏路径最多增长至触及单元规模；阈值为本机保守策略，不能声称跨机器最优。没有整板真实河网或Minecraft对照。

### 四、遗留半截

第四项离线实现/对拍完成；大类其他阶段、计划及失败API、道路长城外层事务继续。无remote不能push，没有部署。

### 五、下一轮先做什么

补提供方拥有的稳定API和生命周期失败解释；将消费者从内部实现类型迁开，再继续阶段拆分及施工事务守卫。

### 六、踩到的坑

单纯O(触及数)不是所有输入都快，必须测密集场景。异常处理必须涵盖prepare自身，try资源创建前失败也要清；只测正常close会遗漏残留。原冻结类不随优化改动。


## 2026-11-20：第37轮，细湖阶段等价迁出（第五项第一步）

### 一、改了什么

行为等价：将NetworkTracer的细湖洪泛及内部邻域判据原序迁入314行FineLakeRefiner，保留私有入口转发、共享boolean掩码、X诊断归属和采样顺序。独立冻结c29c2f9原算法在测试源FrozenFineLakeRefiner，不调用新算法；未在此次迁移混入清理优化。

### 二、我验证了什么（附变异）

完整test/compileJava成功47项、失败/错误/跳过0。四项新测试逐位比较湖mask、完整边界/尺寸/高度/挖深和累计统计，20个平移/乱序盆地全部成功，另覆盖干地及板块边界拒绝；反射调用原生产私有入口验证真实转发。整湖丢弃3个断言失败、原入口断接1个失败，errors/skipped均0，checkout恢复及字节核对。证据docs/evidence/fine-stage-mutations-20261120.json；迁出段原SHA256为29b5947c769ed357d9273e9703ed641ccd03d306595fec12c684b97d2a3ec32b。

### 三、我没能验证什么

没有整板Minecraft结果或真实地形采样；不把20个合成盆地当所有地形证明。NetworkTracer剩余大方法仍超1000行，尚未完成大类拆分。

### 四、遗留半截

细湖阶段迁出完成，按触及单元清理仍未改；第五项其他阶段及第六、七项继续。无remote无法push，未部署。

### 五、下一轮先做什么

以冻结细湖产物为守卫，改共享掩码为触及列表生命周期；异常/提前拒绝也必须清理，测稀疏和密集成本后再决定阈值。

### 六、踩到的坑

必须同时断言成功湖非空和拒绝路径，否则两个实现都返回null会假对拍。Frozen类保留独立算法，不让它转调新组件；统计只排除纳秒耗时，其余精确比较。


## 2026-11-20：第36轮，完整键有界缓存（第三项）

### 一、改了什么

行为变更/性能：ChunkPlanCache保留线程本地、32项容量、engine身份、take移除和generation清空契约，存储改为完整键开放寻址，删除回移探测链。默认关闭的chunkPlanCacheStats仅统计当前线程/代次；查找有容量边界，损坏不能无限循环。复查探针改为正向验收，旧缺陷证据保留。

### 二、我验证了什么（附变异）

7417352上开工。完整test/compileJava成功43项、失败/错误/跳过0；6项新测试含15000步随机Map对拍及真实另一线程clear。六组负控均完成编译并产生断言失败：容量改1→1、去身份→1、破坏删除链→3、take不移除→3、取消代次失效→1、强开诊断→1。XML完整6项且无skipped/errors/worker崩溃，精确checkout及字节恢复。证据docs/evidence/chunk-plan-cache-mutations-20261120.json。

生产组件正向探针对角线/横向均32项保留，旧会话不影响新进度；见startup-cache-fixed-20261120.json。隔离JVM对照冻结7417352缓存：对角线首次查32项miss由31变0，横向均0；1048576查询9轮原始纳秒与分配量见chunk-plan-cache-benchmark-20261120.json。查找本身新实现较慢，每线程Ring分配608→1096字节；只证明避免冲突丢命中，不能宣称整体提速。

### 三、我没能验证什么

没有Minecraft真实命中率、回落计算耗时、世界生成总时长或FPS/MSPT。计数不跨线程汇总；基准只有空RiverEngine类型替身，未假冒引擎构建收益。

### 四、遗留半截

前三项已交付；细湖掩码、大类分阶段迁移、正式计划/失败API、道路长城外层可恢复事务继续。没有remote不能push，没有部署。

### 五、下一轮先做什么

迁出细湖阶段并冻结旧实现逐产物对拍，再按实际触及单元清掩码；保留洪泛顺序、浮点与并行预采样规则。

### 六、踩到的坑

身份测试必须构造真实同桶不同身份，已用65次鸽巢固定前置条件。初次删除链负控让旧无界查找挂起，取栈定位并只结束本仓指定测试worker；该次不算红。加边界后六组全部重跑，拒绝缺项/跳过/崩溃的报告。未把杀进程或编译失败算守卫有效。


## 2026-11-20：第35轮，运行期有界重试（第二项）

### 一、改了什么

行为变更：RuntimePrewarmQueue接入正式运行期worker，统一在途去重、失败/撤回/拒绝三种原因、1至60秒指数退避和最多256项重试。异常使扫描中心失效，已到期的板块也能绕过未移动中心；成功清重试、离开覆盖清旧项、停止关闭队列。负载和保存等待增加世界存活检查，不中断已进入的共享构建。默认扫描周期、容量及河流结果不变。

### 二、我验证了什么（附变异）

f94507f上开工；完整test/compileJava成功37项、失败/错误/跳过0，新增8项真实worker端口行为及1项正式扫描接线。七组锚点变异：丢重试5失败、重复owner1、忽略退避1、忽略停止1、退避上限改600秒1、保留离开覆盖的重试1、扫描不绕过未移动中心1。最后一组是静态接线，其余是行为断言，errors均0；精确checkout恢复及原字节确认。证据`docs/evidence/runtime-retry-mutations-20261120.json`，完整日志build/round35-runtime-final.log。

### 三、我没能验证什么

没有真实ServerLevel停服/移动/负载混跑或Minecraft故障注入；测试通过正式worker端口注入故障。默认60秒扫描仍决定何时查看已到期重试，不宣称1秒后必定实际执行。没有帧率或预热总时长收益；引擎构建内部尚不可协作取消。

### 四、遗留半截

第二项已交付；第三至七项继续推进。重试表淘汰会同时使中心失效，因此不把被淘汰失败永久当已覆盖。没有remote，不能push；没有部署。

### 五、下一轮先做什么

区块计划缓存改为固定容量的完整键查找，先钉住对角线/引擎身份/take/clear和冲突删除，再对拍缓存产物及测查询成本。细湖掩码和大类迁移随后。

### 六、踩到的坑

原StreamsRuntimeLoadPolicy是CRLF，保持原行尾；空白检查仅按cr-at-eol正确识别CR，不做全文件换行。编译失败不能算变异红，七组都检查新JUnit XML及实际断言失败。运行期失败要记录原因，不能只删pending然后永远跳过中心。

## 2026-11-20：第34轮，启动会话归属修复（第一项）

### 一、改了什么

行为变更：RiverStartupSession接入真实beginStartup/磁盘探测/构建/stop。每轮独立拥有任务、模式租约和进度身份，关闭撤销未开始的Future，正在运行的共享引擎工作不强行中断；释放幂等。StreamsStartupProgress增加owner版本，旧公开签名保留兼容，正式工作只用owner版本。旧回调不能清新进度，失败清理不显示100%。没有改河流几何、存档或配置。

### 二、我验证了什么（附变异）

6f838f6干净开工；没有remote。基线20项；恢复变异后test/compileJava成功，28项、失败/错误/跳过0。新增8项直接调用生产会话、真实Future和原进度组件，含真实线程中的共享构建不中断；原静态接线测试补正式submit/stop断言。

| 临时改坏 | JUnit失败 | 层级 |
| --- | --- | --- |
| 进度更新忽略owner | 1 | 生产组件行为 |
| finish忽略owner | 1 | 生产组件行为 |
| clear忽略owner | 1 | 生产组件行为 |
| 不撤销排队Future | 2 | 真实任务端口 |
| 释放后仍保持entered | 2 | 模式租约行为 |
| stop不关闭会话 | 1 | 静态接线 |

唯一锚点、实际JUnit报告，逐项精确git checkout恢复并核对原字节。见`docs/evidence/startup-session-mutations-20261120.json`，日志build/round34-*。静态接线不替代真实世界验证。

### 三、我没能验证什么

没有Minecraft退出/重开时序、启动耗时、FPS/MSPT、4096工作集或真实GPU/采样器资源释放。已进入共享引擎的构建继续运行，未实现引擎内部协作取消。旧无owner公开进度入口只为ABI保留，外部旧调用者无身份隔离；正式启动链已迁移。

### 四、遗留半截

这是全部任务中的第一项，不声称其余完成。原地失败重试、缓存替换、细湖掩码、大类分阶段迁移、计划/失败API、道路长城外层可恢复事务继续推进。上轮复现脚本保留历史现象，稍后缓存修复时改成新的正向审查工具，不能当成功验收套件。无remote无法push，未部署。

### 五、下一轮先做什么

继续迁出运行期真实worker端口，补有界退避和停止感知，保证异步失败/撤回后原地仍能补预热；再做缓存/掩码的独立对拍。所有测试归河流仓库。

### 六、踩到的坑

两个重叠会话测试的初始完成数必须不同，否则旧更新恰好写回同值会掩盖owner缺失。队列撤销先置closed，防止同步完成回调再次释放。只阻断ready并不能保护进度和模式租约；不能拿静态字符串断言替代真实Future行为。

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
