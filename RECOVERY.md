# StreamsReflowing 2.12.8 源码恢复记录

## 目标与验收口径

**验收口径：能按类/按补丁增量改**，不是「全量一次 build 通过」。

## 基准件（所有等价性判断都锚在这上面）

```
文件   StreamsReflowing-1.21.1-fabric-2.12.8.jar
路径   D:\minecraft\Plain Craft Launcher 2\.minecraft\versions\【云游四海】WAYFARING\mods\
大小   2,606,486 B
SHA-256 55916c7e334fe9776fa262bc7ca167b9f9417e5982f0cb61d5fa55955bdbc1cb
mod id streamsreflowing    版本 2.12.8
许可   All Rights Reserved（作者 nicejohn）→ 仅本地自用，禁止分发
```

## 工具链（全部离线取自 gradle 缓存）

```
tiny-remapper    0.10.3-fat.jar
vineflower       1.10.1
cfr              0.152（从 Maven Central 下载，走代理）
JDK              21
Gradle           9.4.1
fabric-loom      1.16.3（与 jar 的 MANIFEST 声明一致）
映射             loom.mappings.1_21_1.layered+hash.2198-v2（**Mojang 官方名**）
MC classpath     minecraft-merged + libraries 171 条
```

## 已完成的流水线

```
① remap   intermediary → named（2198-v2 Mojang 名），5.7s，0 报错
          classpath 173 条（MC intermediary + fabric-api + libraries）
② 反编译   Vineflower 1.10.1 → 122 .java / 34,125 行
                          空 record 39 个、裸类型 941 处  ← 不可用
          CFR 0.152        → 122 .java / 28,451 行
                          空 record 1 个、裸类型 0 处       ← 采用
③ 工程    build.gradle + gradle.properties（loom 1.16.3 + Mojang 映射）
          资源从 jar 抽出 156 个文件到 src/main/resources
          原 refmap 丢弃（loom 自己生成）
          accesswidener 从 intermediary 转 named：
            accessible class net/minecraft/world/level/biome/Biome$ClimateSettings
```

## 关键技术发现（复现时别再踩）

1. **映射必须用 `loom.officialMojangMappings()`，不能用 yarn。**
   `layered+hash.2198-v2` 是补丁工程（用 Mojang 映射）生成的，反编译出来是
   `net.minecraft.client.Minecraft` / `Level` / `ServerLevel`。
   若 build.gradle 写 yarn，会得到 3602 个「程序包不存在」的全量失败。

2. **CFR 完胜 Vineflower（对 record 和泛型）**
   ```
   Vineflower  空 record 39 / 正常 record 3 / 裸类型 941 处
   CFR         空 record  1 / 正常 record 41 / 裸类型 0 处
   ```
   Vineflower 1.10.1 没有关闭 record 反编译的开关，导致 382 个
   「字段声明必须为静态」错误（81% 的错误量）。

3. **CFR 留下 4 行 `void varN_M;` 空桩**（它自己也标了
   `/* WARNING - void declaration */`），直接删除即可。

4. **编译错误计数要看是否停在解析阶段**：删掉那 4 行之前只报 8 个错，
   是因为 javac 在解析阶段就中止，从未进入类型检查 —— 那 8 个数字是假象。

## 当前进展 —— **已完成：整树零编译错误，jar 可构建**

```
编译错误：3602（映射错误）→ 1042 → 808 → 0
涉及文件：32 / 122 → 7 / 122 → 0 / 122
产物：build/libs/streamsreflowing-2.12.8-decompiled.jar
      2,733,352 B   条目 448   class 204
```

### 逐轮记录

