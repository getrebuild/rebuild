/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import com.openai.models.chat.completions.ChatCompletionTool;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Zixin
 * @since 2026/6/9
 */
public class ToolDefs {

    /**
     * @return
     */
    public static List<ChatCompletionTool> tools() {
        List<ChatCompletionTool> tools = new ArrayList<>();
        tools.add(new HowtoTool().def());
        return tools;
    }
}
