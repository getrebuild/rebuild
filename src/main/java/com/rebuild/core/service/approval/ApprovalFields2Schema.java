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
import com.rebuild.core.metadata.impl.MetadataModificationException;
import com.rebuild.core.support.i18n.Language;

/**
 * 审批流程字段
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/04
 */
public class ApprovalFields2Schema extends Field2Schema {

    public ApprovalFields2Schema(ID user) {
        super(user);
    }

    /**
     * @param approvalEntity
     * @return Returns true if successful
     * @throws MetadataModificationException
     */
    public boolean createFields(Entity approvalEntity) throws MetadataModificationException {
        if (MetadataHelper.hasApprovalField(approvalEntity)) {
            if (!approvalEntity.containsField(EntityHelper.ApprovalLastUser)) {
                return schema2DatabaseInternal(approvalEntity, buildApporvalLastUser(approvalEntity));
            }
            if (!approvalEntity.containsField(EntityHelper.ApprovalLastTime)) {
                return schema2DatabaseInternal(approvalEntity, buildApporvalLastTime(approvalEntity));
            }
            if (!approvalEntity.containsField(EntityHelper.ApprovalLastRemark)) {
                return schema2DatabaseInternal(approvalEntity, buildApporvalLastRemark(approvalEntity));
            }
            return false;
        }

        if (!(MetadataHelper.hasPrivilegesField(approvalEntity)
                || EasyMetaFactory.valueOf(approvalEntity).isPlainEntity())) {
            throw new RebuildException("UNSUPPORTED ENTITY : " + approvalEntity.getName());
        }

        Field apporvalId = createUnsafeField(approvalEntity, EntityHelper.ApprovalId, Language.L("审批流程"),
                DisplayType.REFERENCE, true, false, false, true, true, null, "RobotApprovalConfig", CascadeModel.Ignore, null, null);
        Field apporvalState = createUnsafeField(approvalEntity, EntityHelper.ApprovalState, Language.L("审批状态"),
                DisplayType.STATE, true, false, false, true, true, null, null, null, null, ApprovalState.DRAFT.getState());
        Field apporvalStepId = createUnsafeField(approvalEntity, EntityHelper.ApprovalStepNode, Language.L("审批步骤"),
                DisplayType.TEXT, true, false, false, true, false, null, null, null, null, null);

        Field apporvalLastUser = buildApporvalLastUser(approvalEntity);
        Field apporvalLastTime = buildApporvalLastTime(approvalEntity);
        Field apporvalLastRemark = buildApporvalLastRemark(approvalEntity);

        schema2DatabaseInternal(
                approvalEntity, apporvalId, apporvalState, apporvalStepId,
                apporvalLastUser, apporvalLastTime, apporvalLastRemark);
        return true;
    }

    // v2.7 最后审批人
    private Field buildApporvalLastUser(Entity approvalEntity) {
        return createUnsafeField(approvalEntity, EntityHelper.ApprovalLastUser, Language.L("最后审批人"),
                DisplayType.REFERENCE, true, false, false, true, true, null, "User", CascadeModel.Ignore, null, null);
    }

    // v3.1 最后审批批注
    private Field buildApporvalLastRemark(Entity approvalEntity) {
        return createUnsafeField(approvalEntity, EntityHelper.ApprovalLastRemark, Language.L("最后审批批注"),
                DisplayType.NTEXT, true, false, false, true, true, null, null, null, null, null);
    }

    // v3.2 最后审批时间
    private Field buildApporvalLastTime(Entity approvalEntity) {
        return createUnsafeField(approvalEntity, EntityHelper.ApprovalLastTime, Language.L("最后审批时间"),
                DisplayType.DATETIME, true, false, false, true, true, null, null, null, null, null);
    }

    private boolean schema2DatabaseInternal(Entity approvalEntity, Field... fields) {
        boolean schemaReady = schema2Database(approvalEntity, fields);

        if (!schemaReady) {
            Application.getCommonsService().delete(recordedMetaIds.toArray(new ID[0]));
            throw new MetadataModificationException(Language.L("无法同步元数据到数据库"));
        }

        MetadataHelper.getMetadataFactory().refresh();
        return true;
    }
}
