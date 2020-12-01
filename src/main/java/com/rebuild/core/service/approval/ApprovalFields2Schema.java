/*
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
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.DisplayType;
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
            return false;
        }
        if (!(MetadataHelper.hasPrivilegesField(approvalEntity) || EasyMetaFactory.valueOf(approvalEntity).isPlainEntity())) {
            throw new RebuildException("Unsupported entity : " + approvalEntity.getName());
        }

        Field apporvalId = createUnsafeField(approvalEntity, EntityHelper.ApprovalId, Language.L("f.approvalId"),
                DisplayType.REFERENCE, true, false, false, true, true, null, "RobotApprovalConfig", CascadeModel.Ignore, null, null);
        Field apporvalState = createUnsafeField(approvalEntity, EntityHelper.ApprovalState, Language.L("f.approvalState"),
                DisplayType.STATE, true, false, false, true, true, null, null, null, null, ApprovalState.DRAFT.getState());
        Field apporvalStepId = createUnsafeField(approvalEntity, EntityHelper.ApprovalStepNode, Language.L("f.approvalStepNode"),
                DisplayType.TEXT, true, false, false, true, false, null, null, null, null, null);

        boolean schemaReady = schema2Database(approvalEntity,
                new Field[] { apporvalId, apporvalState, apporvalStepId });

        if (!schemaReady) {
            Application.getCommonsService().delete(tempMetaId.toArray(new ID[0]));
            throw new MetadataModificationException(Language.L("NotCreateMetasToDb"));
        }

        MetadataHelper.getMetadataFactory().refresh(false);
        return true;
    }
}
