/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
		// 检查循环依赖
		if (record.hasValue("parentDept", false)) {
			ID parentDept = record.getID("parentDept");
			Department parent = Application.getUserStore().getDepartment(parentDept);

			Department that = Application.getUserStore().getDepartment(record.getPrimary());
			if (that.isChildren(parent, true)) {
				throw new DataSpecificationException("子级部门不能同时作为父级部门");
			}
		}

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
