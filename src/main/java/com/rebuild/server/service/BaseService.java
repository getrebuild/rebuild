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
	
	protected PersistManagerFactory persistManagerFactory;

	protected BaseService(PersistManagerFactory persistManagerFactory) {
		super();
		this.persistManagerFactory = persistManagerFactory;
	}
	
	/**
	 * @return
	 */
	abstract public int getEntity();

	/**
	 * @param record
	 * @return
	 */
	public Record create(Record record) {
		return persistManagerFactory.createPersistManager().save(record);
	}

	/**
	 * @param record
	 * @return
	 */
	public Record update(Record record) {
		return persistManagerFactory.createPersistManager().update(record);
	}
	
	/**
	 * @param record
	 * @return
	 */
	public Record createOrUpdate(Record record) {
		return record.getPrimary() == null ? create(record) : update(record);
	}

	/**
	 * @param record
	 * @return
	 */
	public int delete(Record record) {
		return delete(record.getPrimary());
	}
	
	/**
	 * @param recordId
	 * @return
	 */
	public int delete(ID recordId) {
		int affected = persistManagerFactory.createPersistManager().delete(recordId);
		return affected;
	}
}
