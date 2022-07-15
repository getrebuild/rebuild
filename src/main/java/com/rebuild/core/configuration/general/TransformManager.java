/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 记录转换
 *
 * @author devezhao
 * @since 2020/10/27
 */
public class TransformManager implements ConfigManager {

    public static final TransformManager instance = new TransformManager();

    private TransformManager() { }

    /**
     * 前端使用
     *
     * @param sourceEntity
     * @return
     */
    public JSONArray getTransforms(String sourceEntity, ID user) {
        JSONArray data = new JSONArray();
        for (ConfigBean c : getRawTransforms(sourceEntity)) {
            JSONObject config = (JSONObject) c.getJSON("config");
            // 过滤尚未配置或禁用的
            if (config == null || c.getBoolean("disabled")) continue;

            // 无字段映射
            JSONObject fieldsMapping = config.getJSONObject("fieldsMapping");
            if (fieldsMapping == null || fieldsMapping.isEmpty()) continue;

            String target = c.getString("target");
            Entity targetEntity = MetadataHelper.getEntity(target);

            if (targetEntity.getMainEntity() == null) {
                if (!Application.getPrivilegesManager().allowCreate(user, targetEntity.getEntityCode())) {
                    continue;
                }
            } else {
                // To 明细
                if (!Application.getPrivilegesManager().allowUpdate(user, targetEntity.getMainEntity().getEntityCode())) {
                    continue;
                }
            }

            JSONObject item = EasyMetaFactory.toJSON(targetEntity);
            item.put("transid", c.getID("id"));
            item.put("previewMode", config.getIntValue("transformMode") == 2);
            data.add(item);
        }
        return data;
    }

    /**
     * @param configId
     * @param sourceEntity
     * @return
     */
    public ConfigBean getTransformConfig(ID configId, String sourceEntity) {
        if (sourceEntity == null) {
            sourceEntity = getBelongEntity(configId);
        }

        for (ConfigBean c : getRawTransforms(sourceEntity)) {
            if (configId.equals(c.getID("id"))) {
                return c.clone();
            }
        }

        throw new ConfigurationException("No `TransformConfig` found : " + configId);
    }

    @SuppressWarnings("unchecked")
    public List<ConfigBean> getRawTransforms(String sourceEntity) {
        final String cKey = "TransformManager2.2-" + sourceEntity;
        Object cached = Application.getCommonsCache().getx(cKey);
        if (cached != null) {
            return (List<ConfigBean>) cached;
        }

        Object[][] array = Application.createQueryNoFilter(
                "select belongEntity,targetEntity,configId,config,isDisabled,name from TransformConfig where belongEntity = ?")
                .setParameter(1, sourceEntity)
                .array();

        ArrayList<ConfigBean> entries = new ArrayList<>();
        for (Object[] o : array) {
            ConfigBean entry = new ConfigBean()
                    .set("source", o[0])
                    .set("target", o[1])
                    .set("id", o[2])
                    .set("disabled", ObjectUtils.toBool(o[4], false))
                    .set("name", o[5]);

            JSON config = JSON.parseObject((String) o[3]);
            entry.set("config", config);

            entries.add(entry);
        }

        Application.getCommonsCache().putx(cKey, entries);
        return entries;
    }

    private String getBelongEntity(ID configId) {
        Object[] o = Application.createQueryNoFilter(
                "select belongEntity from TransformConfig where configId = ?")
                .setParameter(1, configId)
                .unique();

        if (o == null) {
            throw new ConfigurationException("No `TransformConfig` found : " + configId);
        }
        return (String) o[0];
    }

    @Override
    public void clean(Object cfgid) {
        String cKey = "TransformManager2.2-" + getBelongEntity((ID) cfgid);
        Application.getCommonsCache().evict(cKey);
    }
}
