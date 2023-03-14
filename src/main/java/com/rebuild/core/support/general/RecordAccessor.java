/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import cn.devezhao.persist4j.metadata.MetadataException;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.query.QueryHelper;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 记录快捷存取器（不带权限、不带系统规则）
 *
 * @since 2022/11/25
 * @author ZHAO
 */
public class RecordAccessor {

    final private ID recordId;
    final private Entity rootEntity;
    final private boolean ignoreNoRecord;

    private Record dataSelf;
    private Map<String, Object> dataSeconds;

    /**
     * @param recordId
     */
    public RecordAccessor(ID recordId) {
        this(recordId, Boolean.TRUE);
    }

    /**
     * @param recordId
     * @param ignoreNoRecord
     */
    public RecordAccessor(ID recordId, boolean ignoreNoRecord) {
        this.recordId = recordId;
        this.rootEntity = MetadataHelper.getEntity(recordId.getEntityCode());
        this.ignoreNoRecord = ignoreNoRecord;
    }

    /**
     * 获取值
     *
     * @param field 本实体字段或二级字段
     * @return
     */
    public Object getValue(String field) {
        // 关联字段
        if (field.contains(".")) {
            if (dataSeconds == null) dataSeconds = new HashMap<>();
            if (dataSeconds.containsKey(field)) return dataSeconds.get(field);

            if (MetadataHelper.getLastJoinField(rootEntity, field) == null) {
                throw new MetadataException("No field found : " + field + " in " + rootEntity.getName());
            }

            String[] field_s = field.split("\\.");
            field_s[field_s.length - 1] = null;
            String fieldPrimary = StringUtils.join(field_s, ",");
            fieldPrimary = fieldPrimary.substring(0, fieldPrimary.length() - 1);

            Object[] o = Application.getQueryFactory().uniqueNoFilter(recordId,
                    field, fieldPrimary, rootEntity.getPrimaryField().getName());
            dataSeconds.put(field, o == null ? null : o[0]);

            return dataSeconds.get(field);
        }

        if (dataSelf == null) {
            try {
                dataSelf = QueryHelper.recordNoFilter(recordId);
            } catch (NoRecordFoundException ex) {
                if (ignoreNoRecord) {
                    dataSelf = EntityHelper.forUpdate(recordId, UserService.SYSTEM_USER, Boolean.FALSE);
                } else {
                    throw ex;
                }
            }
        }

        return dataSelf.getObjectValue(field);
    }

    /**
     * @param field
     * @param newValue
     * @return
     * @see #setValue(String, Object, boolean)
     */
    public boolean setValue(String field, Object newValue) {
        return setValue(field, newValue, Boolean.FALSE);
    }
    
    /**
     * 更新值
     *
     * @param field 仅支持本实体字段
     * @param newValue
     * @param checkSame 检查值是否一致，一致则不更新
     * @return
     */
    public boolean setValue(String field, Object newValue, boolean checkSame) {
        if (!rootEntity.containsField(field)) {
            throw new MetadataException("No field found : " + field + " in " + rootEntity.getName());
        }

        if (checkSame) {
            Object oldValue = getValue(field);
            if (NullValue.isNull(newValue) && NullValue.isNull(oldValue)) return false;
            if (newValue != null && newValue.equals(oldValue)) return false;
            if (oldValue != null && oldValue.equals(newValue)) return false;
        }

        Record r = EntityHelper.forUpdate(recordId, UserService.SYSTEM_USER, Boolean.FALSE);
        if (NullValue.isNull(newValue)) r.setNull(field);
        else r.setObjectValue(field, newValue);

        Application.getCommonsService().update(r, false);
        return true;
    }
}
