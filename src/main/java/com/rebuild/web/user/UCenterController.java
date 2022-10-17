/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user;

import cn.devezhao.commons.CodecUtils;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.support.License;
import org.springframework.web.bind.annotation.*;

/**
 * UCenter
 *
 * @author devezhao
 * @since 2022/10/14
 */
@RestController
@RequestMapping("/settings/ucenter")
public class UCenterController {

    @PostMapping("/bind")
    public RespBody bindCloudAccount(@RequestBody JSONObject body) {
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
    public RespBody bindQuery() {
        JSONObject res = License.siteApi("api/ucenter/bind-query");
        String bindAccount = res.getString("bindAccount");
        return RespBody.ok(bindAccount);
    }
}
