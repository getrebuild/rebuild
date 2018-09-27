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

import java.util.HashMap;
import java.util.Map;

import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 11/06/2017
 */
public class CommonService extends BaseService {

	public CommonService(PersistManagerFactory persistManagerFactory) {
		super(persistManagerFactory);
	}
	
	@Override
	public int getEntity() {
		return 0;
	}
	
	@Override
	public Record create(Record record) {
		record = super.create(record);
		createAfter(record);
		return record;
	}
	
	/**
	 * 批量删除
	 * 
	 * @param ids
	 * @return
	 */
	public int delete(ID[] ids) {
		int affected = 0;
		for (ID id : ids) {
			affected += delete(id);
		}
		return affected;
	}
	
	// --
	
	private static final Map<Integer, ServiceExecuteCall> SERVICEEXECUTECALL_MAP = new HashMap<>();
	
	public void registerCall(ServiceExecuteCall call) {
		SERVICEEXECUTECALL_MAP.put(call.getEntity(), call);
	}
	
	/**
	 * 创建后回调
	 * 
	 * @param record
	 */
	protected void createAfter(final Record record) {
		final ServiceExecuteCall call = SERVICEEXECUTECALL_MAP.get(record.getEntity().getEntityCode());
		if (call != null) {
			if (call.isAsync()) {
				ThreadPool.exec(new Runnable() {
					@Override
					public void run() {
						call.call(record);
					}
				});
			} else {
				call.call(record);
			}
		}
	}
}
