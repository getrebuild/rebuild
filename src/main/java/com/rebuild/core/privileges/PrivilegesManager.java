/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.bizz.BizzException;
import cn.devezhao.bizz.privileges.DepthEntry;
import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.bizz.privileges.impl.BizzDepthEntry;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Filter;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.bizz.*;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.general.EntityService;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;

/**
 * 实体安全/权限 管理
 *
 * @author Zhao Fangfang
 * @see Role
 * @see BizzPermission
 * @see BizzDepthEntry
 * @see CustomEntityPrivileges
 */
@Service
public class PrivilegesManager {

    final private UserStore theUserStore;
    final private RecordOwningCache theRecordOwningCache;

    /**
     * @param us
     * @param roc
     */
    protected PrivilegesManager(UserStore us, RecordOwningCache roc) {
        this.theUserStore = us;
        this.theRecordOwningCache = roc;
    }

    /**
     * 获取指定实体的权限集合
     *
     * @param user
     * @param entity
     * @return
     */
    public Privileges getPrivileges(ID user, int entity) {
        User u = theUserStore.getUser(user);
        if (!u.isActive()) {
            return Privileges.NONE;
        } else if (u.isAdmin()) {
            return Privileges.ROOT;
        }
        return u.getOwningRole().getPrivileges(convert2MainEntity(entity));
    }

    /**
     * 创建权限
     *
     * @param user
     * @param entity
     * @return
     */
    public boolean allowCreate(ID user, int entity) {
        return allow(user, entity, BizzPermission.CREATE);
    }

    /**
     * 删除权限
     *
     * @param user
     * @param entity
     * @return
     */
    public boolean allowDelete(ID user, int entity) {
        return allow(user, entity, BizzPermission.DELETE);
    }

    /**
     * 更新权限
     *
     * @param user
     * @param entity
     * @return
     */
    public boolean allowUpdate(ID user, int entity) {
        return allow(user, entity, BizzPermission.UPDATE);
    }

    /**
     * 读取权限
     *
     * @param user
     * @param entity
     * @return
     */
    public boolean allowRead(ID user, int entity) {
        return allow(user, entity, BizzPermission.READ);
    }

    /**
     * 分派权限
     *
     * @param user
     * @param entity
     * @return
     */
    public boolean allowAssign(ID user, int entity) {
        return allow(user, entity, BizzPermission.ASSIGN);
    }

    /**
     * 共享权限
     *
     * @param user
     * @param entity
     * @return
     */
    public boolean allowShare(ID user, int entity) {
        return allow(user, entity, BizzPermission.SHARE);
    }

    /**
     * 删除权限
     *
     * @param user
     * @param target
     * @return
     */
    public boolean allowDelete(ID user, ID target) {
        return allow(user, target, BizzPermission.DELETE);
    }

    /**
     * 更新权限
     *
     * @param user
     * @param target
     * @return
     */
    public boolean allowUpdate(ID user, ID target) {
        return allow(user, target, BizzPermission.UPDATE);
    }

    /**
     * 读取权限
     *
     * @param user
     * @param target
     * @return
     */
    public boolean allowRead(ID user, ID target) {
        return allow(user, target, BizzPermission.READ);
    }

    /**
     * 分派权限
     *
     * @param user
     * @param target
     * @return
     */
    public boolean allowAssign(ID user, ID target) {
        return allow(user, target, BizzPermission.ASSIGN);
    }

    /**
     * 共享权限
     *
     * @param user
     * @param target
     * @return
     */
    public boolean allowShare(ID user, ID target) {
        return allow(user, target, BizzPermission.SHARE);
    }

    /**
     * 是否对实体有指定权限
     *
     * @param user
     * @param entity 目标实体
     * @param action 权限动作
     * @return
     */
    public boolean allow(ID user, int entity, Permission action) {
        // PlainEntity: CRUD
        if (action.getMask() <= BizzPermission.READ.getMask() && EasyMetaFactory.valueOf(entity).isPlainEntity()) {
            return true;
        }

        // 全员允许读取、创建，模块自身有权限体系逻辑

        if ((entity == EntityHelper.Feeds || entity == EntityHelper.ProjectTask)
                && (action == BizzPermission.READ || action == BizzPermission.CREATE)) {
            return true;
        } else if (entity == EntityHelper.Attachment && action == BizzPermission.READ) {
            return true;
        }

        Boolean a = userAllow(user);
        if (a != null) {
            return a;
        }

        Role role = theUserStore.getUser(user).getOwningRole();
        if (RoleService.ADMIN_ROLE.equals(role.getIdentity())) {
            return true;
        } else if (action == BizzPermission.READ && MetadataHelper.isBizzEntity(entity)) {
            return true;
        }

        // 取消共享与共享共用权限
        if (action == EntityService.UNSHARE) {
            action = BizzPermission.SHARE;
        }

        if (MetadataHelper.getEntity(entity).getMainEntity() != null) {
            // 明细实体不能使用此方法检查创建权限
            // 明细实体创建 = 主实体更新，因此应该检查主实体记录是否有更新权限
            if (action == BizzPermission.CREATE) {
                throw new PrivilegesException("Unsupported checks detail-entity : " + entity);
            }
            // 明细无分派/共享
            else if (action == BizzPermission.ASSIGN || action == BizzPermission.SHARE) {
                return false;
            }
            action = convert2MainAction(action);
        }

        Privileges ep = role.getPrivileges(convert2MainEntity(entity));
        return ep.allowed(action);
    }

