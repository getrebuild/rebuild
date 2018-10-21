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

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
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
public class RoleService extends GeneralEntityService {

	/**
	 * 管理员权限
	 */
	public static final ID ADMIN_ROLE = ID.valueOf("003-0000000000000001");
	
	protected RoleService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}

	@Override
	public int getEntityCode() {
		return EntityHelper.Role;
	}
	
	@Override
	public Record createOrUpdate(Record record) {
		record = super.createOrUpdate(record);
		Application.getUserStore().refreshRole(record.getPrimary(), false);
		return record;
	}
	
	/**
	 * @param roleId
	 * @param definition
	 */
	public void txUpdatePrivileges(ID roleId, JSONObject definition) {
		Object[][] array = Application.createQuery(
				"select privilegesId,definition,entity,zeroKey from RolePrivileges where roleId = ?")
				.setParameter(1, roleId)
				.array();
		Map<String, Object[]> existsMap = new HashMap<>();
		for (Object[] o : array) {
			if ("N".equals(o[2])) {
				o[2] = o[3];
			}
			existsMap.put(o[2].toString(), o);
		}
		
		JSONObject entityPriv = definition.getJSONObject("entity");
		JSONObject zeroPriv = definition.getJSONObject("zero");
		JSONObject allPriv = new JSONObject();
		allPriv.putAll(entityPriv);
		allPriv.putAll(zeroPriv);
		zeroPriv.clear();
		
		boolean privilegesChanged = false;
		for (Map.Entry<String, Object> e : allPriv.entrySet()) {
			String name = e.getKey();
			String defi = e.getValue().toString();
			if (existsMap.containsKey(name)) {
				Object[] exists = existsMap.get(name);
				// Unchanged
				if (defi.equalsIgnoreCase(exists[1].toString())) {
					continue;
				}
				
				Record priv = EntityHelper.forUpdate((ID) exists[0], Application.currentCallerUser());
				priv.setString("definition", defi);
				super.update(priv);
				privilegesChanged = true;
				
			} else {
				Record priv = EntityHelper.forNew(EntityHelper.RolePrivileges, Application.currentCallerUser());
				priv.setID("roleId", roleId);
				if (entityPriv.containsKey(name)) {
					priv.setString("entity", name);
				} else {
					priv.setString("zeroKey", name);
				}
				priv.setString("definition", defi);
				super.create(priv);
				privilegesChanged = true;
			}
		}
		
		Application.getUserStore().refreshRole(roleId, privilegesChanged);
	}
}
