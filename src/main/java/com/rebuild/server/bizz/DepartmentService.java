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

package com.rebuild.server.bizz;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.base.GeneralEntityService;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
public class DepartmentService extends GeneralEntityService {
	
	/**
	 * 根级部门
	 */
	public static final ID ROOT_DEPT = ID.valueOf("002-0000000000000001");

	protected DepartmentService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}

	@Override
	public int getEntityCode() {
		return EntityHelper.Department;
	}
	
	@Override
	public Record createOrUpdate(Record record) {
		record = super.createOrUpdate(record);
		Application.getUserStore().refreshDepartment(record.getPrimary());
		return record;
	}
	
	@Override
	public int delete(ID record, String[] cascades) {
		int a = super.delete(record, cascades);
		Application.getUserStore().removeDepartment(record, null);
		return a;
	}
	
	public void delete(ID deptId, ID transferTo) {
		super.delete(deptId, null);
 		Application.getUserStore().removeDepartment(deptId, transferTo);
	}
}
