/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.commons.RegexUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 流程节点（包括审批、抄送）
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/06
 */
@SuppressWarnings("unused")
public class FlowNode {

    // 特殊节点

    public static final String NODE_ROOT = "ROOT";
    public static final String NODE_CANCELED = "CANCELED";
    public static final String NODE_REVOKED = "REVOKED";
    public static final String NODE_AUTOAPPROVAL = "AUTOAPPROVAL";

    // 节点类型

    public static final String TYPE_START = "start";
    public static final String TYPE_APPROVER = "approver";
    public static final String TYPE_CC = "cc";
    public static final String TYPE_CONDITION = "condition";
    public static final String TYPE_BRANCH = "branch";

    // 人员类型

    public static final String USER_ALL = "ALL";
    public static final String USER_SELF = "SELF";
    public static final String USER_SPEC = "SPEC";
    public static final String USER_OWNS = "OWNS";

    // 多人联合审批类型

    public static final String SIGN_AND = "AND";  // 会签（默认）
    public static final String SIGN_OR = "OR";    // 或签
    public static final String SIGN_ALL = "ALL";  // 逐个审批（暂未用）

    // --

    @Getter
    private String nodeId;
    @Getter
    private String type;
    private JSONObject dataMap;

    protected String prevNodes;

    /**
     * @param nodeId
     * @param type
     * @param dataMap
     */
    protected FlowNode(String nodeId, String type, JSONObject dataMap) {
        super();
        this.nodeId = nodeId;
        this.type = type;
        this.dataMap = dataMap;
    }

    /**
     * @return
     */
    public String getNodeName() {
        return getDataMap().getString("nodeName");
    }

    /**
     * @return
     */
    public JSONObject getDataMap() {
        return dataMap == null ? JSONUtils.EMPTY_OBJECT : dataMap;
    }

    /**
     * @return
     */
    public String getSignMode() {
        return StringUtils.defaultIfBlank(getDataMap().getString("signMode"), SIGN_OR);
    }

    /**
     * @return
     */
    public boolean getRejectStep() {
        Boolean b = getDataMap().getBoolean("rejectStep");
        return b == null || b;
    }

    /**
     * @return
     */
    public boolean allowSelfSelecting() {
        Boolean b = getDataMap().getBoolean("selfSelecting");
        return b == null || b;
    }

    /**
     * @return
     */
    public boolean allowCcAutoShare() {
        Boolean b = getDataMap().getBoolean("ccAutoShare");
        return b != null && b;
    }

    /**
     * @return
     */
    public boolean allowReferral() {
        Boolean b = getDataMap().getBoolean("allowReferral");
        return b != null && b;
    }

    /**
     * @return
     */
    public boolean allowCountersign() {
        Boolean b = getDataMap().getBoolean("allowCountersign");
        return b != null && b;
    }

    /**
     * @return
     */
    public boolean allowBatch() {
        Boolean b = getDataMap().getBoolean("allowBatch");
        return b != null && b;
    }

    /**
     * 获取相关人员（提交人/审批人/抄送人）
     *
     * @param operator
     * @param record
     * @return
     */
    public Set<ID> getSpecUsers(ID operator, ID record) {
        JSONArray userDefs = getDataMap().getJSONArray("users");
        if (userDefs == null || userDefs.isEmpty()) return Collections.emptySet();

        String userType = userDefs.getString(0);
        if (USER_SELF.equalsIgnoreCase(userType)) {
            Set<ID> users = new HashSet<>();
            ID owning = Application.getRecordOwningCache().getOwningUser(record);
            users.add(owning);
            return users;
        }

        Set<ID> users = new HashSet<>();

        List<String> defsList = new ArrayList<>();
        for (Object o : userDefs) {
            String def = (String) o;
            if (def.startsWith(ApprovalHelper.APPROVAL_SUBMITOR) || def.startsWith(ApprovalHelper.APPROVAL_APPROVER)) {
                ApprovalState state = ApprovalHelper.getApprovalState(record);
                boolean isSubmitted = state == ApprovalState.PROCESSING || state == ApprovalState.APPROVED;

                ID followUser = operator;

                if (def.startsWith(ApprovalHelper.APPROVAL_SUBMITOR)) {
                    if (isSubmitted) {
                        followUser = ApprovalHelper.getSubmitter(record);
                    } else {
                        // 提交人即发起人
                    }
                } else {
                    if (isSubmitted) {
                        // 提交人即审批人
                    } else {
                        followUser = null;  // 未提交
                    }
                }

                if (followUser != null) {
                    Field userField = ApprovalHelper.checkVirtualField(def);
                    if (userField != null) {
                        Object[] ud;
                        // 部门中的用户（如上级）
                        if (userField.getOwnEntity().getEntityCode() == EntityHelper.Department) {
                            Department d = Application.getUserStore().getUser(followUser).getOwningDept();
                            ud = Application.getQueryFactory().uniqueNoFilter((ID) d.getIdentity(), userField.getName());
                        } else {
                            ud = Application.getQueryFactory().uniqueNoFilter(followUser, userField.getName());
                        }

                        if (ud != null && ud[0] != null) {
                            if (userField.getReferenceEntity().getEntityCode() == EntityHelper.Department) {
                                if (ud[0] instanceof ID[]) {
                                    for (ID x : (ID[]) ud[0]) defsList.add(x.toString());
                                } else {
                                    defsList.add(ud[0].toString());
                                }
                            } else {
                                if (ud[0] instanceof ID[]) Collections.addAll(users, (ID[]) ud[0]);
                                else users.add((ID) ud[0]);
                            }
                        }
                    }
                }

            } else {
                defsList.add(def);
            }
        }

        users.addAll(UserHelper.parseUsers(defsList, record));
        users.removeIf(id -> !UserHelper.isActive(id));

        return users;
    }

