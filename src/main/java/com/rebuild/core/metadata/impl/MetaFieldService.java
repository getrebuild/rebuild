/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MetadataException;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.AutoFillinManager;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.core.service.InternalPersistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 请使用协作类 {@link Field2Schema}
 *
 * @author Zixin (RB)
 * @since 08/03/2018
 */
@Slf4j
@Service
public class MetaFieldService extends InternalPersistService implements AdminGuard {

    protected MetaFieldService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.MetaField;
    }

    @Override
    public int delete(ID recordId) {
        Object[] fieldRecord = getPersistManagerFactory().createQuery(
                "select belongEntity,fieldName from MetaField where fieldId = ?")
                .setParameter(1, recordId)
                .unique();
        Field field = null;
        try {
            field = MetadataHelper.getField((String) fieldRecord[0], (String) fieldRecord[1]);
        } catch (MetadataException ignored) {
        }

        // 删除此字段的相关配置记录
        // Field: belongEntity, belongField
        String[] whoUsed = field == null ? new String[0] : new String[]{
                "PickList", "AutoFillinConfig", "NreferenceItem", "Attachment"
        };

        int del = 0;
        for (String who : whoUsed) {
            Entity whichEntity = MetadataHelper.getEntity(who);

            Object belongEntity = "Attachment".equals(who) ? field.getOwnEntity().getEntityCode() : field.getOwnEntity().getName();
            String dsql = String.format(
                    "delete from `%s` where `BELONG_ENTITY` = '%s' and `BELONG_FIELD` = '%s'",
                    whichEntity.getPhysicalName(), belongEntity.toString(), field.getName());
            int d = Application.getSqlExecutor().execute(dsql);

            if (d > 0) {
                log.warn("Deleted configuration of field [ {}.{} ] in [ {} ] : {}",
                        field.getOwnEntity().getName(), field.getName(), who, d);

                if ("PickList".equals(who)) {
                    PickListManager.instance.clean(field);
                } else if ("AutoFillinConfig".equals(who)) {
                    AutoFillinManager.instance.clean(field);
                }
            }
        }

        del += super.delete(recordId);
        return del;
    }

    @Override
    public Record update(Record record) {
        record = super.update(record);

        MetadataHelper.getMetadataFactory().refresh();
        return record;
    }

    /**
     * @see Field2Schema
     */
    @Override
    public Record create(Record record) {
        return super.create(record);
    }
}