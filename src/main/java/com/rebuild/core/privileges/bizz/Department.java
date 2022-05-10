/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges.bizz;

import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.persist4j.engine.ID;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 部门
 *
 * @author devezhao
 * @since 10/08/2018
 */
public class Department extends BusinessUnit {
    private static final long serialVersionUID = -5308455934676294159L;

    public Department(Serializable identity, String name, boolean disabled) {
        super(identity, name, disabled);
    }

    /**
     * 是否子部门（含所有子级）
     *
     * @param child
     * @param recursive
     * @return
     */
    public boolean isChildren(Department child, boolean recursive) {
        if (!recursive) return isChildren((ID) child.getIdentity());

        for (BusinessUnit dept : getChildren()) {
            if (dept.getIdentity().equals(child.getIdentity())) {
                return true;
            } else if (((Department) dept).isChildren(child, true)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否下级部门
     *
     * @param child
     * @return
     */
    public boolean isChildren(ID child) {
        for (BusinessUnit dept : getChildren()) {
            if (dept.getIdentity().equals(child)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取子部门（包括所有子级）
     *
     * @return
     */
    public Set<BusinessUnit> getAllChildren() {
        Set<BusinessUnit> children = new HashSet<>(getChildren());
        for (BusinessUnit child : getChildren()) {
            children.addAll(((Department) child).getAllChildren());
        }
        return Collections.unmodifiableSet(children);
    }
}