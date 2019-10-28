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

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;

/**
 * 业务对象基础服务类定义
 * 
 * @author devezhao
 * @since 12/28/2018
 */
public interface ServiceSpec {
	
	/**
	 * 专供某个实体的
	 * 
	 * @return
	 * @see Application#getEntityService(int)
	 */
	int getEntityCode();
	
	/**
	 * 新建或更新
	 * 
	 * @param record
	 * @return
	 * 
	 * @see #create(Record)
	 * @see #update(Record)
	 */
	Record createOrUpdate(Record record);
	
	/**
	 * 新建
	 * 
	 * @param record
	 * @return
	 */
	Record create(Record record);
	
	/**
	 * 更新
	 * 
	 * @param record
	 * @return
	 */
	Record update(Record record);
	
	/**
	 * 删除
	 * 
	 * @param recordId
	 * @return
	 */
	int delete(ID recordId);
}
