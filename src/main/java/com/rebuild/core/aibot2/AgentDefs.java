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
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.RebuildConfiguration;
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
public class AgentDefs {

    /**
     * 获取指定 Agent（不存在或已禁用返回 null）
     *
     * @param agentId
     * @return
     */
    public static AibotAgent getAgent(ID agentId) {
        ConfigBean cb = getAgentConfig(agentId);
        if (cb == null) return null;

        JSONObject config = (JSONObject) cb.getJSON("config");

        return new AibotAgent()
                .setAgentId(agentId)
                .setName(StringUtils.defaultIfBlank(cb.getString("name"), RebuildConfiguration.get(AibotName)))
                .setModel(config.getString("AibotBaseDefModel"))
                .setDsUrl(config.getString("AibotDSUrl"))
                .setDsSecret(config.getString("AibotDSSecret"))
                .setPrompt(config.getString("AibotBasePrompt"))
                .setKnowledgeBases(toIds(config.getJSONArray("knowledgeBases")))
                .setTools(toSet(config.getJSONArray("tools")))
                .setSkills(toSet(config.getJSONArray("skills")))
                .setTemperature(config.getDouble("AibotTemperature"))
                .setTopP(config.getDouble("AibotTopP"))
                .setContextCompressThreshold(config.getLong("AibotContextCompressThreshold"));
    }

    /**
     * 获取绑定的代理用户（未绑定或无效返回 null）
     *
     * @param agentId
     * @return
     */
    public static ID getBindUser(ID agentId) {
        ConfigBean cb = getAgentConfig(agentId);
        if (cb == null) return null;

        String bindUser = ((JSONObject) cb.getJSON("config")).getString("bindUser");
        if (StringUtils.isBlank(bindUser) || !ID.isId(bindUser)) return null;

        ID user = ID.valueOf(bindUser);
        return UserHelper.isActive(user) ? user : null;
    }

    /**
     * 获取发布配置（enabled/welcome），未配置返回空对象
     *
     * @param agentId
     * @return
     */
    public static JSONObject getPublishConfig(ID agentId) {
        ConfigBean cb = getAgentConfig(agentId);
        if (cb == null) return new JSONObject();

        JSONObject publish = ((JSONObject) cb.getJSON("config")).getJSONObject("publish");
        return publish != null ? publish : new JSONObject();
    }

    private static ConfigBean getAgentConfig(ID agentId) {
        if (agentId == null) return null;

        for (ConfigBean cb : AibotConfigManager.instance.getAgentConfigs()) {
            if (!agentId.equals(cb.getID("id"))) continue;
            if (Boolean.TRUE.equals(cb.getBoolean("isDisabled"))) continue;
            if (cb.getJSON("config") == null) continue;
            return cb;
        }
        return null;
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
