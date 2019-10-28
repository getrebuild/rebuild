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
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.SystemEntityService;
import com.rebuild.server.service.bizz.privileges.AdminGuard;

import java.util.HashMap;
import java.util.Map;

/**
 * for Role
 * 
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
public class RoleService extends SystemEntityService implements AdminGuard {

	/**
	 * 管理员权限
	 */
	public static final ID ADMIN_ROLE = ID.valueOf("003-0000000000000001");
	
	public RoleService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}

	@Override
	public int getEntityCode() {
		return EntityHelper.Role;
	}
	
	@Override
	public Record create(Record record) {
		record = super.create(record);
		Application.getUserStore().refreshRole(record.getPrimary(), false);
		return record;
	}
	
	@Override
	public Record update(Record record) {
		record = super.update(record);
		Application.getUserStore().refreshRole(record.getPrimary(), false);
		return record;
	}
	
	@Override
	public int delete(ID roleId) {
		deleteAndTransfer(roleId, null);
		return 1;
	}
	
	/**
	 * TODO 删除后转移成员到其他角色
	 * 
	 * @param roleId
	 * @param transferTo
	 */
	public void deleteAndTransfer(ID roleId, ID transferTo) {
		super.delete(roleId);
 		Application.getUserStore().removeRole(roleId, transferTo);
	}
	
	/**
	 * @param roleId
	 * @param definition
	 */
	public void updatePrivileges(ID roleId, JSONObject definition) {
		final ID user = Application.getCurrentUser();
		
		Object[][] array = Application.createQuery(
				"select privilegesId,definition,entity,zeroKey from RolePrivileges where roleId = ?")
				.setParameter(1, roleId)
				.array();
		Map<String, Object[]> existsPrivileges = new HashMap<>();
		for (Object[] o : array) {
			if ((int) o[2] == 0) {
				o[2] = o[3];
			}
			existsPrivileges.put(o[2].toString(), o);
		}
		
		JSONObject entityPrivileges = definition.getJSONObject("entity");
		JSONObject zeroPrivileges = definition.getJSONObject("zero");
		JSONObject allPrivileges = new JSONObject();
		allPrivileges.putAll(entityPrivileges);
		allPrivileges.putAll(zeroPrivileges);
		zeroPrivileges.clear();
		
		boolean privilegesChanged = false;
		for (Map.Entry<String, Object> e : allPrivileges.entrySet()) {
			String name = e.getKey();
			String def = e.getValue().toString();
			if (existsPrivileges.containsKey(name)) {
				Object[] exists = existsPrivileges.get(name);
				// Unchanged
				if (def.equalsIgnoreCase(exists[1].toString())) {
					continue;
				}
				
				Record privileges = EntityHelper.forUpdate((ID) exists[0], user);
				privileges.setString("definition", def);
				super.update(privileges);
				privilegesChanged = true;
				
			} else {
				Record privileges = EntityHelper.forNew(EntityHelper.RolePrivileges, user);
				privileges.setID("roleId", roleId);
				if (entityPrivileges.containsKey(name)) {
					privileges.setInt("entity", Integer.parseInt(name));
				} else {
					privileges.setInt("entity", 0);
					privileges.setString("zeroKey", name);
				}
				privileges.setString("definition", def);
				super.create(privileges);
				privilegesChanged = true;
			}
		}
		
		Application.getUserStore().refreshRole(roleId, privilegesChanged);
	}
}
