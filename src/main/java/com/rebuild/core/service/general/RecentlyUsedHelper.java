/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserFilters;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.general.FieldValueHelper;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
     * 获取最近使用
     *
     * @param user
     * @param entity
     * @param type
     * @return
     */
    public static ID[] gets(ID user, String entity, String type) {
        return gets(user, entity, type, 20, null);
    }

    /**
     * 获取最近使用
     *
     * @param user
     * @param entity
     * @param type
     * @param checkFilter
     * @return
     */
    public static ID[] gets(ID user, String entity, String type, String checkFilter) {
        return gets(user, entity, type, 20, checkFilter);
    }

    /**
     * 获取最近使用
     *
     * @param user
     * @param entity
     * @param type
     * @param limit
     * @param checkFilter
     * @return
     */
    protected static ID[] gets(ID user, String entity, String type, int limit, String checkFilter) {
        final String ckey = formatKey(user, entity, type);
        @SuppressWarnings("unchecked")
        LinkedList<ID> cached = (LinkedList<ID>) Application.getCommonsCache().getx(ckey);
        if (cached == null || cached.isEmpty()) return ID.EMPTY_ID_ARRAY;

        Set<ID> missed = new HashSet<>();
        List<ID> data = new ArrayList<>();

        int entityCode = 0;
        for (int i = 0; i < limit && i < cached.size(); i++) {
            final ID raw = cached.get(i);
            entityCode = raw.getEntityCode();

            boolean allowRead = entityCode == EntityHelper.ClassificationData
                    || Application.getPrivilegesManager().allowRead(user, raw);
            if (!allowRead) continue;

            // 是否符合条件
            if (checkFilter != null) {
                if (!QueryHelper.isMatchFilter(raw, checkFilter)) continue;
            }
            // fix:4.0.2
            if (entityCode == EntityHelper.ClassificationData) {
                ClassificationManager.Item item = ClassificationManager.instance.getItem(raw);
                if (item == null || item.isHide()) continue;
            }

            try {
                ID clone = ID.valueOf(raw.toLiteral());
                clone.setLabel(FieldValueHelper.getLabel(raw));
                data.add(clone);
            } catch (NoRecordFoundException ex) {
                missed.add(raw);
            }
        }

        if (!missed.isEmpty()) {
            cached.removeAll(missed);
            Application.getCommonsCache().putx(ckey, cached);
        }

        // 过滤 BIZZ
        if (!data.isEmpty() && MetadataHelper.isBizzEntity(entityCode) && !UserHelper.isAdmin(user)) {
            if (entityCode == EntityHelper.Role || UserFilters.isEnableBizzPart(user)) {
                data.clear();
            }
        }

        return data.toArray(new ID[0]);
    }

    /**
     * 添加最近使用
     *
     * @param user
     * @param id
     * @param type
     */
    public static void add(ID user, ID id, String type) {
        if (EntityHelper.isUnsavedId(id)) return;

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
        return String.format("RS31.%s-%s-%s", user, entity, StringUtils.defaultIfBlank(type, StringUtils.EMPTY));
    }
}
