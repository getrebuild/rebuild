/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyTag;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多引用字段支持
 *
 * @author devezhao
 * @since 2021/11/19
 * @see com.rebuild.core.metadata.easymeta.EasyN2NReference
 */
@Slf4j
public class TagSupport {

    /**
     * 获取标签列表
     *
     * @param field
     * @param recordId 主键
     * @return
     */
    public static String[] items(Field field, ID recordId) {
        if ((int) field.getOwnEntity().getEntityCode() != recordId.getEntityCode()) {
            throw new RebuildException("Bad id for found tag-value : " + field);
        }

        Object[][] array = Application.getPersistManagerFactory().createQuery(
                "select tagName from TagItem where belongField = ? and recordId = ? order by seq")
                .setParameter(1, field.getName())
                .setParameter(2, recordId)
                .array();
        if (array.length == 0) return ArrayUtils.EMPTY_STRING_ARRAY;

        String[] tagNames = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            tagNames[i] = (String) array[i][0];
        }
        return tagNames;
    }

    /**
     * 获取标签列表
     *
     * @param fieldPath
     * @param recordId 主键
     * @return
     */
    public static String[] items(String fieldPath, ID recordId) {
        Object[] last = N2NReferenceSupport.getLastObject(fieldPath, recordId);
        return items((Field) last[0], (ID) last[1]);
    }

    /**
     * 默认值
     *
     * @param field
     * @return
     */
    public static String[] getDefaultValue(EasyTag field) {
        JSONArray tagList = field.getExtraAttrs(Boolean.TRUE).getJSONArray(EasyFieldConfigProps.TAG_LIST);
        if (tagList == null || tagList.isEmpty()) return ArrayUtils.EMPTY_STRING_ARRAY;

        List<String> dv = new ArrayList<>();
        for (Object o : tagList) {
            JSONObject tag = (JSONObject) o;
            if (tag.getBooleanValue("default")) dv.add(tag.getString("name"));
        }
        return dv.toArray(new String[0]);
    }

    /**
     * @param field
     * @return
     */
    public static Map<String, String> getNamesColor(EasyField field) {
        Map<String, String> colors = new HashMap<>();

        JSONArray options = field.getExtraAttrs().getJSONArray(EasyFieldConfigProps.TAG_LIST);
        if (options != null) {
            for (Object o : options) {
                JSONObject item = (JSONObject) o;
                colors.put(item.getString("name"), item.getString("color"));
            }
        }
        return colors;
    }
}
