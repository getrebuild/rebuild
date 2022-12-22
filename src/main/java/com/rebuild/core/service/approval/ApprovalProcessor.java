/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.cache.CacheTemplate;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.PrivilegesGuardContextHolder;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.general.EntityService;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.core.support.SetUser;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.*;

/**
 * 审批处理。此类是作为 ApprovalStepService 的辅助，因为有些逻辑放在 Service 中不合适
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/24
 */
@Slf4j
public class ApprovalProcessor extends SetUser {

    // 最大撤销次数
    private static final int MAX_REVOKED = 100;

    final private ID record;

    // 如未传递，会在需要时根据 record 确定
    private ID approval;
    // 流程定义
    private FlowParser flowParser;

    /**
     * @param record
     */
    public ApprovalProcessor(ID record) {
        this(record, null);
    }

    /**
     * @param record
     * @param approval
     */
    public ApprovalProcessor(ID record, ID approval) {
        this.record = record;
        this.approval = approval;
    }

    /**
     * 1.提交
     *
     * @param selectNextUsers
     * @return
     * @throws ApprovalException
     */
    public boolean submit(JSONObject selectNextUsers) throws ApprovalException {
        final ApprovalState currentState = ApprovalHelper.getApprovalState(this.record);
        if (currentState == ApprovalState.PROCESSING || currentState == ApprovalState.APPROVED) {
            throw new ApprovalException(Language.L("无效审批状态 (%s) ，请刷新后重试", currentState));
        }

        FlowNodeGroup nextNodes = getNextNodes(FlowNode.NODE_ROOT);
        if (!nextNodes.isValid()) {
            log.warn("No next-node be found");
            return false;
        }

        Set<ID> nextApprovers = nextNodes.getApproveUsers(this.getUser(), this.record, selectNextUsers);
        if (nextApprovers.isEmpty()) {
            log.warn("No any approvers special");
            return false;
        }

        Set<ID> ccUsers = nextNodes.getCcUsers(this.getUser(), this.record, selectNextUsers);
        Set<String> ccAccounts = nextNodes.getCcAccounts(this.record);

        Record recordOfMain = EntityHelper.forUpdate(this.record, this.getUser(), false);
        recordOfMain.setID(EntityHelper.ApprovalId, this.approval);
        recordOfMain.setInt(EntityHelper.ApprovalState, ApprovalState.PROCESSING.getState());
        recordOfMain.setString(EntityHelper.ApprovalStepNode, nextNodes.getApprovalNode().getNodeId());
        // Clear on submit
        ApprovalStepService.setApprovalLastX(recordOfMain, null, null);

        Application.getBean(ApprovalStepService.class).txSubmit(recordOfMain, ccUsers, ccAccounts, nextApprovers);

        // 审批时共享
        Set<ID> ccs4share = nextNodes.getCcUsers4Share(this.getUser(), this.record, selectNextUsers);
        share2CcIfNeed(this.record, ccs4share);

        return true;
    }

    /**
     * 2.审批
     *
     * @param approver
     * @param state
     * @param remark
     * @param selectNextUsers
     * @throws ApprovalException
     */
    public void approve(ID approver, ApprovalState state, String remark, JSONObject selectNextUsers) throws ApprovalException {
        approve(approver, state, remark, selectNextUsers, null, null, null);
    }

