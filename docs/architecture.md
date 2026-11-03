# 架构

工具分两层：**静态检查**找语法级差异，**运行时对比**找"两边都能跑但结果不同"的差异。

## 模块划分

```
sql-compat-parent
├── sql-compat-core          规则引擎 + 解析器 + 报告渲染（纯库，无 CLI / 无框架）
├── sql-compat-verify        双库一致性对比（JDBC，运行时）
├── sql-compat-cli           picocli 入口，负责参数解析与文件写出
├── sql-compat-maven-plugin  Maven 插件，把 check 挂到 verify 阶段
└── sql-compat-samples       演示用 fixture（虚构 schema，无任何真实表结构）
```

依赖方向严格单向：`cli → core/verify`、`maven-plugin → core`；`core` 与 `verify` 互不依赖，也都不依赖任何 DI 容器。

## 核心类型（第一层）

| 类型 | 职责 |
|---|---|
| `SqlStatement` | 一条语句：namespace、id、展平后的 SQL、源位置、出现过的动态标签 |
| `RuleContext` | 规则看到的世界：原始 SQL、小写 SQL、可选 AST、位置 |
| `DialectRule` | 规则接口：`id / name / defaultSeverity / description / suggestion / check` |
| `RuleRegistry` | 规则集合，内置 + ServiceLoader 发现，按 id 排序保证输出稳定 |
| `SqlParserFacade` | JSQLParser 包装，**永不抛异常**，失败返回 `ParseOutcome(ast=null, error=…)` |
| `MyBatisXmlParser` | DOM 解析 mapper，展平动态标签，回填行号，记录动态标签名 |
| `CompatEngine` | 采集语句 → 逐条过规则 → 汇总 `Report` |
| `Report` | 统计（按级别计数、最坏级别、解析失败数）+ 明细 |
| `ReportRenderer` | `MarkdownReportRenderer` / `HtmlReportRenderer` |

## 核心类型（第二层）

| 类型 | 职责 |
|---|---|
| `SqlCase` | 一条可移植用例：`id / sql / ordered` |
| `CaseLoader` | 从文件名读取用例，`ORDER BY` 存在即视为对行序敏感 |
| `DbEndpoint` | JDBC 目标，`redactedDsn()` 防止凭据被误打进日志 |
| `DbProbe` | 单目标执行 + 计时（预热 2 轮 + 测量 5 轮取中位数），DML 比影响行数 |
| `ValueNormalizer` | JDBC 值 → 规范字符串，让两个引擎可比 |
| `ResultSetDiffer` | 逐单元格比较，说明第一处差异 |
| `DualDbComparator` | 应用建表造数脚本 → 跑用例 → 汇总 `ComparisonReport` |
| `ComparisonReportRenderer` | Markdown 报告：一致率 + 差异明细 + 全用例表 |

## 为什么这么设计

### 1. 不用 Spring Boot
CLI 工具的冷启动成本直接体现在用户体感上：纯 Java 约 200ms，Spring Boot 容器约 2–3s。对一个"改完代码跑一下"的检查工具，这个差距决定它会不会被真的用起来。

### 2. 解析失败不算错误
方言检查的价值在于"能检出的尽量检出"。一条复杂语句解析失败就整体报错，等于把一个可用工具变成不可用工具。所以：AST 拿不到时，纯文本规则照常运行，`parseError` 只在报告里作为统计项出现。

### 3. 规则通过接口 + ServiceLoader 扩展
规则是会长到几百条的东西。如果每条规则都要改核心代码，这个项目一年后就没法维护了。现在新增一条规则 = 新增一个类 + 一行 service 登记。

### 4. 报告自包含
HTML 报告内嵌 CSS、无外部链接、无 JavaScript。这样它能被当附件发邮件、能放进 CI artifact、能离线双击打开——**演示场景下这三点都比"漂亮的现代前端"重要**。

### 5. 规则按 ID 稳定排序
同一份输入必须产出同一份报告。否则 CI 里 diff 会一直是噪音，而 diff 噪音会让人关掉这个工具。

