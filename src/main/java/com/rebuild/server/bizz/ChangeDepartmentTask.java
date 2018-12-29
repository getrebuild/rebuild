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
import com.rebuild.server.job.BulkTask;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * 用户变更部门后，该用户的业务记录中的所属部门也需要变更
 * 
 * @author devezhao
 * @since 12/29/2018
 */
public class ChangeDepartmentTask extends BulkTask {

	final private ID user;
	final private ID deptNew;
	
	/**
	 * @param user
	 * @param deptNew
	 */
	protected ChangeDepartmentTask(ID user, ID deptNew) {
		this.user = user;
		this.deptNew = deptNew;
	}
	
	@Override
	public void run() {
		// TODO 变更部门 10M 超时，线程执行 ???
		
		String updeptSql = "update `{0}` set OWNING_DEPT = ''%s'' where OWNING_USER = ''%s''";
		updeptSql = String.format(updeptSql, deptNew.toLiteral(), user.toLiteral());
		
		List<String> updeptSqls = new ArrayList<>();
		for (Entity e : MetadataHelper.getEntities()) {
			if (EntityHelper.hasPrivilegesField(e)) {
				String sql = MessageFormat.format(updeptSql, e.getPhysicalName());
				updeptSqls.add(sql);
			}
		}
		
		Application.getSQLExecutor().executeBatch(updeptSqls.toArray(new String[updeptSqls.size()]), 60 * 10);
	}
}
