/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper.datalist;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.portals.ChartManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.query.AdvFilterParser;
import com.rebuild.utils.JSONUtils;

/**
 * 解析已知的个性化过滤条件
 *
 * @author devezhao
 * @since 2020/6/13
 */
public class ProtocolFilter {

    final private String protocol;

    /**
     * @param protocol via:xxx ref:xxx
     */
    public ProtocolFilter(String protocol) {
        this.protocol = protocol;
    }

    /**
     * @return
     */
    public String toSqlWhere() {
        String[] ps = protocol.split(":");
        switch (ps[0]) {
            case "via" : {
                return parseVia(ps[1]);
            }
            case "ref" : {
                return parseRef(ps[1]);
            }
        }
        return null;
    }

    /**
     * @param content
     * @return
     */
    public String parseVia(String content) {
        final ID anyId = ID.isId(content) ? ID.valueOf(content) : null;
        if (anyId == null) return null;

        // via Charts
        if (anyId.getEntityCode() == EntityHelper.ChartConfig) {
            ConfigEntry chart = ChartManager.instance.getChart(anyId);
            JSONObject filterExp = ((JSONObject) chart.getJSON("config")).getJSONObject("filter");
            return new AdvFilterParser(filterExp).toSqlWhere();
        }

        return null;
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
        String referenceDataFilter = EasyMeta.valueOf(field).getExtraAttr("referenceDataFilter");

        if (JSONUtils.wellFormat(referenceDataFilter)) {
            JSONObject advFilter = JSON.parseObject(referenceDataFilter);
            if (advFilter.get("items") != null && !advFilter.getJSONArray("items").isEmpty()) {
                return new AdvFilterParser(advFilter).toSqlWhere();
            }
        }
        return null;
    }
}