| 轮次 | 错误 | 文件 | 主要动作 |
|---|---:|---:|---|
| compile8 | 974 | 30 | 接入 3 个可选兼容 jar（DH API / Sable / SableBeyond） |
| compile9 | 914 | 21 | 修 7 个小文件：假枚举、裸 cast、`error.message()`、重复声明等 |
| compile10 | 880 | 15 | 修 4 处 `extends Enum`、`(Object)this`、`Comparator` 类型见证 |
| compile11 | 858 | 12 | 修 `SimpleFluidRenderHandler(this,...)`、`ColorProviderRegistry`、重复变量 |
| compile12 | 834 | 11 | **抽出 Sable 内嵌的 `sable-companion` jar**（修掉 12 个 companion.math 错误） |
| compile13 | 810 | 7 | **排除 `forge/`+`neoforge/` 平台目录**；整段重写 `BakedBandTable` |
| compile14 | 808 | 7 | `LowResGrid` lambda 内变量重名 |
| compile15 | 2 | 1 | 7 个子代理并行修完 7 个文件后整树编译（`PlateRiverCache` 缺 catch 浮现） |
| compile16 | 1 | 1 | **从字节码 exception table 还原 `PlateRiverCache` 丢失的 3 个 catch** |
| compile17 | 1 | 1 | `ChunkStepDefer` 删 CFR 塞的不可达 `break` |
| compile18 | 1 | 1 | 同上补齐第二处不可达 `break` |
| compile19 | **0** | **0** | `FormEval` 三元内赋值改成 if/else 全分支赋值 → **BUILD SUCCESSFUL** |

### 从「能编译」到「能运行」还差两步（已补）

编译通过不等于能跑。产物核对时发现两个运行时硬依赖缺失：

```
① META-INF/services/dev.streamsreflowing.platform.services.IPlatformHelper
   内容 dev.streamsreflowing.platform.FabricPlatformHelper
   ⇒ 缺了 Services.PLATFORM 解析不到 → 启动即崩
   ⇒ 已补进 src/main/resources/META-INF/services/

② streamsreflowing.refmap.json
   mixins.json 声明了它，但 loom 默认生成的名字是 <archivesBaseName>-refmap.json
   ⇒ 实测新 jar 的 mixin 注解目标已被 loom **就地** remap 成 intermediary
     （`Lnet/minecraft/class_1959;` / `method_15772`），named MC 引用 0 个
   ⇒ 说明走的是「注解就地重映射」路线，refmap 本就多余
   ⇒ 已从 mixins.json 删掉 "refmap" 声明（并顺手加了 loom.mixin.defaultRefmapName 以备将来需要）
```

**产物结构核对（新 jar vs 原始 jar）：**
```
class   204 / 208   ← 缺的 4 个正是故意排除的 forge/ + neoforge/ mixin
资源    157 / 159   ← 缺的 2 个已在上面补回
新有原始没有  class 0 / 资源 0
```

### 待验证（未做）

**运行时行为未验证。** 结构与编译都成立，但反编译产物里已知有两类**编译发现不了**的语义错误
（CFR 丢失数组扩容重绑定、`finally` 里凭空塞 `continue` 导致整段 unreachable），
因此要用真机验证：装进实例开一局，看 `latest.log` 里河流生成、湖生成、mixin 注入是否正常。

**⇒ 不要因为「编译绿了」就认为等价。**

### 剩余 7 个文件（compile14）

```
550  NetworkTracer            ← 4915 行，占 68%
112  StreamsWorldgen
 52  StreamPrefetcher
 32  NearestRiverIndex
 28  RiverEngine
 22  RiverCarverFeature
 12  LowResGrid
```

**⇒ 122 个 .java 里 115 个已完全干净。**

### 剩余错误模式（已全部识别，修法已验证）

```
需要数组但找到 Object      裸 Object 变量当数组用 → 改声明为真实数组类型
找不到符号 hasNext/next/get/size  裸 Object → 补回 Iterator<T>/List<T>/Map<K,V>
已在方法 X 中定义了变量 Y   CFR 在 lambda 内重复外层变量名 → 内层改名并同步引用
二元运算符 '|' 类型错误     int |= boolean → if (b) { n = 1; }
Record 无法转换为 X         CFR 占位 Record → 改成正确构造
```

