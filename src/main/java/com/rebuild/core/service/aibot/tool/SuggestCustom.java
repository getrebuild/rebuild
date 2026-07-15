/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 当用户需求超出现有工具能力时，提示用户联系定制开发
 *
 * @author devezhao
 * @since 2026/7/10
 */
@Slf4j
public class SuggestCustom implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        JSONObject args = JSON.parseObject(arguments);
        String requirement = args.getString("requirement");

        String message = String.format(
                "您的需求「%s」超出了当前 AI 助手的能力范围，可能需要定制开发。\n\n" +
                "如需进一步了解，请联系我们 https://getrebuild.com/market/go/aitool",
                StringUtils.defaultIfBlank(requirement, "未知需求"));

        return JSONUtils.toJSONObject(
                new String[]{"status", "message"},
                new Object[]{"ok", message});
    }
}
