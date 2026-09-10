/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.engine.ID;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.rebuild.core.aibot2.tool.ToolDefs;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import static com.rebuild.core.support.ConfigurationItem.AibotContextCompressThreshold;
import static com.rebuild.core.support.ConfigurationItem.AibotMaxTokens;
import static com.rebuild.core.support.ConfigurationItem.AibotSeed;
import static com.rebuild.core.support.ConfigurationItem.AibotTemperature;
import static com.rebuild.core.support.ConfigurationItem.AibotTopP;
import static com.rebuild.core.support.RebuildConfiguration.get;

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
    private String name;

    @Getter
    @Setter
    @Accessors(chain = true)
    private String model;

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

    // 模型参数（null 表示未覆盖，回退到系统配置；有效性校验统一在下方 getter 中完成）
    @Getter
    @Setter
    @Accessors(chain = true)
    private Double temperature;

    @Getter
    @Setter
    @Accessors(chain = true)
    private Long maxTokens;

    @Getter
    @Setter
    @Accessors(chain = true)
    private Double topP;

    @Getter
    @Setter
    @Accessors(chain = true)
    private Long seed;

    @Getter
    @Setter
    @Accessors(chain = true)
    private Long contextCompressThreshold;

    public static AibotAgent defaultAgent() {
        return new AibotAgent().setName("default");
    }

    public static AibotAgent defaultAgent(String model, String prompt) {
        return new AibotAgent().setName("default").setModel(model).setPrompt(prompt);
    }

    public String model() {
        return model != null ? model : Config.getDefModel();
    }

    public Double temperature() {
        Double v = (temperature != null) ? temperature : parseDouble(get(AibotTemperature), "temperature");
        return (v != null && v >= 0 && v <= 2) ? v : null;
    }

    public Long maxTokens() {
        Long v = maxTokens != null ? maxTokens : parseLong(get(AibotMaxTokens), "maxTokens");
        return (v != null && v > 0) ? v : null;
    }

    public Double topP() {
        Double v = topP != null ? topP : parseDouble(get(AibotTopP), "topP");
        return (v != null && v >= 0 && v <= 1) ? v : null;
    }

    public Long seed() {
        Long v = seed != null ? seed : parseLong(get(AibotSeed), "seed");
        return (v != null && v >= 0) ? v : null;
    }

    public Long contextCompressThreshold() {
        Long v = contextCompressThreshold != null ? contextCompressThreshold
                : parseLong(get(AibotContextCompressThreshold), "contextCompressThreshold");
        return (v != null && v > 0) ? v : null;
    }

    public List<ChatCompletionTool> tools() {
        return ToolDefs.tools(this);
    }

    public String buildSystemPrompt(String skillName) {
        return SystemPromptBuilder.build(Config.getBasePrompt(), prompt, skillName);
    }

    // 解析系统配置的字符串参数，解析失败时告警并返回 null
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
