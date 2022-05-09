/*!
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import com.rebuild.core.support.i18n.Language;
import com.rebuild.web.BaseController;
import com.rebuild.web.WebConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * When none RBV
 *
 * @author ZHAO
 * @since 2021/6/21
 */
@ConditionalOnMissingClass("com.rebuild.Rbv")
@Controller
public class RbvMissingController extends BaseController {

    @GetMapping({"/h5app/**"})
    public ModelAndView h5app() {
        ModelAndView mv = ErrorPageView.createErrorPage(
                Language.L("免费版不支持手机访问功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)"));
        mv.getModelMap().put(WebConstants.$BUNDLE, Language.getCurrentBundle());
        return mv;
    }

    @GetMapping("/user/login/sso")
    public ModelAndView ssoLogin(HttpServletRequest request) throws IOException {
        String error = "dingtalk".equalsIgnoreCase(getParameterNotNull(request, "protocol"))
                ? Language.L("免费版不支持钉钉集成 [(查看详情)](https://getrebuild.com/docs/rbv-features)")
                : Language.L("免费版不支持企业微信集成 [(查看详情)](https://getrebuild.com/docs/rbv-features)");

        ModelAndView mv = ErrorPageView.createErrorPage(error);
        mv.getModelMap().put(WebConstants.$BUNDLE, Language.getCurrentBundle());
        return mv;
    }
}
