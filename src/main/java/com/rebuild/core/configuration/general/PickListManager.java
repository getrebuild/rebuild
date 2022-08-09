/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 列表项
 *
 * @author Zixin (RB)
 * @since 09/06/2018
 */
public class PickListManager implements ConfigManager {

    public static final PickListManager instance = new PickListManager();

    protected PickListManager() { }

    /**
     * @param field
     * @return
     */
    public JSONArray getPickList(Field field) {
        ConfigBean[] entries = getPickListRaw(field, false);
        for (ConfigBean e : entries) {
            e.set("hide", null);
            e.set("mask", null);
        }
        return JSONUtils.toJSONArray(entries);
    }

    /**
     * @param field
     * @param includeHide
     * @return
     */
    public ConfigBean[] getPickListRaw(Field field, boolean includeHide) {
        return getPickListRaw(field.getOwnEntity().getName(), field.getName(), includeHide);
    }

    /**
     * @param entity
     * @param field
     * @param includeHide
     * @return
     */
    public ConfigBean[] getPickListRaw(String entity, String field, boolean includeHide) {
        final String ckey = String.format("PickList-%s.%s", entity, field);
        ConfigBean[] cached = (ConfigBean[]) Application.getCommonsCache().getx(ckey);

        if (cached == null) {
            Object[][] array = Application.createQueryNoFilter(
                    "select itemId,text,isDefault,isHide,maskValue,color from PickList where belongEntity = ? and belongField = ? order by seq asc")
                    .setParameter(1, entity)
                    .setParameter(2, field)
                    .array();
            List<ConfigBean> list = new ArrayList<>();
            for (Object[] o : array) {
                ConfigBean entry = new ConfigBean()
                        .set("id", o[0])
                        .set("text", o[1])
                        .set("default", o[2])
                        .set("hide", o[3])
                        .set("mask", o[4])
                        .set("color", o[5]);
                list.add(entry);
            }

            cached = list.toArray(new ConfigBean[0]);
            Application.getCommonsCache().putx(ckey, cached);
        }

        List<ConfigBean> ret = new ArrayList<>();
        for (ConfigBean entry : cached) {
            if (includeHide || !entry.getBoolean("hide")) {
                ret.add(entry.clone());
            }
        }
        return ret.toArray(new ConfigBean[0]);
    }

    /**
     * @param itemId
     * @return
     */
    public String getLabel(ID itemId) {
        Serializable s = getItem(itemId);
        return s == null ? null : (String) ((Object[]) s)[0];
    }

    /**
     * @param itemId
     * @return
     */
    public String getColor(ID itemId) {
        Serializable s = getItem(itemId);
        if (s == null) return null;

        String color = (String) ((Object[]) s)[1];
        return StringUtils.defaultIfBlank(color, null);
    }

    private Serializable getItem(ID itemId) {
        final String ckey = "PickListITEM-" + itemId;
        Serializable cached = Application.getCommonsCache().getx(ckey);
        if (cached != null) {
            return cached.equals(DELETED_ITEM) ? null : cached;
        }

        Object[] o = Application.createQueryNoFilter(
                "select text,color from PickList where itemId = ?")
                .setParameter(1, itemId)
                .unique();
        if (o != null) cached = o;
        if (cached == null) cached = DELETED_ITEM;  // 已删除

        Application.getCommonsCache().putx(ckey, cached);
        return cached.equals(DELETED_ITEM) ? null : cached;
    }

    /**
     * @param labelValue
     * @param field
     * @return
     */
    public ID findItemByLabel(String labelValue, Field field) {
        ConfigBean[] items = getPickListRaw(field, true);
        for (ConfigBean item : items) {
            if (StringUtils.equalsIgnoreCase(item.getString("text"), labelValue)) {
                return item.getID("id");
            }
        }
        return null;
    }

    /**
     * 获取默认项
     *
     * @param field
     * @return
     */
    public ID getDefaultItem(Field field) {
        for (ConfigBean e : getPickListRaw(field, false)) {
            if (e.getBoolean("default")) {
                return e.getID("id");
            }
        }
        return null;
    }

    @Override
    public void clean(Object idOrField) {
        if (idOrField instanceof ID) {
            Application.getCommonsCache().evict("PickListITEM-" + idOrField);
        } else if (idOrField instanceof Field) {
            Field field = (Field) idOrField;
            Application.getCommonsCache().evict(String.format("PickList-%s.%s", field.getOwnEntity().getName(), field.getName()));
        }
    }
}
