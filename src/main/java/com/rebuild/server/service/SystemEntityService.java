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

import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.base.BulkContext;
import com.rebuild.server.service.base.QuickCodeReindexTask;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * @author devezhao
 * @since 01/04/2019
 */
public abstract class SystemEntityService extends BaseService implements EntityService {

	protected SystemEntityService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	/**
	 * 助记码
	 * 
	 * @param record
	 */
	protected void setQuickCodeValue(Record record) {
		// 已设置了则不再设置
		if (record.hasValue(EntityHelper.QuickCode)) {
			return;
		}
		
		String quickCode = QuickCodeReindexTask.generateQuickCode(record);
		if (quickCode != null) {
			record.setString(EntityHelper.QuickCode, quickCode);
		}
	}
	
	@Override
	public int delete(ID record, String[] cascades) {
		return delete(record);
	}

	@Override
	public int assign(ID record, ID to, String[] cascades) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int share(ID record, ID to, String[] cascades) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int unshare(ID record, ID accessId) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int bulk(BulkContext context) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String bulkAsync(BulkContext context) {
		throw new UnsupportedOperationException();
	}
}
