/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.aibot2.JsonSchemaValidator;
import com.rebuild.core.aibot2.ReferenceDocs;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author devezhao
 * @since 2026/8/16
 */
public class GetConfigSchema implements Tool {

    private static final ConcurrentMap<String, String> CACHE = new ConcurrentHashMap<>();

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String schema = args.getString("schema");
        if (StringUtils.isBlank(schema)) {
            throw new KnownToolException("schema 不能为空");
        }

        String content = Application.devMode()
                ? getContent(schema) : CACHE.computeIfAbsent(schema, GetConfigSchema::getContent);
        if (StringUtils.isBlank(content)) {
            throw new KnownToolException("未知的 Schema : " + schema + "，可用值见本工具 schema 参数说明");
        }

        return content;
    }

    /**
     * 先取校验 Schema（json），无则取参考文档（md）
     *
     * @param schema
     * @return
     */
    private static String getContent(String schema) {
        String content = JsonSchemaValidator.getSchemaContent(schema);
        if (StringUtils.isBlank(content)) content = ReferenceDocs.getContent(schema);
        return content;
    }

    @Override
    public boolean isSystem() {
        return HIDDEN_SYSTEM;
    }
}
