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

package com.rebuild.server.service;

import java.util.Iterator;
import java.util.Observable;

import org.springframework.util.Assert;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 可注入观察者的服务
 * 
 * @author devezhao
 * @since 12/28/2018
 * 
 * @see OperatingObserver
 */
public abstract class ObservableService extends Observable implements IEntityService {

	final protected IService delegate;
	
	/**
	 * @param aPMFactory
	 */
	public ObservableService(PersistManagerFactory aPMFactory) {
		this.delegate = new BaseService(aPMFactory);
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
			notifyObservers(OperatingContext.valueOf(Application.getCurrentUser(), BizzPermission.CREATE, null, record));
		}
		return record;
	}

	@Override
	public Record update(Record record) {
		Record before = countObservers() > 0 ? getBeforeRecord(record) : null;
		
		record = delegate.update(record);
		
		if (countObservers() > 0) {
			setChanged();
			notifyObservers(OperatingContext.valueOf(Application.getCurrentUser(), BizzPermission.UPDATE, before, record));
		}
		return record;
	}

	@Override
	public int delete(ID recordId) {
		Record deleted = null;
		if (countObservers() > 0) {
			deleted = EntityHelper.forUpdate(recordId, Application.getCurrentUser());
			deleted = getBeforeRecord(deleted);
		}
		
		int affected = delegate.delete(recordId);
		
		if (countObservers() > 0) {
			setChanged();
			notifyObservers(OperatingContext.valueOf(Application.getCurrentUser(), BizzPermission.DELETE, deleted, null));
		}
		return affected;
	}
	
	/**
	 * 操作前获取记录
	 * 
	 * @param reflection
	 * @return
	 */
	protected Record getBeforeRecord(Record reflection) {
		ID primary = reflection.getPrimary();
		Assert.notNull(primary, "Record primary not be bull");
		
		StringBuffer sql = new StringBuffer("select ");
		for (Iterator<String> iter = reflection.getAvailableFieldIterator(); iter.hasNext(); ) {
			sql.append(iter.next()).append(',');
		}
		sql.deleteCharAt(sql.length() - 1);
		sql.append(" from ").append(reflection.getEntity().getName());
		sql.append(" where ").append(reflection.getEntity().getPrimaryField().getName()).append(" = ?");
		
		Record before = Application.createQueryNoFilter(sql.toString()).setParameter(1, primary).record();
		return before;
	}
}
