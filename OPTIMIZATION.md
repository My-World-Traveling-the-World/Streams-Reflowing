# StreamsReflowing 性能优化分析（2026-11-04）

四路并行只读深读的结果汇总：网络构建 / 地形采样与数值算法 / 雕刻每区块 / 并发与原生。
覆盖 185 个 .java 的全部热路径。**本文所有「收益」除特别注明外都是「操作数削减」，
没有任何一条是实测毫秒或倍数** —— 因为分析阶段没有跑 JFR/日志计时。

---

## 0. 先修正三个会误导优化的前提

1. **`plan()` 命中时零分配**，返回同一个 `ColumnPlan` 对象（`RiverEngine:674-692` 的 8192 槽
   per-thread 直接映射缓存）。`RiverCarverFeature` 里 29 处 `plan/planLean/altPlan` 调用
   **不是** 29 次分配。它从来不是瓶颈。
2. **`plan`/`planLean`/`altPlan` 是【三套独立】的 8192 槽缓存**（`RiverEngine:81-103`：
   O/P、Q/R、W/X）。同一列被 `plan()` 和 `planLean()` 各查一次 = **算两遍**（两套语义不同）。
3. **板块采样点按全局晶格精确铺贴，跨板块不重叠**（`LowResGrid:186-187` 用
   `floorDiv(origin,n)*n` 对齐）。跨缝重复率只有 2~4%。

---

## 1. 成本模型：MAX 到底贵在哪

```
POTATO：144 板块 × ~114 列 × 11 次 compute ≈  18 万次
MAX   ： 36 板块 × 116,000 列 × 11 次 compute ≈ 4,600 万次
                                              ⇒ 差 ~250 倍
```

- **每列 11 次 `compute()`**（`StreamsWorldgen:169-192`）：1 次顶端探测 + 9 次二分 +
  1 次单元一致性校验。病态列走 `:222-235` 阶梯兜底，最坏 ~58 次。
- **`terrainSampleSpacing=0` 让 MAX 成为唯一「逐列全采」的档**。POTATO~EXTREME 都是
  「采 1/4 格点再双线性内插」（`LowResGrid:129-131`）。**插值替代逐点采样在板块层面早就做了，
  只有 MAX 没做**（因为它要精度）。
- **MAX 的浪费不是「重复计算」，而是丢失了 MC 的格点共享**：MC 自己一个区块 256 列共享
  25 个噪声格点（cellWidth=4/cellHeight=8）再三线性内插；MAX 走 `getBaseHeight`，
  **每列新建一个 NoiseChunk 整列重填**。

### mode 0 / 1 / 2 的真实差别（只有一个 boolean）

`terrainSamplingMode` 存进 `FastCal` 的 `gradePolicy` 位，**唯一消费点**是
`StreamsWorldgen:1109-1119` 的 `groundHeight`：

```java
if (j == 2) return this.height(x,z);                  // mode 0：判定海拔也用估计，最快
if (j == 1 && corr.gradeSafe(d)) return corr.correct(d) - 1.0;   // mode 1：逐列检查高程带
return 海洋 ? -1000 : 真值.height(x,z);                // 否则回落 vanilla getBaseHeight
```

**网格采样永远走 `land`，与 gradePolicy 无关。**
**MAX 档 `terrainSamplingMode=2` 直接不建估计器**（`StreamsWorldgen:567-568`），
且**没有任何系统属性能把 MAX 拉回 mode 1**（`-Dstreamsreflowing.exact` 只能压到 2）。

---

## 2. 第一梯队：不改结果、可安全落地

