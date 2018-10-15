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

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 持久层服务
 * 
 * @author zhaofang123@gmail.com
 * @since 05/21/2017
 */
public abstract class BaseService {
	
	final protected PersistManagerFactory aPMFactory;

	protected BaseService(PersistManagerFactory factory) {
		super();
		this.aPMFactory = factory;
	}

	/**
	 * @param record
	 * @return
	 */
	public Record create(Record record) {
		return aPMFactory.createPersistManager().save(record);
	}

	/**
	 * @param record
	 * @return
	 */
	public Record update(Record record) {
		return aPMFactory.createPersistManager().update(record);
	}
	
	/**
	 * @param record
	 * @return
	 */
	public Record createOrUpdate(Record record) {
		return record.getPrimary() == null ? create(record) : update(record);
	}

	/**
	 * @param recordId
	 * @return 删除记录数量（包括关联的记录）
	 */
	public int delete(ID recordId) {
		int affected = aPMFactory.createPersistManager().delete(recordId);
		return affected;
	}
}
