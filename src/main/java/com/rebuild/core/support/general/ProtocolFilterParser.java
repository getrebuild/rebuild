/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.AdvFilterManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.service.dashboard.ChartManager;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 解析已知的个性化过滤条件
 *
 * @author devezhao
 * @since 2020/6/13
 */
@Slf4j
public class ProtocolFilterParser {

    final private String protocolExpr;

    /**
     * @param protocolExpr via:xxx:[field] ref:xxx:[id]
     */
    public ProtocolFilterParser(String protocolExpr) {
        this.protocolExpr = protocolExpr;
    }

    /**
     * @return
     */
    public String toSqlWhere() {
        String[] ps = protocolExpr.split(":");
        switch (ps[0]) {
            case "via": {
                return parseVia(ps[1], ps.length > 2 ? ps[2] : null);
            }
            case "ref": {
                return parseRef(ps[1], ps.length > 2 ? ps[2] : null);
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
     */
    public String parseVia(String viaId, String refField) {
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
            String[] entityAndField = refField.split("\\.");
            Assert.isTrue(entityAndField.length == 2, "Bad `via` filter defined");

            JSONObject item = JSONUtils.toJSONObject(
                    new String[] { "field", "op", "value" },
                    new Object[] { entityAndField[1], ParseHelper.EQ, anyId });

            filterExp = JSONUtils.toJSONObject("entity", entityAndField[0]);
            filterExp.put("items", Collections.singletonList(item));
        }

        return filterExp == null ? null : new AdvFilterParser(filterExp).toSqlWhere();
    }

    /**
     * @param content
     * @param cascadingValue
     * @return
     */
    public String parseRef(String content, String cascadingValue) {
        String[] fieldAndEntity = content.split("\\.");
        if (fieldAndEntity.length != 2 || !MetadataHelper.checkAndWarnField(fieldAndEntity[1], fieldAndEntity[0])) {
            return null;
        }

        final Entity entity = MetadataHelper.getEntity(fieldAndEntity[1]);
        final Field field = entity.getField(fieldAndEntity[0]);

        List<String> sqls = new ArrayList<>();

        JSONObject advFilter = getFieldDataFilter(field);
        if (advFilter != null) sqls.add(new AdvFilterParser(advFilter).toSqlWhere());

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

                String sql = String.format("exists (select %s from %s where ^%s = %s and %s = '%s')",
                        fs[1], refEntity.getName(),
                        field.getReferenceEntity().getPrimaryField().getName(), fs[1],
                        refEntity.getPrimaryField().getName(), cascadingValue);
                sqls.add(sql);
            }
        }

        return sqls.isEmpty() ? null
                : "( " + StringUtils.join(sqls, " and ") + " )";
    }

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
