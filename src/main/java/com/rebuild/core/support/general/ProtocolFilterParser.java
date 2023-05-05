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
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.AdvFilterManager;
import com.rebuild.core.configuration.general.DataListCategory;
import com.rebuild.core.configuration.general.ViewAddonsManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.service.dashboard.ChartManager;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    final private String protocolExpr;

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
            default: {
                log.warn("Unknown protocol expr : {}", protocolExpr);
                return null;
            }
        }
    }

    /**
     * @param viaId
     * @param refField
     * @return
     * @see #P_VIA
     */
    protected String parseVia(String viaId, String refField) {
        final ID anyId = ID.isId(viaId) ? ID.valueOf(viaId) : null;
        if (anyId == null) return null;

        JSONObject filterExp = null;

        // via Charts
        if (anyId.getEntityCode() == EntityHelper.ChartConfig) {
            ConfigBean chart = ChartManager.instance.getChart(anyId);
            if (chart != null) filterExp = ((JSONObject) chart.getJSON("config")).getJSONObject("filter");
        }
        // via AdvFilter
        else if (anyId.getEntityCode() == EntityHelper.FilterConfig) {
            ConfigBean filter = AdvFilterManager.instance.getAdvFilter(anyId);
            if (filter != null) filterExp = (JSONObject) filter.getJSON("filter");
        }
        // via OTHERS
        else if (refField != null) {
            // format: Entity.Field
            String[] entityAndField = refField.split("\\.");
            Assert.isTrue(entityAndField.length == 2, "Bad `via` filter defined");

            Field field = MetadataHelper.getField(entityAndField[0], entityAndField[1]);
            String useOp = field.getType() == FieldType.REFERENCE ? ParseHelper.EQ : ParseHelper.IN;

            JSONObject item = JSONUtils.toJSONObject(
                    new String[] { "field", "op", "value" },
                    new Object[] { entityAndField[1], useOp, anyId });

            filterExp = JSONUtils.toJSONObject("entity", entityAndField[0]);
            filterExp.put("items", Collections.singletonList(item));
        }

        return filterExp == null ? null : new AdvFilterParser(filterExp).toSqlWhere();
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
        if (hasFieldCascadingField(field) && ID.isId(cascadingValue)) {
            String cascadingFieldParent = field.getExtraAttrs().getString("_cascadingFieldParent");
            String cascadingFieldChild = field.getExtraAttrs().getString("_cascadingFieldChild");

            if (StringUtils.isNotBlank(cascadingFieldParent)) {
                String[] fs = cascadingFieldParent.split(MetadataHelper.SPLITER_RE);
                sqls.add(String.format("%s = '%s'", fs[1], cascadingValue));
            }

            if (StringUtils.isNotBlank(cascadingFieldChild)) {
                String[] fs = cascadingFieldChild.split(MetadataHelper.SPLITER_RE);
                Entity refEntity = entity.getField(fs[0]).getReferenceEntity();

                String s = String.format("exists (select %s from %s where ^%s = %s and %s = '%s')",
                        fs[1], refEntity.getName(),
                        field.getReferenceEntity().getPrimaryField().getName(), fs[1],
                        refEntity.getPrimaryField().getName(), cascadingValue);
                sqls.add(s);
            }
        }

        return sqls.isEmpty() ? null
                : "( " + StringUtils.join(sqls, " and ") + " )";
    }

    /**
     * @param entity
     * @param value
     * @return
     * @see #P_CATEGORY
     * @see DataListCategory
     */
    protected String parseCategory(String entity, String value) {
        Entity rootEntity = MetadataHelper.getEntity(entity);
        Field categoryField = DataListCategory.instance.getFieldOfCategory(rootEntity);
        if (categoryField == null || StringUtils.isBlank(value)) return "(9=9)";

        DisplayType dt = EasyMetaFactory.getDisplayType(categoryField);
        value = CommonsUtils.escapeSql(value);

        if (dt == DisplayType.MULTISELECT) {
            return String.format("%s && %d", categoryField.getName(), ObjectUtils.toInt(value));
        } else if (dt == DisplayType.N2NREFERENCE) {
            return String.format(
                    "exists (select recordId from NreferenceItem where ^%s = recordId and belongField = '%s' and referenceId = '%s')",
                    rootEntity.getPrimaryField().getName(), categoryField.getName(), value);
        } else if (dt == DisplayType.DATETIME || dt == DisplayType.DATE) {
            String s = value + "0000-01-01 00:00:00".substring(value.length());
            String e = value + "0000-12-31 23:59:59".substring(value.length());
            if (dt == DisplayType.DATE) {
                s = s.split(" ")[0];
                e = e.split(" ")[0];
            }
            return MessageFormat.format("({0} >= ''{1}'' and {0} <= ''{2}'')", categoryField.getName(), s, e);
        }  else {
            return String.format("%s = '%s'", categoryField.getName(), value);
        }
    }
    
    /**
     * @param relatedExpr
     * @param mainid
     * @return
     * @see #P_RELATED
     * @see com.rebuild.web.general.RelatedListController#buildBaseSql(ID, String, String, boolean, ID)
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

        // 附件过滤条件

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
