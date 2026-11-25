# 河流开发指南与交接

## 2026-11-25 · 2.12.8-fix16 · 冻结远景匹配真实河道

**本批最终交付**：`2.12.8-fix16` 已部署WAYFARING，代码提交 `d4c51503`；恢复所有负控后的最终回归103项、零跳过/失败/错误，正式构建成功。部署件与构建件SHA256一致：`60483e6d58b13bd5a736da1a98e7fbf502c475f98dc76c7e3e4eea25eb098032`。旧包备份 `D:\minecraft\Plain Craft Launcher 2\.minecraft\backups\wayfaring-optimization-20261125-204514`，四包完整清单在SeedGen补丁的build/optimization-deployment-manifest.json。未启动完整整合包验收画面或帧率，未改玩家存档。

1. **改动**：远景按sampleTiered选择实际覆盖列的河道，避免近窄河挡住远宽河。抽RiverWaterGeometry复用原施工湖出口上游6格水位过渡，冻结湖邻板范围补6格，仅只读peek、不建板。未改真实河网算法与缓存格式。
2. **验证**：恢复变异后全量103项JUnit，跳过/失败/错误均0，remapJar与planContractJar通过。新增同板宽窄河反例和真实RiverEngine.plan对拍；地形sampler、写存储端口均禁止调用。
3. **未验证**：完整整合包河床与流体画面、实际密集河区尾延迟和帧率未验收。湖床仍为声明GEOMETRY的近似，不承诺最终河岸方块。
4. **负控**：sampleTiered退回sample→覆盖宽河测试红；去掉湖出口过渡→真实engine水79而预览77，对拍红。每组git checkout恢复，表在相邻SeedGen补丁build/next-mutations.json。
5. **下一步**：先固定种子测真实查询分布；nearestCoveringForeign/inAnyChannel还有跨桶重复，但其早停特征可能让全量候选收集负优化，未基准前不盲改。
6. **历史与回退**：基线a4489c1，预览行为修复+施工规则等价抽取。fix16正式包已构建，未改玩家存档；未在本轮声称新增全长生成能力。

## 2026-11-25 · 河网几何远景（2.12.8-fix15）

1. **改动**：注册rivers，从当前世界已就绪的邻板河网冻结引用读取河床、水面及清空区间。peek不认领任务、不等待future、不读盘；任一所需邻板未知则返回UNAVAILABLE。输出GEOMETRY，真实岸材、流体与装饰由实际区块覆盖。
2. **验证**：XML汇总101项JUnit，skipped/failures/errors均0；remapJar、planContractJar通过，新增河床/水面几何和未命中不建板两项。日志build/preview-final.log。
3. **未验证**：未测完整整合包的密集河网尾延迟、帧率及远景画面；几何不等于原河岸真实方块。未扩大全局预热范围，也不为远景启动额外建板。
4. **负控**：河床从水面减深度改为加深度，RiverPreviewTest红；git checkout精确恢复后全量与构建通过。日志build/preview-mutation-inverted-bed.log。
5. **下一步**：固定种子核对河床/水面预览和真实替换，再记录实际密集河区查询耗时；不以合成基准代替游戏实测。
6. **历史与回退**：基线a30bc1d，行为新增，API最低1.2.0，不改缓存格式。906cd5d已推送GitHub main；fix15已部署WAYFARING，SHA256为604b29905898ddfeaaed81d1a036ee0152a92d3d6f884ef6ffe4546872f445de。旧包及配置备份在相邻plaincraft-voxyseedgen-l10n/build/deployment-backups/preview-1.2.0-20261125-162224，未改玩家存档。

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
## 2026-11-24 · 热路径优化

1. **改动**：低邻河查询复用河段编号工作区，跨桶重复段只投影一次，且不能刷新最低高程的候选不再开平方。桶扫描先裁到索引实际范围。
2. **验证**：99 项单测通过；冻结索引对照覆盖 2880 次查询，整板河段和湖掩码签名守卫通过。remapJar 和 planContractJar 成功。
3. **未验证**：未量整合包采样延迟与实际桶重复率。
4. **负控**：沿用独立冻结实现对照，本轮未新增守卫或负控。
5. **下一步**：现场重点测河道密集处的区块生成，以及道路和长城读取河网时的尾延迟。
6. **历史与回退**：基线 6fa0af1；无需迁移河网缓存或存档。

## 2026-11-24 · 密集河网查询对照

1. **改动**：为冻结索引对照增加可选耗时采样，`test --tests '*NearestRiverIndexEquivalenceTest' -PriverQueryBenchmark` 输出 `RIVER_QUERY_BENCH`；普通回归不跑计时。
2. **验证**：99 项单测零跳过、零失败，remapJar 成功。64 段、每批 4096 查询、15 批交替测试，校验值一致。重叠长段旧/新 P50 为 14.64/2.43 μs，短河段为 6.32/1.36 μs；这是按批平均每查询后的中位数。
3. **未验证**：整合包真实河网密度、缓存命中、区块耗时与帧率，未把合成基准数值套到游戏。
4. **负控**：未新增生产守卫；沿用逐查询的冻结实现对照，计时批次另校验输出一致。
5. **下一步**：用固定种子的密集河区统计生成线程和区块尾延迟。
6. **历史与回退**：基线 5d98b91；只改本机测试及构建入口，无存档迁移。
