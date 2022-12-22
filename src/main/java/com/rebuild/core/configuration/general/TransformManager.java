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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记录转换
 *
 * @author devezhao
 * @since 2020/10/27
 */
public class TransformManager implements ConfigManager {

    public static final TransformManager instance = new TransformManager();

    // 任何修改都会清空
    private static final Map<Object, Object> WEAK_CACHED = new ConcurrentHashMap<>();

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
//            item.put("transName", c.getString("name"));
            item.put("previewMode", config.getIntValue("transformMode") == 2);
            data.add(item);
        }
        return data;
    }

    /**
     * @param configId
     * @param sourceEntity [可选]
     * @return Returns clone
     */
    public ConfigBean getTransformConfig(ID configId, String sourceEntity) {
        if (sourceEntity == null) sourceEntity = getBelongEntity(configId);

        for (ConfigBean c : getRawTransforms(sourceEntity)) {
            if (configId.equals(c.getID("id"))) {
                return c.clone();
            }
        }

        throw new ConfigurationException("No `TransformConfig` found : " + configId);
    }

    public List<ConfigBean> getRawTransforms(String sourceEntity) {
        final String cKey = "TransformManager31-" + sourceEntity;
        Object cached = Application.getCommonsCache().getx(cKey);
        if (cached != null) {
            //noinspection unchecked
            return (List<ConfigBean>) cached;
        }

        Object[][] array = Application.createQueryNoFilter(
                "select belongEntity,targetEntity,configId,config,isDisabled,name from TransformConfig where belongEntity = ?")
                .setParameter(1, sourceEntity)
                .array();

        ArrayList<ConfigBean> entries = new ArrayList<>();
        for (Object[] o : array) {
            String name = (String) o[5];
            if (StringUtils.isBlank(name)) name = EasyMetaFactory.getLabel((String) o[1]);

            ConfigBean entry = new ConfigBean()
                    .set("source", o[0])
                    .set("target", o[1])
                    .set("id", o[2])
                    .set("disabled", ObjectUtils.toBool(o[4], false))
                    .set("name", name);

            JSON config = JSON.parseObject((String) o[3]);
            entry.set("config", config);

            entries.add(entry);
        }

        Application.getCommonsCache().putx(cKey, entries);
        return entries;
    }

    // 获取源实体/所属实体
    private String getBelongEntity(ID configId) {
        if (WEAK_CACHED.containsKey(configId)) {
            return (String) WEAK_CACHED.get(configId);
        }

        Object[] o = Application.createQueryNoFilter(
                "select belongEntity from TransformConfig where configId = ?")
                .setParameter(1, configId)
                .unique();
        if (o == null) throw new ConfigurationException("No `TransformConfig` found : " + configId);

        WEAK_CACHED.put(configId, o[0]);
        return (String) o[0];
    }

    /**
     * @param targetEntity
     * @return
     */
    public List<ConfigBean> getDetailImports(String targetEntity) {
        if (WEAK_CACHED.containsKey(targetEntity)) {
            //noinspection unchecked
            return (List<ConfigBean>) WEAK_CACHED.get(targetEntity);
        }

        Object[][] array = Application.createQueryNoFilter(
                "select belongEntity,configId from TransformConfig where targetEntity = ? and isDisabled = 'F'")
                .setParameter(1, targetEntity)
                .array();
        if (array.length == 0) {
            WEAK_CACHED.put(targetEntity, Collections.emptyList());
            return Collections.emptyList();
        }

        List<ConfigBean> imports = new ArrayList<>();
        for (Object[] o : array) {
            ConfigBean c = getTransformConfig((ID) o[1], (String) o[0]);
            JSONObject config = (JSONObject) c.getJSON("config");

            if (config != null && config.getBooleanValue("importsMode")) {
                // 无字段映射
                JSONObject fieldsMapping = config.getJSONObject("fieldsMapping");
                if (fieldsMapping != null && !fieldsMapping.isEmpty()) {
                    imports.add(c);
                }
            }
        }

        WEAK_CACHED.put(targetEntity, imports);
        return imports;
    }

    @Override
    public void clean(Object cfgid) {
        final String cKey = "TransformManager31-" + getBelongEntity((ID) cfgid);
        Application.getCommonsCache().evict(cKey);
        WEAK_CACHED.clear();
    }
}
