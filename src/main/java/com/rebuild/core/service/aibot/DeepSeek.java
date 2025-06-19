/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import org.springframework.util.Assert;

/**
 * @author devezhao
 * @since 2025/6/8
 */
public class DeepSeek {

    public static final String MODEL_CHAT = "deepseek-chat";
    public static final String MODEL_REASONER = "deepseek-reasoner";

    private static OpenAIClient _CLIENT;
    /**
     * @param reset
     * @return
     */
    public static OpenAIClient getClient(boolean reset) {
        if (reset && _CLIENT != null) {
            _CLIENT.close();
        }

        _CLIENT = OpenAIOkHttpClient.builder()
                .baseUrl(getServerUrl(null))
                .apiKey(getSecret())
                .build();
        return _CLIENT;
    }

    /**
     * @param user
     * @param model
     * @return
     */
    public static ChatCompletion createChatCompletion(String user, String model) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage(user)
                .model(model)
                .build();
        return getClient(false).chat().completions().create(params);

//        chatCompletion.choices().forEach(choice -> {
//            System.out.println(choice.message().content());
//        });
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
