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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.rebuild.server.Application;
import com.rebuild.server.DataConstraintException;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.base.GeneralEntityService;

import cn.devezhao.bizz.security.member.User;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 07/25/2018
 */
public class UserService extends GeneralEntityService {
	
	// 系统用户
	public static final ID SYSTEM_USER = ID.valueOf("001-0000000000000000");
	// 管理员
	public static final ID ADMIN_USER = ID.valueOf("001-0000000000000001");
	
	protected UserService(PersistManagerFactory aPMFactory) {
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
		saveAfter(r, true);
		return r;
	}
	
	@Override
	public Record update(Record record) {
		saveBefore(record);
		Record r = super.update(record);
		saveAfter(r, false);
		return r;
	}
	
	@Override
	public int delete(ID record) {
		throw new DataConstraintException("Delete prohibited");
	}
	
	private void saveBefore(Record record) {
		if (record.hasValue("password")) {
			String password = record.getString("password");
			password = EncryptUtils.toSHA256Hex(password);
			record.setString("password", password);
		}
	}
	
	private void saveAfter(Record record, boolean isNew) {
		// TODO 必要时才更新用户缓存
		Application.getUserStore().refreshUser(record.getPrimary());
	}
	
	/**
	 * 改变部门
	 * 
	 * @param user
	 * @param newDept
	 */
	public void txChangeDept(ID user, ID newDept) {
		User u = Application.getUserStore().getUser(user);
		final ID oldDept = u.getOwningBizUnit() == null ? null : (ID) u.getOwningBizUnit().getIdentity();
		
		Record record = EntityHelper.forUpdate(user, Application.currentCallerUser());
		record.setID("deptId", newDept);
		update(record);
		
		// 无需改变记录的所属部门
		if (oldDept == null) {
			return;
		}
		
		String updeptSql = "update `{0}` set OWNING_DEPT = ''%s'' where OWNING_USER = ''%s''";
		updeptSql = String.format(updeptSql, newDept.toLiteral(), user.toLiteral());
		
		List<String> updeptSqls = new ArrayList<>();
		for (Entity e : MetadataHelper.getEntities()) {
			if (EntityHelper.hasPrivilegesField(e)) {
				String sql = MessageFormat.format(updeptSql, e.getPhysicalName());
				updeptSqls.add(sql);
			}
		}
		
		// TODO 10M 超时，线程执行 ???
		Application.getSQLExecutor().executeBatch(updeptSqls.toArray(new String[updeptSqls.size()]), 60 * 10);
	}
	
}
