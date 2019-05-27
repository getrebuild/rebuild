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

package com.rebuild.server.metadata.entityhub;

import com.rebuild.server.Application;
import com.rebuild.server.configuration.AutoFillinManager;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.BaseService;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * TODO
 * 
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/18
 */
public class AutoFillinConfigService extends BaseService {

	protected AutoFillinConfigService(PersistManagerFactory aPMFactory) {
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
	
	private void cleanCache(ID configId) {
		Object[] cfg = Application.createQueryNoFilter(
				"select belongEntity,belongField from AutoFillinConfig where configId = ?")
				.setParameter(1, configId)
				.unique();
		if (cfg != null) {
			Field dest = MetadataHelper.getField((String) cfg[0], (String) cfg[1]);
			AutoFillinManager.instance.clean(dest);
		}
	}
}
