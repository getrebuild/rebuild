/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyFile;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.MixValue;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.RbAssert;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 表单自动回填
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/17
 */
@Slf4j
public class AutoFillinManager implements ConfigManager {

    public static final AutoFillinManager instance = new AutoFillinManager();

    private AutoFillinManager() {}

    /**
     * 获取回填值
     *
     * @param field
     * @param sourceId
     * @return
     */
    public JSONArray getFillinValue(Field field, ID sourceId) {
        final EasyField easyField = EasyMetaFactory.valueOf(field);

        // 内置字段无配置
        if (easyField.isBuiltin()) return JSONUtils.EMPTY_ARRAY;

        final List<ConfigBean> config = new ArrayList<>();
        for (ConfigBean cb : getConfig(field)) config.add(cb.clone());

        // 父级级联
        // 利用表单回填做父级级联字段回填
        // format: [MAIN.]TARGET$$$$SOURCE 其中 [MAIN.] 为明细回填主实体
        String cascadingField = easyField.getExtraAttr(EasyFieldConfigProps.REFERENCE_CASCADINGFIELD);
        if (StringUtils.isNotBlank(cascadingField)) {
            String[] ts = cascadingField.split(MetadataHelper.SPLITER_RE);
            ConfigBean fake = new ConfigBean()
                    .set("target", ts[0])
                    .set("source", ts[1])
                    .set("whenCreate", true)
                    .set("whenUpdate", true)
                    .set("fillinForce", true);

            // 移除冲突的表单回填配置
            for (Iterator<ConfigBean> iter = config.iterator(); iter.hasNext(); ) {
                ConfigBean cb = iter.next();
                if (StringUtils.equals(cb.getString("source"), fake.getString("source"))
                        && StringUtils.equals(cb.getString("target"), fake.getString("target"))) {
                    iter.remove();
                    break;
                }
            }

            config.add(fake);
        }

        if (config.isEmpty()) return JSONUtils.EMPTY_ARRAY;

        final Entity sourceEntity = MetadataHelper.getEntity(sourceId.getEntityCode());
        final Entity targetEntity = field.getOwnEntity();

        Set<String> sourceFields = new HashSet<>();

        for (Iterator<ConfigBean> iter = config.iterator(); iter.hasNext(); ) {
            ConfigBean e = iter.next();
            String sourceField = e.getString("source");
            if (MetadataHelper.getLastJoinField(sourceEntity, sourceField) == null) {
                log.warn("Unknown field `{}` in `{}`", sourceField, sourceEntity.getName());
                iter.remove();
                continue;
            }

            String targetField = e.getString("target");
            // 明细 > 主
            if (targetField.contains(".")) {
                String[] fs = targetField.split("\\.");
                Entity mainEntity = targetEntity.getMainEntity();
                if (!MetadataHelper.checkAndWarnField(mainEntity, fs[1])) {
                    iter.remove();
                    continue;
                }
            }

            sourceFields.add(sourceField);
        }

        if (sourceFields.isEmpty()) return JSONUtils.EMPTY_ARRAY;

        sourceFields.add(sourceEntity.getPrimaryField().getName());
        Record sourceRecord = Application.getQueryFactory().recordNoFilter(sourceId, sourceFields.toArray(new String[0]));

        if (sourceRecord == null) return JSONUtils.EMPTY_ARRAY;

        JSONArray fillin = new JSONArray();
        for (ConfigBean e : config) {
            String sourceField = e.getString("source");
            String targetField = e.getString("target");
            if (!MetadataHelper.checkAndWarnField(targetEntity, targetField)) continue;

            Field targetFieldMeta;
            // 明细 > 主
            if (targetField.contains(".")) {
                String[] fs = targetField.split("\\.");
                targetFieldMeta = targetEntity.getMainEntity().getField(fs[1]);
            } else {
                targetFieldMeta = targetEntity.getField(targetField);
            }
            
            Object value = null;
            if (sourceRecord.hasValue(sourceField, false)) {
                value = sourceRecord.getObjectValue(sourceField);
                value = conversionCompatibleValue(
                        MetadataHelper.getLastJoinField(sourceEntity, sourceField),
                        targetFieldMeta,
                        value);
            }

            // 空值
            if (NullValue.isNull(value) || StringUtils.isBlank(value.toString())) {
                // v3.3 强制回填空值
                if (BooleanUtils.isTrue(e.getBoolean("fillinForce"))) {
                    value = null;
                } else {
                    continue;
                }
            }

            // 日期格式处理
            if (value instanceof Date
                    && (targetFieldMeta.getType() == FieldType.DATE || targetFieldMeta.getType() == FieldType.TIMESTAMP)) {
                value = EasyMetaFactory.valueOf(targetFieldMeta).wrapValue(value);
            }

            ConfigBean clone = e.clone().set("value", value);
            clone.set("source", null);
            fillin.add(clone.toJSON());
        }
        return fillin;
    }