### ★★★ 最危险的一类：编译能过、运行时才炸

**CFR 会丢掉数组扩容后的「重新绑定」，只留扩容代码不留赋值。**
编译完全通过，但运行时写向旧数组 —— **`ArrayIndexOutOfBoundsException` 或反复扩容、缓冲区永不生效**。

**本机实证（`NearestRiverIndex.java` 的 ThreadLocal 缓冲区）：**
```java
// CFR 产物（错）：
int[] nArray2 = D.get()[0];
... nArray2.length 不够 ...
int[] nArray4 = Arrays.copyOf(nArray2, nArray2.length * 2);
System.arraycopy(..., nArray2, ...);      // ← 还写向旧数组
return nArray2;                            // ← 返回旧数组，扩容白做

// 修好（对照 vineflower 的 var15 系列）：
nArray4 = ...;
nArray2 = nArray4;                         // ← 补回这行重绑定
System.arraycopy(..., nArray2, ...);
D.get()[0] = nArray2;                      // ← 写回 ThreadLocal
```

**⇒ 判据：凡是看到 `Arrays.copyOf` / `new X[n*2]` / `Arrays.copyOfRange` 之后
仍使用**旧数组名**，且没有 `old = new` 的赋值，就要怀疑是这一条。**

**⇒ 这类只能靠**语义阅读**发现，编译和单文件 javac 都发现不了。
⇒ 反编译源码要真正能用，必须对「增长/扩容/缓存写回」这几类代码逐处人工核。**

### ★★ 陷阱：修完 attribution 错误后，会冒出一批全新错误

**javac 在存在「attribution 阶段错误」（找不到符号 / 类型不兼容）时，根本不跑 flow 分析。**
所以下面这类错误**在早期日志里看不到**，等前面修干净了才浮出来：

```
CFR 保留了「三元表达式里藏赋值」的写法：
    int n2;
    int n3 = n >= 4 ? a : ( ... n2 = n == 1 ? 0 : 1 );
⇒ Attribution 阶段：通过（n2 有赋值，类型也对）
⇒ Flow 阶段：报「可能尚未初始化变量 n2」
```

**本机实证：**
- `RiverCarverFeature` 22 错修完 → 冒出 1 个 `n8 might not have been initialized`
- `StreamPrefetcher` 52 错修完 → 冒出 2 个 `n2 might not have been initialized`

**⇒ 处理方式：看 vineflower 对照（它常把三元展开成 if/else），
或按「所有分支都赋值」的原则手动展开成 `if/else if/else`。**

**⇒ 规划含义：不要期待「错误数一轮降到 0」，每修完一批 attribution 错误
都会有一小批 flow 错误补位。这是正常的，不是修坏了。**

### ★ 单文件自证编译（不跑 gradle 也能验证）

子代理造的 `_recover/work/single_file_javac.py` 可复用，要点：

```
classpath = classpath.txt 全量（剔除 intermediary 那个 MC jar）
          + mojmap 版 MC merged jar（与 build.gradle 的 officialMojangMappings() 一致）
          + _recover/work/streams-named.jar（提供本 mod 其余已编译类，兄弟类靠它）
javac -proc:none -encoding UTF-8 --release 21
```

**两个必踩的坑：**
1. **源文件与 jar 一律拷到纯 ASCII 临时目录**（如 `%TEMP%\spcheck`）——
   `D:\云游四海-Mod源码` 和 `【云游四海】` 在 javac 的 argfile 里会编码炸
2. **argfile 里路径必须用正斜杠 `/`** —— 反斜杠会被当转义吃掉，报「找不到文件」

### 最关键的一条：用 vineflower-out 当「第二个反编译器意见」

**`_recover/work/vineflower-out/` 里留着同一批 class 的 Vineflower 反编译结果（122 个 .java）。**
虽然 CFR 整体质量远好于 Vineflower（见上面第 2 条），但**两者错的地方不一样**：

