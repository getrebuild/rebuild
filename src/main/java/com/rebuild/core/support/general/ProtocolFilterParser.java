/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.AdvFilterManager;
import com.rebuild.core.configuration.general.DataListCategory38;
import com.rebuild.core.configuration.general.ViewAddonsManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.service.dashboard.ChartManager;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 解析已知的个性化过滤条件
 *
 * @author devezhao
 * @since 2020/6/13
 */
@Slf4j
public class ProtocolFilterParser {

    // 协议

    // via:xxx:[field]
    public static final String P_VIA = "via";
    // ref:xxx:[id]
    public static final String P_REF = "ref";
    // category:entity:value
    public static final String P_CATEGORY = "category";
    // related:field:id
    public static final String P_RELATED = "related";
    // id:x|x|x
    public static final String P_IDS = "ids";

    final private String protocolExpr;

    /**
     */
    public ProtocolFilterParser() {
        this(null);
    }

    /**
     * @param protocolExpr
     */
    public ProtocolFilterParser(String protocolExpr) {
        this.protocolExpr = protocolExpr;
    }

    /**
     * @return
     */
    public String toSqlWhere() {
        Assert.notNull(protocolExpr, "[protocolExpr] cannot be null");
        String[] ps = protocolExpr.split(":");
        Assert.isTrue(ps.length >= 2, "Bad arguments of protocol expr : " + protocolExpr);

        switch (ps[0]) {
            case P_VIA: {
                return parseVia(ps[1], ps.length > 2 ? ps[2] : null);
            }
            case P_REF: {
                return parseRef(ps[1], ps.length > 2 ? ps[2] : null);
            }
            case P_CATEGORY: {
                return parseCategory(ps[1], ps[2]);
            }
            case P_RELATED: {
                return parseRelated(ps[1], ID.valueOf(ps[2]));
            }
            case P_IDS: {
                return parseIds(ps[1]);
            }
            default: {
                log.warn("Unknown protocol expr : {}", protocolExpr);
                return null;
            }
        }
    }

    /**
     * @param viaId
     * @param extParam
     * @return
     * @see #P_VIA
     */
    protected String parseVia(String viaId, String extParam) {
        final ID anyId = ID.isId(viaId) ? ID.valueOf(viaId) : null;
        if (anyId == null) return null;

        JSONObject filterExp = null;

        // via Charts
        if (anyId.getEntityCode() == EntityHelper.ChartConfig) {
            ConfigBean chart = ChartManager.instance.getChart(anyId);
            if (chart != null) {
                JSONObject chartConfig = (JSONObject) chart.getJSON("config");
                filterExp = chartConfig.getJSONObject("filter");

                // be:v4.0 轴-数值条件也生效
                JSONObject axis = chartConfig.getJSONObject("axis");
                JSONArray numsOfAxis = axis == null ? null : axis.getJSONArray("numerical");
                if (!CollectionUtils.isEmpty(numsOfAxis)) {
                    int useAxis = extParam != null && extParam.startsWith("N")
                            ? ObjectUtils.toInt(extParam.substring(1)) : -1;
                    List<String> numsSqls = new ArrayList<>();
                    if (useAxis == -1) {
                        for (Object o : numsOfAxis) {
                            JSONObject axisFilterExp = ((JSONObject) o).getJSONObject("filter");
                            if (ParseHelper.validAdvFilter(axisFilterExp)) {
                                String f = new AdvFilterParser(axisFilterExp).toSqlWhere();
                                if (f != null) numsSqls.add(f);
                                else numsSqls.add("(1=1)");
                            } else {
                                numsSqls.add("(1=1)");
                            }
                        }
                    } else {
                        Object o = numsOfAxis.get(useAxis - 1);
                        JSONObject axisFilterExp = ((JSONObject) o).getJSONObject("filter");
                        if (ParseHelper.validAdvFilter(axisFilterExp)) {
                            String f = new AdvFilterParser(axisFilterExp).toSqlWhere();
                            if (f != null) numsSqls.add(f);
                        }
                    }

                    if (!numsSqls.isEmpty()) {
                        String numsSql = String.format("(%s)", StringUtils.join(numsSqls, " or "));
                        if (ParseHelper.validAdvFilter(filterExp)) {
                            String chartSql = new AdvFilterParser(filterExp).toSqlWhere();
                            return String.format("%s and %s", chartSql, numsSql);
                        }
                        return numsSql;
                    }
                }
            }
        }
        // via AdvFilter
        else if (anyId.getEntityCode() == EntityHelper.FilterConfig) {
            ConfigBean filter = AdvFilterManager.instance.getAdvFilter(anyId);
            if (filter != null) filterExp = (JSONObject) filter.getJSON("filter");
        }
        // via OTHERS
        else if (extParam != null) {
            // format: Entity.Field
            String[] entityAndField = extParam.split("\\.");
            Assert.isTrue(entityAndField.length == 2, "Bad `via` filter defined");

            Field field = MetadataHelper.getField(entityAndField[0], entityAndField[1]);
            String useOp = field.getType() == FieldType.REFERENCE ? ParseHelper.EQ : ParseHelper.IN;

            JSONObject item = JSONUtils.toJSONObject(
                    new String[] { "field", "op", "value" },
                    new Object[] { entityAndField[1], useOp, anyId });

            filterExp = JSONUtils.toJSONObject("entity", entityAndField[0]);
            filterExp.put("items", Collections.singletonList(item));
        }

        return ParseHelper.validAdvFilter(filterExp) ? new AdvFilterParser(filterExp).toSqlWhere() : null;
    }