    /**
     * 是否对指定记录有指定权限
     *
     * @param user
     * @param target 目标记录
     * @param action 权限动作
     * @return
     */
    public boolean allow(ID user, ID target, Permission action) {
        return allow(user, target, action, false);
    }

    /**
     * 是否对指定记录有指定权限
     *
     * @param user
     * @param target 目标记录
     * @param action 权限动作
     * @param ignoreShared 是否忽略通过共享得到的权限
     * @return
     */
    public boolean allow(ID user, ID target, Permission action, boolean ignoreShared) {
        final Entity entity = MetadataHelper.getEntity(target.getEntityCode());

        // PlainEntity: CRUD
        if (action.getMask() <= BizzPermission.READ.getMask()
                && EasyMetaFactory.valueOf(entity).isPlainEntity()) {
            return true;
        }

        Boolean a = userAllow(user);
        if (a != null) {
            return a;
        }

        Role role = theUserStore.getUser(user).getOwningRole();
        if (RoleService.ADMIN_ROLE.equals(role.getIdentity())) {
            return true;
        }

        // BIZZ 全员可读
        if (action == BizzPermission.READ && MetadataHelper.isBizzEntity(entity.getEntityCode())) {
            return true;
        }

        // 用户可修改自己
        if (action == BizzPermission.UPDATE && target.equals(user)) {
            return true;
        }

        // 明细无 分派/共享
        if (entity.getMainEntity() != null) {
            if (action == BizzPermission.ASSIGN || action == BizzPermission.SHARE) {
                return false;
            }
            action = convert2MainAction(action);
        }

        Privileges ep = role.getPrivileges(convert2MainEntity(entity.getEntityCode()));

        boolean allowed = ep.allowed(action);
        if (!allowed) {
            return false;
        }

        final DepthEntry depth = ep.superlative(action);

        if (BizzDepthEntry.NONE.equals(depth)) {
            return false;
        } else if (BizzDepthEntry.GLOBAL.equals(depth)) {
            return andPassCustomFilter(user, target, action, ep);
        }

        ID targetUserId = theRecordOwningCache.getOwningUser(target);
        if (targetUserId == null) {
            return false;
        }

        if (BizzDepthEntry.PRIVATE.equals(depth)) {
            allowed = user.equals(targetUserId);
            if (!allowed) {
                allowed = !ignoreShared && allowViaShare(user, target, action);
            }
            return allowed && andPassCustomFilter(user, target, action, ep);
        }

        User accessUser = theUserStore.getUser(user);
        User targetUser = theUserStore.getUser(targetUserId);
        Department accessUserDept = accessUser.getOwningDept();

        if (BizzDepthEntry.LOCAL.equals(depth)) {
            allowed = accessUserDept.equals(targetUser.getOwningDept());
            if (!allowed) {
                allowed = !ignoreShared && allowViaShare(user, target, action);
            }
            return allowed && andPassCustomFilter(user, target, action, ep);

        } else if (BizzDepthEntry.DEEPDOWN.equals(depth)) {
            if (accessUserDept.equals(targetUser.getOwningDept())) {
                return andPassCustomFilter(user, target, action, ep);
            }

            allowed = accessUserDept.isChildren(targetUser.getOwningDept(), true);
            if (!allowed) {
                allowed = !ignoreShared && allowViaShare(user, target, action);
            }
            return allowed && andPassCustomFilter(user, target, action, ep);
        }

        return false;
    }

    /**
     * 通过共享取得的操作权限（目前只共享了读取权限）
     *
     * @param user
     * @param target 目标记录
     * @param action 权限动作
     * @return
     */
    public boolean allowViaShare(ID user, ID target, Permission action) {
        if (!(action == BizzPermission.READ || action == BizzPermission.UPDATE)) {
            return false;
        }

        // TODO 性能优化

        Entity entity = MetadataHelper.getEntity(target.getEntityCode());
        if (entity.getMainEntity() != null) {
            ID mainId = getMainRecordId(target);
            if (mainId == null) {
                throw new NoRecordFoundException("No record found by detail-id : " + target);
            }

            target = mainId;
            entity = entity.getMainEntity();
        }

        Object[] rights = Application.createQueryNoFilter(
                "select rights from ShareAccess where belongEntity = ? and recordId = ? and shareTo = ?")
                .setParameter(1, entity.getName())
                .setParameter(2, target)
                .setParameter(3, user)
                .unique();
        int rightsMask = rights == null ? 0 : (int) rights[0];
        return (rightsMask & action.getMask()) != 0;
    }

