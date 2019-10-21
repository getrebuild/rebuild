/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.configuration.portals;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import com.rebuild.server.configuration.ConfigManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 可共享的配置
 *
 * @author devezhao
 * @since 2019/10/21
 */
public abstract class ShareToManager<T> implements ConfigManager<T> {

    protected static final Log LOG = LogFactory.getLog(ShareToManager.class);

    // 共享给全部
    public static final String SHARE_ALL = "ALL";
    // 私有
    public static final String SHARE_SELF = "SELF";

    /**
     * @return
     */
    abstract protected String getConfigEntity();

    /**
     * Default: configId,shareTo,createdBy,config
     * @return
     */
    protected String getFieldsForConfig() {
        return "configId,shareTo,createdBy,config";
    }

    /**
     * 清理缓存
     *
     * @param cfgid
     * @param hasApplyType
     */
    protected void cleanWithBelongEntity(ID cfgid, boolean hasApplyType) {
        String ql = String.format("select belongEntity%s from %s where configId = ?", (hasApplyType ? ",applyType" : ""), getConfigEntity());
        Object[] c = Application.createQueryNoFilter(ql).setParameter(1, cfgid).unique();
        if (c != null) {
            Application.getCommonCache().evict(formatCacheKey((String) c[0], hasApplyType ? (String) c[1] : null));
        }
    }

    /**
     * 确定用户使用哪个配置
     *
     * @param user
     * @param belongEntity
     * @param applyType
     * @return
     */
    public ID detectUseConfig(ID user, String belongEntity, String applyType) {
        Object[][] cached = getAllConfig(belongEntity, applyType);
        if (cached.length == 0) {
            return null;
        }

        // 优先使用自己的
        boolean isAdmin = UserHelper.isAdmin(user);
        for (Object[] d : cached) {
            ID createdBy = (ID) d[2];
            if (user.equals(createdBy) || (isAdmin && UserHelper.isAdmin(createdBy))) {
                return (ID) d[0];
            }
        }
        // 其次共享的
        for (Object[] d : cached) {
            if (isShareTo((String) d[1], user)) {
                return (ID) d[0];
            }
        }
        return null;
    }

    /**
     * 获取用户可用的配置列表
     *
     * @param user
     * @param belongEntity
     * @param applyType
     * @return
     */
    protected Object[][] getUsesConfig(ID user, String belongEntity, String applyType) {
        Object[][] cached = getAllConfig(belongEntity, applyType);
        List<Object[]> canUses = new ArrayList<>();
        boolean isAdmin = UserHelper.isAdmin(user);
        for (Object[] d : cached) {
            ID createdBy = (ID) d[2];
            if (user.equals(createdBy) || (isAdmin && UserHelper.isAdmin(createdBy)) || isShareTo((String) d[1], user)) {
                canUses.add(d);
            }
        }
        return canUses.toArray(new Object[0][]);
    }

    /**
     * 获取全部配置（带缓存）
     *
     * @param belongEntity
     * @param applyType
     * @return
     */
    protected Object[][] getAllConfig(String belongEntity, String applyType) {
        String cacheKey = formatCacheKey(belongEntity, applyType);
        Object[][] cached = (Object[][]) Application.getCommonCache().getx(cacheKey);
        if (cached == null) {
            List<String> sqlWhere = new ArrayList<>();
            if (belongEntity != null) {
                sqlWhere.add(String.format("belongEntity = '%s'", belongEntity));
            }
            if (applyType != null) {
                sqlWhere.add(String.format("applyType = '%s'", applyType));
            }

            String ql = String.format("select %s from %s where (1=1) order by modifiedOn desc", getFieldsForConfig(), getConfigEntity());
            if (!sqlWhere.isEmpty()) {
                ql = ql.replace("(1=1)", StringUtils.join(sqlWhere.iterator(), " and "));
            }

            cached = Application.createQueryNoFilter(ql).array();
            Application.getCommonCache().putx(cacheKey, cached);
        }

        if (cached == null) {
            return new Object[0][];
        }

        Object[][] clone = new Object[cached.length][];
        for (int i = 0; i < cached.length; i++) {
            clone[i] = (Object[]) ObjectUtils.clone(cached[i]);
        }
        return clone;
    }

    /**
     * 是否共享
     *
     * @param shareTo
     * @param user
     * @return
     */
    private boolean isShareTo(String shareTo, ID user) {
        if (SHARE_ALL.equals(shareTo)) {
            return true;
        } else if (shareTo.length() >= 20) {
            Set<String> userDefs = new HashSet<>();
            CollectionUtils.addAll(userDefs, shareTo.split(","));
            Set<ID> sharedUsers = UserHelper.parseUsers(userDefs, null);
            return sharedUsers.contains(user);
        }
        return false;
    }

    /**
     * @param belongEntity
     * @param applyType
     * @return
     */
    final protected String formatCacheKey(String belongEntity, String applyType) {
        return String.format("%s-%s-%s.V6", getConfigEntity(),
                StringUtils.defaultIfBlank(belongEntity, "N"),
                StringUtils.defaultIfBlank(applyType, "N")).toUpperCase();
    }

    /**
     * @param array
     * @param useIndex
     */
    protected void sort(Object[][] array, int useIndex) {
        Arrays.sort(array, (foo, bar) -> ObjectUtils.compare((Comparable) foo[useIndex], (Comparable) bar[useIndex]));
    }

    // --

    /**
     * 是否是自己的配置（不是自己的只读）
     *
     * @param user
     * @param configOrUser 配置ID 或 用戶ID
     * @return
     */
    public static boolean isSelf(ID user, ID configOrUser) {
        if (configOrUser.getEntityCode() != EntityHelper.User) {
            configOrUser = getCreatedBy(configOrUser);
        }
        boolean s = user.equals(configOrUser);
        if (s) return true;
        return UserHelper.isAdmin(user) && UserHelper.isAdmin(configOrUser);
    }

    private static final Map<ID, ID> CREATEDBYs = new HashMap<>();
    /**
     * @param configId
     * @return
     */
    static ID getCreatedBy(ID configId) {
        if (CREATEDBYs.containsKey(configId)) {
            return CREATEDBYs.get(configId);
        }

        Entity e = MetadataHelper.getEntity(configId.getEntityCode());
        String ql = String.format("select createdBy from %s where %s = ?", e.getName(), e.getPrimaryField().getName());
        Object[] c = Application.createQueryNoFilter(ql).setParameter(1, configId).unique();
        if (c == null) {
            throw new RebuildException("No config found : " + configId);
        }
        CREATEDBYs.put(configId, (ID) c[0]);
        return (ID) c[0];
    }
}
