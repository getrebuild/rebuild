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

import com.rebuild.server.Application;

import cn.devezhao.persist4j.engine.ID;

/**
 * TODO 取消共享
 * 
 * @author devezhao
 * @since 12/19/2018
 */
public class BulkUnshare extends BulkOperator {

	protected BulkUnshare(BulkContext context, GeneralEntityService ges) {
		super(context, ges);
	}

	@Override
	public Object exec() {
		ID[] records = getWillRecords();
		
		int complated = 0;
		int deleted = 0;
		
		for (ID id : records) {
			if (Application.getSecurityManager().allowedD(context.getOpUser(), id)) {
				int a = ges.unshare(id, context.getToUser(), context.getCascades());
				deleted += (a > 0 ? 1 : 0);
			} else {
				LOG.warn("No have privileges to DELETE : " + context.getOpUser() + " > " + id);
			}
			
			complated++;
			setComplete(complated);
		}
		return deleted;
	}

}
