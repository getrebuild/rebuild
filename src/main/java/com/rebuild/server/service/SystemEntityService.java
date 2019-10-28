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
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.base.QuickCodeReindexTask;

/**
 * 系统实体用
 * 
 * @author devezhao
 * @since 01/04/2019
 */
public class SystemEntityService extends BaseService {

	protected SystemEntityService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	@Override
	public int getEntityCode() {
		return 0;
	}
	
	@Override
	public Record create(Record record) {
		setQuickCodeValue(record);
		return super.create(record);
	}
	
	@Override
	public Record update(Record record) {
		setQuickCodeValue(record);
		return super.update(record);
	}
	
	/**
	 * 助记码
	 * 
	 * @param record
	 */
	private void setQuickCodeValue(Record record) {
		// 已设置了则不再设置
		if (record.hasValue(EntityHelper.QuickCode)) {
			return;
		}
		// 无助记码字段
		if (!record.getEntity().containsField(EntityHelper.QuickCode)) {
			return;
		}
		
		String quickCode = QuickCodeReindexTask.generateQuickCode(record);
		if (quickCode != null) {
			record.setString(EntityHelper.QuickCode, quickCode);
		}
	}
}
