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

package com.rebuild.server.service.base;

import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.BaseService;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 业务实体用，会带有系统设置规则的执行
 * 
 * @author zhaofang123@gmail.com
 * @since 11/06/2017
 */
public class GeneralEntityService extends BaseService {

	public GeneralEntityService(PersistManagerFactory factory) {
		super(factory);
	}
	
	@Override
	public int getEntity() {
		return 0;
	}
	
	/**
	 * @param record
	 * @return
	 */
	public int delete(Record record) {
		ID recordId = record.getPrimary();
		
		// TODO 检查
		
		return super.delete(recordId);
	}
	
	/**
	 * @see #delete(Record)
	 */
	@Override
	public int delete(ID recordId) {
		Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
		String ajql = "select %s from %s where %s = '%s'";
		// TODO 需要哪些字段?
		String fields = "" + entity.getPrimaryField().getName();
		ajql = String.format(ajql, fields, entity.getName(), entity.getPrimaryField().getName(), recordId);
		
		Record record = aPMFactory.createQuery(ajql).record();
		return delete(record);
	}
	
	/**
	 * 共享
	 * 
	 * @param recordId
	 * @return
	 */
	public int share(ID recordId) {
		return 0;
	}
	
	/**
	 * 分配
	 * 
	 * @param recordId
	 * @return
	 */
	public int assgin(ID recordId) {
		return 0;
	}
}
