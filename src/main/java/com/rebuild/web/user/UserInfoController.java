/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.IdParam;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author devezhao
 * @since 2021/8/23
 */
public class UserInfoController {

    @GetMapping("/account/user-info")
    public JSON userInfo(@IdParam ID id) {
        User u = Application.getUserStore().getUser(id);
        Department dept = u.getOwningDept();
        return JSONUtils.toJSONObject(
                new String[]{"name", "dept", "email", "phone", "isActive"},
                new Object[]{u.getFullName(), dept == null ? null : dept.getName(), u.getEmail(), u.getWorkphone(), u.isActive() });
    }
}
