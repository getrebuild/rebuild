/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON format
 *
 * @author devezhao
 * @since 09/29/2018
 */
public class JSONUtils {

    public static final String EMPTY_OBJECT_STR = "{}";
    public static final String EMPTY_ARRAY_STR = "[]";
    public static final JSONObject EMPTY_OBJECT = JSON.parseObject(EMPTY_OBJECT_STR);
    public static final JSONArray EMPTY_ARRAY = JSON.parseArray(EMPTY_ARRAY_STR);

    /**
     * @param key
     * @param value
     * @return
     */
    public static JSONObject toJSONObject(String key, Object value) {
        return toJSONObject(new String[]{key}, new Object[]{value});
    }

    /**
     * @param keys
     * @param values
     * @return
     */
    public static JSONObject toJSONObject(String[] keys, Object[] values) {
        Assert.isTrue(keys.length <= values.length, "K/V 长度不匹配");
        Map<String, Object> map = new HashMap<>(keys.length);
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }
        return (JSONObject) JSON.toJSON(map);
    }

    /**
     * @param keys
     * @param valuesArray
     * @return
     */
    public static JSONArray toJSONObjectArray(String[] keys, Object[][] valuesArray) {
        List<Map<String, Object>> array = new ArrayList<>();
        for (Object[] o : valuesArray) {
            Map<String, Object> map = new HashMap<>(keys.length);
            for (int i = 0; i < keys.length; i++) {
                map.put(keys[i], o[i]);
            }
            array.add(map);
        }
        return (JSONArray) JSON.toJSON(array);
    }

    /**
     * @param items
     * @return
     */
    public static JSONArray toJSONArray(JSONable[] items) {
        if (items == null || items.length == 0) {
            return EMPTY_ARRAY;
        }

        JSONArray array = new JSONArray();
        for (JSONable e : items) {
            array.add(e.toJSON());
        }
        return array;
    }

    /**
     * @param json
     * @return
     */
    public static JSON clone(JSON json) {
        return (JSON) JSON.parse(json.toJSONString());
    }

    /**
     * @param json
     * @return
     */
    public static String prettyPrint(Object json) {
        return JSON.toJSONString(json,
                SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue);
    }

    /**
     * @param text
     * @return
     */
    public static boolean wellFormat(String text) {
        if (StringUtils.isBlank(text)) return false;
        text = text.trim();
        return (text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]"));
    }
}