| # | 改动 | 位置 | 收益 | 依据 |
|---|---|---|---|---|
| 1 | **区块级早退** | `RiverCarverFeature:215` 之后 | 无河区块成本归零（省 3~5 万块读/区块） | `corridorNearChunk`(RiverEngine:960) / `lakeNearChunk`(869) 已存在 |
| 2 | **二分热启动** | `StreamsWorldgen:169-192` + `LowResGrid` 行循环 | 求值 11→6~7（−36~45%） | 行优先 + 行带不跨线程 → hint 天然可用 |
| 3 | 删掉顶端探测 | `StreamsWorldgen:171-174` | −9% 求值 | 已逐分支论证返回值一致 |
| 4 | `startupPlateWorkers` 7→12 | 配置 | 预热 1.3~1.7× | 7 owner 填不满 14 采样线程，`parallelFor` 是屏障式 |
| 5 | `platesWithin` 按板块格记忆化 | `PlateLattice:88,107` | 消 0.3~1.2 MB/区块垃圾 | 只依赖板块整数索引，逐位等价 |
| 6 | `786`/`893-894` 重复 lake 查询合并 | `RiverCarverFeature` | 省 12,800 次 lakeAt/区块 | 同迭代、无副作用 |
| 7 | Voronoi 行区间 + 两次 9 邻域融合 | `LowResGrid:147-209` | 板块前开销 −50~70% | 凸集可二分；需集合相等回归 |
| 8 | `NetworkTracer:2741` 展开 `new int[][]` | 1 行级 | BFS 每单元 5 对象 → 0 | 纯分配 |
| 9 | `StreamsWorldgen` 可变 context | 4 处 | 每列 11 分配 → 1 | `FunctionContext` 只 3 个 getter |
| 10 | `NetworkTracer:2488` `new boolean[n]` 复用 | 1 处 | 466 KB → 0 | 纯分配 |
| 11 | `PlateRiverCache:96` 线程名大小写 | 1 行 | 消潜在 300 秒卡顿 | 预热池名是 `Streams-Startup-Prewarm-`（大写 S） |
| 12 | `CachingTerrainSampler:21-22` → `LongAdder` | 2 处 | 消 cacheline 乒乓 | 只被 `hitPercent()` 读 |
| 13 | `BandCorrection.correct` 改直接索引 | `:1585-1598` | 省每列 ≤24 次线性扫描 | center 构造上恒为 `band*16+8` |
| 14 | `1402` 长除法外提 | 5 行 | 12,800 → 400 | 只依赖 x,z |
| 15 | `Math.hypot` 每列算一次传参 | 每列 3~5 次 → 1 | 纯赚 | 只做「算一次」，完全等价 |
| 16 | `StreamSnowCleanupFeature:261-295` 33³ 裁剪 | 裁到有水的列 + y 限幅 | 143,736 → ~10k 块读/区块 | 需比对树叶 PERSISTENT 语义 |

**#2 的第二重收益往往大于第一重**：`d = est/real` 从 0.55~0.7 压到 0.35~0.45，会让
`StreamsWorldgen:671/677/732/744` 的判决把一批原本 OFF/HYBRID 的世界**翻到 ON**
（grade 不再回落 vanilla `getBaseHeight`）。

---

## 3. 第二梯队：收益大但需先存基线

| # | 改动 | 收益 | 代价 |
|---|---|---|---|
| 17 | **MAX 档 `terrainSamplingMode` 2→1** | 板块采样 −30~50%（唯一能碰最重档位） | **改结果**：网格高度变估计值，河道路径会变 |
| 18 | 段数抽稀（三轮细分 ×8） | 3~8×，全线复利 | 改几何密度，需 `[SR-QUALITY]` 基线 |
| 19 | `P8` 邻域平滑 → 可分离 1D min/max | 20,736 → ~2,000 | 低（可证等价） |
| 20 | `2483` 289 格 → 欧氏距离变换 | 74k → ~2k | 平手规则需 65,536 组对拍 |
| 21 | `NearestRiverIndex` → CSR int 桶表 | 列路径 −30~50% | 低 |
| 22 | 全网格 `ArrayList<Integer>` → `int[]` | route 段 1.3~2× | 中（35 处 sweep） |
| 23 | `PriorityQueue<Integer>` → 桶队列 | O(n log n) → O(n) | 优先级须严格保序 |

---

## 4. 明确不做（每条都有量化理由）

