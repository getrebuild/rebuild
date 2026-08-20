/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.service;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao
 * @since 2026/8/12
 */
@Slf4j
public class AibotConfigManager implements ConfigManager {

    public static final String TYPE_KNOWLEDGE = "KNOWLEDGE";
    public static final String TYPE_SKILL = "SKILL";
    public static final String TYPE_AIBOT_SCHEDULE = "AIBOT_SCHEDULE";
    public static final String TYPE_AIBOT_USERMEMORY = "AIBOT_USERMEMORY";

    public static final AibotConfigManager instance = new AibotConfigManager();

    private AibotConfigManager() {
    }

    /**
     * 获取知识库配置列表
     *
     * @return
     */
    public ConfigBean[] getKnowledgeConfigs() {
        return getConfig(TYPE_KNOWLEDGE);
    }

    /**
     * 获取技能配置列表
     *
     * @return
     */
    public ConfigBean[] getSkillConfigs() {
        return getConfig(TYPE_SKILL);
    }

    /**
     * 获取指定用户的记忆列表
     *
     * @param user
     * @return
     */
    public ConfigBean[] getUserMemoryConfigs(ID user) {
        String cKey = "AibotConfigManager-" + TYPE_AIBOT_USERMEMORY + "-" + user;
        ConfigBean[] cache = (ConfigBean[]) Application.getCommonsCache().getx(cKey);
        if (cache != null) return cache;

        Object[][] array = Application.createQueryNoFilter(
                "select configId,config,createdOn from AibotConfig where type = ? and createdBy = ? order by createdOn desc")
                .setParameter(1, TYPE_AIBOT_USERMEMORY)
                .setParameter(2, user)
                .array();

        List<ConfigBean> list = new ArrayList<>();
        for (Object[] o : array) {
            JSONObject config = JSONUtils.parseObjectSafe((String) o[1]);
            if (config == null) continue;

            ConfigBean cb = new ConfigBean()
                    .set("id", o[0])
                    .set("content", config.getString("content"))
                    .set("level", config.getIntValue("level"))
                    .set("createdOn", o[2]);
            list.add(cb);
        }

        cache = list.toArray(new ConfigBean[0]);
        Application.getCommonsCache().putx(cKey, cache, 300);
        return cache;
    }

    /**
     * @param type
     * @return
     */
    protected ConfigBean[] getConfig(String type) {
        String cKey = "AibotConfigManager-" + type;
        ConfigBean[] cache = (ConfigBean[]) Application.getCommonsCache().getx(cKey);
        if (cache != null) return cache;

        Object[][] array = Application.createQueryNoFilter(
                "select configId,config,isDisabled,name from AibotConfig where type = ?")
                .setParameter(1, type)
                .array();

        List<ConfigBean> list = new ArrayList<>();
        for (Object[] o : array) {
            ConfigBean cb = new ConfigBean()
                    .set("id", o[0])
                    .set("config", o[1] != null ? JSONUtils.parseObjectSafe((String) o[1]) : new JSONObject())
                    .set("name", o[3])
                    .set("isDisabled", o[2] != null && (Boolean) o[2]);
            list.add(cb);
        }

        cache = list.toArray(new ConfigBean[0]);
        Application.getCommonsCache().putx(cKey, cache);
        return cache;
    }

    @Override
    public void clean(Object cfgid) {
        Object type = QueryHelper.queryFieldValue((ID) cfgid, "type");
        if (type == null) return;

        String cKey = "AibotConfigManager-" + type;
        Application.getCommonsCache().evict(cKey);

        // 用户记忆按行归属用户清理
        if (TYPE_AIBOT_USERMEMORY.equals(type)) {
            Object user = QueryHelper.queryFieldValue((ID) cfgid, "createdBy");
            if (user != null) Application.getCommonsCache().evict(cKey + "-" + user);
        }
    }

    @Override
    public String getBelongEntity(ID cfgid, boolean throwIfMiss) {
        return "AibotConfig";
    }
}
