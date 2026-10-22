/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.engine.ID;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.rebuild.core.aibot2.tool.ToolDefs;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.rebuild.core.support.ConfigurationItem.AibotContextCompressThreshold;
import static com.rebuild.core.support.ConfigurationItem.AibotDSSecret;
import static com.rebuild.core.support.ConfigurationItem.AibotDSUrl;
import static com.rebuild.core.support.ConfigurationItem.AibotTemperature;
import static com.rebuild.core.support.ConfigurationItem.AibotTopP;
import static com.rebuild.core.support.RebuildConfiguration.get;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * AI Agent 资源封装层
 * <p>不同 Agent 可组合不同资源形成差异化能力
 *
 * @author Zixin
 * @since 4.1
 */
@Slf4j
public class AibotAgent implements Serializable {
    private static final long serialVersionUID = 8175557470623996356L;

    @Getter
    @Setter
    @Accessors(chain = true)
    private ID agentId;

    @Getter
    @Setter
    @Accessors(chain = true)
    private String name;

    @Getter
    @Setter
    @Accessors(chain = true)
    private String icon;

    @Getter
    @Setter
    @Accessors(chain = true)
    private String model;

    @Getter
    @Setter
    @Accessors(chain = true)
    private String asrModel;

    // Agent 自有 API 连接（地址与秘钥可分别配置，未设置项回退系统配置）
    @Getter
    @Setter
    @Accessors(chain = true)
    private String dsUrl;

    @Getter
    @Setter
    @Accessors(chain = true)
    private String dsSecret;

    @Getter
    @Setter
    @Accessors(chain = true)
    private String prompt;

    @Getter
    @Setter
    @Accessors(chain = true)
    private Set<ID> knowledgeBases;

    @Getter
    @Setter
    @Accessors(chain = true)
    private Set<String> tools;

    @Getter
    @Setter
    @Accessors(chain = true)
    private Set<String> skills;

    @Getter
    @Setter
    @Accessors(chain = true)
    private Double temperature;

    @Getter
    @Setter
    @Accessors(chain = true)
    private Double topP;

    @Getter
    @Setter
    @Accessors(chain = true)
    private Long contextCompressThreshold;

    // 公开访客聊天页（/aibot/pub）能力开关，未配置即为 false（禁用），内部聊天不受影响
    @Getter
    @Setter
    @Accessors(chain = true)
    private boolean allowNewChat;
    @Getter
    @Setter
    @Accessors(chain = true)
    private boolean allowUpload;
    @Getter
    @Setter
    @Accessors(chain = true)
    private boolean allowSkill;

    public OpenAIClient client() {
        if (StringUtils.isBlank(dsUrl) && StringUtils.isBlank(dsSecret)) return Config.getClient();

        return Config.getClient(
                defaultIfBlank(dsUrl, Config.getServerUrl(null)),
                defaultIfBlank(dsSecret, Config.getSecret()));
    }

    public static AibotAgent defaultAgent() {
        return new AibotAgent().setName("default");
    }

    public boolean available() {
        String url = defaultIfBlank(dsUrl, get(AibotDSUrl));
        String secret = defaultIfBlank(dsSecret, get(AibotDSSecret));
        return StringUtils.isNotBlank(url) && StringUtils.isNotBlank(secret);
    }

    public boolean usable() {
        return available() && StringUtils.isNotBlank(model());
    }

    public static AibotAgent defaultAgent(String model, String prompt) {
        return new AibotAgent().setName("default").setModel(model).setPrompt(prompt);
    }

    public String model() {
        return model != null ? model : Config.getDefModel();
    }

    public String asrModel() {
        return asrModel != null ? asrModel : Config.getAsrModel();
    }

    public Double temperature() {
        Double v = (temperature != null) ? temperature : parseDouble(get(AibotTemperature), "temperature");
        return (v != null && v >= 0 && v <= 2) ? v : null;
    }

    public Double topP() {
        Double v = topP != null ? topP : parseDouble(get(AibotTopP), "topP");
        return (v != null && v >= 0 && v <= 1) ? v : null;
    }

    public Long contextCompressThreshold() {
        Long v = contextCompressThreshold != null ? contextCompressThreshold
                : parseLong(get(AibotContextCompressThreshold), "contextCompressThreshold");
        return (v != null && v > 0) ? v : null;
    }

    public List<ChatCompletionTool> tools() {
        return ToolDefs.tools(this);
    }

    public String buildSystemPrompt(String skillName, boolean planMode, boolean planConfirmed) {
        return SystemPromptBuilder.build(
                Config.getBasePrompt(), prompt, allowedSkill(skillName), planMode, planConfirmed, tools);
    }

    public String allowedSkill(String skillName) {
        if (StringUtils.isBlank(skillName) || skills == null) return skillName;

        List<String> allowed = new ArrayList<>();
        for (String n : skillName.split(",")) {
            String name = n.trim();
            for (String s : skills) {
                if (s.equalsIgnoreCase(name)) {
                    allowed.add(s);
                    break;
                }
            }
        }
        return allowed.isEmpty() ? null : String.join(",", allowed);
    }

    private static Double parseDouble(String v, String name) {
        if (StringUtils.isBlank(v)) return null;
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException ex) {
            log.warn("Invalid AI param {} = {}", name, v);
            return null;
        }
    }

    private static Long parseLong(String v, String name) {
        if (StringUtils.isBlank(v)) return null;
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException ex) {
            log.warn("Invalid AI param {} = {}", name, v);
            return null;
        }
    }
}
