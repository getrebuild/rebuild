/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.contacts;

import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.UserFilters;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 通讯录
 * FIXME 通讯录过滤:部门用户隔离
 *
 * @author devezhao
 * @since 2024/11/6
 */
@RestController
public class ContactsController extends BaseController {

    @GetMapping("/contacts/home")
    public ModelAndView pageIndex() {
        return createModelAndView("/contacts/home");
    }

    @GetMapping("/contacts/list-depts")
    public RespBody listDepts(HttpServletRequest request) {
        final ID user = getRequestUser(request);

        JSONArray dtree = new JSONArray();
        Department[] ds = Application.getUserStore().getTopDepartments();
        Arrays.sort(ds);

        for (Department root : ds) {
            if (root.isDisabled()) continue;
            dtree.add(recursiveDeptTree(root));
        }
        return RespBody.ok(dtree);
    }

    private JSONObject recursiveDeptTree(Department parent) {
        JSONObject parentJson = new JSONObject();
        parentJson.put("id", parent.getIdentity());
        parentJson.put("name", parent.getName());
        JSONArray children = new JSONArray();

        BusinessUnit[] ds = parent.getChildren().toArray(new BusinessUnit[0]);
        Arrays.sort(ds);
        for (BusinessUnit child : ds) {
            if (child.isDisabled()) continue;
            children.add(recursiveDeptTree((Department) child));
        }

        if (!children.isEmpty()) {
            parentJson.put("children", children);
        }
        return parentJson;
    }

    @GetMapping("/contacts/list-users")
    public RespBody listUsers(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        ID dept = getIdParameter(request, "dept");
        String q = getParameter(request, "q");
        if (q != null) q = q.toUpperCase().trim();

        User[] users = Application.getUserStore().getAllUsers();
        users = (User[]) UserFilters.filterMembers32(users, user);

        Set<ID> deptIn = null;
        if (dept != null) {
            deptIn = new HashSet<>();
            deptIn.add(dept);

            Department deptBu = UserHelper.getDepartment(dept);
            if (deptBu != null) {
                for (BusinessUnit bu : deptBu.getAllChildren()) {
                    deptIn.add((ID) bu.getIdentity());
                }
            }
        }

        JSONArray array = new JSONArray();
        for (User u : users) {
            if (UserService.SYSTEM_USER.equals(u.getId())) continue;
            Department d = u.getOwningDept();
            if (deptIn != null) {
                if (d == null) continue;
                if (!deptIn.contains((ID) d.getIdentity())) continue;
            }
            if (q != null) {
                if (u.getFullName().toUpperCase().contains(q)
                        || (u.getEmail() != null && u.getEmail().contains(q))
                        || (u.getWorkphone() != null && u.getWorkphone().contains(q)));
                else continue;
            }

            JSONObject item = JSONUtils.toJSONObject(
                    new String[]{"id", "fullName", "email", "workphone", "deptName"},
                    new Object[]{u.getId(), u.getFullName(), u.getEmail(), u.getWorkphone(), d.getName()});
            array.add(item);
        }
        return RespBody.ok(array);
    }
}
