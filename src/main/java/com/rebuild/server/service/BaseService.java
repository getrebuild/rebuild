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

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 基础服务类
 * 
 * @author zhaofang123@gmail.com
 * @since 05/21/2017
 */
public abstract class BaseService implements ServiceSpec {
	
	protected final Log LOG = LogFactory.getLog(getClass());
	
	final private PersistManagerFactory aPMFactory;

	protected BaseService(PersistManagerFactory aPMFactory) {
		super();
		this.aPMFactory = aPMFactory;
	}
	
	@Override
	public Record createOrUpdate(Record record) {
		return record.getPrimary() == null ? create(record) : update(record);
	}
	
	@Override
	public Record create(Record record) {
		return aPMFactory.createPersistManager().save(record);
	}

	@Override
	public Record update(Record record) {
		return aPMFactory.createPersistManager().update(record);
	}

	@Override
	public int delete(ID recordId) {
		int affected = aPMFactory.createPersistManager().delete(recordId);
		Application.getRecordOwningCache().cleanOwningUser(recordId);
		return affected;
	}
	
	/**
	 * @return
	 */
	public PersistManagerFactory getPMFactory() {
		return aPMFactory;
	}
	
	@Override
	public String toString() {
		if (getEntityCode() > 0) {
			return "service." + aPMFactory.getMetadataFactory().getEntity(getEntityCode()).getName() + "@" + Integer.toHexString(hashCode());
		} else {
			return super.toString();
		}
	}
}
