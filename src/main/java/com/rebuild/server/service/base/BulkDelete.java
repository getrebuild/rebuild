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
 * 删除
 * 
 * @author devezhao
 * @since 10/16/2018
 */
public class BulkDelete extends BulkOperator {

	public BulkDelete(BulkContext context, GeneralEntityService ges) {
		super(context, ges);
	}

	@Override
	public Integer operate() {
		ID[] records = getWillRecords();
		this.setTotal(records.length);
		
		int deleted = 0;
		for (ID id : records) {
			if (Application.getSecurityManager().allowedD(context.getOpUser(), id)) {
				int a = ges.delete(id);
				deleted += (a > 0 ? 1 : 0);
			} else {
				LOG.warn("No have privileges to DELETE : " + context.getOpUser() + " > " + id);
			}
			this.setCompleteOne();
		}
		
		this.completedAfter();
		return deleted;
	}
}
