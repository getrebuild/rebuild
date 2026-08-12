/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigManager;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 通用配置管理器。统一管理知识库配置、技能配置等，数据存储于 AibotCommonsConfig 实体，通过 type 字段区分配置类型。
 *
 * @author devezhao
 * @since 2026/8/12
 */
@Slf4j
public class AibotCommonsConfigManager implements ConfigManager {

    // 知识库配置
    public static final String TYPE_KNOWLEDGE = "KNOWLEDGE";
    // AI 技能配置
    public static final String TYPE_SKILL = "SKILL";

    public static final AibotCommonsConfigManager instance = new AibotCommonsConfigManager();

    private AibotCommonsConfigManager() {
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
     * 通用配置查询（带缓存，按 type 维度）
     *
     * @param type
     * @return
     */
    protected ConfigBean[] getConfig(String type) {
        String cKey = "AibotCommonsConfigManager-" + type;
        ConfigBean[] cache = (ConfigBean[]) Application.getCommonsCache().getx(cKey);
        if (cache != null) return cache;

        Object[][] array = Application.createQueryNoFilter(
                "select configId, config, isDisabled, name from AibotCommonsConfig "
                        + "where type = ? order by modifiedOn desc")
                .setParameter(1, type)
                .array();

        List<ConfigBean> list = new ArrayList<>();
        for (Object[] o : array) {
            ConfigBean cb = new ConfigBean()
                    .set("id", o[0])
                    .set("config", JSON.parse((String) o[1]))
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
        Object[] o = Application.createQueryNoFilter(
                "select type from AibotCommonsConfig where configId = ?")
                .setParameter(1, cfgid)
                .unique();
        if (o == null) return;

        String cKey = "AibotCommonsConfigManager-" + o[0];
        Application.getCommonsCache().evict(cKey);
    }

    @Override
    public String getBelongEntity(ID cfgid, boolean throwIfMiss) {
        return "AibotCommonsConfig";
    }
}
