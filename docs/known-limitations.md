# 已知限制

> 工具在真实项目上跑出来的边界，逐条记录并给出绕过方式。
> 比"声称支持一切"有用得多 —— 使用者能据此判断能不能用。

最近一次真实项目实测（146 条语句）：**29 条解析失败 → 修正后 18 条**，其余均能正常扫描。

## 一、解析器（JSQLParser）能力边界

这些语句拿不到 AST，但**文本级规则照常运行**，检出结果不受影响（20 条内置规则全部是文本级）。

| 写法 | 现象 | 为什么 | 绕过 |
|---|---|---|---|
| Oracle `INSERT ALL INTO ... INTO ...` 多表插入 | `unexpected token: "INSERT"` | JSQLParser 不支持 Oracle 多表插入 | 文本规则仍会检出 `IFNULL` / `DATE_FORMAT` 等；AST 类规则跳过 |
| Oracle `CONNECT BY ROOT(...)` / `LEVEL` 层次查询 | `unexpected token: "CONNECT_BY_ROOT"` | JSQLParser 不支持 Oracle 层次查询 | 同上。这类语句迁移到 PG 本来就要用 `WITH RECURSIVE` 重写，属于 A 类人工项 |
| 一个 `<update>` 里拼了多条语句（`UPDATE ...; SELECT 1 FROM DUAL`） | `unexpected token: "SELECT"` | 输入本身不干净：MyBatis 单语句不支持多句 | 拆成两个 `<select>` / `<update>`；Oracle 侧要靠 `allowMultiQueries` |
| 双引号标识符作表别名（`FROM "app_x" t ...` 的个别写法） | `unexpected token: "t"` | JSQLParser 对某些引号组合的别名解析失败 | 影响极小，单条级别 |
| MySQL 8 `JSON_TABLE(... COLUMNS(...))` 表函数 | `unexpected token: "in"` | JSQLParser 不支持表函数 | 文本规则仍生效 |

**当前 20 条规则没有一条依赖 AST。** `RuleContext.ast()` 是给后续规则预留的扩展点
（见路线图 v0.6 的 AST 类规则）。所以上面的解析失败**不会造成漏检** —— 但工具会把这些语句
作为 `PARSE` 级 INFO 报出来，让使用者心里有数。

## 二、动态标签展平

`<if>` / `<foreach>` / `<choose>` 等标签被展平：保留文本、丢掉标签。
其中有两个标签**自带关键字生成职责**，不能只丢不管：

| 标签 | 处理 | 不处理的后果 |
|---|---|---|
| `<where>` | 生成 `WHERE`，并抹掉内层开头的 `AND` / `OR` | 展平后少了 `WHERE`，直接不是合法 SQL |
| `<set>` | 生成 `SET`，并抹掉末尾多余逗号 | 展平后少了 `SET`，`UPDATE` 无法解析 |

> 这两个 bug 是在真实项目上跑出来的：29 条解析失败里有 13 条是它们导致的。
> 修正后降到 18 条。教训是——**展平不是"删标签"，是"模拟标签的语义"**。

**`<choose>` 只能取一个分支**：运行期只走 `when` / `otherwise` 其中一个，全拼进来会让一条语句出现两个
`ORDER BY`。现在只展平第一个分支。

**动态排序的 `${}` 要做特殊处理**：`ORDER BY ${col} ${sort}`、`ORDER BY "${col}" ${sort}`、
`ORDER BY INET_ATON(ip) ${sort}` 这三种写法，逐个替换占位符会得到 `ORDER BY t t` / `ORDER BY f(x) t`
这类非法 SQL。现在分别折叠成 `ORDER BY 1` 与 `ORDER BY f(x) ASC`。

其余已知局限：

| 场景 | 影响 | 说明 |
|---|---|---|
| `<trim prefix="WHERE" ...>` | 展平后可能缺关键字 | `prefix` / `suffix` 属性也会生成关键字，目前不模拟 |
| `<include refid="...">` | 被引用片段的内容不展开 | XML 片段无法跨文件解析，语句会比实际短 |
| 嵌套 `<if>` 里的非法片段 | 展平后可能语法不通 | 只有动态拼装后才合法，静态检查阶段无法判断 |

## 三、规则本身的取舍

| 规则 | 已知误报 / 漏报 | 取舍理由 |
|---|---|---|
| R003 `DML LIMIT` | 2000 字符窗口外的 `LIMIT` 会漏检 | 宁可漏检不可误报 |
| R009 反引号 | 字符串字面量里的反引号会误报 | 真问题远比字面量常见，且报告给了片段可人工排除 |
| R010 `LIMIT` 无 `ORDER BY` | 子查询内有 `ORDER BY` 也算"有排序" | 细分收益小于复杂度 |
| R011 `<foreach>` | 只要语句含 `<foreach>` 就报，不看集合大小 | 这是"请复核"标记，不是缺陷判定；foreach 密集的项目会显得很吵，用 `--ignore R011` 过滤 |
| R012 `REGEXP` | 无法判断原语句是否依赖 MySQL 的默认不区分大小写 | 已在 `docs/manual-decisions.md` 里列为必查项 |
| R016 `<=>` | XML 里写作 `&lt;=&gt;`，两者都认 | 不认转义形式的话，对真实 Mapper 是瞎的 |

## 四、双库对比（第二层）

| 局限 | 说明 |
|---|---|
| Kingbase 用 PostgreSQL 16 近似 | Kingbase 基于 PG，方言高度一致，但**不是** 1:1 实测 |
| 耗时是跨引擎指示性对比 | 同机同数据，但优化器与默认存储不同，不构成 benchmark |
| `FLOAT` / `REAL` 按 10 位有效数字比较 | 避开二进制表示噪声，代价是更小的真实差异看不出来 |
| 用例需自带可移植的建表造数脚本 | 暂不支持从生产库采样或自动生成用例（v0.5 路线图） |

## 五、自动改写

见 [rules.md 的自动改写章节](rules.md)。摘要：6 条可改（3 等价 + 3 需确认），
其余 14 条的改写形状取决于业务语义，逐条列在 [manual-decisions.md](manual-decisions.md)。
