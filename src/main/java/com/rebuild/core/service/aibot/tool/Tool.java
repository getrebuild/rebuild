/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import com.openai.models.chat.completions.ChatCompletionTool;
import com.rebuild.core.support.Lab;

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
    ChatCompletionTool def();

    /**
     * 执行
     *
     * @param arguments
     * @return
     */
    Object execute(String arguments);

}
