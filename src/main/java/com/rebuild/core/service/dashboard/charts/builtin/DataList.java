/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts.builtin;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.DefinedException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.dashboard.charts.ChartData;
import com.rebuild.core.support.general.DataListBuilder;
import com.rebuild.core.support.general.DataListBuilderImpl;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通用数据列表
 *
 * @author devezhao
 * @since 2023/1/6
 */
@Deprecated
public class DataList extends ChartData implements BuiltinChart {

    // 虚拟ID
    public static final ID MYID = ID.valueOf("017-9000000000000004");
    // 名称
    public static final String MYNAME = "DataList";

    public DataList() {
        super(null);
    }

    @Override
    public ID getChartId() {
        return MYID;
    }

    @Override
    public String getChartTitle() {
        return Language.L("数据列表");
    }

    @Override
    public JSON build() {
        Map<String, Object> params = getExtraParams();
        final JSONObject extConfig = (JSONObject) params.get("extconfig");
        if (extConfig == null) return JSONUtils.toJSONObject("error", "UNSET");

        final Entity entity = MetadataHelper.getEntity(extConfig.getString("entity"));
        final JSONArray fields = extConfig.getJSONArray("fields");
        if (fields == null || fields.isEmpty()) return JSONUtils.toJSONObject("error", "UNSET");

        List<Object> fieldsRich = new ArrayList<>();
        for (Object o : fields) {
            String fieldName = (String) o;
            Field lastField = MetadataHelper.getLastJoinField(entity, fieldName);
            if (lastField == null) {
                throw new DefinedException(Language.L("字段 [%s] 已不存在，请调整图表配置", fieldName.toUpperCase()));
            }

            EasyField lastFieldEasy = EasyMetaFactory.valueOf(lastField);
            JSONObject rich = JSONUtils.toJSONObject(
                    new String[]{"field", "type", "label"},
                    new Object[]{fieldName, lastFieldEasy.getDisplayType(), EasyMetaFactory.getLabel(entity, fieldName)});
            fieldsRich.add(rich);
        }

        int pageSize = extConfig.getIntValue("pageSize");
        if (pageSize <= 0) pageSize = 40;
        if (pageSize >= 2000) pageSize = 2000;

        extConfig.put("pageNo", 1);
        extConfig.put("pageSize", pageSize);
        extConfig.put("reload", false);
        extConfig.put("statsField", false);

        DataListBuilder builder = new DataListBuilderImpl(extConfig, getUser());
        JSONObject data = (JSONObject) builder.getJSONResult();
        data.put("fields", fieldsRich);
        return data;
    }
}
