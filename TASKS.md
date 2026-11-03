# StreamsReflowing 优化任务清单

来源：四路并行只读深读（网络构建 / 地形采样与数值算法 / 雕刻每区块 / 并发与原生）+ 主线自查。
`改结果` 列 = 是否改变世界生成输出。**改动量：S ≤ 10 行 / M ≈ 10~50 行 / L > 50 行。**

---

## A 梯队 · 零风险 · 不改结果 · 可立刻做

| ID | 任务 | 位置 | 收益（操作数） | 量 | 需回归验证 |
|---|---|---|---|---|---|
| A01 | 区块级早退：无河区块直接 return | `RiverCarverFeature:215` 后 | 省 3~5 万块读/区块（无河区块归零） | S | **是**（跳过前后区块 hash 全等） |
| A02 | 删掉二分前的顶端探测 | `StreamsWorldgen:171-174` | −9% 求值 | S | 否（已逐分支论证） |
| A03 | 二分热启动：用相邻列高度收窄区间 | `StreamsWorldgen:169-192` + `LowResGrid` 行循环 | −36~45% 求值；且翻转 ON 判决（第二重收益） | L | **是**（`-Diraudit` 分布对比） |
| A04 | `platesWithin` 按板块格记忆化 | `PlateLattice:88,107` | 消 0.3~1.2 MB/区块垃圾 | M | 否（逐位等价） |
| A05 | 合并 `786`/`893-894` 重复 lake 查询 | `RiverCarverFeature` | 省 12,800 次 lakeAt/区块 | S | 否 |
| A06 | Voronoi 行区间 + 两次 9 邻域扫描融合 | `LowResGrid:147-209` | 板块前开销 −50~70% | M | **是**（逐板块集合相等） |
| A07 | BFS 里 `new int[][]` 展开成 4 个方向 | `NetworkTracer:2741` | 每单元 5 对象 → 0 | S | 否 |
| A08 | `new boolean[lowResGrid.size()]` 复用 | `NetworkTracer:2488` | 466 KB → 0 / 候选 | S | 否 |
| A09 | `SinglePointContext` → 可变 `FunctionContext` | `StreamsWorldgen` 4 处 + `RiverFlowFields:242` | 每列 11 分配 → 1 | M | 否 |
| A10 | `CachingTerrainSampler` 计数器 → `LongAdder` | `:21-22` | 消每次采样的 cacheline 乒乓 | S | 否 |
| A11 | `BandCorrection.correct` 改 O(1) 索引 | `:1585-1598` | 省每列 ≤24 次线性扫描 | S | 否 |
| A12 | `floorMod(Mth.getSeed)` 提出 32 轮循环 | `RiverCarverFeature:1402` | 12,800 → 400 次长除法 | S | 否 |
| A13 | `Math.hypot` 每列算一次、传参复用 | `699/812/818` + 8 个 helper | 每列 3~5 次 → 1 | M | 否（只做「算一次」） |
| A14 | `NetworkTracer` 全网格 `ArrayList<Integer>` → `int[]` | `273-279` + 35 处 sweep | route 段 1.3~2×；少 29k~466k 装箱 | L | 否（逐位对齐） |
| A15 | `PriorityQueue<double[]>` → 打包 `long` 堆 | `NetworkTracer:2356/2371/2385` | 分配 → 0 | S | 否 |
| A16 | `chainIds` 4 个 `HashMap<Long,Integer>` → 原始类型开放寻址表 | `NetworkTracer:1897-1926` | 装箱 0；1.5~2.5× | M | 否（保持端点量化不变） |
| A17 | `ArrayList<Double>` → `double[]` | `NetworkTracer:1374-1519` | 逐点装箱 → 0 | M | 否 |
| A18 | `chainIds`/高程传播结果按 build 复用（现被调 ≥8 次） | `NetworkTracer` | 同一拓扑不再反复重解 | M | 否 |
| A19 | `insetFor` 24 次二分 → 256 项反查表 | `NetworkTracer:1672-1685` | O(24) → O(1) | S | 否（可位等价校准） |
| A20 | 湖×段 / 链×湖 / 段×湖 三重全扫 → 桶化或掩码反查 | `1357-1367 / 1811-1815 / 1856-1861` | O(S·L) → O(S+L) | M | 否 |
| A21 | 只取前 4 个却全排序 → 4 槽部分选择 | `NetworkTracer:2508` | O(k log k) → O(k) | S | 否 |
| A22 | `Map<Long,Double>` → 已有 `ColumnCache` | `NetworkTracer:3198` | 装箱 → 0 | S | 否 |
| A23 | `NearestRiverIndex` → CSR `int[]` 定长桶表 + 扩缓存 | `371-455`、`43-49` | 列路径 −30~50%；分配 → 0 | M | 否 |
| A24 | `networkFor` 线程本地 1 槽 → 8 槽直接映射 | `RiverEngine:175-187` | 每列 3~4 次 CHM 查询 → ~0 | S | 否 |
| A25 | `platesWithin` 写入线程本地复用数组 | `PlateLattice:85-118` | 每列 3~4 次分配 → 0 | S | 否 |
| A26 | `P8` 邻域平滑 81 格 → 两次可分离 1D min/max | `RiverCarverFeature:413-430` | 20,736 → ~2,000 | M | 否（可证等价） |
| A27 | `P12` 门 648 `getHeight` 用已有地表预筛 | `:1168-1194` | 648 → ~120 | S | 否 |
| A28 | 删掉 `343`/`370`/`611` 重复的 `plan()` 预编 | `RiverCarverFeature` | 省 256~1,024 次无效调用/区块 | S | 否 |
| A29 | `skinAllWater` 内区改用 `ChunkPlanCache` | `:2031` | 省 256 次完整采样/区块 | S | 可能（两函数语义本就不同） |
| A30 | `StreamVegetationFeature` 按 biome 缓存候选 `PlacedFeature` 列表 | `:287-303` | 每列 O(#features) → O(1) | M | 否 |
| A31 | `ChunkPlanCache` 改 long 键 + 线程本地环形数组 | `:13` | 去跨线程争用 + Key 分配 | M | 否 |
| A32 | `StreamSnowCleanupFeature` 33³ 扫描裁到有水的列 + y 限幅 | `:261-295` | 143,736 → ~10k 块读/区块 | L | **可能**（树叶 PERSISTENT） |
| A33 | `StreamSnowCleanupFeature` 竖直扫描先查 `MOTION_BLOCKING` | `:147` | 每列 34 格 → 视实际雪线 | S | 否 |
| A34 | `RiverFlowField` 5×5 最小二乘 → 可分离卷积 | `:317-376` | O(25n) → O(10n) | M | 否（矩累加可复现） |
| A35 | `RiverFlowField` `PriorityQueue<Long>` → 桶队列 | `:199/251/266/313` | O(n log n) → O(n) | M | 待证（浮点量化） |
| A36 | `CachingTerrainSampler` 容量 65,536 → 262,144 | `StreamsWorldgen:1165` | 消灭 MEDIUM/HIGH 尾部擦边淘汰 | S | 否 |
| A37 | `BandCorrection.gradeSafe` 同类线性扫描改索引 | `StreamsWorldgen:1600-1613` | 每列省一次扫描 | S | 否 |
| A38 | `FormSampler` `ConcurrentHashMap<Thread,Work>` → `ThreadLocal` | `:22` | 去掉每次 height 的 CHM 查表 | S | 否 |
| A39 | `ChunkPlanCache.put` 的 `BlockState[512]` 快照复用 | `RiverCarverFeature:161` | 2 KB/区块 → 0 | S | 否 |
| A40 | `LowResGrid` 双线性内插循环展开（手写 ILP） | `:357-378` | 小（只覆盖 25% 格点） | M | 否 |

---

## B 梯队 · 需先存基线 · 收益大

| ID | 任务 | 收益 | 代价 | 量 |
|---|---|---|---|---|
| B01 | **MAX 档 `terrainSamplingMode` 2→1** | 板块采样 −30~50%（唯一能碰最重档位） | **改结果**：网格高度变估计值 | S |
| B02 | **段数抽稀**（三轮细分 → ×8） | **3~8×，全线复利** | 改几何密度，需 `[SR-QUALITY]` baseline | M |
| B03 | `2483` 的 17×17=289 格 → 精确欧氏距离变换 | 74k → ~2k | 平手规则需 65,536 组对拍 | L |
| B04 | `PriorityQueue<Integer>` 填洼 → 桶队列 | O(n log n) → **O(n)** | 优先级须严格保序 | M |
| B05 | `capture` 4 轮改「变更集驱动」+ 地形缓存提出循环 | 1.5~3× | 收敛判据需保持 | M |
| B06 | `Math.pow(slope, 0.8)` → 1024 项 LUT + 插值 | 每列 1~2 次 libm 调用 | **改结果**（~1e-6 相对误差，floor 偶发差 1 格） | M |
| B07 | `atan2` 8 方向编号 → 4 次比较 + 符号查表 | 每列 1 次 → 常数 | 需复刻「tie 向上取整」 | S |
| B08 | `WATER_SKIN` 并入 `RIVER_CARVER`（内区零查询） | 省 256 planLean + 576 列扫描 | 中：须保留「无河区块也铺水皮」路径 | M |
| B09 | `RIVER_FLOW` 并入 `STREAM_VEGETATION` 或 `SNOW_CLEANUP` | 省 512 getHeight + 256 getBiome | 低 | M |
| B10 | `SNOW_CLEANUP` 1024 列环退化为 256 + 复用 carver 外溢信息 | 省 768 planLean | 低-中（改缓存 ABI） | M |

---

## C 梯队 · 配置级（不改代码，立刻有效）

| ID | 任务 | 收益 | 备注 |
|---|---|---|---|
| C01 | `startupPlateWorkers` **7 → 12** | 预热 1.3~1.7× | 纯配置，字段上限 `min(12, processors)` |
| C02 | `runtimeScanIntervalTicks` 20 → 200~1200 | 若开运行时预热，避免每秒一扫 | 当前 `runtimeEnabled=false` 无影响 |
| C03 | `startupRadiusChunks` 128 → 64 | 预热再省约 4 倍面积 | 代价：外围首次走到要现算 |
| C04 | `gpuTerrainSampling` false → true | 精确采样搬 GPU，零新代码 | **用户已明确不开** |

---

## D 梯队 · 缺陷修复（与性能无关，但是真 bug）

| ID | 任务 | 位置 | 后果 |
|---|---|---|---|
| D01 | 线程名判断大小写：预热池名 `Streams-*`（大写 S）不匹配 `startsWith("streamsreflowing-")` | `PlateRiverCache:96` | 预热线程走 300 秒超时路径；依赖成环时干等 300 s |
| D02 | 5 个线程池无关闭契约 | `StreamPrefetcher` / `StreamsPrewarmService` | 切存档残留 ~36 个 daemon 线程 |
| D03 | `Thread.sleep(250)` 在池线程里自旋最长 60 s | `StreamPrefetcher:553-566` | 池线程被让出 60 秒 |
| D04 | 单锁下 O(n) 遍历 `PriorityBlockingQueue` + `remove` | `StreamPrefetcher:506-514/531-539/599-606` | 压力最大时提交延迟最差 |
| D05 | `FormSampler:22` 用 CHM 当 ThreadLocal | `FormSampler` | 每次 height 多一次 CHM 查表 |
| D06 | `1388/1392` 的 `finally { …; continue; }` 类死代码 | 已在反编译修复阶段处理 | — |

---

## E · 明确不做（不要再花工期）

| 想法 | 否决理由 |
|---|---|
| 新写 C++ / JNI / Panama | FFM 在 Java 21 是 **preview**；仓库已有 OpenCL 版（默认关）；**二分不适合批量**（数据依赖 + 每步发散分支）；批量接口与扁平布局已就绪；收益 <15% 且只作用一个存档一次 |
| SIMD（`jdk.incubator.vector`） | `perm[]` 查表是 **gather，JDK 21 x86 C2 无 intrinsic**；数据相关分支无法无分支化；incubator 模块需 `--add-modules`，**mod 加不了**；核已满，ILP ≈0 |
| Y 上限早退（`maxRiverPlanY` 前移到采样） | 高度同时被当绝对高程和**差分坡度**；封顶 → 坡度失真 → 流向改变 → 结果不可控。收益仅 −11% |
| 跨板块采样缓存（照搬道路） | 采样点精确铺贴不重叠，重复率 2~4%；已有 `CachingTerrainSampler`；lock-free 无锁可分 → <1% |
| `columnPlanArray` 换 SoA | 跨 chunk 复用要 4,352 次字段拷贝，比 1 KB 引用数组更贵 → 反而制造 GC |
| 合并 `STREAM_VEGETATION` 与 `SNOW_CLEANUP` | 中间夹整条原版植被/树链，提前跑会判错浮空雪 → 语义错 |
| `BankStyle*` / 捕获 lambda / `ChunkPlanCache` 上限 / `WaterSkinFeature` | 纯 codec / JIT 已消除 / 1 MB 非问题 / 纯转发 |

---

## 统计

```
A 梯队  40 条（零风险、不改结果）
B 梯队  10 条（需基线）
C 梯队   4 条（配置级）
D 梯队   6 条（真缺陷）
E        7 类（明确不做）
```

## 建议的落地批次

```
批次 1（最稳，不动任何语义）
  A02 A04 A05 A07 A08 A09 A10 A11 A12 A15 A19 A21 A22 A24 A25 A28 A36 A37 A38 A39
  + C01 + D01
  ⇒ 全是不需回归验证的分配/常数级修复，外加一条最大的配置收益

批次 2（需回归验证）
  A01 区块早退   A03 二分热启动   A06 Voronoi   A32 雪裁剪
  ⇒ 逐条配 hash / iraudit / 集合相等对比

批次 3（结构性）
  A14 A16 A17 A18 A20 A23 A26 A27 A29 A30 A31 A33 A34 A35 A40
  ⇒ 网络构建与特征合并，改动面大但都可证等价

批次 4（要你拍板精度语义）
  B01 MAX mode 2→1   B02 段数抽稀
```
