/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.web.base;

import cn.devezhao.bizz.security.member.Member;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import com.rebuild.web.IllegalParameterException;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户/部门/角色获取
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
@Controller
@RequestMapping("/commons/search/")
public class UsersGetting extends BaseControll {
	
	@RequestMapping("users")
	public void loadUsers(HttpServletRequest request, HttpServletResponse response) {
		String type = getParameter(request, "type", "User");
		String q = getParameter(request, "q");
		
		Member[] members;
		if ("User".equalsIgnoreCase(type)) {
			members = Application.getUserStore().getAllUsers();
		} else if ("Department".equalsIgnoreCase(type)) {
			members = Application.getUserStore().getAllDepartments();
		} else if ("Role".equalsIgnoreCase(type)) {
			members = Application.getUserStore().getAllRoles();
		} else {
			throw new IllegalParameterException("Unknow type of bizz : " + type);
		}
		
		List<JSON> filtered = new ArrayList<>();
		for (Member m : members) {
			if (m.isDisabled()) {
				continue;
			}
			
			String name = m.getName();
			if (m instanceof User) {
				name = ((User) m).getFullName();
				if (!((User) m).isActive()) {
					continue;
				}
			}
			
			if (StringUtils.isBlank(q) || (StringUtils.isNotBlank(q) && name.contains(q))) {
				JSON item = JSONUtils.toJSONObject(new String[] { "id", "text" },
						new String[] { m.getIdentity().toString(), name });
				filtered.add(item);
				if (filtered.size() >= 100) {
					break;
				}
			}
		}
		
		writeSuccess(response, filtered);
	}

	/**
	 * 获取符合 UserSelector 组件的数据
	 *
	 * @param request
	 * @param response
	 * @throws IOException
	 *
	 * @see UserHelper#parseUsers(JSONArray, ID)
	 */
	@RequestMapping("user-selector")
	public void parseUserSelectorRaw(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameter(request, "entity");
		JSON users = ServletUtils.getRequestJson(request);
		Entity hadEntity = MetadataHelper.containsEntity(entity) ? MetadataHelper.getEntity(entity) : null;
		
		List<JSON> formatted = new ArrayList<>();
		String[] keys = new String[] { "id", "text" };
		for (Object item : (JSONArray) users) {
			String idOrField = (String) item;
			if (ID.isId(idOrField)) {
				String name = UserHelper.getName(ID.valueOf(idOrField));
				if (name != null) {
					formatted.add(JSONUtils.toJSONObject(keys, new String[] { idOrField, name }));
				}
			} else if (hadEntity != null && hadEntity.containsField(idOrField.split("//.")[0])) {
				String fullLabel = EasyMeta.getLabel(hadEntity, idOrField);
				formatted.add(JSONUtils.toJSONObject(keys, new String[] { idOrField, fullLabel }));
			}
		}
		writeSuccess(response, formatted);
	}
}
