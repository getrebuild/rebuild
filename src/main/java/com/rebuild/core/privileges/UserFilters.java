/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.bizz.security.member.Member;
import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户过滤类
 *
 * @author devezhao
 * @since 5/28/2023
 */
@Slf4j
public class UserFilters {

    /**
     * @param user
     * @return
     * @see ZeroEntry#EnableBizzPart
     */
    public static boolean isEnableBizzPart(ID user) {
        if (UserHelper.isAdmin(user)) return false;
        return Application.getPrivilegesManager().allow(user, ZeroEntry.EnableBizzPart);
    }

    /**
     * 是否允许访问
     *
     * @param user
     * @param bizzId
     * @return
     */
    public static boolean allowAccessBizz(ID user, ID bizzId) {
        if (user.equals(bizzId) || UserHelper.isAdmin(user)) return true;
        if (bizzId.getEntityCode() == EntityHelper.Role) return false;

        if (isEnableBizzPart(user)) {
            final User ub = Application.getUserStore().getUser(user);

            if (bizzId.getEntityCode() == EntityHelper.Department) {
                return ub.getOwningDept() != null && bizzId.equals(ub.getOwningDept().getIdentity());
            }

            if (bizzId.getEntityCode() == EntityHelper.Team) {
                for (Team m : ub.getOwningTeams()) {
                    if (bizzId.equals(m.getIdentity())) return true;
                }
                return false;
            }

            if (bizzId.getEntityCode() == EntityHelper.User) {
                return user.equals(bizzId);
            }
        }

        return true;
    }

    /**
     * 过滤
     *
     * @param members
     * @param currentUser
     * @return
     */
    @SuppressWarnings({"ConstantValue", "SuspiciousMethodCalls"})
    public static Member[] filterMembers32(Member[] members, ID currentUser) {
        if (members == null || members.length == 0) return members;
        if (!isEnableBizzPart(currentUser)) return members;

        final String depth = "D";  // support: L,D,G

        final User user = Application.getUserStore().getUser(currentUser);
        final Department dept = user.getOwningDept();
        final Set<ID> deptChildren = "D".equals(depth) ? UserHelper.getAllChildren(dept) : Collections.emptySet();

        Set<Member> filteredMembers = new HashSet<>();
        for (Member m : members) {
            if (m instanceof User) {
                // 本部门
                if (dept.isMember(m.getIdentity())) filteredMembers.add(m);
                // 下级部门
                if ("D".equals(depth)) {
                    for (ID child : deptChildren) {
                        if (Application.getUserStore().getDepartment(child).isMember(m.getIdentity())) {
                            filteredMembers.add(m);
                        }
                    }
                }

            } else if (m instanceof Department) {
                // 本部门
                if (dept.equals(m)) filteredMembers.add(m);
                // 下级部门
                if ("D".equals(depth) && deptChildren.contains(m.getIdentity())) filteredMembers.add(m);

            } else if (m instanceof Team) {
                // 团队成员
                if (((Team) m).isMember(user.getId())) filteredMembers.add(m);
            }
        }

        return filteredMembers.toArray(new Member[0]);
    }

    /**
     * 部门用户隔离 SQL 条件
     *
     * @param bizzEntityCode
     * @param user
     * @return
     */
    public static String getEnableBizzPartFilter(int bizzEntityCode, ID user) {
        if (!isEnableBizzPart(user)) return null;

        final User ub = Application.getUserStore().getUser(user);
        Set<ID> in = new HashSet<>();
        String where;

        if (bizzEntityCode == EntityHelper.Team) {
            for (Member m : ub.getOwningTeams()) in.add((ID) m.getIdentity());

            if (in.isEmpty()) {
                where = "1=2";
            } else {
                where = "teamId in ('" + StringUtils.join(in, "','") + "')";
            }

        } else {
            if (bizzEntityCode == EntityHelper.Role) return "1=2";

            in.add((ID) ub.getOwningDept().getIdentity());
            in.addAll(UserHelper.getAllChildren(ub.getOwningDept()));

            // `deptId` in User and Department
            where = "deptId in ('" + StringUtils.join(in, "','") + "')";
        }

        return where;
    }
}
