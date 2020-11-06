/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.EntityPrivileges;
import cn.devezhao.bizz.security.member.*;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Initialization;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.bizz.CombinedRole;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.privileges.bizz.ZeroPrivileges;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户体系缓存
 *
 * @author zhaofang123@gmail.com
 * @since 09/16/2018
 */
@Component
public class UserStore implements Initialization {

    private static final Logger LOG = LoggerFactory.getLogger(UserStore.class);

    final private Map<ID, User> USERS = new ConcurrentHashMap<>();
    final private Map<ID, Role> ROLES = new ConcurrentHashMap<>();
    final private Map<ID, Department> DEPTS = new ConcurrentHashMap<>();
    final private Map<ID, Team> TEAMS = new ConcurrentHashMap<>();

    final private Map<String, ID> USERS_NAME2ID = new ConcurrentHashMap<>();
    final private Map<String, ID> USERS_MAIL2ID = new ConcurrentHashMap<>();

    final private PersistManagerFactory aPMFactory;

    private boolean isLoaded;

    protected UserStore(PersistManagerFactory aPMFactory) {
        this.aPMFactory = aPMFactory;
    }

    /**
     * @param username
     * @return
     */
    public boolean existsName(String username) {
        return USERS_NAME2ID.containsKey(normalIdentifier(username));
    }

    /**
     * @param email
     * @return
     */
    public boolean existsEmail(String email) {
        return USERS_MAIL2ID.containsKey(normalIdentifier(email));
    }

    /**
     * @param emailOrName
     * @return
     */
    public boolean existsUser(String emailOrName) {
        return existsName(emailOrName) || existsEmail(emailOrName);
    }

    /**
     * @param userId
     * @return
     */
    public boolean existsUser(ID userId) {
        return USERS.containsKey(userId);
    }

    /**
     * @param bizzId
     * @return
     */
    public boolean existsAny(ID bizzId) {
        if (bizzId.getEntityCode() == EntityHelper.User) {
            return USERS.containsKey(bizzId);
        } else if (bizzId.getEntityCode() == EntityHelper.Role) {
            return ROLES.containsKey(bizzId);
        } else if (bizzId.getEntityCode() == EntityHelper.Department) {
            return DEPTS.containsKey(bizzId);
        } else if (bizzId.getEntityCode() == EntityHelper.Team) {
            return TEAMS.containsKey(bizzId);
        }
        return false;
    }

    /**
     * @param username
     * @return
     * @throws NoMemberFoundException
     */
    public User getUserByName(String username) throws NoMemberFoundException {
        ID userId = USERS_NAME2ID.get(normalIdentifier(username));
        if (userId == null) {
            throw new NoMemberFoundException("No User found: " + username);
        }
        return getUser(userId);
    }

    /**
     * @param email
     * @return
     * @throws NoMemberFoundException
     */
    public User getUserByEmail(String email) throws NoMemberFoundException {
        ID userId = USERS_MAIL2ID.get(normalIdentifier(email));
        if (userId == null) {
            throw new NoMemberFoundException("No User found: " + email);
        }
        return getUser(userId);
    }

    /**
     * @param emailOrName
     * @return
     * @throws NoMemberFoundException
     */
    public User getUser(String emailOrName) throws NoMemberFoundException {
        if (existsEmail(emailOrName)) {
            return getUserByEmail(emailOrName);
        } else {
            return getUserByName(emailOrName);
        }
    }

    /**
     * @param userId
     * @return
     * @throws NoMemberFoundException
     */
    public User getUser(ID userId) throws NoMemberFoundException {
        User u = USERS.get(userId);
        if (u == null) {
            throw new NoMemberFoundException("No User found: " + userId);
        }
        return u;
    }

    /**
     * @return
     */
    public User[] getAllUsers() {
        return USERS.values().toArray(new User[0]);
    }

    /**
     * @param deptId
     * @return
     * @throws NoMemberFoundException
     */
    public Department getDepartment(ID deptId) throws NoMemberFoundException {
        Department b = DEPTS.get(deptId);
        if (b == null) {
            throw new NoMemberFoundException("No Department found: " + deptId);
        }
        return b;
    }

