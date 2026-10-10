/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 记录操作类工具（分配/共享/取消共享）JSON 定义契约测试，不依赖数据库
 *
 * @author RB
 * @since 2026/10/10
 */
class RecordOpToolsJsonTest {

    @Test
    void validateToolDefinitions() throws Exception {
        assertToolJson("AssignRecord", Arrays.asList("recordIds", "toUser"));
        assertToolJson("ShareRecord", Arrays.asList("recordIds", "toUsers"));
        assertToolJson("UnshareRecord", Arrays.asList("recordIds"));
    }

    /**
     * 工具定义须与 Java 类同名，且必填参数、附加属性等契约保持一致
     *
     * @param name 工具（类）名称
     * @param required 期望的必填参数
     */
    private void assertToolJson(String name, List<String> required) throws Exception {
        JSONObject json = readToolJson(name);

        Assertions.assertEquals("function", json.getString("type"), name + " type 必须为 function");

        JSONObject func = json.getJSONObject("function");
        Assertions.assertNotNull(func, name + " 缺少 function 定义");
        Assertions.assertEquals(name, func.getString("name"), name + " function.name 必须与类名一致");
        Assertions.assertTrue(StringUtils.isNotBlank(json.getString("userDescription")),
                name + " userDescription 不能为空");
        Assertions.assertTrue(StringUtils.isNotBlank(func.getString("description")),
                name + " function.description 不能为空");

        JSONObject params = func.getJSONObject("parameters");
        Assertions.assertNotNull(params, name + " 缺少 parameters 定义");
        Assertions.assertEquals("object", params.getString("type"), name + " parameters.type 必须为 object");
        Assertions.assertEquals(Boolean.FALSE, params.getBoolean("additionalProperties"),
                name + " 必须禁止附加属性");

        JSONArray requiredJson = params.getJSONArray("required");
        Assertions.assertNotNull(requiredJson, name + " 缺少 required 定义");
        List<String> actual = new ArrayList<>(requiredJson.toJavaList(String.class));
        Assertions.assertEquals(required.size(), actual.size(), name + " 必填参数数量不一致 : " + actual);
        Assertions.assertTrue(actual.containsAll(required), name + " 必填参数不一致 : " + actual);
    }

    private JSONObject readToolJson(String name) throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/aibot2/tool/" + name + ".json")) {
            Assertions.assertNotNull(is, "工具定义文件不存在 : " + name + ".json");
            return JSON.parseObject(IOUtils.toString(is, StandardCharsets.UTF_8));
        }
    }
}