    /**
     * 2.审批
     *
     * @param approver
     * @param state
     * @param remark
     * @param selectNextUsers
     * @param addedData
     * @param checkUseGroup
     * @param rejectNode
     * @throws ApprovalException
     */
    public void approve(ID approver, ApprovalState state, String remark, JSONObject selectNextUsers, Record addedData, String checkUseGroup, String rejectNode) throws ApprovalException {
        final ApprovalStatus status = checkApprovalState(ApprovalState.PROCESSING);

        final Object[] stepApprover = Application.createQueryNoFilter(
                "select stepId,state,node,approvalId from RobotApprovalStep where recordId = ? and approver = ? and node = ? and isCanceled = 'F' order by createdOn desc")
                .setParameter(1, this.record)
                .setParameter(2, approver)
                .setParameter(3, getCurrentNodeId(status))
                .unique();
        if (stepApprover == null || (Integer) stepApprover[1] != 1) {
            throw new ApprovalException(Language.L(stepApprover == null
                    ? Language.L("当前流程已经被其他人审批")
                    : Language.L("你已经审批过当前流程")));
        }

        Record approvedStep = EntityHelper.forUpdate((ID) stepApprover[0], approver);
        approvedStep.setInt("state", state.getState());
        approvedStep.setDate("approvedTime", CalendarUtils.now());
        if (StringUtils.isNotBlank(remark)) {
            approvedStep.setString("remark", remark);
        }

        this.approval = (ID) stepApprover[3];
        FlowNodeGroup nextNodes = getNextNodes((String) stepApprover[2]);

        Set<ID> nextApprovers = null;
        String nextNode = null;

        // 回退至节点
        if (state == ApprovalState.REJECTED && rejectNode != null) {
            nextNode = rejectNode;
            approvedStep.setInt("state", ApprovalState.BACKED.getState());
        } else if (state == ApprovalState.APPROVED && !nextNodes.isLastStep()) {
            nextApprovers = nextNodes.getApproveUsers(this.getUser(), this.record, selectNextUsers);
            // 自选审批人
            nextApprovers.addAll(getSelfSelectedApprovers(nextNodes));

            if (nextApprovers.isEmpty()) {
                throw new ApprovalException(Language.L("下一流程无审批人可用，请联系管理员配置"));
            }

            FlowNode nextApprovalNode = nextNodes.getApprovalNode();
            nextNode = nextApprovalNode != null ? nextApprovalNode.getNodeId() : null;
        }

        Set<ID> ccUsers = nextNodes.getCcUsers(this.getUser(), this.record, selectNextUsers);
        Set<String> ccAccounts = nextNodes.getCcAccounts(this.record);

        FlowNode currentNode = getFlowParser().getNode((String) stepApprover[2]);
        Application.getBean(ApprovalStepService.class)
                .txApprove(approvedStep, currentNode.getSignMode(), ccUsers, ccAccounts, nextApprovers, nextNode, addedData, checkUseGroup);

        // 同意时共享
        if (state == ApprovalState.APPROVED) {
            Set<ID> ccs4share = nextNodes.getCcUsers4Share(this.getUser(), this.record, selectNextUsers);
            share2CcIfNeed(this.record, ccs4share);
        }
    }

    /**
     * 3.撤回
     *
     * @throws ApprovalException
     */
    public void cancel() throws ApprovalException {
        final ApprovalStatus status = checkApprovalState(ApprovalState.PROCESSING);

        Application.getBean(ApprovalStepService.class).txCancel(
                this.record, status.getApprovalId(), getCurrentNodeId(status), false);
    }

    /**
     * 2.1.催审
     *
     * @return -1=频率超限 5m
     */
    public int urge() {
        final ApprovalStatus status = checkApprovalState(ApprovalState.PROCESSING);
        this.approval = status.getApprovalId();

        final String sentKey = String.format("URGE:%s-%s", this.approval, this.record);
        if (Application.getCommonsCache().getx(sentKey) != null) {
            return -1;
        }

        int sent = 0;
        String entityLabel = EasyMetaFactory.getLabel(MetadataHelper.getEntity(this.record.getEntityCode()));

        JSONArray step = getCurrentStep(status);
        for (Object o : step) {
            JSONObject s = (JSONObject) o;
            if (s.getIntValue("state") != 1) continue;

            ID approver = ID.valueOf(s.getString("approver"));
            String urgeMsg = Language.L("有一条 %s 记录正在等待你审批，请尽快审批", entityLabel);
            Application.getNotifications().send(MessageBuilder.createApproval(approver, urgeMsg, this.record));
            sent++;
        }

        // 5m
        Application.getCommonsCache().putx(sentKey, CalendarUtils.now(), CacheTemplate.TS_MINTE * 5);
        return sent;
    }

