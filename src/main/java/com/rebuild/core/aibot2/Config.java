/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.engine.ID;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.Timeout;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.ReasoningEffort;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.models.Model;
import com.openai.models.models.ModelListPage;
import com.rebuild.core.Application;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.rebuild.core.privileges.bizz.ZeroEntry.AllowUseAiBot;

/**
 * @author Zixin
 * @since 2025/6/8
 */
@Slf4j
public class Config {

    private static OpenAIClient CLIENT;

    private static final Map<String, OpenAIClient> OWN_CLIENTS = new ConcurrentHashMap<>();

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
        if (reset) resetClients();

        if (CLIENT != null) return CLIENT;

        CLIENT = createClient(getServerUrl(null), getSecret());
        return CLIENT;
    }

    public static void resetClients() {
        if (CLIENT != null) {
            CLIENT.close();
            CLIENT = null;
        }
        for (OpenAIClient c : OWN_CLIENTS.values()) c.close();
        OWN_CLIENTS.clear();
    }

    /**
     * @param baseUrl
     * @param apiKey
     * @return
     */
    public static OpenAIClient getClient(String baseUrl, String apiKey) {
        Assert.notNull(baseUrl, "[baseUrl] is not set");
        Assert.notNull(apiKey, "[apiKey] is not set");

        return OWN_CLIENTS.computeIfAbsent(baseUrl + "|" + apiKey, k -> createClient(baseUrl, apiKey));
    }

    /**
     * 创建客户端（使用指定参数，不从已保存配置读取）
     *
     * @param baseUrl
     * @param apiKey
     * @return
     */
    public static OpenAIClient createClient(String baseUrl, String apiKey) {
        Assert.notNull(baseUrl, "[baseUrl] is not set");
        Assert.notNull(apiKey, "[apiKey] is not set");

        if (!baseUrl.endsWith("/")) baseUrl += "/";

        // 连接超时 30s、读取超时 300s（流式响应分块间隔兜底）、写入超时 60s
        // 注意不可设置 request 总超时，否则会截断长时间流式输出
        Timeout timeout = Timeout.builder()
                .connect(Duration.ofSeconds(30))
                .read(Duration.ofSeconds(300))
                .write(Duration.ofSeconds(60))
                .build();

        return OpenAIOkHttpClient.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .timeout(timeout)
                .maxRetries(3)  // 缓解 TLS 握手被 RST 等偶发网络失败
                .build();
    }

    /**
     * 连通性测试用短超时客户端（不缓存，用完关闭），避免接口假死时保存被长时间阻塞
     *
     * @param baseUrl
     * @param apiKey
     * @return
     */
    private static OpenAIClient createTestClient(String baseUrl, String apiKey) {
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        Timeout timeout = Timeout.builder()
                .connect(Duration.ofSeconds(10))
                .read(Duration.ofSeconds(20))
                .write(Duration.ofSeconds(10))
                .build();

        return OpenAIOkHttpClient.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .timeout(timeout)
                .maxRetries(1)
                .build();
    }

    /**
     * @param system
     * @param agent
     * @return
     */
    public static ChatCompletionCreateParams.Builder createBuilder(String system, AibotAgent agent) {
        if (agent == null) agent = AibotAgent.defaultAgent();
        String model = agent.model();
        if (StringUtils.isBlank(model)) model = getDefModel();

        ChatCompletionCreateParams.Builder b = ChatCompletionCreateParams.builder()
                .model(model);
        if (StringUtils.isNotBlank(system)) b.addSystemMessage(system);

        Double temperature = agent.temperature();
        if (temperature != null) b.temperature(temperature);
        Double topP = agent.topP();
        if (topP != null) b.topP(topP);

        String reasoningEffort = agent.reasoningEffort();
        if (StringUtils.isNotBlank(reasoningEffort)) {
            b.reasoningEffort(ReasoningEffort.of(reasoningEffort));
        }

        return b;
    }

    /**
     * 是否可用（即配置了AI参数）
     *
     * @return
     */
    public static boolean availableAiBot() {
        return RebuildConfiguration.getAibotAccount() != null;
    }

    /**
     * 默认助手是否可用（全局配置完整且未禁用）
     *
     * @return
     */
    public static boolean defaultAgentAvailable() {
        return availableAiBot()
                && !RebuildConfiguration.getBool(ConfigurationItem.AibotDefaultDisabled);
    }

    /**
     * 指定用户可用的 AI 助手名称，不可用返回 null
     *
     * @param user
     * @return
     */
    public static String getAvailableAiBot(ID user) {
        if ((defaultAgentAvailable() || AgentDefs.hasUsableAgent(user))
                && Application.getPrivilegesManager().allow(user, AllowUseAiBot)) {
            return RebuildConfiguration.get(ConfigurationItem.AibotName);
        }
        return null;
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
        return RebuildConfiguration.get(ConfigurationItem.AibotBaseDefModel);
    }

    /**
     * 测试 API 连通性，并校验模型是否在可用列表中（端点未提供模型列表时跳过校验），通过返回 null
     *
     * @param baseUrl 留空使用已保存配置
     * @param apiKey  留空使用已保存配置
     * @param model   留空使用已保存配置
     * @return 失败信息
     */
    public static String testApiConnectivity(String baseUrl, String apiKey, String model) {
        baseUrl = StringUtils.defaultIfBlank(baseUrl, RebuildConfiguration.get(ConfigurationItem.AibotDSUrl));
        apiKey = StringUtils.defaultIfBlank(apiKey, RebuildConfiguration.get(ConfigurationItem.AibotDSSecret));
        model = StringUtils.defaultIfBlank(model, getDefModel());

        // 地址或秘钥未配置时不测试
        if (StringUtils.isBlank(baseUrl) || StringUtils.isBlank(apiKey)) return null;

        OpenAIClient client = createTestClient(baseUrl, apiKey);
        try {
            ModelListPage models;
            try {
                models = client.models().list();
            } catch (OpenAIServiceException ex) {
                // 部分兼容网关未实现 /models，无法校验模型列表，不阻断保存
                if (ex.statusCode() == 404) {
                    log.warn("Models list not supported by endpoint, skip model validation : {}", baseUrl);
                    return null;
                }
                throw ex;
            }

            if (StringUtils.isBlank(model)) return null;

            for (Model m : models.data()) {
                if (model.equals(m.id())) return null;
            }
            return Language.L("模型不可用") + ":" + model;

        } catch (Exception e) {
            log.error("Failed to test aibot api connectivity", e);
            return Language.L("API 连接失败") + ":" + CommonsUtils.getRootMessage(e);
        } finally {
            client.close();
        }
    }

    /**
     * 语音识别模型（未配置即不启用语音识别）
     *
     * @return
     */
    public static String getAsrModel() {
        return RebuildConfiguration.get(ConfigurationItem.AibotAsrModel);
    }

    /**
     * 视觉模型（未配置即不启用图片识别）
     *
     * @return
     */
    public static String getVisionModel() {
        return RebuildConfiguration.get(ConfigurationItem.AibotVisionModel);
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

}
