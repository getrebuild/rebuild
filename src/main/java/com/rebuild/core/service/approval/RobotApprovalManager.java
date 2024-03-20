/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 审批流程管理
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/24
 */
@Slf4j
public class RobotApprovalManager implements ConfigManager {

    public static final RobotApprovalManager instance = new RobotApprovalManager();

    private RobotApprovalManager() {
    }

    private static final String CKEY_PREFIX = "RobotApprovalManager2-";

    /**
     * 获取实体/记录流程状态
     *
     * @param entity
     * @param recordId
     * @return null 表示没有流程
     */
    public ApprovalState hadApproval(Entity entity, ID recordId) {
        if (entity.getMainEntity() != null || !MetadataHelper.hasApprovalField(entity)) return null;

        // 记录的
        if (recordId != null) {
            if (!recordId.getEntityCode().equals(entity.getEntityCode())) {
                log.warn("Entity and Record/ID mismatch : {}, {}", entity, recordId);
                CommonsUtils.printStackTrace();
                return null;
            }

            Object[] o = Application.getQueryFactory().uniqueNoFilter(
                    recordId, EntityHelper.ApprovalId, EntityHelper.ApprovalState);
            if (o != null && o[0] != null) {
                return (ApprovalState) ApprovalState.valueOf((Integer) o[1]);
            }
        }

        // 实体的
        FlowDefinition[] defs = getFlowDefinitions(entity);
        for (FlowDefinition def : defs) {
            if (!def.isDisabled()) {
                return ApprovalState.DRAFT;
            }
        }

        return null;
    }


    /**
     * 获取实体是否有流程
     *
     * @param entity
     * @return
     */
    public boolean hadApproval(Entity entity) {
        if (entity.getMainEntity() != null || !MetadataHelper.hasApprovalField(entity)) return false;

        FlowDefinition[] defs = getFlowDefinitions(entity);
        for (FlowDefinition def : defs) {
            if (!def.isDisabled()) return true;
        }
        return false;
    }

    /**
     * @param approvalId
     * @return
     * @see #getFlowDefinition(Entity, ID)
     */
    public FlowDefinition getFlowDefinition(ID approvalId) {
        Object[] o = Application.getQueryFactory().uniqueNoFilter(approvalId, "belongEntity");
        Entity entity = MetadataHelper.getEntity((String) o[0]);
        return getFlowDefinition(entity, approvalId);
    }

    /**
     * @param entity
     * @param approvalId
     * @return
     */
    public FlowDefinition getFlowDefinition(Entity entity, ID approvalId) {
        FlowDefinition[] defs = getFlowDefinitions(entity);
        for (FlowDefinition def : defs) {
            if (approvalId.equals(def.getID("id"))) {
                return def;
            }
        }
        throw new ConfigurationException("No `RobotApprovalConfig` found : " + approvalId);
    }

    /**
     * 获取用户可用流程
     *
     * @param recordId
     * @param user
     * @return
     */
    public FlowDefinition[] getFlowDefinitions(ID recordId, ID user) {
        FlowDefinition[] defs = getFlowDefinitions(MetadataHelper.getEntity(recordId.getEntityCode()));
        if (defs.length == 0) {
            return new FlowDefinition[0];
        }

        ID owning = Application.getRecordOwningCache().getOwningUser(recordId);
        // 过滤可用的
        List<FlowDefinition> workable = new ArrayList<>();
        for (FlowDefinition def : defs) {
            if (def.isDisabled() || !def.isWorkable()) continue;

            FlowParser flowParser = def.createFlowParser();
            FlowNode root = flowParser.getNode("ROOT");  // 发起人节点

            // 发起人匹配
            JSONArray users = root.getDataMap().getJSONArray("users");
            if (users == null || users.isEmpty()) {
                // v3.5.5/3.5.6 默认所有人可发起
                users = JSON.parseArray("['ALL']");
            }

            final String rootUser = users.getString(0);
            if (FlowNode.USER_ALL.equals(rootUser)
                    || (FlowNode.USER_OWNS.equals(rootUser) && owning.equals(user))
                    || UserHelper.parseUsers(users, recordId).contains(user)) {

                // 过滤条件
                JSONObject filter = root.getDataMap().getJSONObject("filter");
                if (QueryHelper.isMatchAdvFilter(recordId, filter)) {
                    workable.add(def);
                }
            }
        }
        return workable.toArray(new FlowDefinition[0]);
    }

    /**
     * 获取指定实体的所有审批流程（含禁用的）
     *
     * @param entity
     * @return
     */
    public FlowDefinition[] getFlowDefinitions(Entity entity) {
        final String cKey = CKEY_PREFIX + entity.getName();
        FlowDefinition[] defs = (FlowDefinition[]) Application.getCommonsCache().getx(cKey);
        if (defs != null) {
            return defs;
        }

        Object[][] array = Application.createQueryNoFilter(
                "select flowDefinition,isDisabled,name,configId,modifiedOn from RobotApprovalConfig where belongEntity = ?")
                .setParameter(1, entity.getName())
                .array();

        List<FlowDefinition> list = new ArrayList<>();
        for (Object[] o : array) {
            FlowDefinition def = (FlowDefinition) new FlowDefinition()
                    .set("flowDefinition", JSON.parseObject((String) o[0]))
                    .set("disabled", o[1])
                    .set("name", o[2])
                    .set("id", o[3]);
            list.add(def);
        }

        defs = list.toArray(new FlowDefinition[0]);
        Application.getCommonsCache().putx(cKey, defs);
        return defs;
    }

    @Override
    public void clean(Object entity) {
        final String cKey = CKEY_PREFIX + ((Entity) entity).getName();
        Application.getCommonsCache().evict(cKey);
    }
}