    /**
     * @return
     */
    public Department[] getAllDepartments() {
        return DEPTS.values().toArray(new Department[0]);
    }

    /**
     * 获取一级部门列表
     *
     * @return
     */
    public Department[] getTopDepartments() {
        List<Department> top = new ArrayList<>();
        for (Department dept : DEPTS.values()) {
            if (dept.getParent() == null) {
                top.add(dept);
            }
        }
        return top.toArray(new Department[0]);
    }

    /**
     * @param roleId
     * @return
     * @throws NoMemberFoundException
     */
    public Role getRole(ID roleId) throws NoMemberFoundException {
        Role r = ROLES.get(roleId);
        if (r == null) {
            throw new NoMemberFoundException("No Role found: " + roleId);
        }
        return r;
    }

    /**
     * @return
     */
    public Role[] getAllRoles() {
        return ROLES.values().toArray(new Role[0]);
    }

    /**
     * @param teamId
     * @return
     * @throws NoMemberFoundException
     */
    public Team getTeam(ID teamId) throws NoMemberFoundException {
        Team t = TEAMS.get(teamId);
        if (t == null) {
            throw new NoMemberFoundException("No Team found: " + teamId);
        }
        return t;
    }

    /**
     * @return
     */
    public Team[] getAllTeams() {
        return TEAMS.values().toArray(new Team[0]);
    }

    /**
     * 刷新用户
     *
     * @param userId
     */
    public void refreshUser(ID userId) {
        Object[] o = aPMFactory.createQuery("select " + USER_FS + " from User where userId = ?")
                .setParameter(1, userId)
                .unique();
        final User newUser = new User(
                userId, (String) o[1], (String) o[2], (String) o[8], (String) o[3], (String) o[4], (Boolean) o[5]);
        final ID deptId = (ID) o[6];
        final ID roleId = (ID) o[7];

        final User oldUser = existsUser(userId) ? getUser(userId) : null;
        if (oldUser != null) {
            Role role = oldUser.getOwningRole();
            if (role != null) {
                role.removeMember(oldUser);
            }

            Department dept = oldUser.getOwningDept();
            if (dept != null) {
                dept.removeMember(oldUser);
            }

            for (Team team : oldUser.getOwningTeams().toArray(new Team[0])) {
                team.removeMember(oldUser);
                team.addMember(newUser);
            }

            // 邮箱可更改
            if (oldUser.getEmail() != null) {
                USERS_MAIL2ID.remove(normalIdentifier(oldUser.getEmail()));
            }
        }

        if (deptId != null) {
            getDepartment(deptId).addMember(newUser);
        }

        if (roleId != null) {
            getRole(roleId).addMember(newUser);
            refreshUserRoleAppends(newUser);
        }

        store(newUser);
    }

    /**
     * 移除用户
     *
     * @param userId
     */
    public void removeUser(ID userId) {
        final User oldUser = getUser(userId);

        // 移除成员
        if (oldUser.getOwningDept() != null) {
            oldUser.getOwningDept().removeMember(oldUser);
        }
        if (oldUser.getOwningRole() != null) {
            oldUser.getOwningRole().removeMember(oldUser);
        }
        for (Team team : oldUser.getOwningTeams()) {
            team.removeMember(oldUser);
        }

        // 移除缓存
        for (Map.Entry<String, ID> e : USERS_NAME2ID.entrySet()) {
            if (e.getValue().equals(userId)) {
                USERS_NAME2ID.remove(e.getKey());
                break;
            }
        }
        for (Map.Entry<String, ID> e : USERS_MAIL2ID.entrySet()) {
            if (e.getValue().equals(userId)) {
                USERS_MAIL2ID.remove(e.getKey());
                break;
            }
        }
        USERS.remove(userId);
    }

