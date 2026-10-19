/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.aibot2.service.AibotConfigManager;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigNotFoundException;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

import static com.rebuild.core.support.ConfigurationItem.AibotName;

/**
 * Agent 加载入口（读取走 AibotConfigManager 缓存）
 *
 * @author Zixin
 * @since 4.1
 */
@Slf4j
public class AgentDefs {

    /**
     * @param agentId
     * @return
     */
    public static AibotAgent getAgentQuietly(ID agentId) {
        try {
            return getAgent(agentId);
        } catch (ConfigNotFoundException ignored) {}
        return null;
    }

    /**
     * 获取指定 Agent
     *
     * @param agentId
     * @return
     */
    public static AibotAgent getAgent(ID agentId) {
        ConfigBean cb = AibotConfigManager.instance.getConfigById(agentId, AibotConfigManager.TYPE_AGENT);
        if (cb == null) throw new ConfigNotFoundException(agentId);

        JSONObject conf = (JSONObject) cb.getJSON("config");
        return new AibotAgent()
                .setAgentId(agentId)
                .setName(StringUtils.defaultIfBlank(cb.getString("name"), RebuildConfiguration.get(AibotName)))
                .setIcon(conf.getString("icon"))
                .setModel(conf.getString("AibotBaseDefModel"))
                .setDsUrl(conf.getString("AibotDSUrl"))
                .setDsSecret(conf.getString("AibotDSSecret"))
                .setPrompt(conf.getString("AibotBasePrompt"))
                .setKnowledgeBases(toIds(conf.getJSONArray("knowledgeBases")))
                .setTools(toSet(conf.getJSONArray("tools")))
                .setSkills(toSet(conf.getJSONArray("skills")))
                .setTemperature(conf.getDouble("AibotTemperature"))
                .setTopP(conf.getDouble("AibotTopP"))
                .setContextCompressThreshold(conf.getLong("AibotContextCompressThreshold"));
    }

    private static Set<ID> toIds(JSONArray array) {
        if (array == null) return null;

        Set<ID> ids = new HashSet<>();
        for (Object o : array) {
            String s = o == null ? null : o.toString();
            if (ID.isId(s)) ids.add(ID.valueOf(s));
        }
        return ids;
    }

    private static Set<String> toSet(JSONArray array) {
        if (array == null) return null;

        Set<String> set = new HashSet<>();
        for (Object o : array) {
            if (o != null) set.add(o.toString());
        }
        return set;
    }
}
