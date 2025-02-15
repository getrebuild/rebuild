/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.bizz.security.member.Member;
import cn.devezhao.bizz.security.member.NoMemberFoundException;
import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.bizz.CombinedRole;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.approval.FlowNode;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.ImageMaker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 用户帮助类
 *
 * @author devezhao
 * @since 10/14/2018
 */
@Slf4j
public class UserHelper {

    // 默认头像
    public static final String DEFAULT_AVATAR = "/assets/img/avatar.png";

    /**
     * 是否管理员
     *
     * @param userId
     * @return
     */
    public static boolean isAdmin(ID userId) {
        try {
            return Application.getUserStore().getUser(userId).isAdmin();
        } catch (NoMemberFoundException ex) {
            log.error("No User found : {}", userId);
        }
        return false;
    }

    /**
     * 是否超级管理员
     *
     * @param userId
     * @return
     */
    public static boolean isSuperAdmin(ID userId) {
        return UserService.ADMIN_USER.equals(userId) || UserService.SYSTEM_USER.equals(userId);
    }

    /**
     * 是否激活
     *
     * @param bizzId ID of User/Role/Department/Team
     * @return
     */
    public static boolean isActive(ID bizzId) {
        try {
            switch (bizzId.getEntityCode()) {
                case EntityHelper.User:
                    return Application.getUserStore().getUser(bizzId).isActive();
                case EntityHelper.Department:
                    return !Application.getUserStore().getDepartment(bizzId).isDisabled();
                case EntityHelper.Role:
                    return !Application.getUserStore().getRole(bizzId).isDisabled();
                case EntityHelper.Team:
                    return !Application.getUserStore().getTeam(bizzId).isDisabled();
            }

        } catch (NoMemberFoundException ex) {
            log.error("No bizz found : {}", bizzId);
        }
        return false;
    }

    /**
     * 获取用户部门
     *
     * @param userId
     * @return
     */
    public static Department getDepartment(ID userId) {
        try {
            User u = Application.getUserStore().getUser(userId);
            return u.getOwningDept();
        } catch (NoMemberFoundException ex) {
            log.error("No User found : {}", userId);
        }
        return null;
    }

    /**
     * 获取所有子部门ID（包括自己）
     *
     * @param parent
     * @return
     */
    public static Set<ID> getAllChildren(Department parent) {
        Set<ID> children = new HashSet<>();
        children.add((ID) parent.getIdentity());
        for (BusinessUnit child : parent.getAllChildren()) {
            children.add((ID) child.getIdentity());
        }
        return children;
    }

    /**
     * 获取名称
     *
     * @param bizzId ID of User/Role/Department/Team
     * @return
     */
    public static String getName(ID bizzId) {
        try {
            switch (bizzId.getEntityCode()) {
                case EntityHelper.User:
                    return Application.getUserStore().getUser(bizzId).getFullName();
                case EntityHelper.Department:
                    return Application.getUserStore().getDepartment(bizzId).getName();
                case EntityHelper.Role:
                    return Application.getUserStore().getRole(bizzId).getName();
                case EntityHelper.Team:
                    return Application.getUserStore().getTeam(bizzId).getName();
            }

        } catch (NoMemberFoundException ex) {
            log.error("No bizz found : {}", bizzId);
        }
        return null;
    }

    /**
     * @param bizzId
     * @return
     */
    public static String getNameNotry(ID bizzId) {
        if (bizzId == null) return "[NULL]";
        try {
            return getName(bizzId);
        } catch (NoMemberFoundException ignored) {
            return FieldValueHelper.MISS_REF_PLACE;
        }
    }

    /**
     * 获取部门或角色下的成员
     *
     * @param groupId ID of Role/Department/Team
     * @return
     */
    public static Member[] getMembers(ID groupId) {
        Set<Principal> ms = null;
        try {
            switch (groupId.getEntityCode()) {
                case EntityHelper.Department:
                    ms = Application.getUserStore().getDepartment(groupId).getMembers();
                    break;
                case EntityHelper.Role:
                    ms = Application.getUserStore().getRole(groupId).getMembers();
                    break;
                case EntityHelper.Team:
                    ms = Application.getUserStore().getTeam(groupId).getMembers();
                    break;
                default:
                    break;
            }

        } catch (NoMemberFoundException ex) {
            log.error("No group found : " + groupId);
        }

        if (ms == null || ms.isEmpty()) {
            return new Member[0];
        }
        //noinspection SuspiciousToArrayCall
        return ms.toArray(new Member[0]);
    }

