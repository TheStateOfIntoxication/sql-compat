# 架构

## 模块划分

```
sql-compat-parent
├── sql-compat-core      规则引擎 + 解析器 + 报告渲染（纯库，无 CLI / 无框架）
├── sql-compat-cli       picocli 入口，负责参数解析与文件写出
└── sql-compat-samples   演示用 fixture（虚构 schema，无任何真实表结构）
```

依赖方向严格单向：`cli → core`，`core` 不依赖 `cli`，`core` 也不依赖任何 DI 容器。

## 核心类型

| 类型 | 职责 |
|---|---|
| `SqlStatement` | 一条语句：namespace、id、展平后的 SQL、源位置 |
| `RuleContext` | 规则看到的世界：原始 SQL、小写 SQL、可选 AST、位置 |
| `DialectRule` | 规则接口：`id / name / defaultSeverity / description / suggestion / check` |
| `RuleRegistry` | 规则集合，内置 + ServiceLoader 发现，按 id 排序保证输出稳定 |
| `SqlParserFacade` | JSQLParser 包装，**永不抛异常**，失败返回 `ParseOutcome(ast=null, error=…)` |
| `MyBatisXmlParser` | DOM 解析 mapper，展平动态标签，回填行号 |
| `CompatEngine` | 采集语句 → 逐条过规则 → 汇总 `Report` |
| `Report` | 统计（按级别计数、最坏级别、解析失败数）+ 明细 |
| `ReportRenderer` | `MarkdownReportRenderer` / `HtmlReportRenderer` |

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

## 数据流

```
input path
   │
   ├─ 目录 → Files.walk → 过滤 *.xml / *.sql → 排序（保证稳定）
   │
   ▼
MyBatisXmlParser.parse        （.xml）
CompatEngine.parseSqlFile     （.sql，按 ; 切分）
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

## 测试策略

| 层 | 手段 |
|---|---|
| 规则层 | 每条规则 2 个单测：**能检出** + **不误报**（后者更重要） |
| 解析层 | fixture mapper 覆盖 if / foreach / 嵌套动态标签 |
| 引擎层 | 端到端：fixtures → 报告 → 断言问题条数与"无问题语句保持干净" |
| 报告层 | 断言渲染结果含关键片段（防止模板改坏） |

覆盖率由 JaCoCo 在 `verify` 阶段产出，排除第三方包（`net/sf/jsqlparser/**`）—— 那不是我们该负责的代码。