    /**
     * 2.2.转审
     *
     * @param approver
     * @param toUser
     */
    public void referral(ID approver, ID toUser) {
        final Object[] stepApprover = findProcessingStepApprover(approver);
        if (toUser.equals(stepApprover[2])) {
            throw new ApprovalException(Language.L("不能转审给自己"));
        }

        Application.getBean(ApprovalStepService.class).txReferral((ID) stepApprover[0], toUser);
    }

    /**
     * 2.3.加签
     *
     * @param approver
     * @param toUsers
     */
    public void countersign(ID approver, ID[] toUsers) {
        final Object[] stepApprover = findProcessingStepApprover(approver);
        Application.getBean(ApprovalStepService.class).txCountersign((ID) stepApprover[0], toUsers);
    }

    /**
     * 3.撤销（管理员）
     *
     * @throws ApprovalException
     */
    public void revoke() throws ApprovalException {
        final ApprovalStatus status = checkApprovalState(ApprovalState.APPROVED);

        Object[] count = Application.createQueryNoFilter(
                "select count(stepId) from RobotApprovalStep where recordId = ? and state = ?")
                .setParameter(1, this.record)
                .setParameter(2, ApprovalState.REVOKED.getState())
                .unique();
        if (ObjectUtils.toInt(count[0]) >= MAX_REVOKED) {
            throw new ApprovalException(Language.L("记录撤销次数已达 %d 次，不能再次撤销", MAX_REVOKED));
        }

        Application.getBean(ApprovalStepService.class).txCancel(
                this.record, status.getApprovalId(), getCurrentNodeId(status), true);
    }

    /**
     * @return
     */
    public FlowNode getCurrentNode() {
        return getFlowParser().getNode(getCurrentNodeId(null));
    }

    /**
     * @return
     * @see #getNextNode(String)
     */
    protected FlowNode getNextNode() {
        return getNextNode(getCurrentNodeId(null));
    }

    /**
     * 获取下一节点
     *
     * @param currentNode
     * @return
     */
    protected FlowNode getNextNode(String currentNode) {
        Assert.notNull(currentNode, "[currentNode] cannot be null");

        List<FlowNode> nextNodes = getFlowParser().getNextNodes(currentNode);
        if (nextNodes.isEmpty()) return null;

        FlowNode firstNode = nextNodes.get(0);
        if (!FlowNode.TYPE_BRANCH.equals(firstNode.getType())) {
            return firstNode;
        }

        int bLength = nextNodes.size();
        for (FlowNode node : nextNodes) {
            // 匹配最后一个
            if (--bLength == 0) {
                return getNextNode(node.getNodeId());
            }

            FlowBranch branch = (FlowBranch) node;
            if (branch.matches(record)) {
                return getNextNode(branch.getNodeId());
            }
        }
        return null;
    }

    /**
     * @return
     * @see #getNextNodes(String)
     */
    public FlowNodeGroup getNextNodes() {
        return getNextNodes(getCurrentNodeId(null));
    }

    /**
     * 获取下一组节点。遇到审批人节点则终止，在审批节点前有抄送节点也会返回
     *
     * @param currentNode
     * @return
     */
    protected FlowNodeGroup getNextNodes(String currentNode) {
        Assert.notNull(currentNode, "[currentNode] cannot be null");

        FlowNodeGroup nodes = new FlowNodeGroup();
        FlowNode next = null;
        while (true) {
            next = getNextNode(next != null ? next.getNodeId() : currentNode);
            if (next == null) {
                break;
            }

            nodes.addNode(next);
            if (FlowNode.TYPE_APPROVER.equals(next.getType())) {
                break;
            }
        }
        return nodes;
    }

    /**
     * 获取当前审批节点 ID
     *
     * @param useStatus
     * @return
     */
    private String getCurrentNodeId(ApprovalStatus useStatus) {
        if (useStatus == null) useStatus = ApprovalHelper.getApprovalStatus(this.record);

        String currentNode = useStatus.getCurrentStepNode();
        if (StringUtils.isBlank(currentNode)
                || useStatus.getCurrentState().getState() >= ApprovalState.REJECTED.getState()) {
            currentNode = FlowNode.NODE_ROOT;
        }
        return currentNode;
    }

