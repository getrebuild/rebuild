/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.service.query.QueryHelper;

import java.util.HashSet;
import java.util.Set;

/**
 * 流程分支（条件）
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/06
 */
public class FlowBranch extends FlowNode {

    private int priority;

    private Set<String> childNodes = new HashSet<>();
    private String lastNode;

    /**
     * @param nodeId
     * @param priority
     * @param dataMap
     */
    protected FlowBranch(String nodeId, int priority, JSONObject dataMap) {
        super(nodeId, TYPE_BRANCH, dataMap);
        this.priority = priority;
    }

    /**
     * @return
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @param child
     */
    protected void addNode(String child) {
        childNodes.add(child);
        lastNode = child;
    }

    /**
     * @return
     */
    protected Set<String> getChildNodes() {
        return childNodes;
    }

    /**
     * @return
     */
    protected String getLastNode() {
        return lastNode;
    }

    /**
     * 匹配条件分支
     *
     * @param record
     * @return
     */
    public boolean matches(ID record) {
        return QueryHelper.isMatchAdvFilter(record, (JSONObject) getDataMap().get("filter"));
    }

    @Override
    public String toString() {
        return super.toString() + ", Priority:" + getPriority();
    }

    /**
     * @param node
     * @return
     */
    public static FlowBranch valueOf(JSONObject node) {
        return new FlowBranch(
                node.getString("nodeId"), node.getIntValue("priority"), node.getJSONObject("data"));
    }
}
