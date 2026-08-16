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
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 获取单条记录的完整字段详情
 *
 * @author devezhao
 * @since 2026/8/9
 */
@Slf4j
public class GetRecord implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String entityName = args.getString("entity");
        if (StringUtils.isBlank(entityName)) {
            throw new KnownToolException("实体名称不能为空");
        }

        Entity entity = ToolHelper.resolveEntity(entityName);
        if (entity == null) {
            throw new KnownToolException("未知实体 : " + entityName + ToolHelper.suggestEntity(entityName));
        }

        ID recordId = ToolHelper.resolveId(args.getString("recordId"), "recordId");
        // 校验实体匹配
        if (recordId.getEntityCode() != entity.getEntityCode()) {
            throw new KnownToolException("记录ID与实体不匹配，记录ID对应的实体为 : "
                    + EasyMetaFactory.getLabel(MetadataHelper.getEntity(recordId.getEntityCode())));
        }

        String fields = args.getString("fields");
        JSONArray invalidFields = new JSONArray();
        List<String> queryFields = ToolHelper.buildQueryFields(entity, fields, invalidFields);

        Field primaryField = entity.getPrimaryField();
        Field nameField = entity.getNameField();

        String fieldsSql = ToolHelper.buildFieldsSql(primaryField, nameField, queryFields);
        String sql = String.format("select %s from %s where %s = '%s'",
                fieldsSql, entity.getName(), primaryField.getName(), recordId.toLiteral());

        Object[] row = Application.createQuery(sql).unique();
        if (row == null || row.length == 0) {
            throw new KnownToolException("未找到记录或无权限访问 : " + recordId);
        }

        JSONObject recordJson = ToolHelper.buildRecordJson(entity, primaryField, nameField, queryFields, row);

        JSONObject result = new JSONObject(true);
        result.put("status", "ok");
        result.put("entity", entity.getName());
        result.put("entityLabel", EasyMetaFactory.getLabel(entity));
        if (nameField != null && !nameField.getName().equals(primaryField.getName())) {
            result.put("nameField", nameField.getName());
            result.put("nameFieldLabel", EasyMetaFactory.getLabel(nameField));
        }
        result.put("record", recordJson);
        if (!invalidFields.isEmpty()) {
            result.put("invalidFields", invalidFields);
        }
        return result;
    }

}