    /**
     * @return
     */
    private FlowParser getFlowParser() {
        Assert.notNull(approval, "[approval] cannot be null");
        if (flowParser != null) {
            return flowParser;
        }

        FlowDefinition flowDefinition = RobotApprovalManager.instance.getFlowDefinition(
                MetadataHelper.getEntity(this.record.getEntityCode()), this.approval);
        flowParser = flowDefinition.createFlowParser();
        return flowParser;
    }

    /**
     * 获取当前审批步骤
     *
     * @param useStatus
     * @return returns [S, S]
     */
    public JSONArray getCurrentStep(ApprovalStatus useStatus) {
        if (useStatus == null) useStatus = ApprovalHelper.getApprovalStatus(this.record);

        final String currentNode = useStatus.getCurrentStepNode();

        // 1.哪个批次
        String sql = "select nodeBatch from RobotApprovalStep" +
                " where recordId = ? and approvalId = ? and node = ? and isCanceled = 'F' and isBacked = 'F' order by createdOn desc";
        Object[] lastNode = Application.createQueryNoFilter(sql)
                .setParameter(1, this.record)
                .setParameter(2, this.approval)
                .setParameter(3, currentNode)
                .unique();
        String nodeBatch = lastNode == null || lastNode[0] == null ? null : (String) lastNode[0];

        // 2.批次下的
        sql = "select approver,state,remark,approvedTime,createdOn from RobotApprovalStep"
                + " where recordId = ? and approvalId = ? and node = ? and isCanceled = 'F' and isBacked = 'F'";
        if (StringUtils.isNotBlank(nodeBatch)) sql += " and nodeBatch = '" + nodeBatch + "'";

        Object[][] array = Application.createQueryNoFilter(sql)
                .setParameter(1, this.record)
                .setParameter(2, this.approval)
                .setParameter(3, currentNode)
                .array();

        JSONArray steps = new JSONArray();
        for (Object[] o : array) {
            steps.add(this.formatStep(o, null));
        }
        return steps;
    }

