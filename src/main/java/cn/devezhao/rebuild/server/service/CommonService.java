/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cn.devezhao.rebuild.server.service;

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
