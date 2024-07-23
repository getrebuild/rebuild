/*!
Copyright (c) Ruifang Tech <http://ruifang-tech.com/> and/or its owners. All rights reserved.
*/

package com.rebuild.core.privileges;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * 字段权限
 *
 * @author devezhao
 * @since 2024/7/23
 * @see PrivilegesManager
 */
public class FieldPrivileges {

    /**
     * 可新建?
     * @param field
     * @param user
     * @return
     */
    public boolean isCreatable(Field field, ID user) {
        return true;
    }

    /**
     * 可读?
     * @param field
     * @param user
     * @return
     */
    public boolean isReadble(Field field, ID user) {
        return true;
    }

    /**
     * 可修改?
     * @param field
     * @param user
     * @return
     */
    public boolean isUpdatable(Field field, ID user) {
        return true;
    }
}
