# Agent Note: AI 助手新增记录分配/共享/取消共享工具

Status: implemented

## Problem

AI 助手（`com.rebuild.core.aibot2.tool`）此前只能触达审批类记录操作，缺少「分配 / 共享 / 取消共享」这三种记录级权限动作。用户说「把这条记录分配给张三」「共享给李四看」时，模型只能引导用户去 Web 界面手工操作。

能力缺口只在 AI 工具层：Web 端已有等价实现（`GeneralOperatingController` 的 `record-assign` / `record-share` / `record-unshare-batch`），服务层 API（`EntityService.assign/share/bulk` + `BulkContext` + `BulkUnshare`）也已就绪。

不做的后果之外，还有两个必须同时解决的风险：

1. 分配会改写记录的所属用户并向相关人发通知，取消共享会立刻收回访问权限——两者都不可逆。模型理解偏差若直接落到执行，代价由用户的业务数据承担。
2. 工具以当前聊天用户身份执行，服务层权限由 `PrivilegesGuardInterceptor` 兜底。若不在写操作前做权限校验，用户会先收到确认摘要、确认后才撞上 `AccessDeniedException`。

## Decision

新增三个工具，各自的「Java 实现 + 同名 JSON 定义 + `ToolDefs` 注册」三件套齐备后自动出现在管理中心「能力扩展」与 MCP，前端零改动：

| 工具 | 参数（加粗为必填） | 执行路径 |
| --- | --- | --- |
| `AssignRecord` | **recordIds**、**toUser**、cascades、confirmed | 单条 `EntityService.assign`；多条 `BulkContext(user, ASSIGN, toUser, cascades, records)` + `bulk` |
| `ShareRecord` | **recordIds**、**toUsers**、withUpdate、cascades、confirmed | 逐用户：单条 `share(record, to, cascades, rights)`；多条 `BulkContext(user, SHARE, to, cascades, records)` + `addExtraParam("shareRights", rights)` |
| `UnshareRecord` | **recordIds**、toUsers（缺省=全部）、confirmed | 先 no-filter 查 `ShareAccess` 取 accessId，再按记录 `BulkContext(user, UNSHARE, accessIds, recordId)` + `bulk` |

关键约定（must）：

- **权限预检先于确认摘要**：生成摘要前逐条 `allowAssign` / `allowShare`，权限不足直接中文报错。服务层在执行时仍会二次校验（`BulkAssign` / `BulkShare` / `BulkUnshare` 内），预检只是把失败提前到「用户还没确认」之前。
- **二次确认无状态**：`confirmed != true` 时只返回 `{status:"ok", needConfirm:true, changes, message}`，`message` 要求模型转述摘要、取得用户明确同意后用**相同参数 + confirmed=true** 重调。工具不缓存中间状态——第二次调用重做全部校验并重查 `ShareAccess`，因此「确认摘要」与「执行」之间的数据变化不会被旧快照覆盖。
- **不做 `AdminGuard`、不继承 `RbvTool`**：分配/共享是普通用户按权限即可用的核心功能，不是管理员专属能力。
- **参数解析与摘要展示收敛到 `ToolHelper`**：`resolveRecordIds(Object, int)`（数组或逗号分隔字符串、去重、单次上限 100）、`resolveUsers(Object)`、`recordNames(Entity, List, int)`（一次 no-filter 聚合查询取名称，查不到回落记录 ID）、`resolveCascades(JSONArray)`（实体名或标签 → 内部实体名）。
- **`UnshareRecord` 无匹配共享时直接返回成功**（`unshared:0`）且不要求确认：没有可取消的对象时再让用户确认一次是纯噪音。
- 用户可见错误一律 `KnownToolException` + 简体中文，并给出下一步（如「请使用 QueryRecords 工具查询获取」）。

边界：多条记录须同属一个实体、须是非明细的业务实体、记录须存在、单次 ≤100 条；`cascades` 只在用户明确要求「同时处理相关记录」时传入，取值是实体名/标签，由工具转换为内部名。

## 工具定义契约

