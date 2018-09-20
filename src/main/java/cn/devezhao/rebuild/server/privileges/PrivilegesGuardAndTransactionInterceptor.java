/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cn.devezhao.rebuild.server.privileges;

import java.lang.reflect.Method;
import java.security.Guard;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.metadata.MetadataHelper;

/**
 * 执行 CRUD 方法时做出权限拦截
 * 
 * @author Zhao Fangfang
 * @version $Id: PrivilegesGuardAndTransactionInterceptor.java 546 2013-10-22 09:14:55Z zhaoff@qidapp.com $
 * @since 1.0, 2013-6-21
 */
public class PrivilegesGuardAndTransactionInterceptor extends TransactionInterceptor implements Guard {
	private static final long serialVersionUID = -2995255439499512211L;

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		checkGuard(invocation);
		return super.invoke(invocation);
	}

	@Override
	public void checkGuard(Object object) throws SecurityException {
		MethodInvocation invocation = (MethodInvocation) object;
		if (!isNeedCheck(invocation)) {
		    return;
		}
		
		Object idOrRecord = invocation.getArguments()[0];
		if (!(idOrRecord instanceof Record || idOrRecord instanceof ID)) {
		    throw new IllegalArgumentException("Arguments[0] must be Record or ID object!");
		}
		
		ID recordId = null;
		Entity entity = null;
		ID caller = null;
		if (idOrRecord instanceof Record) {
			recordId = ((Record) idOrRecord).getPrimary();
			entity = ((Record) idOrRecord).getEntity();
			caller = ((Record) idOrRecord).getEditor();
		} else {
			recordId = (ID) idOrRecord;
			entity = MetadataHelper.getEntity(recordId.getEntityCode());
		}
		
		if (!EntityHelper.hasPrivilegesField(entity)) {
		    return;
		}
		// 当前会话用户
		if (caller == null) {
			caller = Application.getCurrentCallerUser();
		}
		
		final Permission permission = getPermissionByMethod(invocation.getMethod(), recordId == null);
		boolean allow = false;
		if (permission == BizzPermission.CREATE) {
			allow = Application.getSecurityManager().allowed(caller, entity.getEntityCode(), permission);
		} else {
			if (recordId == null) {
			    throw new IllegalArgumentException("No primary in record!");
			}
			
			ID target = Application.getSecurityManager().getOwnUser(recordId);
			allow = Application.getSecurityManager().allowed(caller, entity.getEntityCode(), permission, target);
//			if (!allow && entity.getEntityCode() <= 12/* 权限实体 */) {
//			    allow = Application.getSecurityManager().allowedBizz(caller, entity.getEntityCode(), recordId);
//			}
		}
		
		if (!allow) {
		    throw new AccessDeniedException(
		    		"User [ " + caller + " ] not allowed execute action [ " + permission + " ]. "
		    		+ (recordId == null ? "Entity : " + entity.getName() : "ID : " + recordId));
		}
	}
	
	/**
	 * @param invocation
	 * @return
	 */
	private boolean isNeedCheck(MethodInvocation invocation) {
		// 只用事务，不检查权限
		String mName = invocation.getMethod().getName();
		if (mName.startsWith("tx") || mName.startsWith("noGuard")) {
			return false;
		}
		
		Class<?> targetClass = (invocation.getThis() != null ? invocation.getThis().getClass() : null);
		TransactionAttribute txAttr =
				getTransactionAttributeSource().getTransactionAttribute(invocation.getMethod(), targetClass);
		return (txAttr != null);
	}
	
	/**
	 * @param method
	 * @return
	 */
	private Permission getPermissionByMethod(Method method, boolean isNew) {
		String mName = method.getName();
		
		if (mName.startsWith("createOrUpdate")) {
			return isNew ? BizzPermission.CREATE : BizzPermission.UPDATE;
		} else if (mName.startsWith("create")) {
		    return BizzPermission.CREATE;
		} else if (mName.startsWith("update")) {
		    return BizzPermission.UPDATE;
		} else if (mName.startsWith("delete")) {
		    return BizzPermission.DELETE;
		}
		throw new PrivilegesException("Illegal method [ " + method + " ] on Permission give!");
	}
}
