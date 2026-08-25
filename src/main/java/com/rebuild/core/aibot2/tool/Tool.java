/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

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
 * @author Zixin
 * @since 2025/4/19
 */
@Lab
public interface Tool {

    boolean HIDDEN_SYSTEM = true;

    /**
     * 定义
     *
     * @return
     */
    default ChatCompletionTool def() {
        String toolName = getClass().getSimpleName();
        String d = CommonsUtils.getStringOfRes("aibot2/tool/" + toolName + ".json");
        Assert.notNull(d, "Tool definition cannot be null");

        JSONObject json = JSONObject.parseObject(d);
        JSONObject funcJson = json.getJSONObject("function");
        JSONObject paramsJson = funcJson.getJSONObject("parameters");

        FunctionParameters.Builder paramsBuilder = FunctionParameters.builder();
        for (String key : paramsJson.keySet()) {
            paramsBuilder.putAdditionalProperty(key, JsonValue.from(paramsJson.get(key)));
        }

        FunctionDefinition fnDef = FunctionDefinition.builder()
                .name(funcJson.getString("name"))
                .description(funcJson.getString("description"))
                .parameters(paramsBuilder.build())
                .build();

        return ChatCompletionTool.ofFunction(
                ChatCompletionFunctionTool.builder()
                        .function(fnDef)
                        .build());
    }

    /**
     * 是否为系统工具（仅供 AI 使用，不对用户展示）
     *
     * @return
     */
    default boolean isSystem() {
        return false;
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