`src/main/resources/aibot2/tool/<类名>.json` 里 `function.name` 必须等于类名——`Tool.def()` 与 `ToolDefs.getToolJson` 都按 `getClass().getSimpleName()` 取同名资源，错名会在运行期抛 `Tool definition cannot be null`。三个定义的 `required` 分别为 `["recordIds","toUser"]` / `["recordIds","toUsers"]` / `["recordIds"]`，均 `additionalProperties=false`。

「文件名 = 类名 = function.name」这条约束没有编译期保障，靠 `RecordOpToolsJsonTest` 兜底。

## Alternatives considered

- **合成单个工具、用 action 参数区分 assign/share/unshare** — 复用一套参数解析与确认框架看似省事；但三者参数集互不相容（`toUser` 单值 / `toUsers` 多值 / 无目标用户 + 完全不同的摘要与返回值），合并后 schema 里会长期存在「该动作下无效」的字段，模型误用概率高于复用收益，故否决。
- **不做 `confirmed` 二次确认，直接执行** — 少一轮工具调用，交互更顺；但分配与取消共享不可逆，AI 理解偏差的代价由用户承担，故否决。确认环节写进工具协议（`needConfirm`）而不是交给系统提示词叮嘱模型。
- **只依赖 `PrivilegesGuardInterceptor`，不做权限预检** — 兜底依然有效，代码更少；但用户会在确认之后才收到失败，白跑一轮对话，故否决。
- **把记录名直接内联进 `message`** — 模型只需转发一段文本，省掉结构化解析；但记录名最多 100 条，塞进 message 会挤掉「请转述并确认」的指令本身，故改为与 `UpsertRecord` 同构的 `changes` 结构化摘要。
- **不做工具，继续引导用户去 Web 页面操作** — 零新增代码，维持现状；但用户在对话里提出的分配/共享诉求会被推回手工操作，这类需求在 AI 侧始终不可达，故否决。

## Consequences

- **收益**：AI 侧补齐三个记录权限动作，复用既有服务层，Web 端与前端零改动；不可逆操作被强制走「摘要 → 用户确认 → 执行」的对话状态机。
- **代价与已知上限**：每次操作多一轮工具调用（首次返回摘要）；`recordNames` 使每次确认摘要多一次 no-filter 查询（最多 10 条记录名）。
- **已知取舍**：`ShareRecord` 成功消息里的条数取请求条数（多用户循环下累加值会把同一条记录按用户数重复计数），返回体里的 `shared` 仍是真实累加值；`UnshareRecord` 的「当前共享」摘要最多展示 10 条且不追加「等共 N 条」。
- **重访信号**：`EntityService` 的 `assign/share/bulk` 签名变更；给工具加统一限流或审批网关；Agent 工具白名单成为默认配置时，需要把这三个工具名登记进白名单，否则对应 Agent 看不到它们。

## Verification

- `src/test/java/com/rebuild/core/aibot2/tool/RecordOpToolsJsonTest.java`：不继承 `TestSupport`、不依赖数据库，用 `getClass().getResourceAsStream("/aibot2/tool/<name>.json")` 读取并断言 `type=function`、`function.name` 与文件名一致、`required`、`additionalProperties=false`、两个描述非空。
- `./mvnw -o compile -Dexec.skip=true` → BUILD SUCCESS；`./mvnw -o test -Dtest=RecordOpToolsJsonTest -DskipTests=false -Dexec.skip=true` → `Tests run: 1, Failures: 0, Errors: 0`（pom 默认 `<skipTests>true</skipTests>`，必须显式覆盖）。
- 尚未覆盖、需联调环境人工确认：真实权限下的端到端确认流、分配通知与 `BulkShare` 的 `NotificationOnce` 合并、`withUpdate` 在权限不足时自动降级为只读、管理中心「能力扩展」开关、Agent 工具白名单。
- 修复记录：初版「同实体校验」曾因 `Integer` 装箱引用比较对用户自定义实体误报，修复与证据见 [bug-fix: Integer 引用比较](../bug-fix/2026-10-10-integer-boxing-entitycode-compare.md)。