**⇒ 当 CFR 把两个不同局部变量合并成同一个 `Object object` 时，
去 vineflower 里看对应位置（它用 `varNN` 命名、笨但不会合并），
就能确定每个 `object` 原本是哪个变量、什么类型。**

本机实证（RiverCarverFeature.java）：
```
CFR      : Object object = new BlockPos.MutableBlockPos();   ← 1381 行
           object = (int[])nArray3.clone();                  ← 1389 行，同一个名字
Vineflower: var85 = (int[])nArray3.clone();  ← 1906 行，四处比较用的就是它
           （BlockPos 那个临时量是另一个 var）
⇒ 结论：把 BlockPos 那个改名 mutableBlockPos3，1389 行声明成 int[] object
```

**⇒ 同样的方法确定了 `Holder holder2` → `Holder<Biome>`、`Object object` → `Map<Long,int[]>`。**

### 关键经验（别再重新踩）

5. **`sable-companion` 是套在 Sable jar 的 `META-INF/jars/` 里的嵌套 jar**
   （`sable-companion-fabric-1.21.1-1.6.0.jar`），里面才有
   `dev.ryanhcode.sable.companion.math.BoundingBox3i` / `Pose3d` / `SubLevelAccess`。
   **必须单独抽出来 remap 后加进 compileOnly**，Sable 主 jar 里没有这些类。

6. **源码树同时带 `forge/` 与 `neoforge/` 两个平台目录**（各 2 个 mixin）。
   只构建 Fabric 时必须用 `sourceSets.main.java.exclude` 排除，
   否则它们会和 fabric 版同名 mixin 冲突报「不可比较的类型」。

7. **`(Object)this == SomeConcreteType` 在 mixin 里是合法写法**；
   如果还报错，说明同名 mixin 文件有多份（见第 6 条）。

## 依赖注入口径（build.gradle 里的 4 个本地 jar）

```
_recover/libs/named/DistantHorizonsApi-7.2.0.jar                 Distant Horizons API
_recover/libs/named/sable-fabric-1.21.1-2.0.6.jar                 Sable 主体
_recover/libs/named/sablebeyond-fabric-1.21.1-v0.5.0.jar          Sable Beyond（mixin plugin 判 sable_beyond）
_recover/libs/named/sable-companion-fabric-1.21.1-1.6.0.jar       ★ 从 Sable 嵌套 jar 抽出
```

**全部用 `compileOnly files(...)`**（不是 `modCompileOnly`）：这些 jar 已用 tiny-remapper
从 intermediary 预 remap 成 Mojang 官方名，走 `compileOnly` 可绕开 loom 对 mod 元数据的
Loom 版本校验（Sable 声明了 Loom 1.17.487，用 `modCompileOnly` 会被拒）。

## 重跑方式

```
cd D:\云游四海-Mod源码\streams-reflowing-src
set JAVA_HOME=C:\Program Files\Java\jdk-21
gradlew.bat compileJava --no-daemon --console=plain
```

## 文件布局

```
streams-reflowing-src/
├─ src/main/java/                 CFR 反编译产物（122 .java，正在修）
├─ src/main/resources/            从 jar 抽出（156 文件）
├─ build.gradle / gradle.properties / settings.gradle
└─ _recover/
   ├─ work/streams-named.jar      remap 后产物
   ├─ work/cfr-out/               CFR 原始输出（未改）
   ├─ work/vineflower-out/        Vineflower 输出（对照用）
   ├─ work/compile{1..5}.log      各轮编译日志
   └─ tools/cfr-0.152.jar
```

## 法律声明

StreamsReflowing 声明 `All Rights Reserved`。本源码树仅用于**本地研究、调试与互操作修补**。
不得分发源码或由它构建的 jar。若要公开发布改版，必须先取得作者 nicejohn 的许可。