    /**
     * 获取已执行步骤
     *
     * @return returns [ [S,S], [S], [SSS], [S] ]
     */
    public JSONArray getWorkedSteps() {
        final ApprovalStatus status = ApprovalHelper.getApprovalStatus(this.record);
        this.approval = status.getApprovalId();

        Object[][] array = Application.createQueryNoFilter(
                "select approver,state,remark,approvedTime,createdOn,createdBy,node,prevNode,nodeBatch,ccUsers,ccAccounts,attrMore from RobotApprovalStep" +
                        " where recordId = ? and isWaiting = 'F' and isCanceled = 'F' order by createdOn")
                .setParameter(1, this.record)
                .array();
        if (array.length == 0) return JSONUtils.EMPTY_ARRAY;

        Object[] firstStep = null;
        Map<String, List<Object[]>> stepBatchMap = new LinkedHashMap<>();
        for (Object[] o : array) {
            String prevNode = (String) o[7];
            if (firstStep == null && FlowNode.NODE_ROOT.equals(prevNode)) {
                firstStep = o;
            }

            String batch = StringUtils.defaultString((String) o[8], prevNode);
            List<Object[]> stepGroup = stepBatchMap.computeIfAbsent(batch, k -> new ArrayList<>());
            stepGroup.add(o);
        }
        if (firstStep == null) {
            throw new ConfigurationException(Language.L("无效审批记录 (%s)", this.record));
        }

        JSONArray steps = new JSONArray();
        JSONObject submitStep = JSONUtils.toJSONObject(
                new String[]{"submitter", "submitterName", "createdOn", "approvalId", "approvalName", "approvalState"},
                new Object[]{firstStep[5],
                        UserHelper.getName((ID) firstStep[5]),
                        CalendarUtils.getUTCDateTimeFormat().format(firstStep[4]),
                        status.getApprovalId(), status.getApprovalName(), status.getCurrentState().getState()});
        steps.add(submitStep);

        int nodeIndex = 0;
        Map<String, String> nodeIndexNames = new HashMap<>();
        for (Map.Entry<String, List<Object[]>> e : stepBatchMap.entrySet()) {
            nodeIndex++;
            List<Object[]> group = e.getValue();

            // 按审批时间排序
            if (group.size() > 1) {
                group.sort((o1, o2) -> {
                    Date t1 = (Date) (o1[3] == null ? o1[4] : o1[3]);
                    Date t2 = (Date) (o2[3] == null ? o2[4] : o2[3]);
                    return t1.compareTo(t2);
                });
            }

            String node = (String) group.get(0)[6];
            FlowNode flowNode = null;
            try {
                flowNode = getFlowParser().getNode(node);
            } catch (ApprovalException | ConfigurationException ex) {
                log.warn("Cannot parse node : {}", node, ex);
            }

            JSONArray step = new JSONArray();
            for (Object[] o : group) {
                JSONObject s = formatStep(o, flowNode == null ? FlowNode.SIGN_OR : flowNode.getSignMode());

                if (FlowNode.NODE_AUTOAPPROVAL.equals(node)) {
                    // No name
                } else if (FlowNode.NODE_REVOKED.equals(node)) {
                    String nodeName = Language.L("管理员撤销");
                    s.put("nodeName", nodeName);
                } else if (FlowNode.NODE_CANCELED.equals(node)) {
                    String nodeName = Language.L("提交人撤回");
                    s.put("nodeName", nodeName);
                } else {
                    String nodeName = flowNode == null ? null : flowNode.getDataMap().getString("nodeName");
                    if (StringUtils.isBlank(nodeName)) {
                        nodeName = nodeIndexNames.get(node);
                        if (StringUtils.isBlank(nodeName)) {
                            nodeName = Language.L("审批人") + "#" + nodeIndex;
                            nodeIndexNames.put(node, nodeName);
                        }
                    }
                    s.put("nodeName", nodeName);
                }

                s.put("node", node);
                step.add(s);
            }
            steps.add(step);
        }

        return steps;
    }

    private JSONObject formatStep(Object[] step, String signMode) {
        ID approver = (ID) step[0];
        JSONObject s = JSONUtils.toJSONObject(
                new String[]{"approver", "approverName", "state", "remark", "approvedTime", "createdOn", "signMode"},
                new Object[]{
                        approver, UserHelper.getName(approver),
                        step[1], step[2],
                        step[3] == null ? null : CalendarUtils.getUTCDateTimeFormat().format(step[3]),
                        CalendarUtils.getUTCDateTimeFormat().format(step[4]), signMode });

        if (step.length > 9 && step[9] != null) {
            List<String> names = new ArrayList<>();
            for (ID u : (ID[]) step[9]) names.add(UserHelper.getName(u));
            s.put("ccUsers", names);
        }
        if (step.length > 10 && step[10] != null) {
            List<String> mobileOrEmails = new ArrayList<>();
            Collections.addAll(mobileOrEmails, step[10].toString().split(","));
            s.put("ccAccounts", mobileOrEmails);
        }
        if (step.length > 11 && step[11] != null) {
            JSONObject attrMored = JSONArray.parseObject((String) step[11]);
            // 转审
            String referralFrom = attrMored.getString("referralFrom");
            s.put("referralFrom", ID.isId(referralFrom) ? UserHelper.getName(ID.valueOf(referralFrom)) : null);
            // 加签
            String countersignFrom = attrMored.getString("countersignFrom");
            s.put("countersignFrom", ID.isId(countersignFrom) ? UserHelper.getName(ID.valueOf(countersignFrom)) : null);
        }

        return s;
    }