| 想法 | 为什么 |
|---|---|
| **写 C++ / JNI** | FFM 在 Java 21 是 **preview**（JEP 442，JDK 22 才 final），mod 不能要求 `--enable-preview`；仓库**已经搬过一次**（OpenCL 版，默认关）；**二分不适合批量/GPU**（数据依赖链 + 每步发散分支），现有 OpenCL 路径只能「整列 49 格点全算再内插」；`TerrainSampler.heightsBatch` 批量接口与扁平数据布局已就绪；收益 <15% 且只作用于「一个存档一次」 |
| **SIMD（Vector API）** | `perm[]` 查表是 **gather，JDK 21 x86 C2 无 intrinsic**，退化成逐元素 load；每探点都是数据相关分支；`jdk.incubator.vector` 是 incubator，**mod 无法给自己加 `--add-modules`**；核已被 14 线程占满，ILP 收益 ≈0 |
| **Y 上限早退** | `LowResGrid.a[]` 高度同时被当绝对高程和**差分坡度**用；封顶会让 200 格高原列与 130 格邻列坡度失真 → 流向改变 → 结果不可控。收益仅 −11%（384→180，9→8 次） |
| **跨板块采样缓存**（照搬道路） | 采样点精确铺贴不重叠，重复率 2~4%；已有 `CachingTerrainSampler`（4 张 CHM、65,536）；lock-free 无锁可分 → <1% |
| **`columnPlanArray` 换 SoA** | 跨 chunk 复用要 4,352 次字段拷贝，比 1 KB 引用数组更贵 → 反而制造 GC |
| 合并 `STREAM_VEGETATION` 与 `SNOW_CLEANUP` | 中间夹着整条原版植被/树链，提前跑会判错浮空雪 → 语义错 |
| `BankStyle*` / 捕获 lambda / `ChunkPlanCache` 上限 | 纯 codec / JIT 已消除 / 1 MB 不是问题 |

---

## 5. 现有的测量手段（改之前先用）

| 探针 | 位置 | 看什么 |
|---|---|---|
| `[SR-PHASES]` | `NetworkTracer:1325` | 四列：`fill` / `route` / `chains+lakes` / `post` |
| `[SR-ROUTE]` | `NetworkTracer:1162` | `pflood / d8+thin / lakes / ponds / reach / trim` |
| `[SR-CHAINSTEP]` | `1330` | `snap followGround meander confine smooth emit` 比例 |
| `[SR-QUALITY]` | `1340` | `segs` 与 `length` → **mean seg length**（若 ≈0.6~1.0 方块，证实段数 ×8 膨胀） |
| `[SR-LAKESTEP]` | `843` | `fineFloods` 是否远大于 `built` |
| 每板块 ms + cache 命中率 | `StreamsWorldgen:1200-1204` | 采样占板块时间的比例 |
| `classify/height us/call` | `[SR-PREP]` `StreamsWorldgen:305-308` | 单次采样成本 |
| `us/call` | `[SR-BENCH]` `StreamsWorldgen:1197` | 同上 |

开关：`-Dstreamsreflowing.caldiag`（阶段级）、`-Ddeepdiag`、`-Dsampletime`、
`-Diraudit=N`（高度误差分布：exact/±3/±6/7+ 四档）。

### 第一步实验（10 分钟，能定论往哪投人力）

```bash
-Dstreamsreflowing.caldiag=true
```

跑一局后把 121 条 `[SR-PHASES]` 四列求和：

- `Σfill / 总时长 ≤ 20%` → 地形采样不是目标，别碰 #2/#3，先做 #1
- `Σ(route+chains+lakes+post) ≈ 7 × 总时长` → **owner 绑定**，抬 `startupPlateWorkers` 就见效
- `Σall ≈ 14 × 总时长` → **池已饱和**，只能改算法（#18/#19/#20）

---

## 6. 关于「MAX 质量 + POTATO 速度」的边界

**纯度纯 CPU、不改结果的前提下，MAX 能从 ~250× POTATO 压到约 100~130×**（靠第一梯队）。
再往下必须动一样东西：

1. **改精度语义**（#17：MAX 的 mode 2→1）→ 网格高度从「逐列精确」变「带通标定估计」
2. **用 GPU**（`gpuTerrainSampling=true`，1 行配置，现有代码）→ 但会与 C2ME/RoadWeaver 抢 OpenCL

**这不是算法不够好，而是「MAX 的定义本身就是逐列精确采样」—— 精度和速度在这里是同一件事的两面。**

---

## 7. 已知的真实缺陷（与性能无关，但一并记下）

- **5 个线程池全部 daemon、全部 `static final`、没有一个有正常关闭契约**。
  `StreamPrewarmService.stop()` 与 `StreamPrefetcher.onServerStopping()` 都不关 executor
  → 集成服务器切存档时最多约 36 个 daemon 线程跨世界存活。
