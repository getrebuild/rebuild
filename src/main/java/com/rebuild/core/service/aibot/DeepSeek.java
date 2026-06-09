/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * @author devezhao
 * @since 2025/4/15
 */
public class DeepSeek {

    /**
     * @param fcName
     * @param fcDesc
     * @return
     */
    public static JSONObject getDeepSeekFc(String fcName, String fcDesc) {
        JSONObject c = JSON.parseObject(DS_FC);
        JSONObject func = c.getJSONObject("function");
        func.put("name", fcName);
        func.put("description", fcDesc);
        return c;
    }

    /**
     * DS 模型基础参数
     */
    static final String DS_PARAM = "{" +
            "  'model': 'deepseek-chat'," +
            "  'frequency_penalty': 0," +
            "  'max_tokens': 8192," +
            "  'presence_penalty': 0," +
            "  'response_format': {" +
            "    'type': 'text'" +
            "  }," +
            "  'stream': false," +
            "  'stream_options': null," +
            "  'temperature': 1," +
            "  'top_p': 1," +
            "  'tools': null" +
            "}";

    /**
     * FunctionCalling
     */
    static final String DS_FC = "{" +
            "  'type': 'function'," +
            "  'function': {" +
            "    'name': ''," +
            "    'description': ''," +
            "    'parameters': {" +
            "      'type': 'object'," +
            "      'properties': {}," +
            "      'required': []" +
            "    }" +
            "  }" +
            "}";
}
