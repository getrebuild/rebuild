/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.License;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * UCenter
 *
 * @author devezhao
 * @since 2022/10/14
 */
@RestController
@RequestMapping("/settings/ucenter")
public class UCenterController extends BaseController {

    @PostMapping("/bind")
    public RespBody bindCloudAccount(@RequestBody JSONObject body, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        RbAssert.isAllow(UserHelper.isSuperAdmin(user), Language.L("仅超级管理员可操作"));

        String account = body.getString("cloudAccount");
        String passwd = body.getString("cloudPasswd");

        String bindUrl = String.format("api/ucenter/bind-account?account=%s&passwd=%s",
                CodecUtils.urlEncode(account), CodecUtils.urlEncode(passwd));
        JSONObject res = License.siteApiNoCache(bindUrl);

        String hasError = res.getString("error");
        if (hasError == null) {
            License.siteApiNoCache("api/ucenter/bind-query");  // clear
            return RespBody.ok();
        } else {
            return RespBody.error(hasError);
        }
    }

    @GetMapping("/bind-query")
    public RespBody bindQuery(HttpServletRequest request) {
        JSONObject res = License.siteApi("api/ucenter/bind-query");
        res.put("canBind", UserHelper.isSuperAdmin(getRequestUser(request)));
        return RespBody.ok(res);
    }
}
