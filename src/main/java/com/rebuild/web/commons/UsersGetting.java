/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.bizz.security.member.Member;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.InvalidParameterException;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户/部门/角色/团队 获取
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
@RestController
@RequestMapping("/commons/search/")
public class UsersGetting extends BaseController {

    @GetMapping("users")
    public JSON loadUsers(HttpServletRequest request) {
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
        } else {
            throw new InvalidParameterException("Unknown type of bizz entity : " + type);
        }
        // 排序
        members = UserHelper.sortMembers(members);

        JSONArray found = new JSONArray();

        // 全部用户
        if (getBoolParameter(request, "atall") && "User".equals(type) && StringUtils.isBlank(query)
                && Application.getPrivilegesManager().allow(getRequestUser(request), ZeroEntry.AllowAtAllUsers)) {
            found.add(JSONUtils.toJSONObject(
                    new String[]{"id", "text"}, new Object[]{UserService.ALLUSERS, Language.L("所有人")}));
        }

        for (Member m : members) {
            if (m.isDisabled()) continue;

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

                found.add(JSONUtils.toJSONObject(
                        new String[]{"id", "text"}, new Object[]{m.getIdentity(), name}));

                // 最多显示40个
                if (found.size() >= 40) break;
            }
        }

        return found;
    }

    /**
     * 获取符合 UserSelector 组件的数据
     *
     * @param request
     * @see UserHelper#parseUsers(JSONArray, ID)
     */
    @PostMapping("user-selector")
    public JSON parseUserSelectorRaw(HttpServletRequest request, @EntityParam(required = false) Entity useEntity) {
        final JSON users = ServletUtils.getRequestJson(request);

        List<String[]> shows = new ArrayList<>();
        for (Object item : (JSONArray) users) {
            String idOrField = (String) item;
            if (ID.isId(idOrField)) {
                String name = UserHelper.getName(ID.valueOf(idOrField));
                if (name != null) {
                    shows.add(new String[] { idOrField, name });
                }

            } else if (useEntity != null && MetadataHelper.getLastJoinField(useEntity, idOrField) != null) {
                String fieldLabel = EasyMetaFactory.getLabel(useEntity, idOrField);
                shows.add(new String[] { idOrField, fieldLabel });
            }
        }

        return JSONUtils.toJSONObjectArray(
                new String[] {  "id", "text" }, shows.toArray(new String[0][]));
    }
}
