# 河流后续开发交接

## 第46轮：组织备份和玩家首页（2026-11-22）

### 一、改了什么
建立 WAYFARING 组织私有仓库并同步 main 和 v2.12.8-merged；新增玩家 README，记录溪河湖泊、安装依赖和可选联动。

### 二、我验证了什么
核对远端提交、Fabric 所需依赖和河流自身发布的预热状态。仅文档变更，未新增回归或负控。

### 三、我没能验证什么
未验证新世界或重新打包；未见原作者许可公开分发此恢复源码。

### 四、遗留半截的东西
现有源码与历史含保留全部权利的恢复代码，仓库保持私有，见 RECOVERY.md。

### 五、下一轮先做什么
若准备公开，先取得对应授权，或另设只含自有实现和说明的新公开仓库。

### 六、我踩到但已解决的坑
独立运行只需要共用 API；合装道路时复用其内嵌 API。GitHub 可见性不改变原作品许可。

## 2026-11-21：第45轮，开局UI修复、负控与部署完成

### 一、改了什么

磁盘缓存探测前发布进度并逐板更新；构建延续计时与总百分比；失败不计成功，失败卡片保留到世界停止；旧会话不能污染新世界；小屏卡片适配。仅修展示与统计，不改生成几何、预热范围、并行调度、模板、就绪门与存档格式。

### 二、我验证了什么（附变异）

本轮完整回归实际结果：77→83，失败/错误/跳过0；compileJava成功。新增测试均归属河流自身仓库；行为测试驱动正式进度发布器，接线检查仅证明源码入口未旁路，不冒充客户端运行。


S1失败计成功、S2失败状态被清空、S3缓存计数恒零、S4正式入口断开发布，4组定向6项各失败1项，均AssertionFailedError，错误/跳过0。S4为源码接线守卫，其余直接驱动会话与进度。逐文件checkout及SHA恢复；最终83项全绿，compileJava/planContractJar/remapJar成功。fix10已部署并独立复核SHA。

| 负控 | 临时改坏什么 | 断言失败数 | 恢复 |
| --- | --- | --- | --- |
| S1 | 失败板块也计入成功完成 | 1（红） | checkout及SHA一致 |
| S2 | 失败时抹掉进度状态 | 1（红） | checkout及SHA一致 |
| S3 | 缓存探测计数恒零 | 1（红） | checkout及SHA一致 |
| S4 | 正式服务入口断开缓存阶段发布 | 1（红） | checkout及SHA一致 |

S4仅验证正式服务入口的源码接线；其余三组为真实会话与计数行为。

### 三、我没能验证什么

未启动真实Minecraft加载界面，未测实际帧时和整合包Mixin最终运行效果。

### 四、遗留半截

本轮UI代码、测试、构建和部署已完成；实机加载界面及性能尚未验收。此前架构/性能路线未在本轮扩大实施。

### 五、下一轮先做什么

在WAYFARING新世界实测河流缓存→构建、长城稳定并行主阶段、道路走廊复查与扩域轮次；改变GUI比例，确认卡片完整。继续观察H地图。生成质量与性能变化仍需真实游戏数据，本轮不作倍数或帧率承诺。

### 六、踩到的坑

本机Gradle9测试参数文件涉及GBK与中文路径，按既有README/HANDOFF命令处理；不修改依赖校验元数据。只读进程检查未发现Minecraft客户端。当前均只本地操作，不重试此前受限远端传输。

本轮证据：docs/evidence/startup-ui-mutations-20261121.json 与 startup-ui-deployment-20261121.json。
旧包备份与SHA保护回退脚本：D:\云游四海-Mod源码\build\three-mod-startup-ui-20261121-160927。只替换三个模组，未改配置和存档；未启动游戏，回退脚本只预演，未实际回退。
12组负控全部红并恢复；对外道路API共26个class与部署前逐字一致，Architectury生成的内部命名空间随成品版本变化；启用API提供者唯一。生成枚举持久化名字未修改。

## 2026-11-21：第44轮，测试成品部署交接

### 一、改了什么

无生产代码变化；第43轮6018de1成品与最新长城、道路部署至WAYFARING实例。替换原河流前已备份并验完整SHA；配置、存档、跟随更新目录及第三方包未改。

### 二、我验证了什么（附变异）

安装后独立重读SHA等于19aa6e6c492287cfd6981e813f2fcb8da028d8f37c984496393e6170eae1e6e5，streamsreflowing ID唯一。道路API唯一，目标Loader/API满足三包声明的这两项依赖。沿用实际77项全绿、候选4/颜色2/诊断6组红且恢复记录；本轮未重跑或新增负控。

