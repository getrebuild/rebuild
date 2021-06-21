/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import com.rebuild.core.support.i18n.Language;
import com.rebuild.web.BaseController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author ZHAO
 * @since 2021/6/21
 */
@ConditionalOnMissingClass("com.rebuild.Rbv")
@Controller
public class RbvMissingController extends BaseController {

    @GetMapping({"/h5app/**"})
    public ModelAndView pageH5app() {
        ModelAndView mv = createModelAndView("/error/error");
        mv.getModelMap().put("error_code", 400);
        mv.getModelMap().put("error_msg",
                Language.L("免费版不支持手机访问功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)"));
        return mv;
    }
}
