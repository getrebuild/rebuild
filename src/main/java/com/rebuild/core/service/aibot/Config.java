/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import org.springframework.util.Assert;

import java.util.List;

/**
 * @author devezhao
 * @since 2025/4/15
 */
public class Config {

    /**
     * @param path
     * @return
     */
    public static String getServerUrl(String path) {
        String url = RebuildConfiguration.get(ConfigurationItem.AibotDSUrl);
        if (path != null) url += "/" + path;
        return url.replace("//", "/");
    }

    /**
     * @return
     */
    public static String getSecret() {
        String sk = RebuildConfiguration.get(ConfigurationItem.AibotDSSecret);
        Assert.notNull(sk, "[AibotDSSecret] is not set");
        return sk;
    }

    /**
     * @return
     */
    public static String getBasePrompt() {
        return RebuildConfiguration.get(ConfigurationItem.AibotBasePrompt);
    }

    /**
     * @return
     */
    public static JSONObject getDeepSeekParams() {
        return JSON.parseObject(DS_PARAM);
    }

    /**
     * @return
     */
    public static JSONObject getDeepSeekParams(JSON tools) {
        JSONObject c = getDeepSeekParams();
        if (tools instanceof List) c.put("tools", tools);
        else c.getJSONArray("tools").add(tools);
        return c;
    }

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
