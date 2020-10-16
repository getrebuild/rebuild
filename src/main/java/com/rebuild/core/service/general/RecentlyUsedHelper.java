/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.support.general.FieldValueWrapper;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * 最近使用的数据（引用型字段搜索）
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/25
 */
public class RecentlyUsedHelper {

    // 最大缓存数量
    private static final int MAXNUM_PRE_ENTITY = 50;

    /**
     * 获取最近使用（最多10个）
     *
     * @param user
     * @param entity
     * @param type
     * @return
     */
    public static ID[] gets(ID user, String entity, String type) {
        return gets(user, entity, type, 10);
    }

    /**
     * 获取最近使用
     *
     * @param user
     * @param entity
     * @param type
     * @param limit
     * @return
     */
    public static ID[] gets(ID user, String entity, String type, int limit) {
        final String key = formatKey(user, entity, type);
        @SuppressWarnings("unchecked")
        LinkedList<ID> cached = (LinkedList<ID>) Application.getCommonsCache().getx(key);
        if (cached == null || cached.isEmpty()) {
            return ID.EMPTY_ID_ARRAY;
        }

        Set<ID> missed = new HashSet<>();
        List<ID> data = new ArrayList<>();
        for (int i = 0; i < limit && i < cached.size(); i++) {
            final ID raw = cached.get(i);
            if (!(raw.getEntityCode() == EntityHelper.ClassificationData || Application.getPrivilegesManager().allowRead(user, raw))) {
                continue;
            }

            try {
                String label = FieldValueWrapper.getLabel(raw);
                ID clone = ID.valueOf(raw.toLiteral());
                clone.setLabel(label);
                data.add(clone);
            } catch (NoRecordFoundException ex) {
                missed.add(raw);
            }
        }

        if (!missed.isEmpty()) {
            cached.removeAll(missed);
            Application.getCommonsCache().putx(key, cached);
        }
        return data.toArray(new ID[0]);
    }

    /**
     * 添加搜索缓存
     *
     * @param user
     * @param id
     * @param type
     */
    public static void add(ID user, ID id, String type) {
        final String key = formatKey(user, MetadataHelper.getEntityName(id), type);
        @SuppressWarnings("unchecked")
        LinkedList<ID> cached = (LinkedList<ID>) Application.getCommonsCache().getx(key);
        if (cached == null) {
            cached = new LinkedList<>();
        } else {
            cached.remove(id);
        }

        if (cached.size() > MAXNUM_PRE_ENTITY) {
            cached.removeLast();
        }

        cached.addFirst(id);
        Application.getCommonsCache().putx(key, cached);
    }

    /**
     * 清理缓存
     *
     * @param user
     * @param entity
     * @param type
     */
    public static void clean(ID user, String entity, String type) {
        final String key = formatKey(user, entity, type);
        Application.getCommonsCache().evict(key);
    }

    private static String formatKey(ID user, String entity, String type) {
        return String.format("RS.%s-%s-%s", user, entity, StringUtils.defaultIfBlank(type, StringUtils.EMPTY));
    }
}