- **`PlateRiverCache:96` 的线程名判断是全小写 `streamsreflowing-`**，而两个预热池
  叫 `Streams-Startup-Prewarm-` / `Streams-Runtime-Prewarm-`（**大写 S**）→ 走到
  `get(300 秒)` 超时路径。若 plate 依赖成环，预热线程会先干等 300 秒。
- **`StreamPrefetcher:553-566` 的 `Thread.sleep(250)` 在池线程里自旋，最长 60 秒占用一个线程。**
- **`StreamPrefetcher:506-514 / 531-539 / 599-606` 在单锁下 O(n) 遍历 `PriorityBlockingQueue`
  + `remove`**，发生在紧急 chunk demand 的提交路径上。
- **`FormSampler:22` 用 `ConcurrentHashMap<Thread, Work>` 当 ThreadLocal**（同文件 25-26 行
  已经在用 `LongAdder`，说明作者知道正确写法）。

---

## 8. 实施阶段的修正（2026-11-04，S3 子代理实测反馈）

以下是**分析结论被实施时证伪**的三条。记在这里，免得以后有人照着错方案重做。

### 8.1 ❌ A02「删掉二分前的顶端探测」—— 已证伪，不要做

`StreamsWorldgen` 原第 171-174 行：
```java
if (densityFunction.compute(new SinglePointContext(n, maxY, n2)) > 0.390625) return maxY + 1;
```

**只在「该列从 minY 到 maxY 单调（下实上空）」时删掉才等价。**
反例：`minY=-64, maxY=320`，第一次 `mid = (-64+320)>>>1 = 128`；只要该列 128 处是空气
（洞穴 / 悬垂 / 浮岛 / 3D 噪声的 slide/squeeze），`hi` 立刻降到 128，二分收敛到**真实地表 p**
并返回 `p+1`；而原实现返回 `321`（`maxY+1`）。**差一个世界高度。**

而 `finalDensity` 是 3D 噪声 + slide/squeeze，单调性不可证 —— 而且 `b()` 阶梯兜底
的存在本身就说明作者预期非单调列。**⇒ 保留原行。**

### 8.2 ❌ A11「用 `floor((d-8)/16)` 直接索引 BandCorrection」—— 公式会取错带

建表处虽然 `center = 带起点 + 8`，但 `centers` 是**稀疏**的：只保留样本 ≥6 的带，
带与带之间可以有 32 / 48 的空档。连续 16 的公式会落到错误的带上。

**正确做法：对 `centers` 做 lower_bound 二分**（找第一个 `centers[i] >= d` 的下标），
在前置守卫（`d > centers[0]` 且 `d < centers[len-1]`）下落点唯一，
与原线性扫描的 `n` 恒等，后续 lerp 一字未动 → 逐位等价。

### 8.3 ❌ A04「按 `(floor(x/b), floor(z/b))` 记忆化 9 个 seed 距离」—— 前提错了

`d7 = sqrt((seedX-d)^2 + (seedZ-d2)^2)` 含**连续的 d/d2**，按板块格记忆化整份结果
会改变结果。

**正确做法：只缓存真正只依赖整数索引的那个量 —— 9 个 `seedX/seedZ` 原值**
（`(n+j, n2+i)` 的纯函数，含实例的 seed/scale/jitter）。命中即 0 次 `Hashing.unit`
（原每次调用 18 次）；`-d`、`-d2`、sqrt、比较、数组大小、遍历顺序、平局规则全部不动
→ 逐位等价。四个 9 邻域方法（`platesWithin` / `plateAt` / `nearestTwo` / `borderDistance`）
共用，因为是同一份收益来源。

**线程安全：必须每实例 `ThreadLocal`**（`PlateLattice` 全仓只有 `RiverEngine:145` 一处 new，
但 sampler 线程池会并发读写，普通字段会串数据）。

### 8.4 ⏭ A06「融合两次 9 邻域扫描」—— 跨文件 + 很可能更慢

1. `PlateDomain` 的 `a`（PlateLattice）与 `b`（PlateKey）都是 `private final`，且不在
   `LowResGrid` 的范围内 ⇒ 融合必须再改一个文件（加一个委托方法）。
