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

import org.springframework.util.Assert;

import com.rebuild.server.service.bizz.privileges.PrivilegesGuardInterceptor;

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
public class CommonService extends BaseService {

	/**
	 * @param aPMFactory
	 */
	public CommonService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	/**
	 * 批量删除
	 * 
	 * @param ids
	 * @return
	 */
	public int delete(ID[] deleted) {
		Assert.notNull(deleted, "'deleted' not be null");
		int affected = 0;
		for (ID id : deleted) {
			affected += super.delete(id);
		}
		return affected;
	}
	
	/**
	 * 批量新建/更新
	 * 
	 * @param saved
	 * @return
	 */
	public int createOrUpdate(Record[] saved) {
		Assert.notNull(saved, "'saved' not be null");
		int affected = 0;
		for (Record record : saved) {
			super.createOrUpdate(record);
			affected++;
		}
		return affected;
	}
	
	/**
	 * 批量新建/更新/删除
	 * 
	 * @param saved
	 * @param deleted
	 * @return
	 */
	public int createOrUpdate(Record[] saved, ID[] deleted) {
		int affected = 0;
		affected += createOrUpdate(saved);
		affected += delete(deleted);
		return affected;
	}
}