    /**
     * 刷新角色
     *
     * @param roleId
     */
    public void refreshRole(ID roleId) {
        final Role oldRole = ROLES.get(roleId);
        if (oldRole != null) {
            for (Principal u : toMemberArray(oldRole)) {
                oldRole.removeMember(u);
            }
        }

        Object[] o = aPMFactory.createQuery("select roleId,name,isDisabled from Role where roleId = ?")
                .setParameter(1, roleId)
                .unique();
        final Role newRole = new Role(roleId, (String) o[1], (Boolean) o[2]);

        // Members
        Object[][] array = aPMFactory.createQuery("select userId from User where roleId = ?")
                .setParameter(1, roleId)
                .array();
        for (Object[] member : array) {
            newRole.addMember(getUser((ID) member[0]));
        }

        loadPrivileges(newRole);

        ROLES.put(roleId, newRole);
        refreshRoleAppends(roleId);
    }

    /**
     * 移除角色
     *
     * @param roleId
     * @param transferTo
     */
    public void removeRole(ID roleId, ID transferTo) {
        final Role role = getRole(roleId);
        // 转至新角色
        if (transferTo != null) {
            Role transferToRole = getRole(transferTo);
            for (Principal user : role.getMembers()) {
                transferToRole.addMember(user);
            }
        }

        for (Principal u : toMemberArray(role)) {
            role.removeMember(u);
        }

        ROLES.remove(roleId);
        refreshRoleAppends(roleId);
    }

    /**
     * 刷新部门
     *
     * @param deptId
     */
    public void refreshDepartment(ID deptId) {
        final Department oldDept = DEPTS.get(deptId);
        if (oldDept != null) {
            for (Principal u : toMemberArray(oldDept)) {
                oldDept.removeMember(u);
            }
        }

        Object[] o = aPMFactory.createQuery("select name,isDisabled,parentDept from Department where deptId = ?")
                .setParameter(1, deptId)
                .unique();
        final Department newDept = new Department(deptId, (String) o[0], (Boolean) o[1]);

        // Members
        Object[][] array = aPMFactory.createQuery("select userId from User where deptId = ?")
                .setParameter(1, deptId)
                .array();
        for (Object[] member : array) {
            newDept.addMember(getUser((ID) member[0]));
        }

        // 组织父子级部门关系
        final ID newParent = (ID) o[2];
        if (oldDept != null) {
            BusinessUnit oldParent = oldDept.getParent();
            if (oldParent != null) {
                oldParent.removeChild(oldDept);
                if (oldParent.getIdentity().equals(newParent)) {
                    oldParent.addChild(newDept);
                } else if (newParent != null) {
                    getDepartment(newParent).addChild(newDept);
                }

            } else if (newParent != null) {
                getDepartment(newParent).addChild(newDept);
            }

            for (BusinessUnit child : oldDept.getChildren().toArray(new BusinessUnit[0])) {
                oldDept.removeChild(child);
                newDept.addChild(child);
            }

        } else if (newParent != null && DEPTS.get(newParent) != null /* init */) {
            getDepartment(newParent).addChild(newDept);
        }

        DEPTS.put(deptId, newDept);
    }

    /**
     * 移除部门
     *
     * @param deptId
     * @param transferTo
     */
    public void removeDepartment(ID deptId, ID transferTo) {
        final Department dept = getDepartment(deptId);
        // 转至新部门
        if (transferTo != null) {
            Department transferToDept = getDepartment(transferTo);
            for (Principal user : dept.getMembers()) {
                transferToDept.addMember(user);
            }
        }

        if (dept.getParent() != null) {
            dept.getParent().removeChild(dept);
        }
        for (Principal u : toMemberArray(dept)) {
            dept.removeMember(u);
        }
        DEPTS.remove(deptId);
    }

    /**
     * 刷新团队
     *
     * @param teamId
     */
    public void refreshTeam(ID teamId) {
        final Team oldTeam = TEAMS.get(teamId);
        if (oldTeam != null) {
            for (Principal u : toMemberArray(oldTeam)) {
                oldTeam.removeMember(u);
            }
        }

        Object[] o = aPMFactory.createQuery("select teamId,name,isDisabled from Team where teamId = ?")
                .setParameter(1, teamId)
                .unique();
        final Team newTeam = new Team(teamId, (String) o[1], (Boolean) o[2]);

        // Members
        Object[][] array = aPMFactory.createQuery("select userId from TeamMember where teamId = ?")
                .setParameter(1, teamId)
                .array();
        for (Object[] member : array) {
            newTeam.addMember(getUser((ID) member[0]));
        }

        TEAMS.put(teamId, newTeam);
    }

