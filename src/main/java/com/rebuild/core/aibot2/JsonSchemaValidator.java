/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * JSON Schema 通用校验工具类
 *
 * @author devezhao
 * @since 08/12/2026
 */
@Slf4j
public class JsonSchemaValidator {

    public static final String ADV_FILTER = "adv-filter";
    public static final String TRANSFORM_CONFIG = "transform-config";
    public static final String TRIGGER_CONFIG = "trigger-config";
    public static final String APPROVAL_FLOW = "approval-flow";
    public static final String FORM_LAYOUT = "form-layout";
    public static final String LIST_LAYOUT = "list-layout";
    public static final String NAV_MENU = "nav-menu";
    public static final String CHART_CONFIG = "chart-config";
    public static final String DASHBOARD_CONFIG = "dashboard-config";

    private static final Map<String, String> SCHEMA_RES_MAP = new HashMap<>();
    static {
        SCHEMA_RES_MAP.put(ADV_FILTER, "json-schema/adv-filter-schema.json");
        SCHEMA_RES_MAP.put(TRANSFORM_CONFIG, "json-schema/transform-config-schema.json");
        SCHEMA_RES_MAP.put(TRIGGER_CONFIG, "json-schema/trigger-config-schema.json");
        SCHEMA_RES_MAP.put(APPROVAL_FLOW, "json-schema/approval-flow-schema.json");
        SCHEMA_RES_MAP.put(FORM_LAYOUT, "json-schema/form-layout-schema.json");
        SCHEMA_RES_MAP.put(LIST_LAYOUT, "json-schema/list-layout-schema.json");
        SCHEMA_RES_MAP.put(NAV_MENU, "json-schema/nav-menu-schema.json");
        SCHEMA_RES_MAP.put(CHART_CONFIG, "json-schema/chart-config-schema.json");
        SCHEMA_RES_MAP.put(DASHBOARD_CONFIG, "json-schema/dashboard-config-schema.json");
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ConcurrentMap<String, JsonSchema> SCHEMAS = new ConcurrentHashMap<>();

    /**
     * 校验 JSON 数据体是否符合指定 Schema。未找到对应 schema 则忽略
     * data 支持 JSON 对象/JSON 格式字符串，或任何可序列化的对象（Map、Bean 等）
     *
     * @param schemaName
     * @param data
     * @return
     */
    public static boolean validate(String schemaName, Object data) {
        if (data == null) return false;

        String schemaRes = SCHEMA_RES_MAP.get(schemaName);
        if (schemaRes == null) return false;

        JsonSchema schema = getSchema(schemaRes);
        if (schema == null) return true;

        try {
            JsonNode node;
            if (data instanceof CharSequence) {
                String dataStr = data.toString();
                if (!JSONUtils.wellFormat(dataStr)) return false;
                node = MAPPER.readTree(dataStr);
            } else {
                node = MAPPER.valueToTree(data);
            }

            if (node.isObject() && node.isEmpty()) return false;

            Set<ValidationMessage> errors = schema.validate(node);
            if (errors.isEmpty()) return true;

            for (ValidationMessage error : errors) {
                log.warn("JsonSchema invalid : {} : {} [{}]", error.getMessage(), data, schemaRes);
            }
            return false;

        } catch (Exception ex) {
            log.warn("JsonSchema validate error : {} [{}]", data, schemaRes, ex);
            return true;
        }
    }

    private static JsonSchema getSchema(String schemaRes) {
        JsonSchema cached = SCHEMAS.get(schemaRes);
        if (cached != null) return cached;

        JsonSchema loaded = loadSchema(schemaRes);
        if (loaded != null) {
            JsonSchema existing = SCHEMAS.putIfAbsent(schemaRes, loaded);
            return existing != null ? existing : loaded;
        }
        return null;
    }

    private static JsonSchema loadSchema(String schemaRes) {
        String schemaStr = CommonsUtils.getStringOfRes(schemaRes);
        if (StringUtils.isBlank(schemaStr)) {
            log.error("Cannot load schema of res : {}", schemaRes);
            return null;
        }

        try {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            return factory.getSchema(schemaStr);
        } catch (Exception ex) {
            log.error("Cannot parse schema : {}", schemaRes, ex);
            return null;
        }
    }
}
