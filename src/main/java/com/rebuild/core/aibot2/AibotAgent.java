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

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * AI Agent 资源封装层
 * <p>不同 Agent 可组合不同资源形成差异化能力
 *
 * @author Zixin
 * @since 4.1
 */
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

    public static AibotAgent defaultAgent() {
        return new AibotAgent().setName("default");
    }

    public static AibotAgent defaultAgent(String model, String prompt) {
        return new AibotAgent().setName("default").setModel(model).setPrompt(prompt);
    }

    public String model() {
        return model != null ? model : Config.getDefModel();
    }

    public List<ChatCompletionTool> tools() {
        return ToolDefs.tools(this);
    }

    public String buildSystemPrompt(String skillName) {
        return SystemPromptBuilder.build(Config.getBasePrompt(), prompt, skillName);
    }
}
