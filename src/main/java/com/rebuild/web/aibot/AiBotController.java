/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.aibot;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.License;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.WebConstants;
import com.rebuild.web.commons.RbvMissingController;
import com.rebuild.web.user.signup.LoginAction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static cn.devezhao.commons.web.WebUtils.CURRENT_USER;
import static com.rebuild.utils.AppUtils.URL_AUTHTOKEN;
import static com.rebuild.web.WebConstants.AUTH_TOKEN;

/**
 * @author devezhao
 * @since 2025/4/12
 */
@Slf4j
@RestController
@RequestMapping("/aibot")
public class AiBotController extends LoginAction {

    @GetMapping("chat")
    public ModelAndView chatIndex(HttpServletRequest request, HttpServletResponse response) {
        if (!License.isCommercial()) {
            return RbvMissingController.errorUnsupported(null);
        }

        ID user = AppUtils.getRequestUser(request);
        String authToken;
        // token exchange
        if (user == null && (authToken = request.getParameter(URL_AUTHTOKEN)) != null) {
            user = AuthTokenManager.verifyToken(authToken, false, true);
            if (user != null) {
                ServletUtils.setSessionAttribute(request, CURRENT_USER, user);
                request.setAttribute(AUTH_TOKEN, authToken);
                UserContextHolder.setUser(user);
                request.setAttribute(WebConstants.$USER, Application.getUserStore().getUser(user));
            }
        }

        // 自动登录（记住登录）
        if (user == null) {
            String useAlt = ServletUtils.readCookie(request, CK_AUTOLOGIN);
            if (StringUtils.isNotBlank(useAlt)) {
                ID altUser = (ID) Application.getCommonsCache().getx(PREFIX_ALT + useAlt);
                if (altUser != null && UserHelper.isActive(altUser)) {
                    loginSuccessed(request, response, altUser, true);
                    user = altUser;
                    UserContextHolder.setUser(user);
                    request.setAttribute(WebConstants.$USER, Application.getUserStore().getUser(user));
                }
            }
        }

        ModelAndView mv = createModelAndView("/aibot/chat-view");
        mv.getModelMap().put("pageFooter", Language.L("由 REBUILD AI 助手强力驱动"));
        return mv;
    }
}