### 三、我没能验证什么

未启动Minecraft或采真实预热、颜色、帧时与联动数据。实例已有polytone/chestfill/punchy三组客户端重复ID，未改，须确认版本。回退脚本未实际回退安装；只读核对不能证明运行成功。

### 四、遗留半截

7个大类、整板BuildContext和独立旧产物守卫待做，准确OpenCL采样保持有意关闭。河流没有remote，不能push；本次安装不替代远程备份。

### 五、下一轮先做什么

按相邻roadweaver-next/docs/PLAYTEST-20261121.md记启动链与真实阶段耗时，用有效计时决定采样/路由/候选扫描/缓存工作集的后续顺序。

### 六、踩到的坑

新旧版本名同为fix9，须按完整SHA识别：原安装e742f358开头，现19aa6e6c开头。完整备份/清单在工作区build/three-mod-playtest-20261121-091158，统一回执在相邻roadweaver-next/docs/evidence/three-mod-playtest-deployment-20261121.json；包回退不撤销世界方块。

## 2026-11-21：第43轮，修复恒零采样计时并迁出诊断

### 一、改了什么

诊断行为修复/生成行为等价：StreamsWorldgen的benchcalls三阶段覆盖同一个long j后全部j-j，六个耗时表达式恒零。将循环/计时/输出迁到63行TerrainCallBenchmark，每个阶段保留独立边界。原20260810种子、两批各1000列、坐标顺序、每批估算→原版高度→群系顺序和累计sink保留；可选估算器缺席标不可测，不报“0ms”。两批坐标不同，原cold/warm标签改为batch并加中文说明。原开关未设仍不执行；不动实际采样器选择、地形算法和schema。StreamsWorldgen减少22物理行，仍未达千行目标。

### 二、我验证了什么（附变异）

先保留旧生产入口，仅新增诊断组件及测试，4项中真实入口字节码守卫失败1项，实测发现6处LLOAD同一局部变量后LSUB；不是凭日志猜测。迁移后4项全过：可控时钟由真实循环内的三种采样器推进，逐阶段精确断言7000/11000/3000ns，同时独立核对2000个坐标、次数/次序、累计sink、两批差异、缺估算器和故障不发布完整测量。单独字节码守卫确认真实入口只调用一次新组件，不能只测未接线类。

| 临时改坏什么 | 实际4项定向测试的断言失败数 | 证据层次 |
| --- | --- | --- |
| 估算计时恒零 | 1 | 真实测量循环 |
| 原版高度计时恒零 | 2 | 真实测量循环 |
| 群系计时恒零 | 2 | 真实测量循环 |
| 缺估算器显示0而非不可测 | 1 | 真实测量循环 |
| 丢第二批 | 2 | 真实测量循环 |
| 正式入口断开新组件 | 1 | 编译后静态接线 |

六组正常编译、全为AssertionFailedError、errors/skipped0；逐组checkout并逐字恢复。见docs/evidence/terrain-bench-{original-failure,mutations}-20261121.json。恢复后完整test/compileJava/planContractJar/remapJar成功 **77项，失败/错误/跳过0**，build/round43-final.log。真实成品再跑颜色双命名空间绑定、修复前后颜色指令对比，以及同一API消费者named/intermediary两端CONTRACT_PASS；见*-diagnostic-final-20261121.json。

### 三、我没能验证什么

可控时钟证明计时计算正确，不是Minecraft性能实测。没有真实ServerLevel执行benchcalls、真实耗时占比或帧率；日志格式的批次标签有意更正，旧cold/warm数据不能继续当冷热对照。静态入口守卫不冒充世界运行。

### 四、遗留半截

全量JDK21词法清点表明河流仍7个生产类超过1000有效行，注释不计：NetworkTracer3570、RiverCarverFeature3213、StreamPrefetcher1740、StreamsWorldgen1653、StreamsConfigFabric1326、OpenCLAccurateProgram1061、RiverEngine1008。已有超限未因本轮新增小组件而消失；不能宣称架构全部完成。OpenCL准确采样保持原有关闭政策。河流无remote，未push/部署；相邻仓push仍被此前自动审批拒绝阻挡，未绕过。

### 五、下一轮先做什么

先给河流板块上下文与整板旧产物建立守卫，再按洪泛/盆地/路由/链后处理拆分；逐个压到≤1000有效行。当前完整清点/分步路线统一见相邻roadweaver-next/docs/THREE-MOD-OPTIMIZATION-20261121.md。诊断修复后再采真实数据，不能沿用恒零日志给性能排序。

