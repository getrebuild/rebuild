/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao
 * @since 2021/8/25
 */
public class QueryHelper {

    /**
     * 指定记录是否符合过滤条件
     *
     * @param recordId
     * @param advFilter
     * @return
     */
    public static boolean isMatchAdvFilter(ID recordId, JSONObject advFilter) {
        if (!ParseHelper.validAdvFilter(advFilter)) return true;

        String filterSql = new AdvFilterParser(advFilter).toSqlWhere();
        if (filterSql != null) {
            Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
            String sql = MessageFormat.format(
                    "select {0} from {1} where {0} = ? and {2}",
                    entity.getPrimaryField().getName(), entity.getName(), filterSql);
            Object[] m = Application.createQueryNoFilter(sql).setParameter(1, recordId).unique();
            return m != null;
        }
        return true;
    }

    /**
     * @param sql
     * @param useEntity
     * @return
     */
    public static Query createQuery(String sql, Entity useEntity) {
        if (MetadataHelper.hasPrivilegesField(useEntity) || useEntity.getMainEntity() != null) {
            return Application.createQuery(sql);
        }
        return Application.createQueryNoFilter(sql);
    }

    /**
     * 获取完整记录
     *
     * @param recordId
     * @return
     */
    public static Record recordNoFilter(ID recordId) {
        Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());

        List<String> fields = new ArrayList<>();
        for (Field field : entity.getFields()) {
            fields.add(field.getName());
        }

        String sql = String.format("select %s from %s where %s = ?",
                StringUtils.join(fields, ","), entity.getName(),
                entity.getPrimaryField().getName());

        Record record = Application.createQueryNoFilter(sql).setParameter(1, recordId).record();
        Assert.notNull(record, "RECORD NOT EXISTS : " + recordId);
        return record;
    }

    /**
     * 获取明细（完整）记录
     *
     * @param mainId
     * @return
     */
    public static List<Record> detailsNoFilter(ID mainId) {
        Entity detailEntity = MetadataHelper.getEntity(mainId.getEntityCode()).getDetailEntity();

        List<String> fields = new ArrayList<>();
        for (Field field : detailEntity.getFields()) {
            fields.add(field.getName());
        }

        String sql = String.format("select %s from %s where %s = ?",
                StringUtils.join(fields, ","), detailEntity.getName(),
                MetadataHelper.getDetailToMainField(detailEntity).getName());

        return Application.createQueryNoFilter(sql).setParameter(1, mainId).list();
    }
}
