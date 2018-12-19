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
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.stuxuhai.jpinyin.PinyinHelper;
import com.rebuild.server.Application;
import com.rebuild.server.bizz.privileges.PrivilegesGuardInterceptor;
import com.rebuild.server.bizz.privileges.User;
import com.rebuild.server.helper.BulkTaskExecutor;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.server.service.BaseService;
import com.rebuild.server.service.OperateContext;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Filter;
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
public class GeneralEntityService extends BaseService  {
	
	private static final Log LOG = LogFactory.getLog(GeneralEntityService.class);
	
	/** 
	 * 取消共享 */
	public static final Permission UNSHARE  = new BizzPermission("X", 0, true);
	
	/**
	 * @param aPMFactory
	 */
	protected GeneralEntityService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	/**
	 * @param aPMFactory
	 * @param observers
	 */
	protected GeneralEntityService(PersistManagerFactory aPMFactory, List<Observer> observers) {
		super(aPMFactory);
		
		// 注入观察者
		for (Observer o : observers) {
			addObserver(o);
		}
	}
	
	/**
	 * 此服务类所属实体
	 * 
	 * @return
	 */
	public int getEntityCode() {
		return 0;
	}
	
	@Override
	public Record create(Record record) {
		setQuickCodeValue(record);
		return super.create(record);
	}
	
	@Override
	public Record update(Record record) {
		setQuickCodeValue(record);
		return super.update(record);
	}
	
	/**
	 * 设置助记码字段值
	 * 
	 * @param record
	 */
	protected void setQuickCodeValue(Record record) {
		if (!record.getEntity().containsField(EntityHelper.QuickCode)) {
			return;
		}
		
		Field nameField = record.getEntity().getNameField();
		if (!record.hasValue(nameField.getName()) || record.hasValue(EntityHelper.QuickCode)) {
			return;
		}
		
		DisplayType dt = EasyMeta.getDisplayType(nameField);
		if (dt == DisplayType.TEXT) {
			String name = record.getString(nameField.getName());
			String qcode = null;
			if (StringUtils.isNotBlank(name)) {
				try {
					qcode = PinyinHelper.getShortPinyin(name).toUpperCase();
				} catch (Exception e) {
					LOG.error("ShortPinyin : " + name, e);
				}
			}
			
			if (StringUtils.isBlank(qcode)) {
				record.setNull(EntityHelper.QuickCode);
			} else {
				record.setString(EntityHelper.QuickCode, qcode);
			}
		}
	}
	
	/**
	 * 获取级联操作记录
	 * 
	 * @param recordOfMain 主记录
	 * @param cascadeEntities 级联实体
	 * @param action 动作
	 * @return
	 */
	protected Map<String, Set<ID>> getCascadeRecords(ID recordOfMain, String[] cascadeEntities, Permission action) {
		if (cascadeEntities == null || cascadeEntities.length == 0) {
			return Collections.emptyMap();
		}
		
		Map<String, Set<ID>> entityRecordsMap = new HashMap<>();
		Entity mainEntity = MetadataHelper.getEntity(recordOfMain.getEntityCode());
		for (String ref : cascadeEntities) {
			Entity refEntity = MetadataHelper.getEntity(ref);
			
			String sql = "select %s from %s where ( ";
			sql = String.format(sql, refEntity.getPrimaryField().getName(), refEntity.getName());
			
			Field[] refToFields = MetadataHelper.getReferenceToFields(mainEntity, refEntity);
			for (Field refToField : refToFields) {
				sql += refToField.getName() + " = '" + recordOfMain + "' or ";
			}
			sql = sql.substring(0, sql.length() - 4);  // remove last ' or '
			sql += " )";

			Filter filter = Application.getSecurityManager().createQueryFilter(Application.currentCallerUser(), action);
			Object[][] array = Application.getQueryFactory().createQuery(sql, filter).array();
			
			Set<ID> records = new HashSet<>();
			for (Object[] o : array) {
				records.add((ID) o[0]);
			}
			entityRecordsMap.put(ref, records);
		}
		return entityRecordsMap;
	}
	
	@Override
	public int delete(ID record) {
		return delete(record, null);
	}
	
	/**
	 * 删除。带级联删除选项
	 * 
	 * @param record
	 * @param cascades 级联删除的实体
	 * @return
	 */
	public int delete(ID record, String[] cascades) {
		super.delete(record);
		Application.getRecordOwningCache().cleanOwningUser(record);
		
		int affected = 1;
		
		Map<String, Set<ID>> cass = getCascadeRecords(record, cascades, BizzPermission.DELETE);
		for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("级联删除 - " + e.getKey() + " > " + e.getValue());
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
		
		Record assigned = EntityHelper.forUpdate(record, (ID) toUser.getIdentity());
		assigned.setID(EntityHelper.OwningUser, (ID) toUser.getIdentity());
		assigned.setID(EntityHelper.OwningDept, (ID) toUser.getOwningDept().getIdentity());
		
		Record before = countObservers() > 0 ? getBeforeRecord(assigned) : null;
		
		super.update(assigned);
		Application.getRecordOwningCache().cleanOwningUser(record);
		
		int affected = 1;
		
		Map<String, Set<ID>> cass = getCascadeRecords(record, cascades, BizzPermission.ASSIGN);
		for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("级联分派 - " + e.getKey() + " > " + e.getValue());
			}
			
			for (ID id : e.getValue()) {
				affected += assign(id, to, null);
			}
		}
		
		if (countObservers() > 0) {
			setChanged();
			notifyObservers(OperateContext.valueOf(Application.currentCallerUser(), BizzPermission.ASSIGN, before, assigned));
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
		String entityName = MetadataHelper.getEntityName(record);
		Object[] exists = aPMFactory.createQuery(
				"select accessId,rights from ShareAccess where belongEntity = ? and recordId = ? and shareTo = ?")
				.setParameter(1, entityName)
				.setParameter(2, record.toLiteral())
				.setParameter(3, to.toLiteral())
				.unique();
		if (exists != null) {
			LOG.warn("记录已分享过，忽略本次操作 : " + record);
			return 1;
		}
		
		ID currentUser = Application.currentCallerUser();
		
		Record shared = EntityHelper.forNew(EntityHelper.ShareAccess, currentUser);
		shared.setString("belongEntity", entityName);
		shared.setString("recordId", record.toLiteral());
		shared.setString("shareTo", to.toLiteral());
		shared.setInt("rights", BizzPermission.READ.getMask());
		super.create(shared);
		
		int affected = 1;
		
		Map<String, Set<ID>> cass = getCascadeRecords(record, cascades, BizzPermission.SHARE);
		for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("级联共享 - " + e.getKey() + " > " + e.getValue());
			}
			
			for (ID id : e.getValue()) {
				affected += share(id, to, null);
			}
		}
		
		if (countObservers() > 0) {
			setChanged();
			notifyObservers(OperateContext.valueOf(currentUser, BizzPermission.SHARE, null, shared));
		}
		return affected;
	}
	
	/**
	 * TODO 取消共享
	 * 
	 * @param record
	 * @param to
	 * @param cascades 级联共享的实体
	 * @return
	 */
	public int unshare(ID record, ID to, String[] cascades) {
		return 0;
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
		} else if (context.getAction() == UNSHARE) {
			return new BulkDelete(context, this);
		}
		throw new UnsupportedOperationException("Unsupported bulk action : " + context.getAction());
	}
}
