/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.CascadeModel;
import com.rebuild.core.Application;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.Field2Schema;
import com.rebuild.core.metadata.impl.MetaFieldService;
import com.rebuild.core.metadata.impl.MetadataModificationException;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.i18n.Language;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 审批流程字段
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/04
 */
@Slf4j
public class ApprovalFields2Schema extends Field2Schema {

    public ApprovalFields2Schema() {
        super(UserService.ADMIN_USER);
    }

    /**
     * @param entity
     * @return Returns true if successful
     * @throws MetadataModificationException
     */
    public boolean createFields(Entity entity) throws MetadataModificationException {
        if (MetadataHelper.hasApprovalField(entity)) {
            if (!entity.containsField(EntityHelper.ApprovalLastUser)) {
                return schema2DatabaseInternal(entity, buildApporvalLastUser(entity));
            }
            if (!entity.containsField(EntityHelper.ApprovalLastTime)) {
                return schema2DatabaseInternal(entity, buildApporvalLastTime(entity));
            }
            if (!entity.containsField(EntityHelper.ApprovalLastRemark)) {
                return schema2DatabaseInternal(entity, buildApporvalLastRemark(entity));
            }
            if (!entity.containsField(EntityHelper.ApprovalStepUsers)) {
                return schema2DatabaseInternal(entity,
                        buildApprovalStepUsers(entity), buildApprovalStepNodeName(entity));
            }
            return false;
        }

        if (!(MetadataHelper.hasPrivilegesField(entity)
                || EasyMetaFactory.valueOf(entity).isPlainEntity())) {
            throw new RebuildException("UNSUPPORTED ENTITY : " + entity.getName());
        }

        Field apporvalId = createUnsafeField(entity, EntityHelper.ApprovalId, Language.L("审批流程"),
                DisplayType.REFERENCE, true, false, false, true, true, null, "RobotApprovalConfig", CascadeModel.Ignore, null, null);
        Field apporvalState = createUnsafeField(entity, EntityHelper.ApprovalState, Language.L("审批状态"),
                DisplayType.STATE, true, false, false, true, true, null, null, null, null, ApprovalState.DRAFT.getState());
        Field apporvalStepNode = createUnsafeField(entity, EntityHelper.ApprovalStepNode, Language.L("审批步骤"),
                DisplayType.TEXT, true, false, false, true, false, null, null, null, null, null);

        Field apporvalLastUser = buildApporvalLastUser(entity);
        Field apporvalLastTime = buildApporvalLastTime(entity);
        Field apporvalLastRemark = buildApporvalLastRemark(entity);
        Field approvalStepUsers = buildApprovalStepUsers(entity);
        Field approvalStepNodeName = buildApprovalStepNodeName(entity);

        schema2DatabaseInternal(
                entity, apporvalId, apporvalState, apporvalStepNode,
                apporvalLastUser, apporvalLastTime, apporvalLastRemark, approvalStepUsers, approvalStepNodeName);
        return true;
    }

    // v2.7 最后审批人
    private Field buildApporvalLastUser(Entity entity) {
        return createUnsafeField(entity, EntityHelper.ApprovalLastUser, Language.L("最后审批人"),
                DisplayType.REFERENCE, true, false, false, true, true, null, "User", CascadeModel.Ignore, null, null);
    }

    // v3.1 最后审批批注
    private Field buildApporvalLastRemark(Entity entity) {
        return createUnsafeField(entity, EntityHelper.ApprovalLastRemark, Language.L("最后审批批注"),
                DisplayType.NTEXT, true, false, false, true, true, null, null, null, null, null);
    }

    // v3.2 最后审批时间
    private Field buildApporvalLastTime(Entity entity) {
        return createUnsafeField(entity, EntityHelper.ApprovalLastTime, Language.L("最后审批时间"),
                DisplayType.DATETIME, true, false, false, true, true, null, null, null, null, null);
    }

    // v3.7 当前审批人
    private Field buildApprovalStepUsers(Entity entity) {
        return createUnsafeField(entity, EntityHelper.ApprovalStepUsers, Language.L("当前审批人"),
                DisplayType.N2NREFERENCE, true, false, false, true, true, null, "User", CascadeModel.Ignore, null, null);
    }

    // v3.7 当前审批步骤
    private Field buildApprovalStepNodeName(Entity entity) {
        return createUnsafeField(entity, EntityHelper.ApprovalStepNodeName, Language.L("审批步骤"),
                DisplayType.TEXT, true, false, false, true, true, null, null, null, null, null);
    }

    private boolean schema2DatabaseInternal(Entity entity, Field... fields) {
        boolean schemaReady = schema2Database(entity, fields);

        if (!schemaReady) {
            Application.getCommonsService().delete(recordedMetaIds.toArray(new ID[0]));
            throw new MetadataModificationException(Language.L("无法同步元数据到数据库"));
        }

        MetadataHelper.getMetadataFactory().refresh();
        return true;
    }

    /**
     * @param entity
     * @return
     */
    public boolean dropFields(Entity entity) {
        final String[] approvalFields = new String[] {
                EntityHelper.ApprovalId, EntityHelper.ApprovalState, EntityHelper.ApprovalStepNode,
                EntityHelper.ApprovalLastUser, EntityHelper.ApprovalLastTime, EntityHelper.ApprovalLastRemark,
                EntityHelper.ApprovalStepUsers, EntityHelper.ApprovalStepNodeName
        };

        List<String> drops = new ArrayList<>();
        List<ID> metas = new ArrayList<>();
        for (String s : approvalFields) {
            if (!entity.containsField(s)) continue;

            Field field = entity.getField(s);
            drops.add(String.format("drop column `%s`", field.getPhysicalName()));
            metas.add(EasyMetaFactory.valueOf(field).getMetaId());
        }

        String ddl = String.format("alter table `%s` ", entity.getPhysicalName());
        ddl += StringUtils.join(drops, ", ");

        try {
            Application.getSqlExecutor().execute(ddl, DDL_TIMEOUT);
        } catch (Throwable ex) {
            log.error("DDL ERROR : \n" + ddl, ex);
            return false;
        }

        for (ID id : metas) {
            Application.getBean(MetaFieldService.class).delete(id);
        }
        MetadataHelper.getMetadataFactory().refresh();
        return true;
    }
}
