/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot2;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

/**
 * @author Zixin
 * @since 2025/6/8
 */
public class DeepSeek {

    public static final String MODEL_DS_CHAT = "deepseek-chat";
    public static final String MODEL_DS_REASONER = "deepseek-reasoner";
    public static final String MODEL_GPT_5 = "gpt-5";
    public static final String MODEL_GPT_5M = "gpt-5-mini";

    private static OpenAIClient CLIENT;

    /**
     * @return
     */
    public static OpenAIClient getClient() {
        return getClient(false);
    }

    /**
     * @param reset
     * @return
     */
    public static OpenAIClient getClient(boolean reset) {
        if (reset && CLIENT != null) CLIENT.close();

        CLIENT = OpenAIOkHttpClient.builder()
                .baseUrl(getServerUrl(null))
                .apiKey(getSecret())
                .build();
        return CLIENT;
    }

    /**
     * @param system
     * @param model
     * @return
     */
    public static ChatCompletionCreateParams.Builder createBuilder(String system, String model) {
        if (StringUtils.isBlank(model)) model = getDefModel();
        ChatCompletionCreateParams.Builder b = ChatCompletionCreateParams.builder()
                .model(model);
        if (StringUtils.isNotBlank(system)) b.addSystemMessage(system);
        return b;
    }

    /**
     * @param path
     * @return
     */
    public static String getServerUrl(String path) {
        String url = RebuildConfiguration.get(ConfigurationItem.AibotDSUrl);
        if (!url.endsWith("/")) url += "/";
        if (path == null) return url;

        if (path.startsWith("/")) path = path.substring(1);
        return url + path;
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
    public static String getDefModel() {
        String model = RebuildConfiguration.get(ConfigurationItem.AibotBaseDefModel);
        return StringUtils.defaultIfBlank(model, MODEL_DS_CHAT);
    }
}
