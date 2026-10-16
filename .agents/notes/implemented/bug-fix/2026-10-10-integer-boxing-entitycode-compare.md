# Agent Note: AI 工具跨实体校验误报 —— Integer 引用比较

Status: implemented

## Problem

三个新 AI 工具（`AssignRecord` / `ShareRecord` / `UnshareRecord`）上线后，凡涉及用户自定义实体的调用一律失败，日志为：

```
TOOL_WARN AssignRecord 只能处理同一实体的记录，记录 995-01a123575e550055 属于其他实体
```

反常点：只传一条记录也报「属于其他实体」。而编码 ≤127 的系统实体（用户 001、动态 040）却能通过同一检查，报出的是后续业务错误——这个「挑实体」现象是定位该缺陷的关键线索。

本缺陷发生在三个新工具（实现与决策见 [feature: AI 助手新增记录分配/共享/取消共享工具](../feature/2026-10-10-aibot-record-assign-share-unshare.md)）上线后的联调阶段。

## Decision

记录与实体的同实体判断收敛到 `ToolHelper.isSameEntity(ID, Entity)`，用 `(int)` 强转做数值比较：

```java
public static boolean isSameEntity(ID recordId, Entity entity) {
    return (int) recordId.getEntityCode() == (int) entity.getEntityCode();
}
```

- `ToolHelper.checkRecordEntity` 改为复用该方法，异常文案与行为不变；
- 三个工具改用 `!ToolHelper.isSameEntity(id, entity)` 判断，错误文案不变；
- 新增 `ToolHelperEntityTest`：不依赖数据库，用 `Proxy` 造 Entity 替身，覆盖大编码实体（995）与小编码实体（001）两种场景。

## 根因

persist4j 的两个 API 都返回装箱类型，且编译器不会报错：

```java
ID.getEntityCode()      // java.lang.Integer
Entity.getEntityCode()  // java.lang.Integer
```

因此 `id.getEntityCode() != entity.getEntityCode()` 编译为 `if_acmpeq`（引用比较），不是数值比较。`Integer` 只在 -128~127 有缓存：

- 用户自定义实体编码 990+（如订单 995、回款 993）→ 两个数值相等的 `Integer` 是不同实例 → 条件恒真 → 单条也误报；
- 系统实体编码 ≤127（001、040…）→ 命中缓存是同一实例 → 偶然正确，掩盖了问题。

仓库既有代码一直规避此坑（`ToolHelper.checkRecordEntity`、`N2NReferenceSupport`、`TagSupport` 均写 `(int) x.getEntityCode()`），这也是 `GetRecord` 等工具始终正常的原因；只有本次新增的三个工具写了裸 `!=`。

## Alternatives considered

- **三处直接补 `(int)` 强转**：diff 最小，但比较散落三处且无法单测保护；改为收敛到 `isSameEntity`。
- **改用 `Integer.equals` / `Objects.equals`**：值语义同样正确，但与仓库既有的数值比较风格不一致，且 `Objects.equals` 引入额外 null 语义；未采用。
- **修改/封装 persist4j 使其返回 `int`**：第三方依赖不可控，爆炸半径过大；未采用。
- **只补黑盒 E2E 用例、不改代码结构**：MCP/聊天驱动的 E2E 成本高、不适合 CI；改为 helper + 单测，E2E 仅作部署后复验。

## Consequences

- 收益：分配/共享/取消共享在用户自定义实体上恢复正常；同实体比较只剩一个实现点，评审与测试都有了明确抓手。
- 代价与上限：`isSameEntity` 需要调用方持有 `Entity`；单测用替身只覆盖比较语义，不覆盖真实业务流，仍依赖一次部署后的 MCP 复验。
- 重访信号：persist4j 若改为返回 `int`，或引入静态检查（SpotBugs `BoxedPrimitiveEquality` / Error Prone），注释与测试可以简化。

## Verification

- 修复前：`javap` 显示编译产物为 `if_acmpeq`；用真实 persist4j jar 运行时验证装箱行为（995 引用不等、001/040 引用相等、140 引用不等）；MCP 实测单条 995 必报错，而 001/040 通过该检查。
- 修复后：`javap` 显示 `intValue + if_icmpne`（数值比较），打包产物 `target/rebuild.jar` 内字节码同样复核通过；`ToolHelperEntityTest` 与 `RecordOpToolsJsonTest` 全部通过（`Tests run: 2, Failures: 0, Errors: 0`）。
- 部署后 MCP 复验：单条共享应返回待确认摘要（needConfirm）而非报错——部署后回填结论。
