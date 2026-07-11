/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import com.alibaba.fastjson.JSONObject;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.rebuild.core.support.Lab;
import com.rebuild.utils.CommonsUtils;
import org.springframework.util.Assert;

/**
 * 探索中
 * https://api-docs.deepseek.com/zh-cn/guides/function_calling
 *
 * @author Zixin
 * @since 2025/4/19
 */
@Lab
public interface Tool {

    /**
     * 定义 Tool
     *
     * @return
     */
    default ChatCompletionTool def() {
        String toolName = getClass().getSimpleName();
        String d = CommonsUtils.getStringOfRes(String.format("tool/%s.json", toolName));
        Assert.notNull(d, "Tool definition cannot be null");

        JSONObject json = JSONObject.parseObject(d);
        JSONObject funcJson = json.getJSONObject("function");
        JSONObject paramsJson = funcJson.getJSONObject("parameters");

        // 构建 FunctionParameters
        FunctionParameters.Builder paramsBuilder = FunctionParameters.builder();
        for (String key : paramsJson.keySet()) {
            paramsBuilder.putAdditionalProperty(key, JsonValue.from(paramsJson.get(key)));
        }

        // 构建 FunctionDefinition
        FunctionDefinition fnDef = FunctionDefinition.builder()
                .name(funcJson.getString("name"))
                .description(funcJson.getString("description"))
                .parameters(paramsBuilder.build())
                .build();

        // 构建 ChatCompletionTool
        return ChatCompletionTool.ofFunction(
                ChatCompletionFunctionTool.builder()
                        .function(fnDef)
                        .build());
    }

    /**
     * 执行
     *
     * @param arguments
     * @return
     * @throws Exception
     */
    Object tool(String arguments) throws Exception;

}
