/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.exception.jdbc.SqlSyntaxException;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.support.i18n.Language;
import org.springframework.stereotype.Service;

/**
 * 审批流程
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/27
 */
@Service
public class RobotApprovalConfigService extends BaseConfigurationService implements AdminGuard {

    protected RobotApprovalConfigService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.RobotApprovalConfig;
    }

    @Override
    public Record create(Record record) {
        String entity = record.getString("belongEntity");
        new ApprovalFields2Schema().createFields(MetadataHelper.getEntity(entity));
        return super.create(record);
    }

    @Override
    public Record update(Record record) {
        if (record.hasValue("flowDefinition")) {
            int inUsed = ApprovalHelper.checkInUsed(record.getPrimary());
            if (inUsed > 0) {
                throw new DataSpecificationException(Language.L("有 %d 条记录正在使用此流程，禁止修改", inUsed));
            }
        }
        return super.update(record);
    }

    @Override
    public int delete(ID recordId) {
        int inUsed = 0;
        try {
            inUsed = ApprovalHelper.checkInUsed(recordId);
        } catch (SqlSyntaxException sqlex) {
            // fix: 表已不存在
            if (!ThrowableUtils.getRootCause(sqlex).getLocalizedMessage().contains("doesn't exist")) {
                throw sqlex;
            }
        }

        if (inUsed > 0) {
            throw new DataSpecificationException(Language.L("有 %d 条记录正在使用此流程，禁止删除", inUsed));
        }
        return super.delete(recordId);
    }

    @Override
    protected void cleanCache(ID cfgid) {
        Object[] cfg = Application.createQueryNoFilter(
                "select belongEntity from RobotApprovalConfig where configId = ?")
                .setParameter(1, cfgid)
                .unique();
        if (cfg != null) {
            Entity entity = MetadataHelper.getEntity((String) cfg[0]);
            RobotApprovalManager.instance.clean(entity);
        }
    }
}
