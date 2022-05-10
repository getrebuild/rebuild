/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * 流程解析
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/06
 */
public class FlowParser {

    private static final JSONObject EMPTY_FLOWS = JSONUtils.toJSONObject("nodes", new Object[0]);

    final private JSON flowDefinition;

    private final Map<String, FlowNode> nodeMap = new HashMap<>();

    /**
     * @param flowDefinition
     */
    public FlowParser(JSON flowDefinition) {
        this.flowDefinition = flowDefinition == null ? EMPTY_FLOWS : flowDefinition;
        preparedNodes(((JSONObject) this.flowDefinition).getJSONArray("nodes"), null);
    }

    /**
     * @param nodes
     * @param ownBranch
     */
    private void preparedNodes(JSONArray nodes, FlowBranch ownBranch) {
        String prevNode = null;
        if (ownBranch != null) {
            prevNode = ownBranch.getNodeId();
        }

        for (Object o : nodes) {
            // 节点
            JSONObject node = (JSONObject) o;
            String nodeId = node.getString("nodeId");
            if (!FlowNode.TYPE_CONDITION.equals(node.getString("type"))) {
                FlowNode flowNode = FlowNode.valueOf(node);
                if (prevNode != null) {
                    flowNode.prevNodes = prevNode;
                }
                prevNode = nodeId;
                nodeMap.put(nodeId, flowNode);

                if (ownBranch != null) {
                    ownBranch.addNode(nodeId);
                }
            }

            // 分支
            JSONArray branches = node.getJSONArray("branches");
            if (branches != null) {
                Set<String> prevNodes = new HashSet<>();
                for (Object b : branches) {
                    JSONObject branch = (JSONObject) b;
                    String branchNodeId = branch.getString("nodeId");
                    FlowBranch flowBranch = FlowBranch.valueOf(branch);
                    if (prevNode != null) {
                        flowBranch.prevNodes = prevNode;
                    }
                    nodeMap.put(branchNodeId, flowBranch);

                    preparedNodes(branch.getJSONArray("nodes"), flowBranch);
                    prevNodes.add(flowBranch.getLastNode());
                }
                prevNode = StringUtils.join(prevNodes, "|");
            }
        }
    }

    /**
     * @param nodeId
     * @return
     */
    public List<FlowNode> getNextNodes(String nodeId) {
        List<FlowNode> next = new ArrayList<>();
        for (FlowNode node : getAllNodes()) {
            if (node.prevNodes != null && node.prevNodes.contains(nodeId)) {
                next.add(node);
            }
        }

        if (next.isEmpty()) {
            return Collections.emptyList();
        }

        // 非条件分支，只会有一个节点
        if (!FlowNode.TYPE_BRANCH.equals(next.get(0).getType())) {
            return next;
        }

        // 条件节点优先级排序
        next.sort((o1, o2) -> {
            int p1 = ((FlowBranch) o1).getPriority();
            int p2 = ((FlowBranch) o2).getPriority();
            return Integer.compare(p1, p2);
        });
        return next;
    }

    /**
     * @param nodeId
     * @return
     */
    public FlowNode getNode(String nodeId) {
        if (nodeMap.containsKey(nodeId)) {
            return nodeMap.get(nodeId);
        }
        throw new ApprovalException(Language.L("无效审批步骤节点 (%s)", nodeId));
    }

    /**
     * 是否有审批人节点，没有审批人节点的无效
     *
     * @return
     */
    public boolean hasApproverNode() {
        for (FlowNode node : nodeMap.values()) {
            if (node.getType().equals(FlowNode.TYPE_APPROVER)) return true;
        }
        return false;
    }

    /**
     * @return
     */
    protected Collection<FlowNode> getAllNodes() {
        return nodeMap.values();
    }

    /**
     * @return
     */
    protected JSON getFlowDefinition() {
        return flowDefinition;
    }

    /**
     * @param nodeId
     * @param space
     */
    protected void prettyPrint(String nodeId, String space) {
        space = space == null ? "" : space;
        FlowNode node = getNode(nodeId);
        System.out.println(space + node);

        List<FlowNode> next = this.getNextNodes(nodeId);
        for (FlowNode n : next) {
            prettyPrint(n.getNodeId(), space + "  ");
        }
    }
}
