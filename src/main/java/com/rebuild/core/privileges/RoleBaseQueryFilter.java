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
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
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
    public String evaluate(Entity entity) {
        if (user == null || !user.isActive()) {
            return DENIED.evaluate(null);
        } else if (user.isAdmin()) {
            return ALLOWED.evaluate(null);
        }

        Entity useMain = null;
        if (!MetadataHelper.hasPrivilegesField(entity)) {
            // NOTE BIZZ 实体全部用户可见
            if (MetadataHelper.isBizzEntity(entity) || EasyMetaFactory.valueOf(entity).isPlainEntity()) {
                return ALLOWED.evaluate(null);
            } else if (entity.getMainEntity() != null) {
                useMain = entity.getMainEntity();
            } else {
                log.warn("None privileges entity use query-filter : {}", entity);
                return DENIED.evaluate(null);
            }
        }

        // 未配置权限的默认拒绝
        // 明细实体使用主实体权限
        Privileges ep = user.getOwningRole().getPrivileges(
                useMain != null ? useMain.getEntityCode() : entity.getEntityCode());
        if (ep == Privileges.NONE) {
            return DENIED.evaluate(null);
        }

        DepthEntry de = ep.superlative(useAction);
        if (de == BizzDepthEntry.GLOBAL) {
            return ALLOWED.evaluate(null);
        }

        String ownFormat = "%s = '%s'";
        Field dtmField = null;
        if (useMain != null) {
            dtmField = MetadataHelper.getDetailToMainField(entity);
            ownFormat = dtmField.getName() + "." + ownFormat;
        }

        if (de == BizzDepthEntry.PRIVATE) {
            return appendShareFilter(entity, dtmField,
                    String.format(ownFormat, EntityHelper.OwningUser, user.getIdentity()));
        }

        Department dept = user.getOwningDept();
        String deptSql = String.format(ownFormat, EntityHelper.OwningDept, dept.getIdentity());

        if (de == BizzDepthEntry.LOCAL) {
            return appendShareFilter(entity, dtmField, deptSql);
        } else if (de == BizzDepthEntry.DEEPDOWN) {
            Set<String> sqls = new HashSet<>();
            sqls.add(deptSql);

            for (BusinessUnit child : dept.getAllChildren()) {
                sqls.add(String.format(ownFormat, EntityHelper.OwningDept, child.getIdentity()));
            }
            return appendShareFilter(entity, dtmField, "(" + StringUtils.join(sqls, " or ") + ")");
        }

        return DENIED.evaluate(null);
    }

    /**
     * 共享权限
     *
     * @param entity
     * @param detailToMainField
     * @param filtered
     * @return
     */
    protected String appendShareFilter(Entity entity, Field detailToMainField, String filtered) {
        if (user == null) return filtered;

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

        return "(" + filtered + " or " + shareFilter + ")";
    }
}