    /**
     * 移除团队
     *
     * @param teamId
     */
    public void removeTeam(ID teamId) {
        final Team team = getTeam(teamId);
        for (Principal u : toMemberArray(team)) {
            team.removeMember(u);
        }
        TEAMS.remove(teamId);
    }

    /**
     * @param user
     */
    private void refreshUserRoleAppends(User user) {
        // 最高权限无需合并
        if (user.getMainRole().getIdentity().equals(RoleService.ADMIN_ROLE)) {
            return;
        }

        // 附加权限（角色）
        Object[][] appends = aPMFactory.createQuery("select roleId from RoleMember where userId = ?")
                .setParameter(1, user.getId())
                .array();
        Set<Role> actived = new HashSet<>();
        for (Object[] a : appends) {
            Role role = ROLES.get(a[0]);
            if (role != null && !role.isDisabled()) {
                actived.add(role);
            }
        }

        if (actived.isEmpty()) return;
        new CombinedRole(user, actived);
    }

    /**
     * @param roleId
     */
    private void refreshRoleAppends(ID roleId) {
        // 初始化时无需处理
        if (!isLoaded) return;

        for (User user : USERS.values()) {
            Role role = user.getOwningRole();
            if (role == null) continue;

            if (role.getIdentity().equals(roleId)
                    || (role instanceof CombinedRole && ((CombinedRole) role).getRoleAppends().contains(roleId))) {
                refreshUserRoleAppends(user);
            }
        }
    }

    private static final String USER_FS = "userId,loginName,email,fullName,avatarUrl,isDisabled,deptId,roleId,workphone";

    @Override
    public void init() {
        // 用户

        Object[][] array = aPMFactory.createQuery("select " + USER_FS + " from User").array();
        for (Object[] o : array) {
            ID userId = (ID) o[0];
            User user = new User(
                    userId, (String) o[1], (String) o[2], (String) o[8], (String) o[3], (String) o[4], (Boolean) o[5]);
            store(user);
        }
        LOG.info("Loaded [ " + USERS.size() + " ] users.");

        // 角色

        array = aPMFactory.createQuery("select roleId from Role").array();
        for (Object[] o : array) {
            this.refreshRole((ID) o[0]);
        }
        LOG.info("Loaded [ " + ROLES.size() + " ] roles.");

        // 附加角色
        for (User user : USERS.values()) {
            if (user.getMainRole() != null) {
                refreshUserRoleAppends(user);
            }
        }

        // 部门

        array = aPMFactory.createQuery("select deptId,parentDept from Department").array();
        Map<ID, Set<ID>> parentTemp = new HashMap<>();
        for (Object[] o : array) {
            ID deptId = (ID) o[0];
            this.refreshDepartment(deptId);

            ID parent = (ID) o[1];
            if (parent != null) {
                Set<ID> child = parentTemp.computeIfAbsent(parent, k -> new HashSet<>());
                child.add(deptId);
            }
        }

        // 组织部门关系
        for (Map.Entry<ID, Set<ID>> e : parentTemp.entrySet()) {
            BusinessUnit parent = getDepartment(e.getKey());
            for (ID child : e.getValue()) {
                parent.addChild(getDepartment(child));
            }
        }

        LOG.info("Loaded [ " + DEPTS.size() + " ] departments.");

        // 团队

        array = aPMFactory.createQuery("select teamId from Team").array();
        for (Object[] o : array) {
            this.refreshTeam((ID) o[0]);
        }
        LOG.info("Loaded [ " + TEAMS.size() + " ] teams.");

        isLoaded = true;
    }

    /**
     * @param user
     */
    private void store(User user) {
        USERS.put(user.getId(), user);
        USERS_NAME2ID.put(normalIdentifier(user.getName()), user.getId());
        if (user.getEmail() != null) {
            USERS_MAIL2ID.put(normalIdentifier(user.getEmail()), user.getId());
        }
    }

