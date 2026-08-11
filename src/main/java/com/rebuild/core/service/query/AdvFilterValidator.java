/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Set;

/**
 * 过滤条件数据体（AdvFilter）校验器。基于标准 JSON Schema（draft 2020-12）校验
 *
 * @see AdvFilterParser
 * @see com/rebuild/core/service/query/adv-filter-schema.json
 */
@Slf4j
public class AdvFilterValidator {

    // Schema 资源文件（与 AdvFilterParser 同类路径）
    private static final String SCHEMA_RES = "com/rebuild/core/service/query/adv-filter-schema.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static volatile JsonSchema SCHEMA = null;

    /**
     * 校验过滤条件数据体是否符合 Schema，不符合时输出日志
     *
     * @param filterExpr 过滤表达式
     * @return 是否符合
     */
    public static boolean validate(JSONObject filterExpr) {
        if (filterExpr == null || filterExpr.isEmpty()) {
            log.warn("AdvFilter invalid : filterExpr is empty");
            return false;
        }

        JsonSchema schema = getSchema();
        if (schema == null) return true;  // Schema 加载失败不校验

        try {
            JsonNode node = MAPPER.readTree(filterExpr.toJSONString());
            Set<ValidationMessage> errors = schema.validate(node);
            if (errors.isEmpty()) return true;

            for (ValidationMessage error : errors) {
                log.warn("AdvFilter invalid : {} : {}", error.getMessage(), filterExpr);
            }
            return false;
        } catch (Exception ex) {
            log.warn("AdvFilter validate error : {}", filterExpr, ex);
            return true;
        }
    }

    /**
     * 加载 Schema（惰性、仅一次）。双重检查锁定避免高频查询路径上的锁竞争
     */
    private static JsonSchema getSchema() {
        if (SCHEMA != null) return SCHEMA;

        String schemaStr = CommonsUtils.getStringOfRes(SCHEMA_RES);
        if (StringUtils.isBlank(schemaStr)) {
            log.error("Cannot load adv-filter schema of res : {}", SCHEMA_RES);
            return null;
        }

        synchronized (AdvFilterValidator.class) {
            if (SCHEMA != null) return SCHEMA;

            try {
                JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
                SCHEMA = factory.getSchema(schemaStr);
            } catch (Exception ex) {
                log.error("Cannot parse adv-filter schema : {}", SCHEMA_RES, ex);
                return null;
            }
            return SCHEMA;
        }
    }
}
