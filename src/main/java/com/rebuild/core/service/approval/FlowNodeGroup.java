/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.privileges.UserHelper;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

/**
 * 1个审批节点+N个抄送节点
 *
 * @author devezhao zhaofang123@gmail.com
 * @see FlowNode
 * @since 2019/07/11
 */
public class FlowNodeGroup {

    private Set<FlowNode> nodes = new HashSet<>();

    protected FlowNodeGroup() {
        super();
    }

    /**
     * @param node
     */
    public void addNode(FlowNode node) {
        Assert.isNull(getApprovalNode(), "Cannot add multiple approved nodes");
        nodes.add(node);
    }

    /**
     * @return
     */
    public boolean allowSelfSelectingCc() {
        for (FlowNode node : nodes) {
            if (node.getType().equals(FlowNode.TYPE_CC) && node.allowSelfSelecting()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return
     */
    public boolean allowSelfSelectingApprover() {
        FlowNode node = getApprovalNode();
        return node != null && node.allowSelfSelecting();
    }

    /**
     * @param operator
     * @param recordId
     * @param selectUsers
     * @return
     */
    public Set<ID> getCcUsers(ID operator, ID recordId, JSONObject selectUsers) {
        Set<ID> users = new HashSet<>();
        // 一般就一个，但不排除多个 CC 节点
        for (FlowNode node : nodes) {
            if (FlowNode.TYPE_CC.equals(node.getType())) {
                users.addAll(node.getSpecUsers(operator, recordId));
            }
        }

        if (selectUsers != null) {
            users.addAll(UserHelper.parseUsers(selectUsers.getJSONArray("selectCcs"), recordId, Boolean.TRUE));
        }
        return users;
    }

    /**
     * @param operator
     * @param recordId
     * @param selectUsers
     * @return
     */
    public Set<ID> getCcUsers4Share(ID operator, ID recordId, JSONObject selectUsers) {
        Set<ID> users = new HashSet<>();
        FlowNode firstNode = null;
        for (FlowNode node : nodes) {
            if (FlowNode.TYPE_CC.equals(node.getType()) && node.allowCcAutoShare()) {
                users.addAll(node.getSpecUsers(operator, recordId));

                if (firstNode == null) {
                    firstNode = node;
                }
            }
        }

        // 因为 CC 会合并，此处以第一个 CC 节点的设置为准
        if (firstNode != null && selectUsers != null) {
            users.addAll(UserHelper.parseUsers(selectUsers.getJSONArray("selectCcs"), recordId, Boolean.TRUE));
        }
        return users;
    }

    /**
     * @param recordId
     * @return
     */
    public Set<String> getCcAccounts(ID recordId) {
        Set<String> mobileOrEmails = new HashSet<>();
        // 一般就一个，但不排除多个 CC 节点
        for (FlowNode node : nodes) {
            if (FlowNode.TYPE_CC.equals(node.getType())) {
                mobileOrEmails.addAll(node.getCcAccounts(recordId));
            }
        }
        return mobileOrEmails;
    }

    /**
     * @param operator
     * @param recordId
     * @param selectUsers
     * @return
     */
    public Set<ID> getApproveUsers(ID operator, ID recordId, JSONObject selectUsers) {
        Set<ID> users = new HashSet<>();

        FlowNode node = getApprovalNode();
        if (node != null) {
            users.addAll(node.getSpecUsers(operator, recordId));
        }

        if (selectUsers != null) {
            users.addAll(UserHelper.parseUsers(selectUsers.getJSONArray("selectApprovers"), recordId, Boolean.TRUE));
        }
        return users;
    }

    /**
     * 如果没有审批节点了就当做最终审批
     *
     * @return
     */
    public boolean isLastStep() {
        // TODO 对审批最后一步加强判断
        return getApprovalNode() == null;
    }

    /**
     * @return
     */
    public boolean isValid() {
        return !nodes.isEmpty();
    }

    /**
     * 获取审批节点
     *
     * @return
     */
    public FlowNode getApprovalNode() {
        for (FlowNode node : nodes) {
            if (FlowNode.TYPE_APPROVER.equals(node.getType())) {
                return node;
            }
        }
        return null;
    }

    /**
     * 联合审批模式
     *
     * @return
     */
    public String getSignMode() {
        FlowNode node = getApprovalNode();
        return node == null ? FlowNode.SIGN_OR : node.getSignMode();
    }
    
    /**
     * @return
     */
    public String getGroupId() {
        StringBuilder sb = new StringBuilder();
        for (FlowNode node : nodes) {
            sb.append(node.getNodeId());
        }
        return sb.toString();
    }
}
