/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.CommonsUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.springframework.util.Assert;

/**
 * @author Zixin
 * @since 2025/6/8
 */
public class Config {

    private static OpenAIClient CLIENT;

    private static volatile String SYSTEM_PROMPT_CACHE;

    /**
     * 共享的 Tika 实例（文件内容解析与 MIME 检测）
     */
    public static final Tika TIKA = new Tika();
    static {
        TIKA.setMaxStringLength(1024 * 1024 * 50);  // 50M
    }

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
        if (reset && CLIENT != null) {
            CLIENT.close();
            CLIENT = null;
        }

        if (CLIENT != null) return CLIENT;

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

    // --

    /**
     * 是否可用（即配置了AI参数）
     *
     * @return
     */
    public static boolean availableAiBot() {
        return RebuildConfiguration.get(ConfigurationItem.AibotDSSecret) != null;
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
    public static String getBasePrompt() {
        return RebuildConfiguration.get(ConfigurationItem.AibotBasePrompt);
    }

    /**
     * 系统级提示词（前端能力说明等，来自资源文件）
     *
     * @return
     */
    public static String getSystemCapabilityPrompt() {
        if (SYSTEM_PROMPT_CACHE == null) {
            String res = CommonsUtils.getStringOfRes("aibot2/system-prompt.md");
            SYSTEM_PROMPT_CACHE = res == null ? null : StringUtils.trimToEmpty(res);
        }
        return SYSTEM_PROMPT_CACHE == null ? "" : SYSTEM_PROMPT_CACHE;
    }

    /**
     * @return
     */
    public static String getDefModel() {
        return RebuildConfiguration.get(ConfigurationItem.AibotBaseDefModel);
    }
}
