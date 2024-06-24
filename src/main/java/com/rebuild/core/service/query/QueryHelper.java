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
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.utils.CommonsUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 常用查询封装
 *
 * @author devezhao
 * @since 2021/8/25
 */
public class QueryHelper {

    /**
     * @param recordId
     * @param advFilter
     * @return
     * @see #isMatchAdvFilter(ID, JSONObject, boolean)
     */
    public static boolean isMatchAdvFilter(ID recordId, JSONObject advFilter) {
        return isMatchAdvFilter(recordId, advFilter, Boolean.FALSE);
    }

    /**
     * @param recordId
     * @param advFilter
     * @param useVarRecord
     * @return
     * @see #isMatchFilter(ID, String)
     */
    public static boolean isMatchAdvFilter(ID recordId, JSONObject advFilter, boolean useVarRecord) {
        if (!ParseHelper.validAdvFilter(advFilter)) return true;

        String filterSql = useVarRecord ? new AdvFilterParser(advFilter, recordId).toSqlWhere()
                : new AdvFilterParser(advFilter).toSqlWhere();

        return isMatchFilter(recordId, filterSql);
    }

    /**
     * 指定记录是否符合过滤条件
     *
     * @param recordId
     * @param filterSql
     * @return
     */
    public static boolean isMatchFilter(ID recordId, String filterSql) {
        if (StringUtils.isBlank(filterSql)) return true;

        Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
        String sql = MessageFormat.format(
                "select {0} from {1} where {0} = ? and {2}",
                entity.getPrimaryField().getName(), entity.getName(), filterSql);

        Object[] m = Application.createQueryNoFilter(sql).setParameter(1, recordId).unique();
        return m != null;
    }

    /**
     * @param sql
     * @param useEntity 决定是否使用权限查询
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
     * @throws NoRecordFoundException
     * @see QueryFactory#recordNoFilter(ID, String...)
     */
    public static Record recordNoFilter(ID recordId) throws NoRecordFoundException {
        Record o = Application.getQueryFactory().recordNoFilter(recordId);

        if (o == null) throw new NoRecordFoundException(recordId);
        return o;
    }

    /**
     * 获取明细列表记录
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

    /**
     * 获取（默认明细）明细列表 ID
     *
     * @param mainId
     * @return
     */
    public static List<ID> detailIdsNoFilter(ID mainId) {
        Entity detailEntity = MetadataHelper.getEntity(mainId.getEntityCode()).getDetailEntity();
        return detailIdsNoFilter(mainId, detailEntity);
    }

    /**
     * 获取（指定）明细列表 ID
     *
     * @param mainId
     * @param detailEntity
     * @return
     */
    public static List<ID> detailIdsNoFilter(ID mainId, Entity detailEntity) {
        String sql = String.format("select %s from %s where %s = ? order by autoId asc",
                detailEntity.getPrimaryField().getName(),
                detailEntity.getName(),
                MetadataHelper.getDetailToMainField(detailEntity).getName());

        Query query = Application.createQueryNoFilter(sql).setParameter(1, mainId);
        Object[][] array = query.array();
        List<ID> ids = new ArrayList<>();

        for (Object[] o : array) ids.add((ID) o[0]);
        return ids;
    }

    /**
     * 根据明细 ID 得到主记录 ID
     *
     * @param detailId
     * @return
     * @throws NoRecordFoundException
     */
    public static ID getMainIdByDetail(ID detailId) throws NoRecordFoundException {
        Field dtmField = MetadataHelper.getDetailToMainField(MetadataHelper.getEntity(detailId.getEntityCode()));
        Object[] o = Application.getQueryFactory().uniqueNoFilter(detailId, dtmField.getName());

        if (o == null) throw new NoRecordFoundException(detailId);
        else return (ID) o[0];
    }

    /**
     * 记录是否存在
     *
     * @param recordId
     * @return
     */
    public static boolean exists(ID recordId) {
        Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
        Object[] o = Application.getQueryFactory().uniqueNoFilter(recordId, entity.getPrimaryField().getName());
        return o != null;
    }

    /**
     * @param queryFields
     * @param queryValue
     * @param forceQueryName
     * @return
     */
    public static ID queryIdValue(Field[] queryFields, String queryValue, boolean forceQueryName) {
        Entity entity = queryFields[0].getOwnEntity();

        StringBuilder sql = new StringBuilder(
                String.format("select %s from %s where ", entity.getPrimaryField().getName(), entity.getName()));
        for (Field qf : queryFields) {
            String qfName = qf.getName();
            if (forceQueryName && qf.getType() == FieldType.REFERENCE) qfName = "&" + qfName;
            sql.append(String.format("%s = '%s' or ", qfName, CommonsUtils.escapeSql(queryValue)));
        }
        sql = new StringBuilder(sql.substring(0, sql.length() - 4));

        Object[] found = Application.createQueryNoFilter(sql.toString()).unique();
        return found == null ? null : (ID) found[0];
    }

    /**
     * @param recordId
     * @param fieldName
     * @return
     */
    public static Object queryFieldValue(ID recordId, String fieldName) {
        Object[] o = Application.getQueryFactory().uniqueNoFilter(recordId, fieldName);
        return o == null || o[0] == null ? null : o[0];
    }

    /**
     * 根据 Record 中的字段获取数据库中的记录
     *
     * @param base
     * @return
     * @throws NoRecordFoundException
     */
    public static Record querySnap(Record base) throws NoRecordFoundException {
        final ID primaryId = base.getPrimary();
        Assert.notNull(primaryId, "Record primary cannot be null");

        Set<String> fields = new HashSet<>(base.getAvailableFields());
        fields.add(base.getEntity().getPrimaryField().getName());

        Record snap = Application.getQueryFactory().recordNoFilter(primaryId, fields.toArray(new String[0]));

        if (snap == null) throw new NoRecordFoundException(primaryId);
        return snap;
    }
}
