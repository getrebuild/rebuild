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

import static com.rebuild.core.support.i18n.Language.L;

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
        return errorUnsupported(L("手机访问"));
    }

    @GetMapping("/user/login/sso")
    public ModelAndView ssoLogin(HttpServletRequest request) {
        String protocol = getParameterNotNull(request, "protocol");
        String error = "dingtalk".equalsIgnoreCase(protocol)
                ? L("钉钉集成")
                : "feishu".equalsIgnoreCase(protocol) ? L("飞书集成") : L("企业微信集成");
        return errorUnsupported(error);
    }

    @GetMapping("/admin/robot/sops")
    public ModelAndView sopList() {
        return errorUnsupported(L("业务进度"));
    }

    @GetMapping("/admin/extforms")
    public ModelAndView extformList() {
        return errorUnsupported(L("外部表单"));
    }

    @GetMapping("/admin/frontjs-code")
    public ModelAndView frontjs() {
        return errorUnsupported(" FrontJS ");
    }

    @GetMapping("/admin/i18n/translation")
    public ModelAndView i18nList() {
        return errorUnsupported(L("多语言"));
    }

    @GetMapping("/admin/data/data-syncer")
    public ModelAndView dataSyncerList() {
        return errorUnsupported(L("数据同步"));
    }

    @GetMapping("/admin/users-config")
    public ModelAndView usersConfig() {
        return errorUnsupported(L("用户配置"));
    }

    private ModelAndView errorUnsupported(String featName) {
        String error = L("免费版不支持%s功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)", featName);
        ModelAndView mv = ErrorPageView.createErrorPage(error);
        mv.getModelMap().put(WebConstants.$BUNDLE, Language.getCurrentBundle());
        return mv;
    }
}
