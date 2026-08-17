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
import com.rebuild.core.support.License;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        List<String> errors = validateErrors(schemaName, data);
        if (errors == null || errors.isEmpty()) return true;

        for (String error : errors) {
            log.warn("JsonSchema invalid : {} : {}", error, data);
        }
        return false;
    }

    /**
     * 校验并返回错误明细（供 AI 工具反馈给模型自修复）
     *
     * @param schemaName
     * @param data
     * @return 错误列表。null 表示 schema 不可用（未挂载或数据为空），空列表表示校验通过
     */
    public static List<String> validateErrors(String schemaName, Object data) {
        if (!License.isRbvAttached()) return null;
        if (data == null) return null;

        String schemaRes = SCHEMA_RES_MAP.get(schemaName);
        if (schemaRes == null) return null;

        JsonSchema schema = getSchema(schemaRes);
        if (schema == null) return null;

        try {
            JsonNode node;
            if (data instanceof CharSequence) {
                String dataStr = data.toString();
                if (!JSONUtils.wellFormat(dataStr)) return Collections.singletonList("数据不是合法的 JSON 格式");
                node = MAPPER.readTree(dataStr);
            } else {
                node = MAPPER.valueToTree(data);
            }

            if (node.isObject() && node.isEmpty()) return Collections.singletonList("数据不能为空对象");

            List<String> errors = new ArrayList<>();
            for (ValidationMessage error : schema.validate(node)) {
                errors.add(error.getMessage());
            }
            return errors;

        } catch (Exception ex) {
            log.warn("JsonSchema validate error : {} [{}]", data, schemaRes, ex);
            return Collections.singletonList("数据解析失败 : " + ex.getLocalizedMessage());
        }
    }

    /**
     * 获取 Schema 原文（供 AI 工具注入给模型作为生成约束）
     *
     * @param schemaName
     * @return null 表示 schema 不存在或不可用
     */
    public static String getSchemaContent(String schemaName) {
        String schemaRes = SCHEMA_RES_MAP.get(schemaName);
        if (schemaRes == null) return null;
        return CommonsUtils.getStringOfRes(schemaRes);
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
