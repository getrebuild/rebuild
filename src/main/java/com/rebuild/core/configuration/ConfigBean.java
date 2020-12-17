/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.JSONable;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/20
 */
public class ConfigBean implements Serializable, Cloneable, JSONable {
    private static final long serialVersionUID = -2618040374508703332L;

    private Map<String, Object> data;

    public ConfigBean() {
        this.data = new HashMap<>();
    }

    /**
     * @param name
     * @param value Remove if null
     * @return
     */
    public ConfigBean set(String name, Object value) {
        Assert.notNull(name, "[name] cannot be null");
        if (value == null) {
            data.remove(name);
        } else {
            data.put(name, value);
        }
        return this;
    }

    public ID getID(String name) {
        return (ID) data.get(name);
    }

    public String getString(String name) {
        return (String) data.get(name);
    }

    public Boolean getBoolean(String name) {
        return (Boolean) data.get(name);
    }

    public Integer getInteger(String name) {
        return (Integer) data.get(name);
    }

    public Long getLong(String name) {
        return (Long) data.get(name);
    }

    public JSON getJSON(String name) {
        return (JSON) data.get(name);
    }

    @SuppressWarnings({"unchecked", "unused"})
    public <T> T get(String name, Class<T> returnType) {
        return (T) data.get(name);
    }

    @Override
    public ConfigBean clone() {
        try {
            super.clone();
        } catch (CloneNotSupportedException ignored) {
        }

        ConfigBean c = new ConfigBean();
        for (Map.Entry<String, Object> e : this.data.entrySet()) {
            Object v = e.getValue();
            if (v instanceof JSON) {
                v = JSONUtils.clone((JSON) v);
            }
            c.set(e.getKey(), v);
        }
        return c;
    }

    @Override
    public JSON toJSON() {
        return (JSONObject) JSON.toJSON(this.data);
    }

    @Override
    public JSON toJSON(String... specFields) {
        Map<String, Object> map = new HashMap<>();
        for (String s : specFields) {
            map.put(s, data.get(s));
        }
        return (JSONObject) JSON.toJSON(map);
    }

    /**
     * @return
     */
    public Map<String, Object> toMap() {
        return data;
    }
}
