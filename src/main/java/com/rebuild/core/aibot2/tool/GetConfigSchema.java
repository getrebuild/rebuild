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
import org.apache.commons.lang3.StringUtils;

/**
 * 获取配置数据体的 JSON Schema。
 * 在调用 BuildTrigger 等配置类工具前，先用本工具获取对应 Schema 作为生成约束
 *
 * @author devezhao
 * @since 2026/8/16
 */
public class GetConfigSchema implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String schema = args.getString("schema");
        if (StringUtils.isBlank(schema)) {
            throw new KnownToolException("schema 不能为空");
        }

        String content = JsonSchemaValidator.getSchemaContent(schema);
        if (StringUtils.isBlank(content)) {
            throw new KnownToolException("未知的 Schema : " + schema + "，可用值见本工具 schema 参数说明");
        }

        return content;
    }

    @Override
    public boolean isSystem() {
        return !Application.devMode();
    }
}