    /**
     * @param content
     * @param cascadingValue
     * @return
     * @see #P_REF
     */
    public String parseRef(String content, String cascadingValue) {
        String[] fieldAndEntity = content.split("\\.");
        if (fieldAndEntity.length != 2 || !MetadataHelper.checkAndWarnField(fieldAndEntity[1], fieldAndEntity[0])) {
            return null;
        }

        final Entity entity = MetadataHelper.getEntity(fieldAndEntity[1]);
        final Field field = entity.getField(fieldAndEntity[0]);

        List<String> sqls = new ArrayList<>();

        // 字段附加过滤条件
        JSONObject fieldFilter = getFieldDataFilter(field);
        if (ParseHelper.validAdvFilter(fieldFilter)) {
            String s = new AdvFilterParser(fieldFilter).toSqlWhere();
            if (StringUtils.isNotBlank(s)) sqls.add(s);
        }

        // 父级级联字段
        ID[] cascadingValueIds = null;
        if (hasFieldCascadingField(field) && StringUtils.isNotBlank(cascadingValue)) {
            Set<ID> cascadingValueIdList = new HashSet<>();
            for (String s : cascadingValue.split("[,;]")) {
                if (ID.isId(s)) cascadingValueIdList.add(ID.valueOf(s));
            }
            if (!cascadingValueIdList.isEmpty()) cascadingValueIds = cascadingValueIdList.toArray(new ID[0]);
        }

        if (cascadingValueIds != null) {
            // 可能同时存在父子级
            String cascadingFieldParent = field.getExtraAttrs().getString("_cascadingFieldParent");
            String cascadingFieldChild = field.getExtraAttrs().getString("_cascadingFieldChild");
            // v35 多个使用第一个
            if (cascadingFieldChild != null) cascadingFieldChild = cascadingFieldChild.split(";")[0];

            List<String> parentAndChind = new ArrayList<>();

            // 选子级时（只会有一个ID）
            if (StringUtils.isNotBlank(cascadingFieldParent)) {
                String[] fs = cascadingFieldParent.split(MetadataHelper.SPLITER_RE);
                Entity refEntity;
                // 明细使用主实体的
                if (fs[0].contains(".")) {
                    String[] d2m = fs[0].split("\\.");
                    refEntity = MetadataHelper.getField(d2m[0], d2m[1]).getReferenceEntity();
                } else {
                    refEntity = entity.getField(fs[0]).getReferenceEntity();
                }

                if (refEntity.getEntityCode().equals(cascadingValueIds[0].getEntityCode())) {
                    parentAndChind.add(String.format("%s = '%s'", fs[1], cascadingValueIds[0]));
                }
            }

            // 选父级时（会有多个ID）
            if (StringUtils.isNotBlank(cascadingFieldChild)) {
                String[] fs = cascadingFieldChild.split(MetadataHelper.SPLITER_RE);
                Entity refEntity;
                // 明细使用主实体的
                if (fs[0].contains(".")) {
                    String[] d2m = fs[0].split("\\.");
                    refEntity = MetadataHelper.getField(d2m[0], d2m[1]).getReferenceEntity();
                } else {
                    refEntity = entity.getField(fs[0]).getReferenceEntity();
                }

                if (refEntity.getEntityCode().equals(cascadingValueIds[0].getEntityCode())) {
                    String ps = String.format("%s in ('%s')",
                            refEntity.getPrimaryField().getName(), StringUtils.join(cascadingValueIds, "','"));
                    String s = String.format("exists (select %s from %s where ^%s = %s and ( %s ))",
                            fs[1], refEntity.getName(),
                            field.getReferenceEntity().getPrimaryField().getName(), fs[1], ps);
                    parentAndChind.add(s);
                }
            }

            if (!parentAndChind.isEmpty()) sqls.add("( " + StringUtils.join(parentAndChind, " or ") + " )");
        }

        return sqls.isEmpty() ? null
                : "( " + StringUtils.join(sqls, " and ") + " )";
    }

