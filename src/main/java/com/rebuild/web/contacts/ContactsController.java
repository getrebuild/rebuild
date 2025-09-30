/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.contacts;

import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.bizz.security.member.Member;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hankcs.hanlp.HanLP;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.UserFilters;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Comparator;
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
        Department[] ds;
        if (UserFilters.isEnableBizzPart(user)) {
            User ub = Application.getUserStore().getUser(user);
            ds = new Department[]{ub.getOwningDept()};
        } else {
            ds = Application.getUserStore().getTopDepartments();
            Arrays.sort(ds);
        }

        JSONArray dtree = new JSONArray();
        for (Department d : ds) {
            if (d.isDisabled()) continue;
            dtree.add(recursiveDeptTree(d));
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
        final ID dept = getIdParameter(request, "dept");
        String q = getParameter(request, "q");
        if (q != null) q = q.toUpperCase().trim();

        String sort42 = getParameter(request, "sort");
        boolean sortNewer = "newer".equals(sort42);

        User[] users = Application.getUserStore().getAllUsers();
        Member[] usersMembers = UserFilters.filterMembers32(users, user);
        if ("name".equals(sort42)) {
            Arrays.sort(usersMembers, Comparator.comparing(Member::getName));
        } else if (sortNewer) {
            // 新建
        } else {
            usersMembers = UserHelper.sortMembers(usersMembers);
        }

        Set<ID> deptAndChild = null;
        if (dept != null) {
            deptAndChild = new HashSet<>();
            deptAndChild.add(dept);

            Department deptObj = Application.getUserStore().getDepartment(dept);
            if (deptObj != null) {
                for (BusinessUnit bu : deptObj.getAllChildren()) {
                    deptAndChild.add((ID) bu.getIdentity());
                }
            }
        }

        JSONArray array = new JSONArray();
        for (Member m : usersMembers) {
            User u = (User) m;
            if (UserService.SYSTEM_USER.equals(u.getId())) continue;
//            if (!u.isActive()) continue;

            Department d = u.getOwningDept();
            if (deptAndChild != null) {
                if (d == null) continue;
                if (!deptAndChild.contains((ID) d.getIdentity())) continue;
            }

            if (q != null) {
                if (q.endsWith("*")) {
                    String prefix = q.substring(0, q.length() - 1);
                    // A-Z
                    if (prefix.length() == 1 && Character.isUpperCase(prefix.charAt(0))) {
                        String piny = HanLP.convertToPinyinString(u.getFullName(), "", false);
                        if (!piny.toUpperCase().startsWith(prefix)) {
                            continue;
                        }
                    }
                    // prefix
                    else if (!(StringUtils.startsWithIgnoreCase(u.getFullName(), q)
                            || StringUtils.startsWithIgnoreCase(u.getEmail(), q)
                            || StringUtils.startsWithIgnoreCase(u.getWorkphone(), q))) {
                        continue;
                    }
                }
                // includes
                else if (!(StringUtils.containsIgnoreCase(u.getFullName(), q)
                        || StringUtils.containsIgnoreCase(u.getEmail(), q)
                        || StringUtils.containsIgnoreCase(u.getWorkphone(), q))) {
                    continue;
                }
            }

            JSONObject item = JSONUtils.toJSONObject(
                    new String[]{"id", "fullName", "email", "workphone", "deptName", "avtive"},
                    new Object[]{u.getId(), u.getFullName(), u.getEmail(), u.getWorkphone(), d == null ? "-" : d.getName(), u.isActive()});
            if (sortNewer) {
                item.put("_created", QueryHelper.queryFieldValue(u.getId(), "createdOn"));
            }
            array.add(item);
        }

        if (sortNewer) {
            array.sort((o1, o2) -> {
                JSONObject o11 = (JSONObject) o1;
                JSONObject o22 = (JSONObject) o2;
                return o11.getDate("_created").compareTo(o22.getDate("_created"));
            });
        }

        return RespBody.ok(array);
    }
}