### 6. 本地零依赖，双库验证放 CI
第一层只要 JDK，任何时候都能跑、能演示。第二层需要两个活库，则放到 CI：runner 自带 Docker，用 `services:` 起 `mysql:8.0` 与 `postgres:16`，开发者本机不需要装任何数据库。
**开发环境要轻，验证环境要真** —— 两边都满足，才有人真的用。

### 7. 值归一化是第二层的命门
不归一，几乎每条用例都会"不一致"，但原因不是真差异：MySQL 的 `TINYINT(1)` 返回 `1`、PostgreSQL 的 `boolean` 返回 `true`；`DECIMAL` 运算后标度不同；时间戳精度与格式不同。
反过来，**故意不归一**整数除法（MySQL `1/2`=0.5，PG=0）与 `||` 语义（MySQL 当逻辑或）—— 那是真实陷阱，必须报出来。

## 数据流

### 第一层：静态检查

```
input path
   │
   ├─ 目录 → Files.walk → 过滤 *.xml / *.sql → 排序（保证稳定）
   │
   ▼
MyBatisXmlParser.parse        （.xml）
CompatEngine.parseSqlFile     （.sql，按 ; 切分并剥行注释）
   │
   ▼
List<SqlStatement>
   │
   ▼  for each statement
SqlParserFacade.parse → ParseOutcome(ast | error)
   │
   ▼
RuleContext(statement, ast)
   │
   ▼  for each rule in registry
DialectRule.check → List<Issue>
   │
   ▼
ScanResult(statement, issues, parseError)
   │
   ▼
Report → ReportRenderer → html / md
```

### 第二层：双库对比

```
cases/*.sql  ──► CaseLoader ──► SqlCase(id, sql, ordered)
                                        │
        setup.sql（两库各跑一遍）         │
                    │                   ▼
                    │        DualDbComparator
                    │           ├─ DbProbe(mysql)    ──► 预热 + 测量，取中位数
                    │           └─ DbProbe(postgres) ──┘
                    │                   │
                    │                   ▼
                    │            ValueNormalizer（两侧值归一）
                    │                   │
                    └────────► ResultSetDiffer（无序则排序后比，有序则按位比）
                                        │
                                        ▼
                        ComparisonReport（一致率 + 差异明细 + 耗时）
```

## 测试策略

| 层 | 手段 |
|---|---|
| 规则层 | 每条规则 2 个单测：**能检出** + **不误报**（后者更重要） |
| 解析层 | fixture mapper 覆盖 if / foreach / 嵌套动态标签 |
| 引擎层 | 端到端：fixtures → 报告 → 断言问题条数与"无问题语句保持干净" |
| 报告层 | 断言渲染结果含关键片段（防止模板改坏） |
| 归一化层 | 断言"应该收敛的收敛、应该保留差异的保留" |
| 运行时层 | `DualDbComparisonIT` 需要两个活库，未设环境变量时整类跳过 |

覆盖率由 JaCoCo 在 `verify` 阶段产出，排除第三方包（`net/sf/jsqlparser/**`）—— 那不是我们该负责的代码。
### 8. Maven 插件要"能无痛加"，所以要会自己认输

插件形态最容易犯的错是：一加进 pom，构建就红。比如把 `src/main/resources` 下的 `logback.xml`
当 mapper 解析，或者在没有 mapper 的模块上也执行扫描报错。

所以插件做了两个前置判断：

| 判断 | 理由 |
|---|---|
| 前 4KB 不含 `<mapper` 的 XML 直接排除 | 否则 `logback.xml` / `mybatis-config.xml` 会让整个构建抛异常 |
| 扫描到 0 条语句时跳过并返回成功 | 插件要能加到任意模块上，没有 mapper 不该算失败 |

另外**默认不改源文件、默认不失败**：不配 `failOn` 就只出报告，让人先看一版再决定要不要卡口。
