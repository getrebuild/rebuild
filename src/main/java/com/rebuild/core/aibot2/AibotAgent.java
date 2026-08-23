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
 * AI Agent 资源封装层。
 * <p>将模型、提示词、知识库、工具等资源聚合到 Agent 中，
 * Chat 通过 Agent 获取资源，Agent 内部委托给现有全局组件。
 * 不同 Agent 可组合不同资源形成差异化能力。</p>
 * <p>当前仅提供唯一默认实例 {@link #defaultAgent()}，行为与全局配置一致。
 * 后续需要差异化时，构造不同 Agent 实例即可。</p>
 *
 * @author Zixin
 * @since 4.1
 */
public class AibotAgent implements Serializable {
    private static final long serialVersionUID = 1L;

    @Getter
    @Setter
    @Accessors(chain = true)
    private String name;

    /** 模型名称，null 表示使用全局默认模型 */
    @Getter
    @Setter
    @Accessors(chain = true)
    private String model;

    /** 基础提示词，null 表示使用全局基础提示词 */
    @Getter
    @Setter
    @Accessors(chain = true)
    private String prompt;

    /** 关联的知识库 ID 集合，null 表示使用全部已启用知识库 */
    @Getter
    @Setter
    @Accessors(chain = true)
    private Set<ID> knowledgeBases;

    /** 可用的工具名称集合，null 表示使用全部可用工具 */
    @Getter
    @Setter
    @Accessors(chain = true)
    private Set<String> tools;

    /**
     * 获取唯一默认 Agent，所有资源为 null 表示运行时回退全局默认。
     *
     * @return 默认 Agent 实例
     */
    public static AibotAgent defaultAgent() {
        return new AibotAgent().setName("default");
    }

    // -- 资源访问方法（内部委托现有组件） --

    /**
     * 获取实际使用的模型（非空时返回自身配置，否则回退全局默认）
     *
     * @return 模型名称
     */
    public String model() {
        return model != null ? model : Config.getDefModel();
    }

    /**
     * 获取实际使用的提示词（非空时返回自身配置，否则回退全局默认）
     *
     * @return 基础提示词
     */
    public String prompt() {
        return prompt != null ? prompt : Config.getBasePrompt();
    }

    /**
     * 获取可用工具列表（按 Agent 配置过滤）
     *
     * @return 工具定义列表
     */
    public List<ChatCompletionTool> tools() {
        return ToolDefs.tools(this);
    }

    /**
     * 构建系统提示词
     *
     * @param skillName 技能名称，可为 null
     * @return 系统提示词
     */
    public String buildSystemPrompt(String skillName) {
        return SystemPromptBuilder.build(prompt(), skillName, this);
    }
}
