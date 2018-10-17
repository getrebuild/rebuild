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

package com.rebuild.server.bizz.privileges;

import java.lang.reflect.Method;
import java.security.Guard;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.base.BulkContext;
import com.rebuild.server.service.base.GeneralEntityService;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 权限验证 - 拦截 Service 方法
 * 
 * @author devezhao
 * @since 10/12/2018
 */
public class PrivilegesGuardInterceptor implements MethodInterceptor, Guard {
	
//	private static final Log LOG = LogFactory.getLog(PrivilegesGuardInterceptor.class);

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
				throw new IllegalArgumentException("First argument must be BulkContext");
			}
			
			BulkContext context = (BulkContext) first;
			ID caller = Application.currentCallerUser();
			
			if (!Application.getSecurityManager().allowed(caller, context.getMainEntity().getEntityCode(), context.getAction())) {
				throw new AccessDeniedException(
			    		"User [ " + caller + " ] not allowed execute action [ " + context.getAction() + " ]. Entity : " + context.getMainEntity());
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
			throw new IllegalArgumentException("First argument must be Record/ID");
		}
		
		ID caller = Application.currentCallerUser();
		Permission action = getPermissionByMethod(invocation.getMethod(), recordId == null);
		
		boolean isAllowed = false;
		if (action == BizzPermission.CREATE) {
			isAllowed = Application.getSecurityManager().allowed(caller, entity.getEntityCode(), action);
		} else {
			if (recordId == null) {
			    throw new IllegalArgumentException("No primary in record!");
			}
			
			isAllowed = Application.getSecurityManager().allowed(caller, recordId, action);
		}
		
		if (!isAllowed) {
		    throw new AccessDeniedException(
		    		"User [ " + caller + " ] not allowed execute action [ " + action + " ]. " + (recordId == null ? "Entity : " + entity : "Record : " + recordId));
		}
	}
	
	/**
	 * @param method
	 * @return
	 */
	protected Permission getPermissionByMethod(Method method, boolean isNew) {
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
		}
		return null;
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
				|| act.startsWith("assign") || act.startsWith("share")
				|| act.startsWith("bulk");
	}
}