    /**
     * 获取可回退节点
     *
     * @return
     */
    public JSONArray getBackSteps() {
        ApprovalStatus status = ApprovalHelper.getApprovalStatus(this.record);
        this.approval = status.getApprovalId();

        String currentNode = getCurrentNodeId(status);
        if (FlowNode.NODE_ROOT.equals(currentNode)) return JSONUtils.EMPTY_ARRAY;

        FlowParser flowParser = getFlowParser();
        LinkedList<String[]> set = new LinkedList<>();
        while (currentNode != null) {
            FlowNode node = flowParser.getNode(currentNode);
            if (!FlowNode.TYPE_CC.equals(node.getType())) {
                set.addFirst(new String[] { node.getNodeId(), node.getDataMap().getString("nodeName") });
            }

            currentNode = node.prevNodes;
            if (FlowNode.NODE_ROOT.equals(currentNode)) currentNode = null;
        }
        if (set.size() < 2) return JSONUtils.EMPTY_ARRAY;

        // 移除当前步骤
        set.removeLast();

        JSONArray back = new JSONArray();
        int nodeIndex = 0;
        for (String[] s : set) {
            nodeIndex++;

            if (StringUtils.isBlank(s[1])) {
                s[1] = Language.L("审批人") + "#" + nodeIndex;
            }
            back.add(JSONUtils.toJSONObject(new String[] { "node", "nodeName" }, s ));
        }
        return back;
    }

    /**
     * 会签时自选的审批人
     *
     * @param nextNodes
     * @return
     */
    public Set<ID> getSelfSelectedApprovers(FlowNodeGroup nextNodes) {
        String node = nextNodes.getApprovalNode() == null ? null : nextNodes.getApprovalNode().getNodeId();
        if (node == null) return Collections.emptySet();

        Object[][] array = Application.createQueryNoFilter(
                "select approver from RobotApprovalStep where recordId = ? and approvalId = ? and node = ? and isWaiting = 'T' and isCanceled = 'F'")
                .setParameter(1, this.record)
                .setParameter(2, this.approval)
                .setParameter(3, node)
                .array();

        Set<ID> set = new HashSet<>();
        for (Object[] o : array) {
            set.add((ID) o[0]);
        }
        return set;
    }

    /**
     * 共享给抄送人。注意非主事物
     *
     * @param recordId
     * @param shareTo
     */
    protected static void share2CcIfNeed(ID recordId, Set<ID> shareTo) {
        if (CommonsUtils.isEmpty(shareTo)) return;

        final EntityService es = Application.getEntityService(recordId.getEntityCode());
        for (ID user : shareTo) {
            if (!Application.getPrivilegesManager().allowRead(user, recordId)) {
                // force share
                PrivilegesGuardContextHolder.setSkipGuard(recordId);
                try {
                    es.share(recordId, user, null);
                } finally {
                    PrivilegesGuardContextHolder.getSkipGuardOnce();
                }
            }
        }
    }

    private ApprovalStatus checkApprovalState(ApprovalState mustbe) {
        final ApprovalStatus status = ApprovalHelper.getApprovalStatus(this.record);
        if (status.getCurrentState() != mustbe) {
            throw new ApprovalException(Language.L("无效审批状态 (%s) ，请刷新后重试", status.getCurrentState()));
        }
        return status;
    }

    private Object[] findProcessingStepApprover(ID approver) {
        final ApprovalStatus status = checkApprovalState(ApprovalState.PROCESSING);
        this.approval = status.getApprovalId();

        String currentNodeId = getCurrentNodeId(status);
        Object[] stepApprover = Application.createQueryNoFilter(
                "select stepId,state,approver from RobotApprovalStep where recordId = ? and approvalId = ? and node = ? and approver = ? and isCanceled = 'F'")
                .setParameter(1, this.record)
                .setParameter(2, this.approval)
                .setParameter(3, currentNodeId)
                .setParameter(4, approver)
                .unique();

        if (stepApprover == null || (int) stepApprover[1] != 1) {
            throw new ApprovalException(Language.L(stepApprover == null
                    ? Language.L("当前流程已经被其他人审批")
                    : Language.L("你已经审批过当前流程")));
        }
        return stepApprover;
    }
}
