/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.extform;

import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import static com.rebuild.core.support.i18n.Language.$L;

/**
 * @author devezhao
 * @since 2020/12/8
 */
@RestController
public class ExtformAdminController extends BaseController {

    @GetMapping("/admin/extforms")
    public ModelAndView pageList() {
        RbAssert.isCommercial($L("免费版不支持外部表单功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)"));
        return createModelAndView("/admin/extform/extform-list");
    }
}
