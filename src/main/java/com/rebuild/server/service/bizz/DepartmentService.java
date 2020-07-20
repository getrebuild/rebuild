/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.bizz;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.SystemEntityService;
import com.rebuild.server.service.bizz.privileges.Department;

/**
 * for Department
 * 
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
public class DepartmentService extends SystemEntityService {

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
        checkAdminGuard(BizzPermission.CREATE, null);

		record = super.create(record);
		Application.getUserStore().refreshDepartment(record.getPrimary());
		return record;
	}
	
	@Override
	public Record update(Record record) {
        checkAdminGuard(BizzPermission.UPDATE, record.getPrimary());

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
        checkAdminGuard(BizzPermission.DELETE, null);

		Department dept = Application.getUserStore().getDepartment(deptId);
		if (!dept.getChildren().isEmpty()) {
			throw new DataSpecificationException("Has child department");
		}
		
		super.delete(deptId);
 		Application.getUserStore().removeDepartment(deptId, transferTo);
	}

    /**
     * @param action
     * @param dept
     * @see com.rebuild.server.service.bizz.privileges.AdminGuard
     */
    private void checkAdminGuard(Permission action, ID dept) {
        ID currentUser = Application.getCurrentUser();
        if (UserHelper.isAdmin(currentUser)) return;

        if (action == BizzPermission.CREATE || action == BizzPermission.DELETE) {
            throw new PrivilegesException("无操作权限 (E2)");
        }

        // 用户可自己改自己的部门
        ID currentDeptOfUser = (ID) Application.getUserStore().getUser(currentUser).getOwningDept().getIdentity();
        if (action == BizzPermission.UPDATE && dept.equals(currentDeptOfUser)) {
            return;
        }
        throw new PrivilegesException("无操作权限");
    }
}
