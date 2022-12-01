/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.core.privileges.UserHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 可共享的配置
 *
 * @author devezhao
 * @since 2019/10/21
 */
public abstract class ShareToManager implements ConfigManager {

    /**
     * 共享给全部
     */
    public static final String SHARE_ALL = "ALL";
    /**
     * 私有
     */
    public static final String SHARE_SELF = "SELF";

    /**
     * @return
     */
    abstract protected String getConfigEntity();

    /**
     * Default: configId,shareTo,createdBy,config
     *
     * @return
     */
    protected String getConfigFields() {
        return "configId,shareTo,createdBy,config";
    }

    /**
     * 清理缓存
     *
     * @param cfgid
     * @param hasApplyType
     */
    protected void cleanWithBelongEntity(ID cfgid, boolean hasApplyType) {
        String ql = String.format("select belongEntity%s from %s where configId = ?",
                (hasApplyType ? ",applyType" : ""), getConfigEntity());
        Object[] c = Application.createQueryNoFilter(ql).setParameter(1, cfgid).unique();
        if (c != null) {
            Application.getCommonsCache().evict(formatCacheKey((String) c[0], hasApplyType ? (String) c[1] : null));
        }
    }

    /**
     * 确定用户使用哪个配置
     *
     * @param user
     * @param belongEntity
     * @param applyType
     * @return
     * @see #detectUseConfig(ID, String, String, boolean)
     */
    public ID detectUseConfig(ID user, String belongEntity, String applyType) {
        return detectUseConfig(user, belongEntity, applyType, Boolean.TRUE);
    }

    /**
     * @param user
     * @param belongEntity
     * @param applyType
     * @param firstUseSelf
     * @return
     */
    protected ID detectUseConfig(ID user, String belongEntity, String applyType, boolean firstUseSelf) {
        final Object[][] alls = getAllConfig(belongEntity, applyType);
        if (alls.length == 0) return null;

        // 1.优先使用自己的
        if (firstUseSelf) {
            for (Object[] d : alls) {
                ID createdBy = (ID) d[2];
                if (UserHelper.isSelf(user, createdBy)) {
                    return (ID) d[0];
                }
            }
        }

        // 2.其次使用共享的
        for (Object[] d : alls) {
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
        for (Object[] d : cached) {
            ID createdBy = (ID) d[2];
            if (UserHelper.isSelf(user, createdBy) || isShareTo((String) d[1], user)) {
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
        final String cacheKey = formatCacheKey(belongEntity, applyType);
        Object[][] cached = (Object[][]) Application.getCommonsCache().getx(cacheKey);

        if (cached == null) {
            List<String> sqlWhere = new ArrayList<>();
            if (belongEntity != null) {
                sqlWhere.add(String.format("belongEntity = '%s'", belongEntity));
            }
            if (applyType != null) {
                sqlWhere.add(String.format("applyType = '%s'", applyType));
            }

            String ql = String.format(
                    "select %s from %s where (1=1) order by modifiedOn desc", getConfigFields(), getConfigEntity());
            if (!sqlWhere.isEmpty()) {
                ql = ql.replace("(1=1)", StringUtils.join(sqlWhere.iterator(), " and "));
            }

            cached = Application.createQueryNoFilter(ql).array();
            Application.getCommonsCache().putx(cacheKey, cached);
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
     * 是否为共享成员
     *
     * @param shareTo
     * @param user
     * @return
     */
    private boolean isShareTo(String shareTo, ID user) {
        if (SHARE_ALL.equals(shareTo)) {
            return true;

        } else if (shareTo != null && shareTo.length() >= 20) {
            Set<String> userDefs = new HashSet<>();
            CollectionUtils.addAll(userDefs, shareTo.split(","));
            Set<ID> sharedUsers = UserHelper.parseUsers(userDefs, null);
            return sharedUsers.contains(user);

        } else {  // SELF
            return false;
        }
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
}
