/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.configuration;

import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.ClassificationManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.base.QuickCodeReindexTask;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.bizz.privileges.AdminGuard;
import org.apache.commons.lang.StringUtils;

/**
 * 分类数据
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/10
 */
public class ClassificationService extends ConfigurationService implements AdminGuard {

	protected ClassificationService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	@Override
	public int getEntityCode() {
		return EntityHelper.Classification;
	}
	
	@Override
	public int delete(ID recordId) {
		// 检查是否被使用
		Object[][] used = Application.createQueryNoFilter(
				"select extConfig from MetaField where displayType = 'CLASSIFICATION'")
				.array();
		for (Object[] o : used) {
			if (StringUtils.contains((String) o[0], recordId.toLiteral())) {
				throw new DataSpecificationException("此分类数据正在被使用，不能删除");
			}
		}
		
		return super.delete(recordId);
	}
	
	@Override
	protected void cleanCache(ID configId) {
		ClassificationManager.instance.clean(configId);
	}
	
	// -- for DataItem
	
	/**
	 * @param record
	 * @return
	 */
	public Record createOrUpdateItem(Record record) {
		boolean reindex = setFullNameValue(record);
		// New
		if (record.getPrimary() == null) {
			return this.create(record);
		}
		
		// Update
		record = super.update(record);
		if (reindex) {
			final ID itemId = record.getPrimary();
			cleanCache(itemId);
			final long start = System.currentTimeMillis();
			ThreadPool.exec(() -> {
				try {
					reindexFullNameByParent(itemId);
				} finally {
					long cost = System.currentTimeMillis() - start;
					if (cost > 2000 || Application.devMode()) {
						LOG.info("Reindex FullName [ " + itemId + " ] in " + cost + " ms");
					}
				}
			});
		}
		return record;
	}

	/**
	 * @param itemId
	 */
	public void deleteItem(ID itemId) {
		super.delete(itemId);
		this.cleanCache(itemId);
	}

	/**
	 * 补充 fullName
	 * 
	 * @param record
	 * @return
	 */
	private boolean setFullNameValue(Record record) {
		if (record.hasValue("fullName") || !record.hasValue("name")) {
			return false;
		}
		
		String fullName = record.getString("name");
		String quickCode = QuickCodeReindexTask.generateQuickCode(fullName);
		ID parent = record.getID("parent");
		if (parent == null && record.getPrimary() != null) {
			Object[] o = Application.createQueryNoFilter(
					"select parent from ClassificationData where itemId = ?")
					.setParameter(1, record.getPrimary())
					.unique();
			parent = o == null ? null : (ID) o[0];
		}
		
		if (parent != null) {
			fullName = ClassificationManager.instance.getFullName(parent) + "." + fullName;
		}
		record.setString("fullName", fullName);
		record.setString("quickCode", quickCode);
		return true;
	}
	
	/**
	 * 重建子级 fullName
	 * 
	 * @param parent
	 * @return
	 * @see #reindexFullNameByParent(ID, ID)
	 */
	protected int reindexFullNameByParent(ID parent) {
		Object[] data = Application.createQueryNoFilter(
				"select dataId from ClassificationData where itemId = ?")
				.setParameter(1, parent)
				.unique();
		if (data == null) {
			return 0;
		}
		return reindexFullNameByParent(parent, (ID) data[0]);
	}
	
	/**
	 * 重建子级 fullName
	 * 
	 * @param parent
	 * @param dataId 可选。但指定此值处理效率较高
	 * @return
	 */
	protected int reindexFullNameByParent(ID parent, ID dataId) {
		String sql = "select itemId,name,parent from ClassificationData where parent = ?";
		if (dataId != null) {
			sql += " and dataId = '" + dataId + "'";
		}
		Object[][] array = Application.createQueryNoFilter(sql)
				.setParameter(1, parent)
				.array();

		int reindex = 0;
		for (Object[] c : array) {
			ID itemId = (ID) c[0];
			String fullName = (String) c[1];
			String quickCode = QuickCodeReindexTask.generateQuickCode(fullName);
			if (c[2] != null) {
				String pfn = ClassificationManager.instance.getFullName((ID) c[2]);
				fullName = pfn + "." + fullName;
			}
			Record record = EntityHelper.forUpdate(itemId, UserService.SYSTEM_USER, false);
			record.setString("fullName", fullName);
			record.setString("quickCode", quickCode);
			super.update(record);
			reindex++;
			
			cleanCache(itemId);
			reindex += reindexFullNameByParent(itemId, dataId);
		}
		return reindex;
	}
}
