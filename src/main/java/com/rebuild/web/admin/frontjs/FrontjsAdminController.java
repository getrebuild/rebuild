/*
Copyright (c) Ruifang Tech <http://ruifang-tech.com/> and/or its owners. All rights reserved.
*/

package com.rebuild.web.admin.frontjs;

import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author devezhao
 * @since 2021/6/8
 */
@Controller
public class FrontjsAdminController extends BaseController {

    @GetMapping("/admin/frontjs-codes")
    public ModelAndView pageList() {
        RbAssert.isCommercial(
                Language.L("免费版不支持 FrontJS 功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)"));
        return createModelAndView("/admin/frontjs/frontjs-list");
    }
}
