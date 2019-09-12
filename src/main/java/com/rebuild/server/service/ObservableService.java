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

package com.rebuild.server.service;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.helper.cache.NoRecordFoundException;
import com.rebuild.server.metadata.EntityHelper;
import org.springframework.util.Assert;

import java.util.Iterator;
import java.util.Observable;

/**
 * 可注入观察者的服务
 * 
 * @author devezhao
 * @since 12/28/2018
 * 
 * @see OperatingObserver
 */
public abstract class ObservableService extends Observable implements EntityService {
	
	// 删除前触发的动作
	protected static final Permission DELETE_BEFORE = new BizzPermission("DELETE_BEFORE", 0, false);
	
	final protected ServiceSpec delegate;
	
	/**
	 * @param aPMFactory
	 */
	public ObservableService(PersistManagerFactory aPMFactory) {
		this.delegate = new SystemEntityService(aPMFactory);
	}
	
	@Override
	public Record createOrUpdate(Record record) {
		return record.getPrimary() == null ? create(record) : update(record);
	}

	@Override
	public Record create(Record record) {
		record = delegate.create(record);
		
		if (countObservers() > 0) {
			setChanged();
			notifyObservers(OperatingContext.create(Application.getCurrentUser(), BizzPermission.CREATE, null, record));
		}
		return record;
	}

	@Override
	public Record update(Record record) {
		final Record before = countObservers() > 0 ? record(record) : null;
		
		record = delegate.update(record);
		
		if (countObservers() > 0) {
			setChanged();
			notifyObservers(OperatingContext.create(Application.getCurrentUser(), BizzPermission.UPDATE, before, record));
		}
		return record;
	}

	@Override
	public int delete(ID recordId) {
		Record deleted = null;
		if (countObservers() > 0) {
			deleted = EntityHelper.forUpdate(recordId, Application.getCurrentUser());
			deleted = record(deleted);
			
			// 删除前触发，做一些状态保持
			setChanged();
			notifyObservers(OperatingContext.create(Application.getCurrentUser(), DELETE_BEFORE, deleted, null));
		}
		
		int affected = delegate.delete(recordId);
		
		if (countObservers() > 0) {
			setChanged();
			notifyObservers(OperatingContext.create(Application.getCurrentUser(), BizzPermission.DELETE, deleted, null));
		}
		return affected;
	}
	
	/**
	 * 用于操作前获取原记录
	 * 
	 * @param example
	 * @return
	 */
	protected Record record(Record example) {
		ID primary = example.getPrimary();
		Assert.notNull(primary, "Record primary not be bull");
		
		StringBuilder sql = new StringBuilder("select ");
		for (Iterator<String> iter = example.getAvailableFieldIterator(); iter.hasNext(); ) {
			sql.append(iter.next()).append(',');
		}
		sql.deleteCharAt(sql.length() - 1);
		sql.append(" from ").append(example.getEntity().getName());
		sql.append(" where ").append(example.getEntity().getPrimaryField().getName()).append(" = ?");
		
		Record current = Application.createQueryNoFilter(sql.toString()).setParameter(1, primary).record();
		if (current == null) {
			throw new NoRecordFoundException("ID: " + primary);
		}
		return current;
	}
	
	/**
	 * @return
	 */
	protected ServiceSpec delegate() {
		return delegate;
	}
}
