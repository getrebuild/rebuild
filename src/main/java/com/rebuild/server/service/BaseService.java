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

import java.util.Observable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 持久层服务
 * 
 * @author zhaofang123@gmail.com
 * @since 05/21/2017
 */
public abstract class BaseService extends Observable {
	
	protected final Log LOG = LogFactory.getLog(getClass());
	
	final protected PersistManagerFactory aPMFactory;

	protected BaseService(PersistManagerFactory aPMFactory) {
		super();
		this.aPMFactory = aPMFactory;
	}

	/**
	 * 新建或更新
	 * 
	 * @param record
	 * @return
	 * @see #create(Record)
	 * @see #update(Record)
	 */
	public Record createOrUpdate(Record record) {
		return record.getPrimary() == null ? create(record) : update(record);
	}
	
	/**
	 * 新建
	 * 
	 * @param record
	 * @return
	 */
	public Record create(Record record) {
		record = aPMFactory.createPersistManager().save(record);
		
		if (countObservers() > 0) {
			setChanged();
			notifyObservers(OperateContext.valueOf(Application.currentCallerUser(), BizzPermission.CREATE, null, record));
		}
		return record;
	}

	/**
	 * 更新
	 * 
	 * @param record
	 * @return
	 */
	public Record update(Record record) {
		Record before = countObservers() > 0 ? getBeforeRecord(record) : null;
		
		record = aPMFactory.createPersistManager().update(record);
		
		if (countObservers() > 0) {
			setChanged();
			notifyObservers(OperateContext.valueOf(Application.currentCallerUser(), BizzPermission.UPDATE, before, record));
		}
		return record;
	}

	/**
	 * 删除
	 * 
	 * @param recordId
	 * @return 删除记录数量。包括关联的记录，自定义实体都选择了 remove-link 级联模式，因此基本不会自动关联删除
	 */
	public int delete(ID recordId) {
		Record deleted = countObservers() > 0 ? getBeforeRecord(EntityHelper.forUpdate(recordId, null)) : null;
		
		int affected = aPMFactory.createPersistManager().delete(recordId);
		
		if (countObservers() > 0) {
			setChanged();
			notifyObservers(OperateContext.valueOf(Application.currentCallerUser(), BizzPermission.DELETE, deleted, null));
		}
		return affected;
	}
	
	/**
	 * TODO 操作前获取记录
	 * 
	 * @param willOper
	 * @return
	 */
	protected Record getBeforeRecord(Record willOper) {
		return null;
	}
}
