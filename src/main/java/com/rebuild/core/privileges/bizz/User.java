/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges.bizz;

import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.privileges.RoleService;
import com.rebuild.core.privileges.UserService;
import org.apache.commons.lang.StringUtils;

/**
 * 用户
 *
 * @author Zixin (RB)
 * @since 09/16/2018
 */
public class User extends cn.devezhao.bizz.security.member.User {
    private static final long serialVersionUID = 15823574375847575L;

    private String email;
    private String workphone;
    private String fullName;
    private String avatarUrl;

    private CombinedRole combinedRole;

    public User(ID userId, String loginName, String email, String workphone,
                String fullName, String avatarUrl, boolean disabled) {
        super(userId, loginName, disabled);
        this.email = email;
        this.workphone = workphone;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
    }

    /**
     * @return
     */
    public ID getId() {
        return (ID) getIdentity();
    }

    /**
     * @return
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return
     */
    public String getWorkphone() {
        return workphone;
    }

    /**
     * @return
     */
    public String getFullName() {
        return StringUtils.defaultIfBlank(fullName, this.getName().toUpperCase());
    }

    /**
     * @return
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * @return
     */
    public Department getOwningDept() {
        return (Department) super.getOwningBizUnit();
    }

    /**
     * 是否管理员
     *
     * @return
     */
    public boolean isAdmin() {
        if (getIdentity().equals(UserService.ADMIN_USER)
                || getIdentity().equals(UserService.SYSTEM_USER)) return true;

        Role role = getOwningRole();
        if (role == null) return false;
        if (role.getIdentity().equals(RoleService.ADMIN_ROLE)) return true;

        return role instanceof CombinedRole && ((CombinedRole) role).getRoleAppends().contains(RoleService.ADMIN_ROLE);
    }

    /**
     * 是否激活/可用。如果用户所属部门或角色被禁用，用户同样也不可用
     */
    @Override
    public boolean isActive() {
        if (isDisabled()) {
            return false;
        }
        if (getOwningDept() == null || getOwningDept().isDisabled()) {
            return false;
        }
        return getOwningRole() != null && !getOwningRole().isDisabled();
    }

    /**
     * @param combinedRole
     */
    protected void setCombinedRole(CombinedRole combinedRole) {
        this.combinedRole = combinedRole;
    }

    /**
     * @return Returns {@link CombinedRole} or {@link Role}
     */
    @Override
    public Role getOwningRole() {
        return combinedRole != null ? combinedRole : getMainRole();
    }

    /**
     * 获取主角色
     *
     * @return
     */
    public Role getMainRole() {
        return super.getOwningRole();
    }
}
