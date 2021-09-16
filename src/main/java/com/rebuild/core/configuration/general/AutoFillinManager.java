/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyFile;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.MixValue;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * 表单自动回填
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/17
 */
public class AutoFillinManager implements ConfigManager {

    public static final AutoFillinManager instance = new AutoFillinManager();

    private AutoFillinManager() {
    }

    /**
     * 获取回填值
     *
     * @param field
     * @param source
     * @return
     */
    public JSONArray getFillinValue(Field field, ID source) {
        final EasyField easyField = EasyMetaFactory.valueOf(field);

        // 内置字段无配置 @see field-edit.html
        if (easyField.isBuiltin()) return JSONUtils.EMPTY_ARRAY;

        final List<ConfigBean> config = getConfig(field);

        // 父级级联
        // 利用表单回填做父级级联字段回填
        String cascadingField = easyField.getExtraAttr(EasyFieldConfigProps.REFERENCE_CASCADINGFIELD);
        if (StringUtils.isNotBlank(cascadingField)) {
            String[] fs = cascadingField.split(MetadataHelper.SPLITER2);
            ConfigBean fake = new ConfigBean()
                    .set("source", fs[1])
                    .set("target", fs[0])
                    .set("whenCreate", true)
                    .set("whenUpdate", true)
                    .set("fillinForce", true);
            config.add(fake);
        }

        if (config.isEmpty()) return JSONUtils.EMPTY_ARRAY;

        Entity sourceEntity = MetadataHelper.getEntity(source.getEntityCode());
        Entity targetEntity = field.getOwnEntity();
        Set<String> sourceFields = new HashSet<>();
        for (ConfigBean e : config) {
            String sourceField = e.getString("source");
            String targetField = e.getString("target");
            if (!MetadataHelper.checkAndWarnField(sourceEntity, sourceField)
                    || !MetadataHelper.checkAndWarnField(targetEntity, targetField)) {
                continue;
            }

            sourceFields.add(sourceField);
        }
        if (sourceFields.isEmpty()) {
            return JSONUtils.EMPTY_ARRAY;
        }

        String ql = String.format("select %s from %s where %s = ?",
                StringUtils.join(sourceFields, ","),
                sourceEntity.getName(),
                sourceEntity.getPrimaryField().getName());
        Record sourceRecord = Application.createQueryNoFilter(ql).setParameter(1, source).record();
        if (sourceRecord == null) {
            return JSONUtils.EMPTY_ARRAY;
        }

        JSONArray fillin = new JSONArray();
        for (ConfigBean e : config) {
            String sourceField = e.getString("source");
            Object value = null;
            if (sourceRecord.hasValue(sourceField, false)) {
                String targetField = e.getString("target");
                value = conversionCompatibleValue(
                        sourceEntity.getField(sourceField),
                        targetEntity.getField(targetField),
                        sourceRecord.getObjectValue(sourceField));
            }

            // NOTE 忽略空值
            if (value == null || NullValue.is(value) || StringUtils.isBlank(value.toString())) {
                continue;
            }

            ConfigBean clone = e.clone().set("value", value);
            clone.set("source", null);
            fillin.add(clone.toJSON());
        }
        return fillin;
    }

    /**
     * 回填值做兼容处理。例如引用字段回填至文本，要用 Label，而不是 ID
     *
     * @param source
     * @param target
     * @param value
     * @return
     */
    protected Object conversionCompatibleValue(Field source, Field target, Object value) {
        EasyField easyField = EasyMetaFactory.valueOf(source);
        Object newValue = easyField.convertCompatibleValue(value, EasyMetaFactory.valueOf(target));

        // 转换成前端可接受的值
        if (easyField instanceof MixValue) {
            if (!(newValue instanceof String) || easyField instanceof EasyFile) {
                newValue = easyField.wrapValue(newValue);
            }
        }
        return newValue;
    }

    /**
     * 获取配置
     *
     * @param field
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<ConfigBean> getConfig(Field field) {
        final String cKey = "AutoFillinManager-" + field.getOwnEntity().getName() + "." + field.getName();
        Object cached = Application.getCommonsCache().getx(cKey);
        if (cached != null) {
            return (List<ConfigBean>) cached;
        }

        Object[][] array = Application.createQueryNoFilter(
                "select sourceField,targetField,extConfig from AutoFillinConfig where belongEntity = ? and belongField = ?")
                .setParameter(1, field.getOwnEntity().getName())
                .setParameter(2, field.getName())
                .array();

        ArrayList<ConfigBean> entries = new ArrayList<>();
        for (Object[] o : array) {
            ConfigBean entry = new ConfigBean()
                    .set("source", o[0])
                    .set("target", o[1]);
            JSONObject ext = JSON.parseObject((String) o[2]);
            entry.set("whenCreate", ext.getBoolean("whenCreate"))
                    .set("whenUpdate", ext.getBoolean("whenUpdate"))
                    .set("fillinForce", ext.getBoolean("fillinForce"));
            entries.add(entry);
        }

        Application.getCommonsCache().putx(cKey, entries);
        return entries;
    }

    @Override
    public void clean(Object field) {
        Field field2 = (Field) field;
        final String cKey = "AutoFillinManager-" + field2.getOwnEntity().getName() + "." + field2.getName();
        Application.getCommonsCache().evict(cKey);
        Application.getCommonsCache().evict(CKEY_AFARF);
    }

    private static final String CKEY_AFARF = "AutoFillinReadonlyFields";
    /**
     * 自动回填中涉及的自动只读字段
     *
     * @param entity
     * @return
     */
    @SuppressWarnings("unchecked")
    public Set<String> getAutoReadonlyFields(String entity) {
        Map<String, Set<String>> fieldsMap = (Map<String, Set<String>>) Application.getCommonsCache().getx(CKEY_AFARF);
        if (fieldsMap == null) {
            fieldsMap = this.initAutoReadonlyFields();
        }
        return Collections.unmodifiableSet(fieldsMap.getOrDefault(entity, Collections.emptySet()));
    }

    synchronized
    private Map<String, Set<String>> initAutoReadonlyFields() {
        Object[][] array = Application.createQueryNoFilter(
                "select extConfig,belongEntity,targetField from AutoFillinConfig")
                .array();

        CaseInsensitiveMap<String, Set<String>> fieldsMap = new CaseInsensitiveMap<>();
        for (Object[] o : array) {
            JSONObject extConfig = JSON.parseObject((String) o[0]);
            if (extConfig == null || !extConfig.getBooleanValue("readonlyTargetField")) {
                continue;
            }

            String belongEntity = (String) o[1];
            String targetField = (String) o[2];

            Set<String> fields = fieldsMap.computeIfAbsent(belongEntity, k -> new HashSet<>());
            fields.add(targetField);
        }

        Application.getCommonsCache().putx(CKEY_AFARF, fieldsMap);
        return fieldsMap;
    }
}
