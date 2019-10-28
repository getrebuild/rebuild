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

package com.rebuild.server.service.bizz;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.SystemEntityService;
import com.rebuild.server.service.bizz.privileges.AdminGuard;
import com.rebuild.server.service.bizz.privileges.Department;

/**
 * for Department
 * 
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
public class DepartmentService extends SystemEntityService implements AdminGuard {
	
	/**
	 * 根级部门
	 */
	public static final ID ROOT_DEPT = ID.valueOf("002-0000000000000001");

	public DepartmentService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}

	@Override
	public int getEntityCode() {
		return EntityHelper.Department;
	}
	
	@Override
	public Record create(Record record) {
		record = super.create(record);
		Application.getUserStore().refreshDepartment(record.getPrimary());
		return record;
	}
	
	@Override
	public Record update(Record record) {
		record = super.update(record);
		Application.getUserStore().refreshDepartment(record.getPrimary());
		return record;
	}

	@Override
	public int delete(ID recordId) {
		deleteAndTransfer(recordId, null);
		return 1;
	}
	
	/**
	 * TODO 删除后转移成员到其他部门
	 * 
	 * @param deptId
	 * @param transferTo
	 */
	public void deleteAndTransfer(ID deptId, ID transferTo) {
		Department dept = Application.getUserStore().getDepartment(deptId);
		if (!dept.getChildren().isEmpty()) {
			throw new DataSpecificationException("Has child department");
		}
		
		super.delete(deptId);
 		Application.getUserStore().removeDepartment(deptId, transferTo);
	}
}
