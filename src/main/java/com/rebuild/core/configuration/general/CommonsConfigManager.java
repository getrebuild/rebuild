/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ZHAO
 * @since 4/15/2026
 */
public class CommonsConfigManager implements ConfigManager {

    // 记录提醒
    public static final String TYPE_RECORD_ALERTS = "RECORD_ALERTS";

    public static final CommonsConfigManager instance = new CommonsConfigManager();

    private CommonsConfigManager() {
    }

    /**
     * @param entity
     * @return
     */
    public List<JSONObject> getRecordAlerts(String entity) {
        ConfigBean[] cbs = getConfig(TYPE_RECORD_ALERTS, entity);

        List<JSONObject> alerts = new ArrayList<>();
        for (ConfigBean cb : cbs) {
            if (cb.getJSON("config") != null) {
                alerts.add((JSONObject) cb.getJSON("config"));
            }
        }
        return alerts;
    }

    /**
     * 获取配置
     *
     * @param type
     * @param entity
     * @return
     */
    public ConfigBean[] getConfig(String type, String entity) {
        if (entity == null) entity = "N";
        String cKey = "CommonsConfigManager-" + type + "#" + entity;
        ConfigBean[] cache = (ConfigBean[]) Application.getCommonsCache().getx(cKey);
        if (cache != null) return cache;

        Object[][] array = Application.createQueryNoFilter(
                "select configId,config from CommonsConfig where belongEntity = ? and type = ?")
                .setParameter(1, entity)
                .setParameter(2, type)
                .array();

        List<ConfigBean> list = new ArrayList<>();
        for (Object[] o : array) {
            ConfigBean cb = new ConfigBean()
                    .set("id", o[0])
                    .set("config", JSON.parse((String) o[1]));
            list.add(cb);
        }

        cache = list.toArray(new ConfigBean[0]);
        Application.getCommonsCache().putx(cKey, cache);
        return cache;
    }

    @Override
    public void clean(Object cfgid) {
        Object[] o = Application.createQueryNoFilter(
                "select type,belongEntity from CommonsConfig where configId = ?")
                .setParameter(1, cfgid)
                .unique();
        if (o == null) return;

        String cKey = "CommonsConfigManager-" + o[0] + "#" + o[1];
        Application.getCommonsCache().evict(cKey);
    }

    @Override
    public String getBelongEntity(ID cfgid, boolean throwIfMiss) {
        return "CommonsConfig";
    }
}
