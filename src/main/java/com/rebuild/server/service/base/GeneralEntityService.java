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

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Filter;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import com.rebuild.server.business.approval.ApprovalState;
import com.rebuild.server.business.dataimport.DataImporter;
import com.rebuild.server.business.recyclebin.RecycleStore;
import com.rebuild.server.business.series.SeriesGeneratorFactory;
import com.rebuild.server.business.trigger.RobotTriggerObserver;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.cache.NoRecordFoundException;
import com.rebuild.server.helper.task.TaskExecutors;
import com.rebuild.server.metadata.DefaultValueHelper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.BaseService;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.ObservableService;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.bizz.privileges.PrivilegesGuardInterceptor;
import com.rebuild.server.service.bizz.privileges.User;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Observer;
import java.util.Set;

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

	final private PersistManagerFactory aPMFactory;
	
	/**
	 * @param aPMFactory
	 */
	public GeneralEntityService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
		this.aPMFactory = aPMFactory;
	}
	
	/**
	 * @param aPMFactory
	 * @param observers
	 */
	public GeneralEntityService(PersistManagerFactory aPMFactory, List<Observer> observers) {
		this(aPMFactory);

		// 注入观察者（application-ctx.xml）
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
		appendDefaultValue(record);
		checkModifications(record, BizzPermission.CREATE);
		setSeriesValue(record);
		return super.create(record);
	}

	@Override
	public Record update(Record record) {
		if (!checkModifications(record.getPrimary(), BizzPermission.UPDATE)) {
			return record;
		}
		return super.update(record);
	}

	@Override
	public int delete(ID record) {
		return delete(record, null);
	}

	@Override
	public int delete(ID record, String[] cascades) {
		final ID currentUser = Application.getCurrentUser();

		RecycleStore recycleBin = null;
		if (SysConfiguration.getLong(ConfigurableItem.RecycleBinKeepingDays) > 0) {
			recycleBin = new RecycleStore(currentUser);
		}

		if (recycleBin != null) {
			recycleBin.add(record);
		}
		this.deleteInternal(record);
		int affected = 1;

		Map<String, Set<ID>> recordsOfCascaded = getRecordsOfCascaded(record, cascades, BizzPermission.DELETE);
		for (Map.Entry<String, Set<ID>> e : recordsOfCascaded.entrySet()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("级联删除 - " + e.getKey() + " > " + e.getValue());
			}

			for (ID id : e.getValue()) {
				if (Application.getSecurityManager().allowedD(currentUser, id)) {
					if (recycleBin != null) {
						recycleBin.add(id, record);
					}

					int deleted = 0;
					try {
						deleted = this.deleteInternal(id);
					} catch (DataSpecificationException ex) {
						LOG.warn("Couldn't delete : " + id + " Ex : " + ex);
					} finally {
						if (deleted > 0) {
							affected++;
						} else if (recycleBin != null) {
							recycleBin.removeLast();
						}
					}
				} else {
					LOG.warn("No have privileges to DELETE : " + currentUser + " > " + id);
				}
			}
		}

		if (recycleBin != null) {
			recycleBin.store();
		}

		return affected;
	}

	/**
	 * @param record
	 * @return
	 * @throws DataSpecificationException
	 */
	private int deleteInternal(ID record) throws DataSpecificationException {
		if (!checkModifications(record, BizzPermission.DELETE)) {
			return 0;
		}
		return super.delete(record);
	}
	
	@Override
	public int assign(ID record, ID to, String[] cascades) {
		final User toUser = Application.getUserStore().getUser(to);
		final Record assignAfter = EntityHelper.forUpdate(record, (ID) toUser.getIdentity(), false);
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
			
			this.delegate.update(assignAfter);
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
			this.delegate.create(sharedAfter);
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
		
		this.delegate.delete(accessId);
		
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
			throw ex;
		} catch (Exception ex) {
			throw new RebuildException(ex);
		}
	}
	
	@Override
	public String bulkAsync(BulkContext context) {
		BulkOperator operator = buildBulkOperator(context);
		return TaskExecutors.submit(operator);
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
			
			StringBuilder sql = new StringBuilder(
					String.format("select %s from %s where ( ", casEntity.getPrimaryField().getName(), casEntity.getName()));

			Field[] reftoFields = MetadataHelper.getReferenceToFields(mainEntity, casEntity);
			for (Field field : reftoFields) {
				sql.append(field.getName()).append(" = '").append(recordMaster).append("' or ");
			}
			// remove last ' or '
			sql.replace(sql.length() - 4, sql.length(), " )");

			Filter filter = Application.getSecurityManager().createQueryFilter(Application.getCurrentUser(), action);
			Object[][] array = Application.getQueryFactory().createQuery(sql.toString(), filter).array();
			
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
	 * 系统相关约束检查。此方法有 3 种结果：
	 * 1. true - 检查通过
	 * 2. false - 检查不通过，但可以忽略的错误（如删除一条不存在的记录）
	 * 3. 抛出异常 - 不可忽略的错误
	 *
	 * @param recordId
	 * @param action [UPDATE|DELDETE]
	 * @return
	 * @throws DataSpecificationException
	 *
	 * @see #checkModifications(Record, Permission)
	 */
	protected boolean checkModifications(ID recordId, Permission action) throws DataSpecificationException {
		final Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());

		// 验证审批状态
		// 需要检查主实体
		Entity checkEntity = entity.getMasterEntity() != null ? entity.getMasterEntity() : entity;
		if (checkEntity.containsField(EntityHelper.ApprovalId)) {
			// 需要验证主记录
			String recordType = StringUtils.EMPTY;
			if (entity.getMasterEntity() != null) {
				recordId = getMasterId(entity, recordId);
				recordType = "主";
			}

			ApprovalState state;
			try {
				state = getApprovalState(recordId);
			} catch (NoRecordFoundException ignored) {
				return false;
			}

			if (state == ApprovalState.APPROVED || state == ApprovalState.PROCESSING) {
				String actionType = action == BizzPermission.UPDATE ? "修改" : "删除";
				String stateType = state == ApprovalState.APPROVED ? "已完成审批" : "正在审批中";
				if (RobotTriggerObserver.getTriggerSource() != null) {
					recordType = "关联" + recordType;
				}
				throw new DataSpecificationException(recordType + "记录" + stateType + "，不能" + actionType);
			}
		}

		return true;
	}

	/**
	 * 系统相关约束检查
	 *
	 * @param newRecord
	 * @param action [CREATE]
	 * @return
	 * @throws DataSpecificationException
	 */
	protected boolean checkModifications(Record newRecord, Permission action) throws DataSpecificationException {
		final Entity entity = newRecord.getEntity();

		// 验证审批状态
		// 验证新建明细（相当于更新主记录）
		Entity masterEntity = entity.getMasterEntity();
		if (masterEntity != null && masterEntity.containsField(EntityHelper.ApprovalId)) {
			Field smt = MetadataHelper.getSlaveToMasterField(entity);
			ApprovalState state = getApprovalState(newRecord.getID(Objects.requireNonNull(smt).getName()));
			if (state == ApprovalState.APPROVED || state == ApprovalState.PROCESSING) {
				String stateType = state == ApprovalState.APPROVED ? "已完成审批" : "正在审批中";
				throw new DataSpecificationException("主记录" + stateType + "，不能添加明细");
			}
		}

		return true;
	}

	/**
	 * 为 {@link Record} 补充默认值
	 *
	 * @param recordOfNew
	 */
	protected void appendDefaultValue(Record recordOfNew) {
		Assert.isNull(recordOfNew.getPrimary(), "Must be new record");

		Entity entity = recordOfNew.getEntity();
		if (MetadataHelper.isBizzEntity(entity.getEntityCode())
				|| !MetadataHelper.hasPrivilegesField(entity)) {
			LOG.warn("Could't append Bizz and non-business entities : " + entity.getName());
			return;
		}

		for (Field field : entity.getFields()) {
			if (MetadataHelper.isCommonsField(field) || recordOfNew.hasValue(field.getName(), true)) {
				continue;
			}

			Object defVal = DefaultValueHelper.exprDefaultValue(field, (String) field.getDefaultValue());
			if (defVal != null) {
				recordOfNew.setObjectValue(field.getName(), defVal);
			}
		}
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

	/**
	 * 获取主记录ID
	 *
	 * @param slaveEntity
	 * @param slaveId
	 * @return
	 * @throws NoRecordFoundException
	 */
	private ID getMasterId(Entity slaveEntity, ID slaveId) throws NoRecordFoundException {
		Field stm = MetadataHelper.getSlaveToMasterField(slaveEntity);
		Object[] o = Application.getQueryFactory().uniqueNoFilter(slaveId, Objects.requireNonNull(stm).getName());
		if (o == null) {
			throw new NoRecordFoundException(slaveId);
		}
		return (ID) o[0];
	}

	/**
	 * 获取审批状态
	 *
	 * @param recordId
	 * @return
	 * @throws NoRecordFoundException
	 */
	private ApprovalState getApprovalState(ID recordId) throws NoRecordFoundException {
		Object[] o = Application.getQueryFactory().uniqueNoFilter(recordId, EntityHelper.ApprovalState);
		if (o == null) {
			throw new NoRecordFoundException(recordId);
		}
		return (ApprovalState) ApprovalState.valueOf((Integer) o[0]);
	}

	/**
	 * 检查/获取重复字段值
	 *
	 * @param record
	 * @return
	 * @throws DataSpecificationException
	 */
	public List<Record> checkRepeated(Record record) throws DataSpecificationException {
		Entity entity = record.getEntity();
		// 仅处理业务实体
		if (!MetadataHelper.hasPrivilegesField(entity)) {
			return Collections.emptyList();
		}

		List<String> norepeatFields = new ArrayList<>();
		for (Iterator<String> iter = record.getAvailableFieldIterator(); iter.hasNext(); ) {
			Field field = entity.getField(iter.next());
			if (field.isRepeatable()
					|| !record.hasValue(field.getName(), false)
					|| MetadataHelper.isCommonsField(field)
					|| EasyMeta.getDisplayType(field) == DisplayType.SERIES) {
				continue;
			}
			norepeatFields.add(field.getName());
		}
		if (norepeatFields.isEmpty()) {
			return Collections.emptyList();
		}

		StringBuilder checkSql = new StringBuilder("select ")
				.append(entity.getPrimaryField().getName()).append(", ")  // 增加一个主键列
				.append(StringUtils.join(norepeatFields.iterator(), ", "))
				.append(" from ")
				.append(entity.getName())
				.append(" where ( ");
		for (String field : norepeatFields) {
			checkSql.append(field).append(" = ? or ");
		}
		checkSql.delete(checkSql.length() - 4, checkSql.length()).append(" )");

		// 排除自己
		if (record.getPrimary() != null) {
			checkSql.append(" and ").append(entity.getPrimaryField().getName())
					.append(" <> ")
					.append(String.format("'%s'", record.getPrimary().toLiteral()));
		}

		Query query = aPMFactory.createQuery(checkSql.toString());

		int index = 1;
		for (String field : norepeatFields) {
			query.setParameter(index++, record.getObjectValue(field));
		}
        return query.setLimit(100).list();
	}
}