    // 统一化 Key
    private String normalIdentifier(String ident) {
        return StringUtils.defaultIfEmpty(ident, "").toLowerCase();
    }

    // Fix: ConcurrentModificationException
    private Principal[] toMemberArray(MemberGroup group) {
        return group.getMembers().toArray(new Principal[0]);
    }

    /**
     * @param role
     */
    private void loadPrivileges(Role role) {
        Object[][] definition = aPMFactory.createQuery(
                "select entity,definition,zeroKey from RolePrivileges where roleId = ?")
                .setParameter(1, role.getIdentity())
                .array();
        for (Object[] d : definition) {
            int entity = (int) d[0];
            Privileges p;
            if (entity == 0) {
                p = new ZeroPrivileges((String) d[2], (String) d[1]);
            } else {
                p = new EntityPrivileges(entity, converEntityPrivilegesDefinition((String) d[1]));
            }
            role.addPrivileges(p);
        }
    }

    /**
     * 转换成 bizz 能识别的权限定义
     *
     * @param definition
     * @return
     * @see EntityPrivileges
     * @see BizzPermission
     */
    private String converEntityPrivilegesDefinition(String definition) {
        JSONObject defJson = JSON.parseObject(definition);
        int C = defJson.getIntValue("C");
        int D = defJson.getIntValue("D");
        int U = defJson.getIntValue("U");
        int R = defJson.getIntValue("R");
        int A = defJson.getIntValue("A");
        int S = defJson.getIntValue("S");

        int deepP = 0;
        int deepL = 0;
        int deepD = 0;
        int deepG = 0;

        // {"A":0,"R":1,"C":4,"S":0,"D":0,"U":0} >> 1:9,2:1,3:1,4:1

        if (C >= 4) {
            deepP += BizzPermission.CREATE.getMask();
            deepL += BizzPermission.CREATE.getMask();
            deepD += BizzPermission.CREATE.getMask();
            deepG += BizzPermission.CREATE.getMask();
        }

        if (D >= 1) {
            deepP += BizzPermission.DELETE.getMask();
        }
        if (D >= 2) {
            deepL += BizzPermission.DELETE.getMask();
        }
        if (D >= 3) {
            deepD += BizzPermission.DELETE.getMask();
        }
        if (D >= 4) {
            deepG += BizzPermission.DELETE.getMask();
        }

        if (U >= 1) {
            deepP += BizzPermission.UPDATE.getMask();
        }
        if (U >= 2) {
            deepL += BizzPermission.UPDATE.getMask();
        }
        if (U >= 3) {
            deepD += BizzPermission.UPDATE.getMask();
        }
        if (U >= 4) {
            deepG += BizzPermission.UPDATE.getMask();
        }

        if (R >= 1) {
            deepP += BizzPermission.READ.getMask();
        }
        if (R >= 2) {
            deepL += BizzPermission.READ.getMask();
        }
        if (R >= 3) {
            deepD += BizzPermission.READ.getMask();
        }
        if (R >= 4) {
            deepG += BizzPermission.READ.getMask();
        }

        if (A >= 1) {
            deepP += BizzPermission.ASSIGN.getMask();
        }
        if (A >= 2) {
            deepL += BizzPermission.ASSIGN.getMask();
        }
        if (A >= 3) {
            deepD += BizzPermission.ASSIGN.getMask();
        }
        if (A >= 4) {
            deepG += BizzPermission.ASSIGN.getMask();
        }

        if (S >= 1) {
            deepP += BizzPermission.SHARE.getMask();
        }
        if (S >= 2) {
            deepL += BizzPermission.SHARE.getMask();
        }
        if (S >= 3) {
            deepD += BizzPermission.SHARE.getMask();
        }
        if (S >= 4) {
            deepG += BizzPermission.SHARE.getMask();
        }

        return "1:" + deepP + ",2:" + deepL + ",3:" + deepD + ",4:" + deepG;
    }
}
