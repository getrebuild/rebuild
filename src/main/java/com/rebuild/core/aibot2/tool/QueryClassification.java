/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 搜索分类字段绑定的分类数据项，获取可填入 CLASSIFICATION 字段的分类项 ID
 *
 * @author RB
 * @since 2026/9/23
 */
@Slf4j
public class QueryClassification implements Tool {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String entityName = args.getString("entity");
        if (StringUtils.isBlank(entityName)) {
            throw new KnownToolException("实体名称不能为空");
        }
        String fieldName = args.getString("field");
        if (StringUtils.isBlank(fieldName)) {
            throw new KnownToolException("分类字段名不能为空");
        }

        Entity entity = ToolHelper.resolveEntity(entityName);
        if (entity == null) {
            throw new KnownToolException("未知实体 : " + entityName + ToolHelper.suggestEntity(entityName));
        }
        if (!entity.isQueryable() || !MetadataHelper.isBusinessEntity(entity)) {
            throw new KnownToolException("实体 [" + EasyMetaFactory.getLabel(entity) + "] 不支持此操作");
        }

        Field field = ToolHelper.resolveField(entity, fieldName);
        if (EasyMetaFactory.getDisplayType(field) != DisplayType.CLASSIFICATION) {
            throw new KnownToolException("字段 [" + EasyMetaFactory.getLabel(field) + "] 不是分类（CLASSIFICATION）字段");
        }

        ID dataId = ClassificationManager.instance.getUseClassification(field, true);
        if (dataId == null) {
            throw new KnownToolException("字段 [" + fieldName + "] 未配置可用的分类数据，请联系管理员");
        }
        int openLevel = ClassificationManager.instance.getOpenLevel(field);

        String keyword = StringUtils.trimToEmpty(args.getString("keyword"));
        int limit = args.getIntValue("limit");
        if (limit < 1) limit = DEFAULT_LIMIT;
        if (limit > MAX_LIMIT) limit = MAX_LIMIT;

        String where = "dataId = ? and level = ? and isHide = 'F'";
        if (StringUtils.isNotBlank(keyword)) {
            where += " and fullName like '%" + CommonsUtils.escapeSql(keyword) + "%'";
        }

        Object[][] array = Application.createQueryNoFilter(
                "select itemId,name,fullName,code from ClassificationData where " + where + " order by code,name")
                .setParameter(1, dataId)
                .setParameter(2, openLevel)
                .setLimit(limit)
                .array();

        Object[] counted = Application.createQueryNoFilter(
                "select count(itemId) from ClassificationData where " + where)
                .setParameter(1, dataId)
                .setParameter(2, openLevel)
                .unique();
        int total = counted != null && counted[0] instanceof Number
                ? ((Number) counted[0]).intValue() : array.length;

        JSONArray items = new JSONArray();
        for (Object[] row : array) {
            JSONObject item = new JSONObject(true);
            item.put("id", row[0].toString());
            item.put("name", row[1]);
            item.put("fullName", row[2]);
            if (row[3] != null) item.put("code", row[3]);
            items.add(item);
        }

        JSONObject ret = new JSONObject(true);
        ret.put("status", "ok");
        ret.put("entity", entity.getName());
        ret.put("field", field.getName());
        ret.put("fieldLabel", EasyMetaFactory.getLabel(field));
        ret.put("classification", getClassificationName(dataId));
        ret.put("keyword", keyword);
        ret.put("total", total);
        ret.put("hasMore", total > array.length);
        ret.put("items", items);
        return ret;
    }

    /**
     * 分类数据名称
     *
     * @param dataId
     * @return
     */
    private String getClassificationName(ID dataId) {
        Object[] row = Application.createQueryNoFilter(
                "select name from Classification where dataId = ?").setParameter(1, dataId).unique();
        return row != null && row[0] != null ? (String) row[0] : null;
    }

    @Override
    public boolean isSystem() {
        return HIDDEN_SYSTEM;
    }
}
