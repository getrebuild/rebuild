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

import org.springframework.util.Assert;

import com.rebuild.server.bizz.privileges.PrivilegesGuardInterceptor;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 普通的 CRUD 服务
 * <br>- 此类有事物
 * <br>- 此类不经过用户权限验证 {@link PrivilegesGuardInterceptor}
 * 
 * @author zhaofang123@gmail.com
 * @since 11/06/2017
 */
public class CommonService extends AbstractBaseService {

	public CommonService(PersistManagerFactory factory) {
		super(factory);
	}
	
	/**
	 * 批量删除
	 * 
	 * @param ids
	 * @return
	 */
	public int delete(ID[] deletes) {
		Assert.notNull(deletes, "'deletes' not be null");
		int affected = 0;
		for (ID id : deletes) {
			affected += super.delete(id);
		}
		return affected;
	}
	
	/**
	 * 批量新建
	 * 
	 * @param records
	 * @return
	 */
	public int createOrUpdate(Record[] records) {
		Assert.notNull(records, "'records' not be null");
		int affected = 0;
		for (Record record : records) {
			super.createOrUpdate(record);
			affected++;
		}
		return affected;
	}
	
	/**
	 * 批量新建/更新/删除
	 * 
	 * @param records
	 * @param deletes
	 * @return
	 */
	public int createOrUpdate(Record[] records, ID[] deletes) {
		int affected = 0;
		affected += createOrUpdate(records);
		affected += delete(deletes);
		return affected;
	}
}
