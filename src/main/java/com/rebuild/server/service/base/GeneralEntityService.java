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

import cn.devezhao.bizz.privileges.PrivilegesException;
import com.rebuild.server.business.approval.ApprovalState;
import com.rebuild.server.helper.cache.NoRecordFoundException;
import com.rebuild.server.service.DataSpecificationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import com.rebuild.server.business.dataio.DataImporter;
import com.rebuild.server.business.series.SeriesGeneratorFactory;
import com.rebuild.server.helper.task.TaskExecutors;
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
 * 业务实体服务，所有业务实体都应该使用此类
 * <br>- 有业务验证
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
			LOG.info(this + " add observer : " + o);
		}
	}
	
	@Override
	public int getEntityCode() {
		return 0;
	}
	
	@Override
	public Record create(Record record) {
		checkModifications(record, BizzPermission.CREATE);
		setSeriesValue(record);
		return super.create(record);
	}

	@Override
	public Record update(Record record) {
		checkModifications(record.getPrimary(), BizzPermission.UPDATE);
		return super.update(record);
	}

	@Override
	public int delete(ID record) {
		return this.delete(record, null);
	}
	
	/**
	 * 自动编号
	 * 
	 * @param record
	 */
	private void setSeriesValue(Record record) {
		Field[] seriesFields = MetadataSorter.sortFields(record.getEntity(), DisplayType.SERIES);
		for (Field field : seriesFields) {
			// 导入模式，不强制生成
			if (record.hasValue(field.getName()) && DataImporter.isInImporting()) {
				continue;
			}
			record.setString(field.getName(), SeriesGeneratorFactory.generate(field));
		}
	}
	
	@Override
	public int delete(ID record, String[] cascades) {
		checkModifications(record, BizzPermission.DELETE);
		super.delete(record);
		int affected = 1;
		
		Map<String, Set<ID>> cass = getRecordsOfCascaded(record, cascades, BizzPermission.DELETE);
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
		final Record assignAfter = EntityHelper.forUpdate(record, (ID) toUser.getIdentity());
		assignAfter.setID(EntityHelper.OwningUser, (ID) toUser.getIdentity());
		assignAfter.setID(EntityHelper.OwningDept, (ID) toUser.getOwningDept().getIdentity());
		
		// 分配前数据
		Record assignBefore = null;
		
		int affected = 0;
		if (to.equals(Application.getRecordOwningCache().getOwningUser(record))) {
			// No need to change
			if (LOG.isDebugEnabled()) {
				LOG.debug("记录所属人未变化，忽略 : " + record);
			}
		} else {
			assignBefore = countObservers() > 0 ? record(assignAfter) : null;
			
			((BaseService) this.delegate).update(assignAfter);
			Application.getRecordOwningCache().cleanOwningUser(record);
			affected = 1;
		}
		
		Map<String, Set<ID>> cass = getRecordsOfCascaded(record, cascades, BizzPermission.ASSIGN);
		for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("级联分派 - " + e.getKey() + " > " + e.getValue());
			}
			for (ID casid : e.getValue()) {
				affected += assign(casid, to, null);
			}
		}
		
		if (countObservers() > 0 && assignBefore != null) {
			setChanged();
			notifyObservers(OperatingContext.create(Application.getCurrentUser(), BizzPermission.ASSIGN, assignBefore, assignAfter));
		}
		return affected;
	}
	
	@Override
	public int share(ID record, ID to, String[] cascades) {
		final ID currentUser = Application.getCurrentUser();
		final String entityName = MetadataHelper.getEntityName(record);
		
		final Record sharedAfter = EntityHelper.forNew(EntityHelper.ShareAccess, currentUser);
		sharedAfter.setID("recordId", record);
		sharedAfter.setID("shareTo", to);
		sharedAfter.setString("belongEntity", entityName);
		sharedAfter.setInt("rights", BizzPermission.READ.getMask());
		
		Object[] hasShared = ((BaseService) this.delegate).getPMFactory().createQuery(
				"select accessId from ShareAccess where belongEntity = ? and recordId = ? and shareTo = ?")
				.setParameter(1, entityName)
				.setParameter(2, record)
				.setParameter(3, to)
				.unique();
		
		int affected = 0;
		boolean shareChange = false;
		if (hasShared != null) {
			// No need to change
			if (LOG.isDebugEnabled()) {
				LOG.debug("记录已共享过，忽略 : " + record);
			}
		} else if (to.equals(Application.getRecordOwningCache().getOwningUser(record))) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("共享至与记录所属为同一用户，忽略 : " + record);
			}
		} else {
			((BaseService) this.delegate).create(sharedAfter);
			affected = 1;
			shareChange = true;
		}
		
		Map<String, Set<ID>> cass = getRecordsOfCascaded(record, cascades, BizzPermission.SHARE);
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
			notifyObservers(OperatingContext.create(currentUser, BizzPermission.SHARE, null, sharedAfter));
		}
		return affected;
	}
	
	@Override
	public int unshare(ID record, ID accessId) {
		ID currentUser = Application.getCurrentUser();
		
		Record unsharedBefore = null;
		if (countObservers() > 0) {
			unsharedBefore = EntityHelper.forUpdate(accessId, currentUser);
			unsharedBefore.setNull("belongEntity");
			unsharedBefore.setNull("recordId");
			unsharedBefore.setNull("shareTo");
			unsharedBefore = record(unsharedBefore);
		}
		
		((BaseService) this.delegate).delete(accessId);
		
		if (countObservers() > 0) {
			setChanged();
			notifyObservers(OperatingContext.create(currentUser, UNSHARE, unsharedBefore, null));
		}
		return 1;
	}
	
	@Override
	public int bulk(BulkContext context) {
		BulkOperator operator = buildBulkOperator(context);
		try {
			return operator.exec();
		} catch (RebuildException ex) {
			throw (RebuildException) ex;
		} catch (Exception ex) {
			throw new RebuildException(ex);
		}
	}
	
	@Override
	public String bulkAsync(BulkContext context) {
		BulkOperator operator = buildBulkOperator(context);
		String taskid = TaskExecutors.submit(operator);
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
	protected Map<String, Set<ID>> getRecordsOfCascaded(ID recordMaster, String[] cascadeEntities, Permission action) {
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
	 * 构造批处理操作
	 *
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

	/**
	 * 检查是否可更改
	 *
	 * @param recordId
	 * @param action [UPDATE|DELDETE]
	 * @return
	 * @throws DataSpecificationException
	 */
	public boolean checkModifications(ID recordId, Permission action)
			throws DataSpecificationException {
		Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
		Entity checkEntity = entity.getMasterEntity() != null ? entity.getMasterEntity() : entity;

		// 验证审批状态
		if (checkEntity.containsField(EntityHelper.ApprovalId)) {
			// 需要验证主记录
			String masterType = "";
			if (entity.getMasterEntity() != null) {
				recordId = getMasterId(entity, recordId);
				entity = entity.getMasterEntity();
				masterType = "主";
			}

			ApprovalState state = getApprovalState(recordId);
			String actionType = action == BizzPermission.UPDATE ? "修改" : "删除";
			if (state == ApprovalState.APPROVED) {
				throw new DataSpecificationException(masterType + "记录已完成审批，不能" + actionType);
			} else if (state == ApprovalState.PROCESSING) {
				throw new DataSpecificationException(masterType + "记录正在审批中，不能" + actionType);
			}
		}

		return true;
	}

	/**
	 * 检查是否可更改
	 *
	 * @param newRecord
	 * @param action [CREATE]
	 * @return
	 * @throws DataSpecificationException
	 */
	public boolean checkModifications(Record newRecord, Permission action)
			throws DataSpecificationException {
		Entity entity = newRecord.getEntity();

		// 验证审批状态
		// 验证新建明细（相当于更新主记录）
		Entity masterEntity = entity.getMasterEntity();
		if (masterEntity != null && masterEntity.containsField(EntityHelper.ApprovalId)) {
			Field stmField = MetadataHelper.getSlaveToMasterField(entity);
			ID masterId = newRecord.getID(stmField.getName());

			ApprovalState state = getApprovalState(masterId);
			if (state == ApprovalState.APPROVED || state == ApprovalState.PROCESSING) {
				String stateType = state == ApprovalState.APPROVED ? "已完成审批" : "正在审批中";
				throw new DataSpecificationException("主记录" + stateType + "，不能添加明细");
			}
		}

		return true;
	}

	/**
	 * @param slaveEntity
	 * @param slaveId
	 * @return
	 */
	private ID getMasterId(Entity slaveEntity, ID slaveId) {
		Field stmField = MetadataHelper.getSlaveToMasterField(slaveEntity);
		String sql = String.format("select %s from %s where %s = ?",
				stmField.getName(), slaveEntity.getName(), slaveEntity.getPrimaryField().getName());
		Object o[] = Application.createQueryNoFilter(sql).setParameter(1, slaveId).unique();
		if (o == null) {
			throw new NoRecordFoundException(slaveId);
		}
		return (ID) o[0];
	}

	/**
	 * @param recordId
	 * @return
	 */
	private ApprovalState getApprovalState(ID recordId) {
		if (recordId == null) {
			throw new NoRecordFoundException();
		}

		Object[] o = Application.getQueryFactory().unique(recordId, EntityHelper.ApprovalState);
		if (o == null) {
			throw new NoRecordFoundException(recordId);
		}
		return (ApprovalState) ApprovalState.valueOf((Integer) o[0]);
	}
}
