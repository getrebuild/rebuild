/*
rebuild - Building your business-systems freely.
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
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.Application;
import com.rebuild.server.business.series.SeriesGeneratorFactory;
import com.rebuild.server.helper.task.BulkTaskExecutor;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.service.BaseService;
import com.rebuild.server.service.ObservableService;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.bizz.privileges.PrivilegesGuardInterceptor;
import com.rebuild.server.service.bizz.privileges.User;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Filter;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 业务实体服务
 * <br>- 会带有系统设置规则的执行，详见 {@link PrivilegesGuardInterceptor}
 * <br>- 会开启一个事务，详见 <tt>application-ctx.xml</tt> 配置
 * 
 * @author zhaofang123@gmail.com
 * @since 11/06/2017
 */
public class GeneralEntityService extends ObservableService  {
	
	private static final Log LOG = LogFactory.getLog(GeneralEntityService.class);
	
	/**
	 * @param aPMFactory
	 */
	public GeneralEntityService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	/**
	 * @param aPMFactory
	 * @param observers
	 */
	public GeneralEntityService(PersistManagerFactory aPMFactory, List<Observer> observers) {
		super(aPMFactory);
		
		// 注入观察者
		for (Observer o : observers) {
			addObserver(o);
		}
	}
	
	@Override
	public int getEntityCode() {
		return 0;
	}
	
	@Override
	public Record create(Record record) {
		setSeriesValue(record);
		setQuickCodeValue(record);
		return super.create(record);
	}
	
	@Override
	public Record update(Record record) {
		setQuickCodeValue(record);
		return super.update(record);
	}
	
	@Override
	public int delete(ID record) {
		return delete(record, null);
	}
	
	/**
	 * 助记码
	 * 
	 * @param record
	 */
	protected void setQuickCodeValue(Record record) {
		// 已设置了则不再设置
		if (record.hasValue(EntityHelper.QuickCode)) {
			return;
		}
		
		String quickCode = QuickCodeReindexTask.generateQuickCode(record);
		if (quickCode != null) {
			record.setString(EntityHelper.QuickCode, quickCode);
		}
	}
	
	/**
	 * 自动编号
	 * 
	 * @param record
	 */
	protected void setSeriesValue(Record record) {
		Field[] seriesFields = MetadataSorter.sortFields(record.getEntity(), DisplayType.SERIES);
		for (Field field : seriesFields) {
			// 导入
			if (record.hasValue(field.getName())) {
				continue;
			}
			
			record.setString(field.getName(), SeriesGeneratorFactory.generate(field));
		}
	}
	
