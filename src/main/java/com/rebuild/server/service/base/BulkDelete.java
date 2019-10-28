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

package com.rebuild.server.service.base;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.service.DataSpecificationException;

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
	public Integer exec() {
		ID[] records = prepareRecords();
		this.setTotal(records.length);
		
		int affected = 0;
		for (ID id : records) {
			if (Application.getSecurityManager().allowedD(context.getOpUser(), id)) {
				try {
					int a = ges.delete(id, context.getCascades());
					affected += (a > 0 ? 1 : 0);
				} catch (DataSpecificationException ex) {
					LOG.warn("Couldn't delete : " + id + " Ex : " + ex);
				}
			} else {
				LOG.warn("No have privileges to DELETE : " + context.getOpUser() + " > " + id);
			}
			this.addCompleted();
		}
		
		this.completedAfter();
		return affected;
	}
}
