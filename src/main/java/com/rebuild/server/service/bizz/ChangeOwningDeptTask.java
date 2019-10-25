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

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.helper.task.HeavyTask;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;

import java.text.MessageFormat;

/**
 * 用户变更部门后，该用户的业务记录中的所属部门也需要变更
 * 
 * @author devezhao
 * @since 12/29/2018
 */
public class ChangeOwningDeptTask extends HeavyTask<Integer> {

	final private ID user;
	final private ID deptNew;
	
	/**
	 * @param user
	 * @param deptNew
	 */
	protected ChangeOwningDeptTask(ID user, ID deptNew) {
		this.user = user;
		this.deptNew = deptNew;
	}
	
	@Override
	public Integer exec() throws Exception {
		LOG.info("Start modifying the `OwningDept` ... " + this.user);
		this.setTotal(MetadataHelper.getEntities().length);

		final String updeptSql = String.format(
				"update `{0}` set `{1}` = ''%s'' where `{2}` = ''%s''", deptNew.toLiteral(), user.toLiteral());
		int changed = 0;
		for (Entity e : MetadataHelper.getEntities()) {
			if (this.isInterrupt()) {
				this.setInterrupted();
				LOG.error("Task interrupted : " + user + " > " + deptNew);
				break;
			}
			if (!EntityHelper.hasPrivilegesField(e)) {
				this.addCompleted();
				continue;
			}

			String sql = MessageFormat.format(updeptSql,
					e.getPhysicalName(),
					e.getField(EntityHelper.OwningDept).getPhysicalName(),
					e.getField(EntityHelper.OwningUser).getPhysicalName());
			Application.getSQLExecutor().execute(sql, 600);
			this.addCompleted();
			changed++;
		}
		LOG.info("Modify the `OwningDept` to complete : " + this.user + " > " +  changed);
		return changed;
	}
}
