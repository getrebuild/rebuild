/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import com.alibaba.fastjson.JSON;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Zixin
 * @since 2026/6/9
 */
@Slf4j
public class HowtoTool implements Tool {

    @Override
    public ChatCompletionTool def() {
        String d = CommonsUtils.getStringOfRes("tool/HowtoTool.json");
        return JSON.parseObject(d, ChatCompletionTool.class);
    }

    @Override
    public Object execute(String arguments) {
        log.info("exec : {}", arguments);
        return null;
    }
}