### 六、踩到的坑

源码反编译留下的复用变量能正常编译、全套旧测试仍绿，但计时从未有效。另一类误导是把不同随机坐标第二批叫warm；这不是同点缓存热态。修复必须同时保护循环的真实工作量，不能删采样换来“计时更低”。


## 2026-11-21：第42轮，颜色Mixin双命名空间绑定修正

### 一、改了什么

开发行为修复/正式算法等价：Fabric的WaterTintMixin源码原用field_5666/5/4作为Shadow，开发目标BiomeColors只有WATER/GRASS/FOLIAGE_COLOR_RESOLVER。已改用真实官方字段名，由Loom转换生产名。三种颜色原包裹顺序、Kind和回写保留；不改色彩算法、配置、schema，也没动被Fabric构建排除的Forge/NeoForge目录。

### 二、我验证了什么（附变异）

先用真实编译class与真实named Minecraft资源（ASM读取、不初始化客户端）验证旧源码：2项均AssertionFailedError，错误/跳过0。命名绑定测试有3字段非空计数；另一项逐条验证三个解析器的读/Kind/一次wrap/原字段写回。证据docs/evidence/water-tint-original-failure-20261121.json。

| 临时改坏什么 | 实际2项定向测试的断言失败数 |
| --- | --- |
| 水色Shadow改回生产名，混入开发源码 | 2 |
| 水色误用草色Kind | 1 |

两组正常编译、errors/skipped0、全为AssertionFailedError，逐组checkout与原字节校验恢复。恢复后完整test/compileJava/planContractJar/remapJar成功 **73项，失败/错误/跳过0**，build/round42-final.log；本轮实际重建remap日志round42-binding-positive.log中原三条Cannot remap告警消失（最终恢复后remap为UP-TO-DATE）。

tools/review/water_tint_artifact_probe.py用javap核实真实开发class及正式intermediary包，三个字段/描述符均对应各自Minecraft目标；正式Mixin类目标也被映射。修复前正式包的三个生产字段本来就正确，不能把旧告警包装成“当前成品必崩”。修复前后正式类全部方法反汇编一致（忽略常量池编号）；原始class字节不同，不能说逐字相同。见water-tint-bindings-20261121.json。

同一编译API消费者再次分别加载真实named契约和正式jar，两端CONTRACT_PASS、10个类签名一致；见plan-api-artifacts-final-20261121.json。

### 三、我没能验证什么

没有启动开发客户端/正式Minecraft应用全部Mixin，没有现场色彩截图、GPU/渲染或帧率测量。字段绑定及指令等价不等于完整客户端兼容证明；其他可选Mixin尚须运行矩阵。

### 四、遗留半截

第41轮几何去重已交付，其他外来河道/邻坡直接桶扫描仍未优化。NetworkTracer等大类与整板旧产物守卫继续待做。河流无remote，不能push，未部署。相邻道路/长城push仍受此前自动审批拒绝阻挡，未重试或绕过。

### 五、下一轮先做什么

按三仓联合报告先补完整板块BuildContext/旧整板产物，再迁洪泛、路由和跨板连接；现场测候选M/U、采样/落块占比及三模组启动。不要用去掉颜色功能或关闭观测来掩盖负载。

### 六、踩到的坑

本次反编译源码里的field_名字恰好能在正式环境绑定，但不能在官方开发命名空间绑定；必须分别验证两端。不能只看构建告警便报正式包故障，也不能只看正式包无误便放任开发链失效。字节码指令一致与class逐字一致是不同证据。


## 2026-11-21：第41轮，最近河段先去重再投影

### 一、改了什么

行为等价/性能：NearestRiverIndex多个空间桶会登记同一河段，冷查询原样合并后重复做包围盒和投影。新增46行RiverCandidateCollector，用分字代次位集在收集时去重；冷查询保留首次桶次序，热缓存只排序U个唯一编号。聚合从M条目排序O(M log M)改为O(M+U log U)，冷投影候选从M降U；去重状态每线程复用，按最大河段数/64增长，代次回卷清戳。未改变空间准入、半径、同距编号、河道/走廊优先或全部RiverSample字段。

### 二、我验证了什么（附变异）

