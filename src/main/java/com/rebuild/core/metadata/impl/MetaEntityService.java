/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.core.service.BaseService;
import org.springframework.stereotype.Service;

/**
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
@Service
public class MetaEntityService extends BaseService implements AdminGuard {

    protected MetaEntityService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.MetaEntity;
    }

    @Override
    public int delete(ID recordId) {
        Object[] entityRecord = getPersistManagerFactory().createQuery(
                "select entityName from MetaEntity where entityId = ?")
                .setParameter(1, recordId)
                .unique();
        final Entity entity = MetadataHelper.getEntity((String) entityRecord[0]);

        // 删除此实体的相关配置记录
        // Field: belongEntity
        String[] whoUsed = new String[]{
                "MetaField", "PickList", "LayoutConfig", "FilterConfig", "ShareAccess", "ChartConfig",
                "Attachment", "AutoFillinConfig", "RobotTriggerConfig", "RobotApprovalConfig",
                "DataReportConfig",
        };
        int del = 0;
        for (String who : whoUsed) {
            Entity whichEntity = MetadataHelper.getEntity(who);
            if (!whichEntity.containsField("belongEntity")) {
                continue;
            }

            String ql = String.format("select %s from %s where belongEntity = '%s'",
                    whichEntity.getPrimaryField().getName(), whichEntity.getName(), entity.getName());
            if (whichEntity.getEntityCode() == EntityHelper.Attachment) {
                ql = ql.split(" belongEntity ")[0] + " belongEntity = " + whichEntity.getEntityCode();
            }

            Object[][] usedArray = getPersistManagerFactory().createQuery(ql).array();
            for (Object[] used : usedArray) {
                if ("MetaField".equalsIgnoreCase(who)) {
                    del += Application.getBean(MetaFieldService.class).delete((ID) used[0]);
                } else {
                    del += super.delete((ID) used[0]);
                }
            }
            if (usedArray.length > 0) {
                LOG.warn("deleted configuration of entity [ " + entity.getName() + " ] in [ " + who + " ] : " + usedArray.length);
            }
        }

        del += super.delete(recordId);
        return del;
    }

    @Override
    public Record update(Record record) {
        record = super.update(record);

        MetadataHelper.getMetadataFactory().refresh(false);
        return record;
    }
}
