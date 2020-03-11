/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.rbmobile;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.LoginToken;
import com.rebuild.api.LoginTokenManager;
import com.rebuild.server.Application;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.RequestFrequencyCounter;
import com.rebuild.web.BaseControll;
import com.rebuild.web.user.signin.LoginControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 移动端认证
 *
 * @author devezhao
 * @since 2020/3/5
 *
 * @see com.rebuild.web.RequestWatchHandler
 * @see AppUtils#getRequestUser(HttpServletRequest)
 */
@Controller
@RequestMapping("/mobile/user/")
@RbMobile
public class MobileAuthControll extends BaseControll {

    // 有效期
    private static final int TOKEN_EXPIRES = 60 * 60 * 12;
    // 防刷
    private static final RequestFrequencyCounter COUNTER = new RequestFrequencyCounter();

    @RequestMapping("login")
    public void mobileLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String ipAddr = ServletUtils.getRemoteAddr(request);
        if (COUNTER.counter(ipAddr).add().seconds(60).than(5)) {
            writeFailure(response, "请求太频繁，请稍后重试");
            return;
        }

        final JSONObject post = (JSONObject) ServletUtils.getRequestJson(request);

        String username = post.getString("username");
        String password = post.getString("password");
        String hasError = LoginToken.checkUser(username, password);
        if (hasError != null) {
            writeFailure(response, hasError);
            return;
        }

        User loginUser = Application.getUserStore().getUser(username);
        String token = LoginTokenManager.generateToken(loginUser.getId(), TOKEN_EXPIRES);

        LoginControll.createLoginLog(request, loginUser.getId());
        writeSuccess(response, buildUserData(token, loginUser));
    }

    @RequestMapping("logout")
    public void mobileLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String xAuthToken = request.getHeader(AppUtils.MOBILE_HF_AUTHTOKEN);
        ID user = LoginTokenManager.verifyToken(xAuthToken, true);

        if (user != null) {
            // TODO 退出时间
        }

        writeSuccess(response);
    }

    @RequestMapping("verfiy-token")
    public void verfiyToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String xAuthToken = request.getHeader(AppUtils.MOBILE_HF_AUTHTOKEN);
        ID user = LoginTokenManager.refreshToken(xAuthToken, TOKEN_EXPIRES);

        if (user != null) {
            User loginUser = Application.getUserStore().getUser(user);
            writeSuccess(response, buildUserData(xAuthToken, loginUser));
        } else {
            writeFailure(response);
        }
    }

    private JSONObject buildUserData(String token, User user) {
        JSONObject data = JSONUtils.toJSONObject("token", token);
        data.put("email", user.getEmail());
        data.put("fullName", user.getFullName());
        data.put("avatarUrl", user.getAvatarUrl());
        return data;
    }
}
