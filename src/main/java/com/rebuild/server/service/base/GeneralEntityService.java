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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.Application;
import com.rebuild.server.service.BaseService;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;

/**
 * 业务实体用，会带有系统设置规则的执行
 * 
 * @author zhaofang123@gmail.com
 * @since 11/06/2017
 */
public class GeneralEntityService extends BaseService {
	
	private static final Log LOG = LogFactory.getLog(GeneralEntityService.class);
	
	protected GeneralEntityService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	/**
	 * 此服务类所属实体
	 * 
	 * @return
	 */
	public int getEntityCode() {
		return 0;
	}
	
	@Override
	public int delete(ID record) {
		return super.delete(record);
	}
	
	/**
	 * @param records
	 * @return 实际删除数量。请注意请求删除数量和实际删除数量可能不一致，因为可能没有删除权限
	 * 
	 * @see #delete(ID)
	 */
	public int bulkDelete(ID records[]) {
		ID user = Application.currentCallerUser();
		int deleted = 0;
		for (ID id : records) {
			if (Application.getSecurityManager().allowed(user, id, BizzPermission.DELETE)) {
				int affected = this.delete(id);
				deleted += (affected > 0 ? 1 : 0);
			} else {
				LOG.warn("No have privileges to delete : " + user + " > " + id);
			}
		}
		return deleted;
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
