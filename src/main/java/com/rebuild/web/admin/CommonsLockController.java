/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.api.RespBody;
import com.rebuild.core.support.CommonsLock;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author RB
 * @since 2022/5/5
 */
@RestController
@RequestMapping("/admin/lock/")
public class CommonsLockController extends BaseController {

    @RequestMapping("lock")
    public RespBody lock(HttpServletRequest request, @IdParam ID id) {
        boolean locked = CommonsLock.lock(id, getRequestUser(request));
        return locked ? RespBody.ok() : RespBody.errorl("操作失败 (已被锁定)");
    }

    @RequestMapping("unlock")
    public RespBody unlock(HttpServletRequest request, @IdParam ID id) {
        boolean unlocked = CommonsLock.unlock(id, getRequestUser(request));
        return unlocked ? RespBody.ok() : RespBody.errorl("操作失败 (已被锁定)");
    }
}
