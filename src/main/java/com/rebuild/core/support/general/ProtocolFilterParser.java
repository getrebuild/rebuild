/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

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
import org.springframework.util.Assert;

import java.util.Collections;

/**
 * 解析已知的个性化过滤条件
 *
 * @author devezhao
 * @since 2020/6/13
 */
public class ProtocolFilterParser {

    final private String protocolExpr;

    /**
     * @param protocolExpr via:xxx ref:xxx
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
                return parseRef(ps[1]);
            }
            default:
                return null;
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
        // via others
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
     * @return
     */
    public String parseRef(String content) {
        String[] fieldAndEntity = content.split("\\.");
        if (fieldAndEntity.length != 2 || !MetadataHelper.checkAndWarnField(fieldAndEntity[1], fieldAndEntity[0])) {
            return null;
        }

        Field field = MetadataHelper.getField(fieldAndEntity[1], fieldAndEntity[0]);
        JSONObject advFilter = getFieldDataFilter(field);
        return advFilter == null ? null :  new AdvFilterParser(advFilter).toSqlWhere();
    }

    /**
     * 是否启用了数据过滤
     *
     * @param field
     * @return
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
}
