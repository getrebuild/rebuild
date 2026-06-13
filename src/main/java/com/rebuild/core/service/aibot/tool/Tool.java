/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import com.alibaba.fastjson.JSON;
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
        return JSON.parseObject(d, ChatCompletionTool.class);
    }

    /**
     * 执行
     *
     * @param arguments
     * @return
     */
    Object tool(String arguments);

}
