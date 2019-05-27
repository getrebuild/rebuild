/*
rebuild - Building your business-systems freely.
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

package com.rebuild.server.service.configuration;

import com.rebuild.server.service.BaseService;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * TODO
 * 
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/27
 */
public abstract class CleanCacheService extends BaseService {

	protected CleanCacheService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}

	@Override
	public Record createOrUpdate(Record record) {
		record = super.createOrUpdate(record);
		cleanCache(record.getPrimary());
		return record;
	}
	
	@Override
	public int delete(ID recordId) {
		cleanCache(recordId);
		int del = super.delete(recordId);
		return del;
	}
	
	abstract protected void cleanCache(ID configId);
}
