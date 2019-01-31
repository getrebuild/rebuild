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

import com.rebuild.server.Application;
import com.rebuild.server.helper.BlackList;
import com.rebuild.server.helper.task.BulkTaskExecutor;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.SystemEntityService;
import com.rebuild.utils.CommonsUtils;

import cn.devezhao.bizz.security.member.User;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * for User
 * 
 * @author zhaofang123@gmail.com
 * @since 07/25/2018
 */
public class UserService extends SystemEntityService {
	
	// 系统用户
	public static final ID SYSTEM_USER = ID.valueOf("001-0000000000000000");
	// 管理员
	public static final ID ADMIN_USER = ID.valueOf("001-0000000000000001");
	
	public UserService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}

	@Override
	public int getEntityCode() {
		return EntityHelper.User;
	}
	
	@Override
	public Record create(Record record) {
		saveBefore(record);
		Record r = super.create(record);
		Application.getUserStore().refreshUser(record.getPrimary());
		return r;
	}
	
	@Override
	public Record update(Record record) {
		saveBefore(record);
		Record r = super.update(record);
		Application.getUserStore().refreshUser(record.getPrimary());
		return r;
	}
	
	@Override
	public int delete(ID record) {
		throw new DataSpecificationException("Prohibited");
	}
	
	/**
	 * @param record
	 */
	protected void saveBefore(Record record) {
		if (record.hasValue("loginName")) {
			checkLoginName(record.getString("loginName"));
		}
		
		if (record.hasValue("password")) {
			record.setString("password", checkPassword(record.getString("password")));
		}
		
		if (record.hasValue("email") && Application.getUserStore().exists(record.getString("email"))) {
			throw new DataSpecificationException("邮箱重复");
		}
		
		if (record.getPrimary() == null && !record.hasValue("fullName")) {
			record.setString("fullName", record.getString("loginName").toUpperCase());
		}
		
		setQuickCodeValue(record);
	}
	
	/**
	 * @param loginName
	 * @throws DataSpecificationException
	 */
	private void checkLoginName(String loginName) throws DataSpecificationException {
		if (Application.getUserStore().exists(loginName)) {
			throw new DataSpecificationException("登陆名重复");
		}
		if (!CommonsUtils.isPlainText(loginName) || BlackList.isBlack(loginName)) {
			throw new DataSpecificationException("无效登陆名");
		}
	}
	
	/**
	 * @param password
	 * @return
	 * @throws DataSpecificationException
	 */
	private String checkPassword(String password) throws DataSpecificationException {
		if (password.length() < 6) {
			throw new DataSpecificationException("密码不能小于6位");
		}
		password = EncryptUtils.toSHA256Hex(password);
		return password;
	}
	
	/**
	 * 改变部门
	 * 
	 * @param user
	 * @param deptNew
	 */
	public void updateDepartment(ID user, ID deptNew) {
		User u = Application.getUserStore().getUser(user);
		final ID deptOld = u.getOwningBizUnit() == null ? null : (ID) u.getOwningBizUnit().getIdentity();
		if (deptNew.equals(deptOld)) {
			return;
		}
		
		Record record = EntityHelper.forUpdate(user, Application.getCurrentUser());
		record.setID("deptId", deptNew);
		super.update(record);
		Application.getUserStore().refreshUser(user);
		
		// 改变记录的所属部门
		if (deptOld != null) {
			BulkTaskExecutor.submit(new ChangeOwningDeptTask(user, deptNew));
		}
	}
}