    /**
     * @param userDefs
     * @param record
     * @return
     * @see #parseUsers(Collection, ID, boolean)
     */
    public static Set<ID> parseUsers(JSONArray userDefs, ID record) {
        return parseUsers(userDefs, record, false);
    }

    /**
     * @param userDefs
     * @param record
     * @param filterDisabled
     * @return
     * @see #parseUsers(Collection, ID, boolean)
     */
    public static Set<ID> parseUsers(JSONArray userDefs, ID record, boolean filterDisabled) {
        if (userDefs == null) return Collections.emptySet();

        Set<String> users = new HashSet<>();
        for (Object u : userDefs) {
            users.add((String) u);
        }
        return parseUsers(users, record, filterDisabled);
    }

    /**
     * @param userDefs
     * @param record
     * @return
     * @see #parseUsers(Collection, ID, boolean)
     */
    public static Set<ID> parseUsers(Collection<String> userDefs, ID record) {
        return parseUsers(userDefs, record, false);
    }

    /**
     * 解析用户列表
     *
     * @param userDefs
     * @param recordId
     * @param filterDisabled
     * @return
     */
    public static Set<ID> parseUsers(Collection<String> userDefs, ID recordId, boolean filterDisabled) {
        Entity entity = recordId == null ? null : MetadataHelper.getEntity(recordId.getEntityCode());

        Set<ID> bizzs = new HashSet<>();
        Set<String> useFields = new HashSet<>();
        for (String def : userDefs) {
            if (ID.isId(def)) {
                bizzs.add(ID.valueOf(def));
            } else if (entity != null && MetadataHelper.getLastJoinField(entity, def) != null) {
                useFields.add(def);
            } else {
                if (FlowNode.USER_OWNS.equals(def));  // No warn
                else log.warn("Invalid field or id : {}", def);
            }
        }

        if (!useFields.isEmpty()) {
            useFields.add(entity.getPrimaryField().getName());
            Record bizzValue = Application.getQueryFactory().recordNoFilter(recordId, useFields.toArray(new String[0]));

            if (bizzValue != null) {
                for (String field : bizzValue.getAvailableFields()) {
                    Object value = bizzValue.getObjectValue(field);
                    if (value == null) continue;

                    if (value instanceof ID[]) {
                        CollectionUtils.addAll(bizzs, (ID[]) value);
                    } else {
                        bizzs.add((ID) value);
                    }
                }
            }
        }

        Set<ID> users = new HashSet<>();
        for (ID bizz : bizzs) {
            if (bizz.getEntityCode() == EntityHelper.User) {
                users.add(bizz);
            } else {
                Member[] ms = getMembers(bizz);
                for (Member m : ms) {
                    if (m.getIdentity().equals(UserService.SYSTEM_USER)) continue;
                    users.add((ID) m.getIdentity());
                }
            }
        }

        // 过滤禁用用户
        if (filterDisabled) {
            for (Iterator<ID> iter = users.iterator(); iter.hasNext(); ) {
                User u = Application.getUserStore().getUser(iter.next());
                if (!u.isActive()) iter.remove();
            }
        }

        return users;
    }

    /**
     * 生成用户头像
     *
     * @param name
     * @param forceMake
     * @return
     * @see ImageMaker
     */
    public static File generateAvatar(String name, boolean forceMake) {
        if (StringUtils.isBlank(name)) name = "RB";

        File avatarFile = RebuildConfiguration.getFileOfData("avatar-" + name + "29.jpg");
        if (avatarFile.exists()) {
            if (forceMake) {
                FileUtils.deleteQuietly(avatarFile);
            } else {
                return avatarFile;
            }
        }

        ImageMaker.makeAvatar(name, avatarFile);
        return avatarFile;
    }

    /**
     * 通过用户全称找用户（注意同名问题）
     *
     * @param fullName
     * @return
     */
    public static ID findUserByFullName(String fullName) {
        for (User u : Application.getUserStore().getAllUsers()) {
            if (fullName.equalsIgnoreCase(u.getFullName())) {
                return u.getId();
            }
        }
        return null;
    }

    /**
     * @return
     * @see #sortUsers(boolean)
     */
    public static User[] sortUsers() {
        return sortUsers(Boolean.FALSE);
    }

