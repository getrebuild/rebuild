/*
rebuild - Building your system freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.metadata.entityhub;

import org.apache.commons.lang.StringUtils;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.portals.ClassificationManager;
import com.rebuild.server.service.BaseService;
import com.rebuild.server.service.DataSpecificationException;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 分类数据
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/10
 */
public class ClassificationService extends BaseService {

	protected ClassificationService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	@Override
	public Record create(Record record) {
		return super.create(record);
	}
	
	@Override
	public Record update(Record record) {
		record = super.update(record);
		ClassificationManager.cleanCache(record.getPrimary());
		return record;
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
	
	// -- for DataItem
	
	/**
	 * @param record
	 * @return
	 */
	public Record saveItem(Record record) {
		boolean reindex = setFullNameValue(record);
		// New
		if (record.getPrimary() == null) {
			return super.create(record);
		}
		
		// Update
		record = super.update(record);
		if (reindex) {
			ClassificationManager.cleanCache(record.getPrimary());
			reindexFullNameByParent(record.getPrimary());
		}
		return record;
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
		ID parent = record.getID("parent");
		if (parent == null && record.getPrimary() != null) {
			Object[] o = Application.createQueryNoFilter(
					"select parent from ClassificationData where itemId = ?")
					.setParameter(1, record.getPrimary())
					.unique();
			parent = o == null ? null : (ID) o[0];
		}
		
		if (parent != null) {
			fullName = ClassificationManager.getFullName(parent) + "." + fullName;
		}
		record.setString("fullName", fullName);
		return true;
	}
	
	/**
	 * 重建子级 fullName
	 * 
	 * @param parent
	 */
	public void reindexFullNameByParent(ID parent) {
		Object[][] children = Application.createQueryNoFilter(
				"select itemId from ClassificationData where parent = ?")
				.setParameter(1, parent)
				.array();
		for (Object[] c : children) {
			ID itemId = (ID) c[0];
			String parentFullName = ClassificationManager.getFullName(parent);
			String fullName = parentFullName + "." + ClassificationManager.getName(itemId);
			Record record = EntityHelper.forUpdate(itemId, Application.getCurrentUser());
			record.setString("fullName", fullName);
			super.update(record);
			
			ClassificationManager.cleanCache(itemId);
			reindexFullNameByParent(itemId);
		}
	}
	
	/**
	 * 重建指定分类 fullName
	 * 
	 * @param dataId
	 */
	public void reindexFullName(ID dataId) {
		Object[][] items = Application.createQueryNoFilter(
				"select itemId from ClassificationData where dataId = ?")
				.setParameter(1, dataId)
				.array();
		for (Object[] item : items) {
			ID itemId = (ID) item[0];
			ClassificationManager.cleanCache(itemId);
			
			String fullName = ClassificationManager.getFullName(itemId);
			Record record = EntityHelper.forUpdate(itemId, Application.getCurrentUser());
			record.setString("fullName", fullName);
			super.update(record);
		}
	}
}
