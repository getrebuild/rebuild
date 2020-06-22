/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.base;

import cn.devezhao.bizz.security.member.Member;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
 * 用户/部门/角色/团队 获取
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
@Controller
@RequestMapping("/commons/search/")
public class UsersGetting extends BaseControll {
	
	@RequestMapping("users")
	public void loadUsers(HttpServletRequest request, HttpServletResponse response) {
		final String type = getParameter(request, "type", "User");
		final String query = getParameter(request, "q");
		
		Member[] members;
		if ("User".equalsIgnoreCase(type)) {
			members = Application.getUserStore().getAllUsers();
		} else if ("Department".equalsIgnoreCase(type)) {
			members = Application.getUserStore().getAllDepartments();
		} else if ("Role".equalsIgnoreCase(type)) {
			members = Application.getUserStore().getAllRoles();
		} else if ("Team".equalsIgnoreCase(type)) {
            members = Application.getUserStore().getAllTeams();
        }  else {
			throw new IllegalParameterException("Unknow type of bizz : " + type);
		}
		// 排序
		members = UserHelper.sortMembers(members);
		
		List<JSON> ret = new ArrayList<>();
		for (Member m : members) {
			if (m.isDisabled()) {
				continue;
			}

			String name = m.getName();

			final User ifUser = m instanceof User ? (User) m : null;
			if (ifUser != null) {
			    if (!ifUser.isActive()) continue;
			    name = ifUser.getFullName();
			}

			if (StringUtils.isBlank(query)
                    || StringUtils.containsIgnoreCase(name, query)
                    || (ifUser != null && StringUtils.containsIgnoreCase(ifUser.getName(), query))
                    || (ifUser != null && ifUser.getEmail() != null && StringUtils.containsIgnoreCase(ifUser.getEmail(), query))) {
				JSONObject o = JSONUtils.toJSONObject(new String[] { "id", "text" },
						new String[] { m.getIdentity().toString(), name });
				ret.add(o);
				if (ret.size() >= 40) break;
			}
		}
		
		writeSuccess(response, ret);
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