    /**
     * 自定义权限（此逻辑是建立在原权限体系之上，AND 关系）
     *
     * @param user
     * @param target
     * @param action
     * @param ep
     * @return
     * @see RoleBaseQueryFilter#buildCustomFilter(Privileges)
     */
    private boolean andPassCustomFilter(ID user, ID target, Permission action, Privileges ep) {
        if (!ep.getClass().isAssignableFrom(CustomEntityPrivileges.class)) return true;
        if (((CustomEntityPrivileges) ep).getCustomFilter(action) == null) return true;

        // TODO 性能优化

        Entity entity = MetadataHelper.getEntity(target.getEntityCode());
        Filter customFilter = createQueryFilter(user, action);

        String sql = MessageFormat.format("select {0} from {1} where {0} = ''{2}''",
                entity.getPrimaryField().getName(), entity.getName(), target);

        Object hasOne = Application.getQueryFactory().createQuery(sql, customFilter).unique();
        return hasOne != null;
    }

    /**
     * 获取真实的权限实体。如明细的权限依赖主实体
     *
     * @param entity
     * @return
     */
    private int convert2MainEntity(int entity) {
        Entity em = MetadataHelper.getEntity(entity);
        return em.getMainEntity() == null ? entity : em.getMainEntity().getEntityCode();
    }

    /**
     * 转换明细实体的权限。<tt>删除/新建/更新</tt>明细记录，等于修改主实体，因此要转换成<tt>更新</tt>权限
     *
     * @param detailAction
     * @return
     */
    private Permission convert2MainAction(Permission detailAction) {
        if (detailAction == BizzPermission.CREATE || detailAction == BizzPermission.DELETE) {
            return BizzPermission.UPDATE;
        }
        return detailAction;
    }

    /**
     * 根据明细 ID 获取主记录 ID
     *
     * @param detailId
     * @return
     */
    private ID getMainRecordId(ID detailId) {
        Entity entity = MetadataHelper.getEntity(detailId.getEntityCode());
        Field dtmField = MetadataHelper.getDetailToMainField(entity);

        Object[] primary = Application.getQueryFactory().uniqueNoFilter(detailId, dtmField.getName());
        return primary == null ? null : (ID) primary[0];
    }

    /**
     * @param user
     * @return
     */
    private Boolean userAllow(ID user) {
        if (UserHelper.isAdmin(user)) return Boolean.TRUE;
        if (!theUserStore.getUser(user).isActive()) return Boolean.FALSE;
        return null;
    }

    /**
     * 扩展权限
     *
     * @param user
     * @param entry
     * @return
     * @see ZeroPrivileges
     * @see ZeroPermission
     */
    public boolean allow(ID user, ZeroEntry entry) {
        Boolean a = userAllow(user);
        if (a != null) {
            return a;
        }

        Role role = theUserStore.getUser(user).getOwningRole();
        if (RoleService.ADMIN_ROLE.equals(role.getIdentity())) {
            return true;
        }

        if (role.hasPrivileges(entry.name())) {
            return role.getPrivileges(entry.name()).allowed(ZeroPermission.ZERO);
        }
        return entry.getDefaultVal();
    }

    /**
     * 创建基于角色权限的查询过滤器
     *
     * @param user
     * @return
     * @see #createQueryFilter(ID, Permission)
     */
    public Filter createQueryFilter(ID user) {
        return createQueryFilter(user, BizzPermission.READ);
    }

    /**
     * 创建基于角色权限的查询过滤器
     *
     * @param user
     * @param action
     * @return
     */
    public Filter createQueryFilter(ID user, Permission action) {
        User theUser = theUserStore.getUser(user);
        if (theUser.isAdmin()) {
            return RoleBaseQueryFilter.ALLOWED;
        }
        return new RoleBaseQueryFilter(theUser, action);
    }

    // --

    // 权限动作集合
    private static final Permission[] RB_PERMISSIONS = new Permission[] {
            BizzPermission.CREATE,
            BizzPermission.DELETE,
            BizzPermission.UPDATE,
            BizzPermission.READ,
            BizzPermission.ASSIGN,
            BizzPermission.SHARE,
            EntityService.UNSHARE
    };

    /**
     * @param name
     * @return
     */
    public static Permission parse(String name) {
        for (Permission p : RB_PERMISSIONS) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        throw new BizzException("Unknown Permission : " + name);
    }
}