    /**
     * 按全称排序的用户列表
     *
     * @param isAll 是否包括未激活用户
     * @return
     * @see UserStore#getAllUsers()
     */
    public static User[] sortUsers(boolean isAll) {
        User[] users = Application.getUserStore().getAllUsers();
        // 排除未激活
        if (!isAll) {
            List<User> list = new ArrayList<>();
            for (User u : users) {
                if (u.isActive()) {
                    list.add(u);
                }
            }
            users = list.toArray(new User[0]);
        }

        sortMembers(users);
        return users;
    }

    /**
     * 成员排序
     *
     * @param members
     * @return
     */
    public static Member[] sortMembers(Member[] members) {
        if (members == null || members.length == 0) {
            return new Member[0];
        }

        if (members[0] instanceof Comparable) {
            Arrays.sort(members);
        } else {
            Arrays.sort(members, Comparator.comparing(Member::getName));
        }
        return members;
    }

    /**
     * 获取用户的附加角色
     *
     * @param user
     * @return
     * @see CombinedRole
     */
    public static Set<ID> getRoleAppends(ID user) {
        Role role = Application.getUserStore().getUser(user).getOwningRole();
        if (role instanceof CombinedRole) {
            return ((CombinedRole) role).getRoleAppends();
        }
        return null;
    }

    /**
     * 获取用户的所有角色
     *
     * @param userId
     * @return
     */
    public static Set<ID> getRolesOfUser(ID userId) {
        Role role = Application.getUserStore().getUser(userId).getOwningRole();
        Set<ID> s = new HashSet<>();
        if (role != null) {
            s.add((ID) role.getIdentity());
            if (role instanceof CombinedRole) {
                s.addAll(((CombinedRole) role).getRoleAppends());
            }
        }
        return Collections.unmodifiableSet(s);
    }

    /**
     * 获取用户的所有团队
     *
     * @param userId
     * @return
     */
    public static Set<ID> getTeamsOfUser(ID userId) {
        Set<ID> s = new HashSet<>();
        for (Team t : Application.getUserStore().getUser(userId).getOwningTeams()) {
            s.add((ID) t.getIdentity());
        }
        return Collections.unmodifiableSet(s);
    }

    /**
     * 获取附加了指定角色的用户
     *
     * @param roleId
     * @return
     */
    public static Set<ID> getMembersOfRole(ID roleId) {
        Object[][] array = Application.createQueryNoFilter(
                "select userId from RoleMember where roleId = ?")
                .setParameter(1, roleId)
                .array();

        Set<ID> s = new HashSet<>();
        for (Object[] o : array) s.add((ID) o[0]);
        return s;
    }

    /**
     * 是否是自己的（管理员有特殊处理）
     *
     * @param user
     * @param otherUserOrAnyRecordId
     * @return
     */
    public static boolean isSelf(ID user, ID otherUserOrAnyRecordId) {
        try {
            ID createdBy = otherUserOrAnyRecordId;
            if (otherUserOrAnyRecordId.getEntityCode() != EntityHelper.User) {
                createdBy = getCreatedBy(otherUserOrAnyRecordId);
                if (createdBy == null) return false;
            }

            if (createdBy.equals(user)) return true;

            // 所有管理员被视为同一用户
            return isAdmin(createdBy) && isAdmin(user);
        } catch (Exception ex) {
            log.warn("Check isSelf error : {}, {}", user, otherUserOrAnyRecordId, ex);
            return false;
        }
    }

    private static ID getCreatedBy(ID anyRecordId) {
        final String ckey = "CreatedBy-" + anyRecordId;
        ID createdBy = (ID) Application.getCommonsCache().getx(ckey);
        if (createdBy != null) {
            return createdBy;
        }

        Entity entity = MetadataHelper.getEntity(anyRecordId.getEntityCode());
        if (!entity.containsField(EntityHelper.CreatedBy)) {
            log.warn("No [createdBy] field in [{}]", entity.getEntityCode());
            return null;
        }

        Object[] c = Application.getQueryFactory().uniqueNoFilter(anyRecordId, EntityHelper.CreatedBy);
        if (c == null) {
            throw new RebuildException("No record found : " + anyRecordId);
        }

        createdBy = (ID) c[0];
        Application.getCommonsCache().putx(ckey, createdBy);
        return createdBy;
    }
}