    /**
     * 获取外部抄送人（手机或邮箱）
     *
     * @param record
     * @return
     */
    public Set<String> getCcAccounts(ID record) {
        JSONArray accountFields = getDataMap().getJSONArray("accounts");
        if (accountFields == null || accountFields.isEmpty()) return Collections.emptySet();

        Entity useEntity = MetadataHelper.getEntity(record.getEntityCode());
        List<String> useFields = new ArrayList<>();

        for (Object o : accountFields) {
            if (MetadataHelper.getLastJoinField(useEntity, (String) o) != null) {
                useFields.add((String) o);
            }
        }
        if (useFields.isEmpty()) return Collections.emptySet();

        Object[] o = Application.getQueryFactory().uniqueNoFilter(record, useFields.toArray(new String[0]));
        if (o == null) return Collections.emptySet();

        Set<String> mobileOrEmail = new HashSet<>();
        for (Object me : o) {
            String me2 = me == null ? null : me.toString();
            if (RegexUtils.isCNMobile(me2) || RegexUtils.isEMail(me2)) {
                mobileOrEmail.add(me2);
            }
        }
        return mobileOrEmail;
    }

    @Override
    public String toString() {
        String string = String.format("Id:%s, Type:%s", nodeId, type);
        if (prevNodes != null) {
            string += ", Prev:" + prevNodes;
        }
        if (dataMap != null) {
            string += ", Data:" + dataMap.toJSONString();
        }
        return string;
    }

    @Override
    public int hashCode() {
        return this.nodeId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return obj instanceof FlowNode && obj.hashCode() == this.hashCode();
    }

    /**
     * 节点可编辑字段
     *
     * @return
     */
    public JSONArray getEditableFields() {
        JSONArray editableFields = dataMap == null ? null : dataMap.getJSONArray("editableFields");
        if (editableFields == null) return null;

        editableFields = (JSONArray) JSONUtils.clone(editableFields);
        for (Object o : editableFields) {
            JSONObject field = (JSONObject) o;
            field.put("nullable", !((Boolean) field.remove("notNull")));
        }
        return editableFields;
    }

    /**
     * 限时审批
     *
     * @return
     */
    public JSONObject getExpiresAuto() {
        JSONObject expiresAuto = dataMap == null ? null : dataMap.getJSONObject("expiresAuto");
        if (expiresAuto == null) return null;
        if (expiresAuto.getIntValue("expiresAuto") <= 0) return null;
        return expiresAuto;
    }

    /**
     * @param recordId
     * @param approver
     * @return `>=2` 表示超时时间
     */
    public long getRemarkReq(ID recordId, ID approver) {
        // 0=选填, 1=必填, 2=超时必填
        int reqType = getDataMap().getIntValue("remarkReq");
        if (reqType < 2) return reqType;

        return getExpiredTime(recordId, approver);
    }

    /**
     * @param recordId
     * @param approver
     * @return
     * @see com.rebuild.rbv.approval.ApprovalExpiresAutoJob#getExpiredTime(Date, JSONObject, ID)
     */
    public long getExpiredTime(ID recordId, ID approver) {
        // 超时必填 @see ApprovalExpiresAutoJob
        Object[] stepApprover = Application.createQueryNoFilter(
                "select createdOn,stepId from RobotApprovalStep where recordId = ? and approver = ? and node = ? and isCanceled = 'F' order by createdOn desc")
                .setParameter(1, recordId)
                .setParameter(2, approver)
                .setParameter(3, this.nodeId)
                .unique();
        if (stepApprover == null) return 0;

        JSONObject eaConf = getExpiresAuto();
        Object o = CommonsUtils.invokeMethod("com.rebuild.rbv.approval.ApprovalExpiresAutoJob#getExpiredTime",
                stepApprover[0], eaConf, recordId);
        long expTime = o == null ? 0 : (Long) o;
        return expTime > 0 ? Math.max(expTime, 2) : 0;
    }

    // --

    /**
     * @param node
     * @return
     */
    public static FlowNode valueOf(JSONObject node) {
        return new FlowNode(
                node.getString("nodeId"), node.getString("type"), node.getJSONObject("data"));
    }
}