02dabc6干净开工；河流无remote。基线实际重跑63项。独立冻结02dabc6完整NearestRiverIndex（原源SHA b457856dabc1548368eefba8bc7e7884d44c3942d3154ba799503c5b7e5050c6），先在旧算法验证夹具；新实现2880组全部公开查询逐字段对拍，含跨桶长段、退化段、负坐标、70索引缓存身份/驱逐、冷/热边界及同距优先。另4项去重器测试含真实私有生产候选入口，非空和输入重复量先断言。恢复后完整test/compileJava/remapJar成功 **71项、失败/错误/跳过0**，build/round41-final.log。

| 故意破坏 | 实际断言失败数 |
| --- | --- |
| 保留重复候选 | 3 |
| 丢弃全部候选 | 7 |
| 不推进代次 | 5 |
| 回卷不清戳 | 1 |

最终四组均8项完整执行、errors/skipped0、正常编译、AssertionFailedError，精确checkout与原字节恢复。见docs/evidence/river-candidate-mutations-20261121.json。最初丢候选触发某断言前的NPE，不计交付；补显式非空断言后四组重跑。

真实新旧内核、JDK21/512MiB、每样本4096次nearestTiered查询、5轮预热、7个交替原始样本见river-candidate-benchmark-20261121.json及tools/benchmarks/river_candidate_probe.py：

| 合成夹具 | 中心冷候选 旧→新 | p50毫秒 旧→新 |
| --- | --- | --- |
| 64条短段（中心无河，其他查询有命中） | 0→0 | 0.9962→0.6254 |
| 96条长重叠段 | 5944→96 | 189.1266→19.6074 |
| 单河段 | 16→1 | 0.2239→0.2251 |
| 512条局部短段 | 1024→121 | 6.8750→1.9694 |

本批稳态调用线程分配旧新相同，未把容器容量改善冒充已测分配下降。单河段略慢；这不是游戏帧率、总预热时间或真实河网收益。

### 三、我没能验证什么

没有真实河网长段分布、总查询比例、峰值GC、退出重开或游戏帧率。未改lowerNeighborGrade/inAnyChannel等直接桶扫描，冻结对拍包含它们但不宣称其已优化。原BiomeColors三条重映射告警仍在，未启动Minecraft验证。

### 四、遗留半截

NetworkTracer其他大类阶段仍未拆完。可继续按现场查询次数决定是否复用去重到外来河道/邻坡查询，不能扩大inAnyChannel原先的桶准入口径。道路纵断面和长城规划正独立分析。无remote不能push，未部署。

### 五、下一轮先做什么

采真实候选M/U分布及最近查询占时，按板块/线程汇总而非逐列日志；只有测到占比再扩大优化。继续保留原河网、湖形、浮点和随机次序，架构阶段拆分继续用独立旧产物。

### 六、踩到的坑

冷查询和整格热缓存的候选次序不同，去重必须分别保留原排序契约。位集不能每次全清最大编号范围，否则小查询代价随历史大世界增长；分字戳解决这一点，回卷仍必须守卫。不能用“采样还非空”代替全部字段和segId对拍。


## 2026-11-20：第40轮，原始堆与链编号阶段迁出

### 一、改了什么

行为等价/重构：NetworkTracer的原始DoubleMinHeap迁为80行包级类，优先洪泛和局部盆地继续共用；链编号/端点计数/并查集迁为100行RiverChains，NetworkTracer公开入口及其他阶段的量化私有入口保留转发。保留散列槽位迭代、union方向、同权比较和浮点次序，不以“分区相同”代替根标签相同。NetworkTracer从本轮前3868降至3705物理行；新增文件全部低于1000行。没有混入新几何算法或保存格式变化。

### 二、我验证了什么（附变异）

完整test/compileJava/planContractJar/remapJar成功 **63项，失败/错误/跳过0**，日志build/round40-primitives-final.log。新增4项：真实PriorityQueue独立oracle对拍26000次混合操作（含初始1024同权扩容、NaN/正负零/无穷）；独立冻结4ec0b9b旧链实现，320组随机/乱序/环/分叉/重复端点精确比较根标签，另断言实际非空链应连/应断；实际NetworkTracer入口及负数半格量化边界也验证。

| 临时改坏什么 | 实际断言失败数 |
| --- | --- |
| 同权时改选右子堆 | 1 |
| 禁用链合并 | 2 |
| 负坐标round改floor | 1 |
| 公开链入口断接 | 2 |

四组都正常编译，4项完整执行、errors/skipped0；精确checkout逐字恢复后63项全通过。证据docs/evidence/river-primitives-{extraction,mutations}-20261120.json含原源SHA和恢复SHA，冻结oracle不调用新阶段。