    /**
     * @param entity
     * @param value
     * @return
     * @see #P_CATEGORY
     * @see DataListCategory38#buildParentFilters(Entity, Object[])
     */
    protected String parseCategory(String entity, String value) {
        String[] filterValues = value.split(CommonsUtils.COMM_SPLITER_RE);
        String where = DataListCategory38.instance.buildParentFilters(MetadataHelper.getEntity(entity), filterValues);
        if (Application.devMode()) log.info("[dev] Parse category : {} >> {}", value, where);
        return where;
    }

    /**
     * @param relatedExpr
     * @param mainid
     * @return
     * @see #P_RELATED
     */
    public String parseRelated(String relatedExpr, ID mainid) {
        // format: Entity.Field
        String[] ef = relatedExpr.split("\\.");
        if (ef.length < 2) {
            log.warn("Incompatible config : {}", relatedExpr);
            return "(1=2)";
        }

        String where = String.format("%s = '%s'", ef[1], mainid);

        Field relatedField = MetadataHelper.getField(ef[0], ef[1]);
        if (relatedField.getType() == FieldType.REFERENCE_LIST) {
            where = String.format(
                    "exists (select recordId from NreferenceItem where ^%s = recordId and belongField = '%s' and referenceId = '%s')",
                    relatedField.getOwnEntity().getPrimaryField().getName(), relatedField.getName(), mainid);
        }

        // 附加过滤条件

        Map<String, JSONObject> vtabFilters = ViewAddonsManager.instance.getViewTabFilters(
                MetadataHelper.getEntity(mainid.getEntityCode()).getName());

        JSONObject hasFilter = vtabFilters.get(relatedExpr);
        if (ParseHelper.validAdvFilter(hasFilter)) {
            String filterSql = new AdvFilterParser(hasFilter).toSqlWhere();
            if (filterSql != null) {
                where += " and " + filterSql;
            }
        }

        return where;
    }

    /**
     * 主键 IN
     *
     * @param idsExpr
     * @return
     */
    public String parseIds(String idsExpr) {
        String[] ids = idsExpr.split("[,|]");
        ID id0 = ID.valueOf(ids[0]);
        Entity entity0 = MetadataHelper.getEntity(id0.getEntityCode());
        return String.format("%s in ('%s')", entity0.getPrimaryField().getName(), StringUtils.join(ids, "','"));
    }

    // --

    /**
     * 附加过滤条件
     *
     * @param field
     * @return
     * @see #parseRef(String, String)
     */
    public static JSONObject getFieldDataFilter(Field field) {
        String dataFilter = EasyMetaFactory.valueOf(field).getExtraAttr(EasyFieldConfigProps.REFERENCE_DATAFILTER);
        if (JSONUtils.wellFormat(dataFilter) && dataFilter.length() > 10) {
            JSONObject advFilter = JSON.parseObject(dataFilter);
            if (advFilter.get("items") != null && !advFilter.getJSONArray ("items").isEmpty()) {
                return advFilter;
            }
        }
        return null;
    }

    /**
     * 是否级联字段
     *
     * @param field
     * @return
     * @see #parseRef(String, String)
     */
    public static boolean hasFieldCascadingField(Field field) {
        return field.getExtraAttrs().containsKey("_cascadingFieldParent")
                || field.getExtraAttrs().containsKey("_cascadingFieldChild");
    }
}
