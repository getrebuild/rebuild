/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.bizz.privileges;

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
import com.rebuild.server.Application;
import com.rebuild.server.helper.cache.NoRecordFoundException;
import com.rebuild.server.helper.cache.RecordOwningCache;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.EntityService;
import com.rebuild.server.service.bizz.RoleService;
import com.rebuild.server.service.bizz.UserService;

/**
 * 实体安全/权限 管理
 * 
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-21
 * 
 * @see Role
 * @see BizzPermission
 * @see BizzDepthEntry
 */
public class SecurityManager {

	final private UserStore theUserStore;
	final private RecordOwningCache theRecordOwningCache;

	/**
	 * @param us
	 * @param roc
	 */
	protected SecurityManager(UserStore us, RecordOwningCache roc) {
		this.theUserStore = us;
		this.theRecordOwningCache = roc;
	}
	
	/**
	 * @param record
	 * @return
	 */
	public ID getOwningUser(ID record) {
		return theRecordOwningCache.getOwningUser(record);
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
		return u.getOwningRole().getPrivileges(convert2MasterEntity(entity));
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
		// CRUD and PlainEntity
		if (action.getMask() <= BizzPermission.READ.getMask() && EasyMeta.valueOf(entity).isPlainEntity()) {
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
		
		// 取消共享与共享公用权限
		if (action == EntityService.UNSHARE) {
			action = BizzPermission.SHARE;
		}
		
		if (MetadataHelper.isSlaveEntity(entity)) {
			// 明细实体不能使用此方法检查创建权限
			// 明细实体创建 = 主实体更新，因此应该检查主实体记录是否有更新权限
			if (action == BizzPermission.CREATE) {
				throw new PrivilegesException("Unsupported checks slave entity : " + entity);
			}
			// 明细无分派/共享
			else if (action == BizzPermission.ASSIGN || action == BizzPermission.SHARE) {
				return false;
			}
			action = convert2MasterAction(action);
		}
		
		Privileges ep = role.getPrivileges(convert2MasterEntity(entity));
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
		// CRUD and PlainEntity
		if (action.getMask() <= BizzPermission.READ.getMask() && EasyMeta.valueOf(target.getEntityCode()).isPlainEntity()) {
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
		
		int entity = target.getEntityCode();
		
		if (action == BizzPermission.READ && MetadataHelper.isBizzEntity(entity)) {
			return true;
		}
		// 用户可修改自己
		if (action == BizzPermission.UPDATE && target.equals(user)) {
			return true;
		}
		
		if (MetadataHelper.isSlaveEntity(entity)) {
			// 明细无分派/共享
			if (action == BizzPermission.ASSIGN || action == BizzPermission.SHARE) {
				return false;
			}
			action = convert2MasterAction(action);
		}
		
		Privileges ep = role.getPrivileges(convert2MasterEntity(entity));
		
		boolean allowed = ep.allowed(action);
		if (!allowed) {
			return false;
		}
		
		final DepthEntry depth = ep.superlative(action);
		
		if (BizzDepthEntry.NONE.equals(depth)) {
			return false;
		} else if (BizzDepthEntry.GLOBAL.equals(depth)) {
			return true;
		}
		
		ID targetUserId = theRecordOwningCache.getOwningUser(target);
		if (targetUserId == null) {
			return false;
		}
		
		if (BizzDepthEntry.PRIVATE.equals(depth)) {
			allowed = user.equals(targetUserId);
			if (!allowed) {
				return allowViaShare(user, target, action);
			}
			return true;
		}
		
		com.rebuild.server.service.bizz.privileges.User accessUser = theUserStore.getUser(user);
		com.rebuild.server.service.bizz.privileges.User targetUser = theUserStore.getUser(targetUserId);
		Department accessUserDept = accessUser.getOwningDept();
		
		if (BizzDepthEntry.LOCAL.equals(depth)) {
			allowed = accessUserDept.equals(targetUser.getOwningDept());
			if (!allowed) {
				return allowViaShare(user, target, action);
			}
			return true;
		} else if (BizzDepthEntry.DEEPDOWN.equals(depth)) {
			if (accessUserDept.equals(targetUser.getOwningDept())) {
				return true;
			}
			
			allowed = accessUserDept.isChildrenAll(targetUser.getOwningDept());
			if (!allowed) {
				return allowViaShare(user, target, action);
			}
			return true;
		}
		
		return false;
	}
	
	/**
	 * 通过共享取得的操作权限（目前只共享了读取权限）
	 * 
	 * @param user
	 * @param target
	 * @param action
	 * @return
	 */
	public boolean allowViaShare(ID user, ID target, Permission action) {
		
		// TODO 目前只共享了读取权限
		// TODO 性能优化-缓存
		
		if (action != BizzPermission.READ) {
			return false;
		}

		Entity entity = MetadataHelper.getEntity(target.getEntityCode());
		if (entity.getMasterEntity() != null) {
			ID masterId = getMasterRecordId(target);
			if (masterId == null) {
				throw new NoRecordFoundException("No record found by slave-id : " + target);
			}
			
			target = masterId;
			entity = entity.getMasterEntity();
		}
		
		Object[] rights = Application.createQueryNoFilter(
				"select rights from ShareAccess where belongEntity = ? and recordId = ? and shareTo = ?")
				.setParameter(1, entity.getName())
				.setParameter(2, target)
				.setParameter(3, user)
				.unique();
		int rightsVal = rights == null ? 0 : (int) rights[0];
		return (rightsVal & BizzPermission.READ.getMask()) != 0;
	}

	/**
	 * 获取真实的权限实体。如明细的权限依赖主实体
	 *
	 * @param entity
	 * @return
	 */
	private int convert2MasterEntity(int entity) {
		Entity em = MetadataHelper.getEntity(entity);
		return em.getMasterEntity() == null ? entity : em.getMasterEntity().getEntityCode();
	}
	
	/**
	 * 转换明细实体的权限。<tt>删除/新建/更新</tt>明细记录，等于修改主实体，因此要转换成<tt>更新</tt>权限
	 * 
	 * @param slaveAction
	 * @return
	 */
	private Permission convert2MasterAction(Permission slaveAction) {
		if (slaveAction == BizzPermission.CREATE || slaveAction == BizzPermission.DELETE) {
			return BizzPermission.UPDATE;
		}
		return slaveAction;
	}
	
	/**
	 * 根据明细 ID 获取主记录 ID
	 * 
	 * @param slaveId
	 * @return
	 */
	private ID getMasterRecordId(ID slaveId) {
		Entity entity = MetadataHelper.getEntity(slaveId.getEntityCode());
		Field stmField = MetadataHelper.getSlaveToMasterField(entity);
		
		Object[] primary = Application.getQueryFactory().uniqueNoFilter(slaveId, stmField.getName());
		return primary == null ? null : (ID) primary[0];
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
	 * @param user
	 * @returny
	 */
	private Boolean userAllow(ID user) {
		if (UserService.ADMIN_USER.equals(user)) {
			return true;
		}
		if (!theUserStore.getUser(user).isActive()) {
			return false;
		}
		return null;
	}
	
	/**
	 * 创建查询过滤器
	 * 
	 * @param user
	 * @return
	 */
	public Filter createQueryFilter(ID user) {
		return createQueryFilter(user, BizzPermission.READ);
	}
	
	/**
	 * 创建查询过滤器
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
}