2. **更要紧：现状是短路**
   `plateDomain.contains(x,z) && plateDomain.borderDistance(x,z) >= margin`。
   MAX 档约 75% 的列在 `contains` 就 false，`borderDistance` 的 9 次 `Math.sqrt` 根本不算。
   融合成「一次扫描同时给 winner + 最近/次近」会让这 75% 的列**白付 9 次 sqrt**。
3. 这部分收益 **已由 A04 的 seed 缓存以等价方式拿到**（`contains` 与随后的
   `borderDistance` 共享同一次 9 邻域 seed 计算）。

### 8.5 ⏭ A40「LowResGrid 双线性内插循环展开」—— 没有循环可展开

367-378 是两条直线双线性内插（各 4 次数组读 + 3 次乘加）。唯一"安全"改写是把
`Math.floorDiv(n,n3)` 换成除法（负数坐标不等价）或把 `/(double)n3` 换成乘倒数
（浮点舍入不同）—— 都不满足逐位等价。

### 8.6 ✅ A09 的一处必要偏离

`openAt` 里的 sampler 会被 `LowResGrid` 的 `streamsreflowing-sampler` 线程池**并发调用**，
用普通实例字段是数据竞争（会真的算错）⇒ 该处用 `ThreadLocal<MutableCtx>`。
主路径 `a()` / `b()` 仍是「每列建 1 个」，严格按原方案。
（旁证：MC 自己的 `ContextProvider` 就是复用可变 context，说明 `compute` 不保留引用。）

---

## 9. 已落地（第二轮，2026-11-04 晚）—— 高海拔列在花钱之前丢弃

**commit `e05ec11`（在 `7bf5e88` 的 40+ 处之后又一条）**

### 问题

`maxRiverPlanY`（默认 140）以上不出河流，但门槛原来只挂在
`plan` / `planLean` / `altPlan` 的 `@At("RETURN")` 上 —— 完整 `ColumnPlan` 算完才换成 `NONE`。
白算的是 `RiverEngine.a(double,double,RiverSample,boolean)` 里最贵的两段：
河床 `inset` 解算（内部调 `lakeAt`）与河方向解算。

### 做法与等价性

三条入口全部漏斗到同一个私有工人方法 `a(DDL...;Z)`，在它的 `@At("HEAD")` 注入即可全罩住。

判据用**与门槛完全相同的公式**算水面高度下界：

```
waterSurfaceY = max(seaLevel-1, floor(gradeY) - insetFor(width, cfg))
             >= floor(gradeY) - insetFor(width, cfg)          // max 只会抬高
```

因此 `floor(gradeY) - insetFor(width,cfg) > ceiling ⇒ waterSurfaceY > ceiling`
⇒ RETURN 处的门槛本来就会拒绝 ⇒ 提前返回 `NONE` **逐位等价**，只是省掉后面的计算。

`gradeY` 来自河段坡度剖面（流向网络早已算好），**不触发任何地形采样** —— 这正是它能
放在采样/解算之前的根本原因。

### 与第 4 节「明确不做」的区别

第 4 节否决的是**在 `LowResGrid` 里给高度封顶**（高度同时当绝对高程和差分坡度用，
封顶会让 200 格高原列与 130 格邻列坡度失真 → 流向改变）。本条**不改任何高度值**，
只在「已经确定要拒绝」时提前退出，所以不触碰坡度语义。

### 验证与测量

```
compileJava 0 错误 / build 0 错误
字节码：plaincraft$gateBeforePlanWork(DDLRiverSample;ZLCallbackInfoReturnable;)V 存在，
        引用 RiverEngine.insetFor(DLStreamsConfig;)I 与 StreamsPrewarmConfig.maxRiverPlanY()I
产物：streamsreflowing-2.12.8-decompiled.jar 3,269,230 B（+853 B，即新增方法）
部署：StreamsReflowing-1.21.1-fabric-2.12.8-heightgate.jar，SHA 525c515e097fb4a4
真机：未验
```

**测量开关**：`-Dplaincraft.streams.heightGateDiag=true`
每 4096 次早退打一行 `[SR-HEIGHTGATE] early exits so far: N`，
一次世界生成就能看出高海拔河流占多少列。
