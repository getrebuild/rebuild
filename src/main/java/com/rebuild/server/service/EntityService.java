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

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.helper.task.TaskExecutors;
import com.rebuild.server.service.base.BulkContext;

/**
 * 业务实体用
 * 
 * @author devezhao
 * @since 12/28/2018
 */
public interface EntityService extends ServiceSpec {
	
	/**
	 * 取消共享，跟随共享权限
	 * @see BizzPermission
	 */
	Permission UNSHARE = new BizzPermission("UNSHARE", 1 << 6, true);

	/**
	 * 删除
	 * 
	 * @param record
	 * @param cascades 需要级联删除的实体
	 * @return
	 */
	int delete(ID record, String[] cascades);
	
	/**
	 * 分派
	 * 
	 * @param record
	 * @param to
	 * @param cascades 需要级联分派的实体
	 * @return
	 */
	int assign(ID record, ID to, String[] cascades);
	
	/**
	 * 共享
	 * 
	 * @param record
	 * @param to
	 * @param cascades 需要级联分派的实体
	 * @return
	 */
	int share(ID record, ID to, String[] cascades);
	
	/**
	 * 取消共享
	 * 
	 * @param record 主记录
	 * @param accessId 共享的 AccessID
	 * @return
	 */
	int unshare(ID record, ID accessId);
	
	/**
	 * 批量大操作
	 * 
	 * @param context
	 * @return
	 */
	int bulk(BulkContext context);
	
	/**
	 * 批量大操作（异步）
	 * 
	 * @param context
	 * @return 任务 ID
	 * @see TaskExecutors
	 */
	String bulkAsync(BulkContext context);
}
