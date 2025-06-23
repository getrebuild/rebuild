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
import org.springframework.util.Assert;

/**
 * @author Zixin
 * @since 2025/6/8
 */
public class DeepSeek {

    public static final String MODEL_CHAT = "deepseek-chat";
    public static final String MODEL_REASONER = "deepseek-reasoner";

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
        return ChatCompletionCreateParams.builder()
                .model(model)
                .addSystemMessage(system);
    }

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
}
