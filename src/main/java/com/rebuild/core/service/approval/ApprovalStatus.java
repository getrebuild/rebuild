/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import org.apache.commons.lang.StringUtils;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2020/08/10
 */
public class ApprovalStatus {

    private ID approvalId;
    private String approvalName;

    private Integer currentState;
    private String currentStepNode;
    private String lastComment;

    final private ID recordId;

    protected ApprovalStatus(ID approvalId, String approvalName, Integer currentState, String currentStepNode, ID recordId) {
        this.approvalId = approvalId;
        this.approvalName = approvalName;
        this.currentState = currentState;
        this.currentStepNode = currentStepNode;
        this.recordId = recordId;
    }

    public ID getApprovalId() {
        return approvalId;
    }

    public String getApprovalName() {
        return approvalName;
    }

    public ApprovalState getCurrentState() {
        return currentState == null
                ? ApprovalState.DRAFT : (ApprovalState) ApprovalState.valueOf(currentState);
    }

    public String getCurrentStepNode() {
        return currentStepNode;
    }

    public String getLastComment() {
        if (currentStepNode == null || lastComment != null) {
            return StringUtils.defaultIfBlank(lastComment, null);
        }

        Object[] last = Application.createQueryNoFilter(
                "select remark from RobotApprovalStep where recordId = ? and node = ? order by modifiedOn desc")
                .setParameter(1, this.recordId)
                .setParameter(2, this.currentStepNode)
                .unique();

        if (last == null) {
            lastComment = StringUtils.EMPTY;
        } else {
            lastComment = StringUtils.defaultIfBlank((String) last[0], StringUtils.EMPTY);
        }

        return StringUtils.defaultIfBlank(lastComment, null);
    }
}