最终成品再次运行tools/review/plan_contract_probe.py，两端10个公开类签名一致、同一消费者字节码均CONTRACT_PASS；证据docs/evidence/plan-api-artifacts-final-20261120.json。不覆盖真实Minecraft加载。

### 三、我没能验证什么

未做整板真实河网或游戏同种子产物对照；PriorityQueue对拍证明本组件次序，不证明所有地形。remapJar仍有原BiomeColors三条映射告警，未把构建成功当全部Mixin运行时验证。没有FPS/MSPT或总预热耗时。

### 四、遗留半截

第五项已完成细湖、原始堆、链编号迁出；NetworkTracer.build仍约1247行，RiverCarverFeature/StreamPrefetcher/StreamsWorldgen的大类分阶段拆分仍未完成。后续需先建立板块BuildContext与整板冻结夹具，再迁洪泛、路由、跨板连接；不能一键搬大块或改计算次序。第六项消费者已分别在道路a235863、长城92425c47完成；第七项离线外层事务已在道路8bc918b、长城d8364723完成，同步默认保留。河流无remote，不能push，未部署。

### 五、下一轮先做什么

先采真实启动/运行期预热和单块施工阶段数据，查新API下的退出重开和失败解释。继续架构时按独立阶段冻结旧实现、再迁移、再变异；新增阶段文件不超1000有效行，不用加注释掩盖巨大方法。

### 六、踩到的坑

旧链说明曾允许槽位顺序改变，本轮仍冻结精确标签以减少影响面；堆同权出队次序会传入洪泛，普通“结果有序”断言抓不到。通过NaN/正负零和同权负控验证，不能把Double比较换成普通大小比较。


## 2026-11-20：第39轮，提供方计划/失败状态契约（第六项提供端）

### 一、改了什么

契约/行为变更：新增纯Java RiverPlanApi v1，固定AVAILABLE/ABSENT/FAILED、不可变列/湖/板块记录、只读Session与预热入口。world为不透明世界句柄，只由提供方验证WorldGenLevel；契约本身不出现游戏或内部引擎类型。Fabric入口安装真实StreamsPlanProvider，WorldBoundRiverSession在调用前后核验引擎身份，旧世界/迟到计划被拒绝，板块列表复制为不可变快照。不打断已进入的共享构建。

启动结果门增加非阻塞snapshot，带唯一session、BUILDING等阶段及有限长失败原因；原ready发布及禁用放行契约保留。planContractJar只导出契约类，运行时由河流成品提供，不让消费方重复内嵌。

### 二、我验证了什么（附变异）

完整test/planContractJar/remapJar成功59项，失败/错误/跳过0。新增API注册表3、启动快照1、世界会话3；已有正式失败接线断言改为要求实际cause。六组变异实际断言红：失败错报缺席1、丢原因1、session恒1→1、忽略世界归属2、迟到查询不复核1、返回可变列表1；均编译成功、无errors/skipped，checkout精确恢复。证据plan-api-mutations-20261120.json。

工具tools/review/plan_contract_probe.py核对named契约与真实intermediary成品10个类的完整javap签名、两端无Minecraft/内部类型依赖；同一消费者只编译一次，在两个实际jar上分别运行，均CONTRACT_PASS。证据plan-api-artifacts-20261120.json含两jar SHA256。常量池重排导致字节不相同，未把字节比较误报ABI破坏。

### 三、我没能验证什么

未启动Fabric/Minecraft联机或退出重进；API探针不实例化实际ServerLevel或完整引擎。remapJar仍输出BiomeColors field_5664/5665/5666三条无法映射告警，构建成功，但本轮未核实其运行时影响，不能声称成品所有Mixin无问题。不能用API ABI对拍代替真实模板/几何验收。

### 四、遗留半截

道路消费端与长城状态命令正在各自仓库验证并独立提交；旧河流兼容适配继续保留。第五项余下大类阶段、七项外层事务未完。提供方查询不提供运行中强制重启按钮；失败需按现有退出重进流程重试，不能调用begin伪造新READY。无remote不能push，未部署。

### 五、下一轮先做什么

完成消费端的缺席/旧版/失败矩阵与负控，再做外层可恢复事务的离线暂停、换代取消与资源结案；跨tick默认仍等真实单块计时。

### 六、踩到的坑

可选API必须物理隔离链接，主消费者不能在缺模组时因类型解析崩溃。纯Java契约也会被remapper重排常量池，验收用签名与同字节码消费者实际运行，不要求class逐字节相同。诊断查询禁止join或触发引擎构建。


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