    /**
     * 表单后端回填
     *
     * @param record
     * @param fillinForce 是否强制（无视配置）
     * @return
     */
    public int fillinRecord(Record record, boolean fillinForce) {
        RbAssert.isCommercial(null);
        final Entity entity = record.getEntity();
        final boolean isNew = record.getPrimary() == null;

        int fillin = 0;
        for (String fieldName : record.getAvailableFields()) {
            if (!entity.containsField(fieldName)) continue;

            EasyField easyField = EasyMetaFactory.valueOf(entity.getField(fieldName));
            if (easyField.getDisplayType() != DisplayType.REFERENCE) continue;

            fillin += fillinRecordItem(easyField.getRawMeta(), record.getObjectValue(fieldName), isNew, fillinForce, record);
        }
        return fillin;
    }

    private int fillinRecordItem(Field field, Object sourceId, boolean isNew, boolean fillinForce, Record into) {
        if (NullValue.isNull(sourceId)) return 0;

        JSONArray fillinValue = getFillinValue(field, (ID) sourceId);
        if (fillinValue.isEmpty()) return 0;

        int fillin = 0;
        for (Object o : fillinValue) {
            JSONObject item = (JSONObject) o;
            boolean fillinBackend2 = fillinForce || item.getBooleanValue("fillinBackend");
            if (!fillinBackend2) continue;

            if (!fillinForce) {
                if (isNew) {
                    if (!item.getBooleanValue("whenCreate")) continue;
                } else {
                    if (!item.getBooleanValue("whenUpdate")) continue;
                }
            }

            String targetFieldName = item.getString("target");
            boolean fillinForce2 = fillinForce || item.getBooleanValue("fillinForce");

            Object value = item.get("value");
            if (value instanceof JSONObject) {
                value = ((JSONObject) value).getString("id");
                value = ID.valueOf(value.toString());
            }

            // 强制回填
            if (fillinForce2) {
                if (value == null) into.setNull(targetFieldName);
                else into.setObjectValue(targetFieldName, value);
            } else {

                // 非强制时检查目标是否有值
                if (into.hasValue(targetFieldName, false)) continue;

                if (!isNew) {
                    Object[] has = Application.getQueryFactory().uniqueNoFilter(into.getPrimary(), targetFieldName);
                    if (has != null && has[0] != null) continue;
                }

                into.setObjectValue(targetFieldName, value);
            }
            fillin++;

            // 继续回填
            EasyField nextField = EasyMetaFactory.valueOf(field.getOwnEntity().getField(targetFieldName));
            if (nextField.getDisplayType() == DisplayType.REFERENCE) {
                fillin += fillinRecordItem(nextField.getRawMeta(), value, isNew, fillinForce, into);
            }
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
        EasyField sourceEasy = EasyMetaFactory.valueOf(source);
        EasyField targetEasy = EasyMetaFactory.valueOf(target);
        Object newValue = sourceEasy.convertCompatibleValue(value, targetEasy);

        // 转换成前端可接受的值

        if (sourceEasy.getDisplayType() == targetEasy.getDisplayType()
                && sourceEasy.getDisplayType() == DisplayType.MULTISELECT) {
            return newValue;  // Long
        }

        if (sourceEasy instanceof MixValue) {
            if (!(newValue instanceof String) || sourceEasy instanceof EasyFile) {
                newValue = sourceEasy.wrapValue(newValue);
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
        final String cKey = "AutoFillinManager34-" + field.getOwnEntity().getName() + "." + field.getName();
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

            JSONObject extra = JSON.parseObject((String) o[2]);
            entry.set("whenCreate", extra.getBooleanValue("whenCreate"))
                    .set("whenUpdate", extra.getBooleanValue("whenUpdate"))
                    .set("fillinForce", extra.getBooleanValue("fillinForce"))
                    .set("fillinBackend", extra.getBooleanValue("fillinBackend"));
            entries.add(entry);
        }

        Application.getCommonsCache().putx(cKey, entries);
        return entries;
    }

    @Override
    public void clean(Object field) {
        Field field2 = (Field) field;
        final String cKey = "AutoFillinManager34-" + field2.getOwnEntity().getName() + "." + field2.getName();
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
        if (fieldsMap == null) fieldsMap = this.initAutoReadonlyFields();

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
