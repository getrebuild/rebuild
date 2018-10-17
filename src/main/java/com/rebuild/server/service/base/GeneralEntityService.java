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

package com.rebuild.server.service.base;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.Application;
import com.rebuild.server.bizz.privileges.PrivilegesGuardInterceptor;
import com.rebuild.server.bizz.privileges.User;
import com.rebuild.server.helper.BulkTaskExecutor;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.BaseService;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 业务实体用
 * - 会带有系统设置规则的执行，详见 {@link PrivilegesGuardInterceptor}
 * - 会开启一个事务，详见 <tt>application-ctx.xml</tt> 配置
 * 
 * @author zhaofang123@gmail.com
 * @since 11/06/2017
 */
public class GeneralEntityService extends BaseService {
	
	private static final Log LOG = LogFactory.getLog(GeneralEntityService.class);
	
	protected GeneralEntityService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	/**
	 * 此服务类所属实体
	 * 
	 * @return
	 */
	public int getEntityCode() {
		return 0;
	}
	
	/**
	 * 获取级联操作记录
	 * 
	 * @param main 主记录
	 * @param cascades 级联实体
	 * @param action 动作
	 * @return
	 */
	public Map<String, Set<ID>> getCascadeRecords(ID main, String[] cascades, Permission action) {
		if (cascades == null || cascades.length == 0) {
			return Collections.emptyMap();
		}
		
		Map<String, Set<ID>> cascadeRecordsMap = new HashMap<>();
		Entity mainEntity = MetadataHelper.getEntity(main.getEntityCode());
		for (String ref : cascades) {
			Entity refEntity = MetadataHelper.getEntity(ref);
			
			String sql = "select %s from %s where ( ";
			sql = String.format(sql, refEntity.getPrimaryField().getName(), refEntity.getName());
			
			Field[] refToFields = MetadataHelper.getReferenceToFields(mainEntity, refEntity);
			for (Field refToField : refToFields) {
				sql += refToField.getName() + " = '" + main + "' or ";
			}
			sql = sql.substring(0, sql.length() - 4);  // remove last ' or '
			sql += " )";

			// TODO 此处查询权限不对，应该查询能分派的记录，而非可读的记录
			
			Object[][] array = Application.createQuery(sql).array();
			Set<ID> records = new HashSet<>();
			for (Object[] o : array) {
				records.add((ID) o[0]);
			}
			cascadeRecordsMap.put(ref, records);
		}
		return cascadeRecordsMap;
	}
	
	@Override
	public int delete(ID record) {
		return delete(record, null);
	}
	
	/**
	 * 删除，带级联删除选项
	 * 
	 * @param record
	 * @param cascades 级联删除的实体
	 * @return
	 */
	public int delete(ID record, String[] cascades) {
		super.delete(record);
		
		int affected = 1;
		
		Map<String, Set<ID>> cass = getCascadeRecords(record, cascades, BizzPermission.DELETE);
		for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("级联删除 - " + e.getKey() + "/" + e.getValue().size());
			}
			for (ID id : e.getValue()) {
				affected += delete(id, null);
			}
		}
		return affected;
	}
	
	/**
	 * 分派
	 * 
	 * @param record
	 * @param to
	 * @param cascades 级联分派的实体
	 * @return
	 */
	public int assign(ID record, ID to, String[] cascades) {
		ID currentOwn = Application.getRecordOwningCache().getOwningUser(record);
		if (currentOwn.equals(to)) {
			LOG.warn("记录所属人未变化，忽略本次操作 : " + record);
			return 1;
		}
		
		User toUser = Application.getUserStore().getUser(to);
		
		Record assign = EntityHelper.forUpdate(record, (ID) toUser.getIdentity());
		assign.setID(EntityHelper.owningUser, (ID) toUser.getIdentity());
		assign.setID(EntityHelper.owningDept, (ID) toUser.getOwningDept().getIdentity());
		super.update(assign);
		
		int affected = 1;
		
		Map<String, Set<ID>> cass = getCascadeRecords(record, cascades, BizzPermission.ASSIGN);
		for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("级联分派 - " + e.getKey() + "/" + e.getValue().size());
			}
			
			for (ID id : e.getValue()) {
				affected += assign(id, to, null);
			}
		}
		return affected;
	}
	
	/**
	 * 共享
	 * 
	 * @param record
	 * @param to
	 * @param cascades 级联共享的实体
	 * @return
	 */
	public int share(ID record, ID to, String[] cascades) {
		Object[] exists = aPMFactory.createQuery(
				"select accessId,rights from ShareAccess where recordId = ? and shareTo = ?")
				.setParameter(1, record.toLiteral())
				.setParameter(2, to.toLiteral())
				.unique();
		if (exists != null) {
			LOG.warn("记录已分享过，忽略本次操作 : " + record);
			return 1;
		}
		
		ID currentUser = Application.currentCallerUser();
		
		Record share = EntityHelper.forNew(EntityHelper.ShareAccess, currentUser);
		share.setInt("entity", record.getEntityCode());
		share.setString("recordId", record.toLiteral());
		share.setString("shareTo", to.toLiteral());
		share.setInt("rights", 999);
		super.create(share);
		
		int affected = 1;
		
		Map<String, Set<ID>> cass = getCascadeRecords(record, cascades, BizzPermission.SHARE);
		for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("级联共享 - " + e.getKey() + "/" + e.getValue().size());
			}
			
			for (ID id : e.getValue()) {
				affected += share(id, to, null);
			}
		}
		return affected;
	}
	
	/**
	 * 批量操作
	 * 
	 * @param context
	 * @return
	 */
	public int bulk(BulkContext context) {
		BulkOperator operator = buildBulkOperator(context);
		Object affected = operator.exec();
		return (Integer) affected;
	}
	
	/**
	 * 批量操作-异步
	 * 
	 * @param context
	 * @return
	 * 
	 * @see BulkTaskExecutor
	 */
	public String bulkAsync(BulkContext context) {
		BulkOperator operator = buildBulkOperator(context);
		String taskid = BulkTaskExecutor.submit(operator);
		return taskid;
	}
	
	/**
	 * @param context
	 * @return
	 */
	private BulkOperator buildBulkOperator(BulkContext context) {
		if (context.getAction() == BizzPermission.DELETE) {
			return new BulkDelete(context, this);
		} else if (context.getAction() == BizzPermission.ASSIGN) {
			return new BulkAssign(context, this);
		} else if (context.getAction() == BizzPermission.SHARE) {
			return new BulkShare(context, this);
		}
		throw new UnsupportedOperationException("Unsupported bulk action : " + context.getAction());
	}
}
