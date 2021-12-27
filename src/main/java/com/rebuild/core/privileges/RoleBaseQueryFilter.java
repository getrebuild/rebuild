/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.bizz.privileges.DepthEntry;
import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.impl.BizzDepthEntry;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.QueryFilter;
import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Filter;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.bizz.CustomEntityPrivileges;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.query.AdvFilterParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 基于角色权限的查询过滤器
 *
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-21
 */
@Slf4j
public class RoleBaseQueryFilter implements Filter, QueryFilter {
    private static final long serialVersionUID = -7388577069739389698L;

    /**
     * 总是拒绝
     */
    public static final Filter DENIED = new RoleBaseQueryFilter() {
        private static final long serialVersionUID = -1841438304452108874L;
        @Override
        public String evaluate(Entity entity) {
            return "( 1 = 0 )";
        }
    };

    /**
     * 总是允许
     */
    public static final Filter ALLOWED = new RoleBaseQueryFilter() {
        private static final long serialVersionUID = -1300184338130890817L;
        @Override
        public String evaluate(Entity entity) {
            return "( 1 = 1 )";
        }
    };

    private RoleBaseQueryFilter() {
        this.user = null;
        this.useAction = null;
    }

    // --

    final private User user;
    final private Permission useAction;

    /**
     * @param user
     * @param useAction
     */
    public RoleBaseQueryFilter(User user, Permission useAction) {
        this.user = user;
        this.useAction = useAction == null ? BizzPermission.READ : useAction;
    }

    @Override
    public String evaluate(int entity) {
        return evaluate(MetadataHelper.getEntity(entity));
    }

    @Override
    public String evaluate(final Entity entity) {
        if (user == null || !user.isActive()) {
            return DENIED.evaluate(null);
        } else if (user.isAdmin()) {
            return ALLOWED.evaluate(null);
        }

        Entity useMainEntity = null;
        if (!MetadataHelper.hasPrivilegesField(entity)) {
            // NOTE BIZZ 实体全部用户可见
            if (MetadataHelper.isBizzEntity(entity) || EasyMetaFactory.valueOf(entity).isPlainEntity()) {
                return ALLOWED.evaluate(null);

            } else if (entity.getMainEntity() != null) {
                useMainEntity = entity.getMainEntity();
            } else {
                log.warn("None privileges entity use `Application#createQueryNoFilter` please : {} \n\t{}",
                        entity, StringUtils.join(Thread.currentThread().getStackTrace(), "\n\t"));

                return DENIED.evaluate(null);
            }
        }

        // 未配置权限的默认拒绝
        // 明细实体使用主实体权限
        final Privileges ep = user.getOwningRole().getPrivileges(
                useMainEntity != null ? useMainEntity.getEntityCode() : entity.getEntityCode());
        if (ep == Privileges.NONE) {
            return DENIED.evaluate(null);
        }

        String owningFormat = "%s = '%s'";
        Field dtmField = null;
        if (useMainEntity != null) {
            dtmField = MetadataHelper.getDetailToMainField(entity);
            owningFormat = dtmField.getName() + "." + owningFormat;
        }

        final String customFilter = buildCustomFilter(ep);
        final String shareFilter = buildShareFilter(entity, dtmField);

        final DepthEntry depth = ep.superlative(useAction);

        // 全部

        if (depth == BizzDepthEntry.GLOBAL) {
            if (customFilter == null) {
                return ALLOWED.evaluate(null);
            } else {
                return String.format("(%s or %s)", customFilter, shareFilter);
            }
        }

        // 本人

        if (depth == BizzDepthEntry.PRIVATE) {
            String baseFilter = String.format(owningFormat, EntityHelper.OwningUser, user.getIdentity());
            return joinFilters(baseFilter, customFilter, shareFilter);
        }

        // 部门

        Department dept = user.getOwningDept();
        String deptFilter = String.format(owningFormat, EntityHelper.OwningDept, dept.getIdentity());

        if (depth == BizzDepthEntry.LOCAL) {
            return joinFilters(deptFilter, customFilter, shareFilter);

        } else if (depth == BizzDepthEntry.DEEPDOWN) {
            Set<String> set = new HashSet<>();
            set.add(deptFilter);
            for (BusinessUnit ch : dept.getAllChildren()) {
                set.add(String.format(owningFormat, EntityHelper.OwningDept, ch.getIdentity()));
            }

            deptFilter = StringUtils.join(set, " or ");
            return joinFilters(deptFilter, customFilter, shareFilter);
        }

        return DENIED.evaluate(null);
    }

    private String joinFilters(String baseFilter, String customFilter, String shareFilter) {
        if (customFilter == null) {
            return String.format("((%s) or %s)", baseFilter, shareFilter);
        } else {
            return String.format("(((%s) and %s) or %s)", baseFilter, customFilter, shareFilter);
        }
    }

    /**
     * 共享权限
     *
     * @param entity
     * @param detailToMainField
     * @return
     */
    private String buildShareFilter(Entity entity, Field detailToMainField) {
        if (user == null) return null;

        String shareFilter = "exists (select rights from ShareAccess where belongEntity = '%s' and shareTo = '%s' and recordId = ^%s)";

        // 明细实体，使用主实体的共享
        if (detailToMainField != null) {
            shareFilter = String.format(shareFilter,
                    detailToMainField.getOwnEntity().getMainEntity().getName(),
                    user.getId(), detailToMainField.getName());
        } else {
            shareFilter = String.format(shareFilter,
                    entity.getName(), user.getId(), entity.getPrimaryField().getName());
        }
        return shareFilter;
    }

    /**
     * 自定义权限
     *
     * @param ep
     * @return
     * @see PrivilegesManager#andPassCustomFilter(ID, ID, Permission, Privileges)
     */
    private String buildCustomFilter(Privileges ep) {
        if (user == null || useAction == null
                || !ep.getClass().isAssignableFrom(CustomEntityPrivileges.class)) return null;

        JSONObject hasFilter = ((CustomEntityPrivileges) ep).getCustomFilter(useAction);
        if (hasFilter == null) return null;

        AdvFilterParser advFilterParser = new AdvFilterParser(hasFilter);
        advFilterParser.setUser(user.getId());
        return advFilterParser.toSqlWhere();
    }
}