	@Override
	public int delete(ID record, String[] cascades) {
		super.delete(record);
		int affected = 1;
		
		Map<String, Set<ID>> cass = getCascadeRecords(record, cascades, BizzPermission.DELETE);
		for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("级联删除 - " + e.getKey() + " > " + e.getValue());
			}
			for (ID casid : e.getValue()) {
				affected += (super.delete(casid) > 0 ? 1 : 0);
			}
		}
		return affected;
	}
	
	@Override
	public int assign(ID record, ID to, String[] cascades) {
		final User toUser = Application.getUserStore().getUser(to);
		final Record assigned = EntityHelper.forUpdate(record, (ID) toUser.getIdentity());
		assigned.setID(EntityHelper.OwningUser, (ID) toUser.getIdentity());
		assigned.setID(EntityHelper.OwningDept, (ID) toUser.getOwningDept().getIdentity());
		
		int affected = 0;
		boolean assignChange = false;
		if (to.equals(Application.getRecordOwningCache().getOwningUser(record))) {
			LOG.warn("记录所属人未变化，忽略 : " + record);
			// 还要继续，因为可能存在关联记录
		} else {
			((BaseService) this.delegate).update(assigned);
			Application.getRecordOwningCache().cleanOwningUser(record);
			affected = 1;
			assignChange = true;
		}
		
		Map<String, Set<ID>> cass = getCascadeRecords(record, cascades, BizzPermission.ASSIGN);
		for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("级联分派 - " + e.getKey() + " > " + e.getValue());
			}
			for (ID casid : e.getValue()) {
				affected += assign(casid, to, null);
			}
		}
		
		if (countObservers() > 0 && assignChange) {
			setChanged();
			Record before = getBeforeRecord(assigned);
			notifyObservers(OperatingContext.create(Application.getCurrentUser(), BizzPermission.ASSIGN, before, assigned));
		}
		return affected;
	}
	
	@Override
	public int share(ID record, ID to, String[] cascades) {
		final ID currentUser = Application.getCurrentUser();
		final String entityName = MetadataHelper.getEntityName(record);
		
		final Record shared = EntityHelper.forNew(EntityHelper.ShareAccess, currentUser);
		shared.setID("recordId", record);
		shared.setID("shareTo", to);
		shared.setString("belongEntity", entityName);
		shared.setInt("rights", BizzPermission.READ.getMask());
		
		Object[] hasShared = ((BaseService) this.delegate).getPMFactory().createQuery(
				"select accessId from ShareAccess where belongEntity = ? and recordId = ? and shareTo = ?")
				.setParameter(1, entityName)
				.setParameter(2, record)
				.setParameter(3, to)
				.unique();
		
		int affected = 0;
		boolean shareChange = false;
		if (hasShared != null) {
			LOG.warn("记录已共享过，忽略 : " + record);
			// 还要继续，因为可能存在关联记录
		} else if (to.equals(Application.getRecordOwningCache().getOwningUser(record))) {
			LOG.warn("共享至与记录所属为同一用户，忽略 : " + record);
		} else {
			((BaseService) this.delegate).create(shared);
			affected = 1;
			shareChange = true;
		}
		
		Map<String, Set<ID>> cass = getCascadeRecords(record, cascades, BizzPermission.SHARE);
		for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("级联共享 - " + e.getKey() + " > " + e.getValue());
			}
			for (ID casid : e.getValue()) {
				affected += share(casid, to, null);
			}
		}
		
		if (countObservers() > 0 && shareChange) {
			setChanged();
			notifyObservers(OperatingContext.create(currentUser, BizzPermission.SHARE, null, shared));
		}
		return affected;
	}
	
	@Override
	public int unshare(ID record, ID accessId) {
		ID currentUser = Application.getCurrentUser();
		Record unshared = EntityHelper.forUpdate(accessId, currentUser);
		if (countObservers() > 0) {
			unshared.setNull("belongEntity");
			unshared.setNull("recordId");
			unshared.setNull("shareTo");
			unshared = getBeforeRecord(unshared);
		}
		
		((BaseService) this.delegate).delete(accessId);
		
		if (countObservers() > 0) {
			setChanged();
			notifyObservers(OperatingContext.create(currentUser, UNSHARE, null, unshared));
		}
		return 1;
	}
	
	@Override
	public int bulk(BulkContext context) {
		BulkOperator operator = buildBulkOperator(context);
		return operator.operate();
	}
	
	@Override
	public String bulkAsync(BulkContext context) {
		BulkOperator operator = buildBulkOperator(context);
		String taskid = BulkTaskExecutor.submit(operator);
		return taskid;
	}
	
	/**
	 * 获取级联操作记录
	 * 
	 * @param recordMaster 主记录
	 * @param cascadeEntities 级联实体
	 * @param action 动作
	 * @return
	 */
	private Map<String, Set<ID>> getCascadeRecords(ID recordMaster, String[] cascadeEntities, Permission action) {
		if (cascadeEntities == null || cascadeEntities.length == 0) {
			return Collections.emptyMap();
		}
		
		Map<String, Set<ID>> entityRecordsMap = new HashMap<>();
		Entity mainEntity = MetadataHelper.getEntity(recordMaster.getEntityCode());
		for (String cas : cascadeEntities) {
			Entity casEntity = MetadataHelper.getEntity(cas);
			
			String sql = "select %s from %s where ( ";
			sql = String.format(sql, casEntity.getPrimaryField().getName(), casEntity.getName());
			
			Field[] reftoFields = MetadataHelper.getReferenceToFields(mainEntity, casEntity);
			for (Field field : reftoFields) {
				sql += field.getName() + " = '" + recordMaster + "' or ";
			}
			sql = sql.substring(0, sql.length() - 4);  // remove last ' or '
			sql += " )";

			Filter filter = Application.getSecurityManager().createQueryFilter(Application.getCurrentUser(), action);
			Object[][] array = Application.getQueryFactory().createQuery(sql, filter).array();
			
			Set<ID> records = new HashSet<>();
			for (Object[] o : array) {
				records.add((ID) o[0]);
			}
			entityRecordsMap.put(cas, records);
		}
		return entityRecordsMap;
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
		} else if (context.getAction() == UNSHARE) {
			return new BulkUnshare(context, this);
		}
		throw new UnsupportedOperationException("Unsupported bulk action : " + context.getAction());
	}
}
