/*
rebuild - Building your system freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.service.bizz.privileges;

import java.lang.reflect.Method;
import java.security.Guard;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.server.service.EntityService;
import com.rebuild.server.service.base.BulkContext;
import com.rebuild.server.service.base.GeneralEntityService;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 权限验证 - 拦截 Service 方法
 * 
 * @author devezhao
 * @since 10/12/2018
 */
public class PrivilegesGuardInterceptor implements MethodInterceptor, Guard {
	
	private static final Log LOG = LogFactory.getLog(PrivilegesGuardInterceptor.class);

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		checkGuard(invocation);
		return invocation.proceed();
	}
	
	@Override
	public void checkGuard(Object object) throws SecurityException {
		MethodInvocation invocation = (MethodInvocation) object;
		if (!isNeedCheck(invocation)) {
			return;
		}
		
		boolean isBulk = invocation.getMethod().getName().startsWith("bulk");
		if (isBulk) {
			Object first = invocation.getArguments()[0];
			if (!(first instanceof BulkContext)) {
				throw new IllegalArgumentException("First argument must be BulkContext!");
			}
			
			BulkContext context = (BulkContext) first;
			ID caller = Application.getCurrentUser();
			
			Entity entity = context.getMainEntity();
			if (!Application.getSecurityManager().allowed(caller, entity.getEntityCode(), context.getAction())) {
				LOG.error("User [ " + caller + " ] not allowed execute action [ " + context.getAction() + " ]. Entity : " + context.getMainEntity());
				throw new AccessDeniedException(formatHumanMessage(context.getAction(), entity, null));
			}
			return;
		}
		
		Object idOrRecord = invocation.getArguments()[0];
		
		ID recordId = null;
		Entity entity = null;
		
		if (idOrRecord instanceof Record) {
			recordId = ((Record) idOrRecord).getPrimary();
			entity = ((Record) idOrRecord).getEntity();
		} else if (idOrRecord instanceof ID) {
			recordId = (ID) idOrRecord;
			entity = MetadataHelper.getEntity(recordId.getEntityCode());
		} else {
			throw new IllegalArgumentException("First argument must be Record/ID!");
		}
		
		ID caller = Application.getCurrentUser();
		Permission action = getPermissionByMethod(invocation.getMethod(), recordId == null);
		
		boolean isAllowed = false;
		if (action == BizzPermission.CREATE) {
			// 明细实体
			if (entity.getMasterEntity() != null) {
				Field field = MetadataHelper.getSlaveToMasterField(entity);
				ID masterId = ((Record) idOrRecord).getID(field.getName());
				if (masterId == null || !Application.getSecurityManager().allowedU(caller, masterId)) {
					throw new AccessDeniedException("你没有添加明细的权限");
				}
				isAllowed = true;
			} else {
				isAllowed = Application.getSecurityManager().allowed(caller, entity.getEntityCode(), action);
			}
			
		} else {
			if (recordId == null) {
			    throw new IllegalArgumentException("No primary in record!");
			}
			
			isAllowed = Application.getSecurityManager().allowed(caller, recordId, action);
		}
		
		if (!isAllowed) {
			LOG.error("User [ " + caller + " ] not allowed execute action [ " + action + " ]. " + (recordId == null ? "Entity : " + entity : "Record : " + recordId));
		    throw new AccessDeniedException(formatHumanMessage(action, entity, recordId));
		}
	}
	
	/**
	 * @param invocation
	 * @return
	 */
	protected boolean isNeedCheck(MethodInvocation invocation) {
		// 仅 GeneralEntityService 或其子类
		if (!GeneralEntityService.class.isAssignableFrom(invocation.getThis().getClass())) {
			return false;
		}
		
		String act = invocation.getMethod().getName();
		return act.startsWith("create") || act.startsWith("update") || act.startsWith("delete") 
				|| act.startsWith("assign") || act.startsWith("share") || act.startsWith("unshare")
				|| act.startsWith("bulk");
	}
	
	/**
	 * @param method
	 * @return
	 */
	private Permission getPermissionByMethod(Method method, boolean isNew) {
		String action = method.getName();
		if (action.startsWith("createOrUpdate")) {
			return isNew ? BizzPermission.CREATE : BizzPermission.UPDATE;
		} else if (action.startsWith("create")) {
		    return BizzPermission.CREATE;
		} else if (action.startsWith("update")) {
		    return BizzPermission.UPDATE;
		} else if (action.startsWith("delete")) {
		    return BizzPermission.DELETE;
		} else if (action.startsWith("assign")) {
		    return BizzPermission.ASSIGN;
		} else if (action.startsWith("share")) {
		    return BizzPermission.SHARE;
		} else if (action.startsWith("unshare")) {
		    return EntityService.UNSHARE;
		}
		return null;
	}
	
	/**
	 * @param action
	 * @param entity
	 * @param target
	 * @return
	 */
	private String formatHumanMessage(Permission action, Entity entity, ID target) {
		String actionHuman = null;
		if (action == BizzPermission.CREATE) {
			actionHuman = "新建";
		} else if (action == BizzPermission.UPDATE) {
			actionHuman = "修改";
		} else if (action == BizzPermission.DELETE) {
			actionHuman = "删除";
		} else if (action == BizzPermission.ASSIGN) {
			actionHuman = "分派";
		} else if (action == BizzPermission.SHARE) {
			actionHuman = "共享";
		} else if (action == EntityService.UNSHARE) {
			actionHuman = "取消共享";
		}
		
		if (target == null) {
			return String.format("你没有%s%s权限", actionHuman, EasyMeta.getLabel(entity));
		}
		return String.format("你没有%s此记录的权限", actionHuman);
	}
}
