/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.frontjs;

import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

/**
 * @author devezhao
 * @since 2021/6/8
 */
@Controller
public class FrontjsAdminController extends BaseController {

    @GetMapping("/admin/frontjs-code")
    public ModelAndView page(HttpServletRequest request) {
        RbAssert.isCommercial(
                Language.L("免费版不支持 FrontJS 功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)"));
        ModelAndView mv = createModelAndView("/admin/frontjs/frontjs-code");
        mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));
        return mv;
    }
}
