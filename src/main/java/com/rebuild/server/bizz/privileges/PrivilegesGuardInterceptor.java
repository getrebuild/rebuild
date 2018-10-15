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
import org.springframework.util.Assert;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;

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
		
		Object idOrRecord = invocation.getArguments()[0];
		
		ID recordId = null;
		Entity entity = null;
		ID caller = null;
		
		if (idOrRecord instanceof Record) {
			recordId = ((Record) idOrRecord).getPrimary();
			entity = ((Record) idOrRecord).getEntity();
			caller = ((Record) idOrRecord).getEditor();
		} else if (idOrRecord instanceof ID) {
			recordId = (ID) idOrRecord;
			entity = MetadataHelper.getEntity(recordId.getEntityCode());
		} else if (idOrRecord instanceof ID[]) {
			Assert.isTrue(((ID[]) idOrRecord).length > 1, "Bulk must have at least 2 IDs");
			entity = MetadataHelper.getEntity(((ID[]) idOrRecord)[0].getEntityCode());
		} else {
			throw new IllegalArgumentException("First arguments must be Record/ID/ID[]");
		}
		
		// 无权限字段的不检查
		if (!EntityHelper.hasPrivilegesField(entity)) {
		    return;
		}
		
		if (caller == null) {
			// 当前会话用户
			caller = Application.currentCallerUser();
		}
		
		boolean isBulk = invocation.getMethod().getName().startsWith("bulkDelete");
		if (isBulk) {
			boolean deleteAllowed = Application.getSecurityManager().allowed(caller, entity.getEntityCode(), BizzPermission.DELETE);
			if (!deleteAllowed) {
				throw new AccessDeniedException(
			    		"User [ " + caller + " ] not allowed execute action [ " + BizzPermission.DELETE + " ]. Entity : " + entity.getName());
			}
			return;
		}
		
		final Permission permission = getPermissionByMethod(invocation.getMethod(), recordId == null);
		boolean isAllowed = false;
		if (permission == BizzPermission.CREATE) {
			isAllowed = Application.getSecurityManager().allowed(caller, entity.getEntityCode(), permission);
		} else {
			if (recordId == null) {
			    throw new IllegalArgumentException("No primary in record!");
			}
			
			isAllowed = Application.getSecurityManager().allowed(caller, recordId, permission);
		}
		
		if (!isAllowed) {
		    throw new AccessDeniedException(
		    		"User [ " + caller + " ] not allowed execute action [ " + permission.getName() + " ]. "
		    		+ (recordId == null ? "Entity : " + entity.getName() : "Record : " + recordId));
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
		String act = invocation.getMethod().getName();
		return act.startsWith("create") || act.startsWith("update")
				|| act.startsWith("delete") || act.startsWith("assign") || act.startsWith("share")
				|| act.startsWith("bulkDelete");
	}
}
