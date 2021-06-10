/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.web.ServletUtils;
import com.rebuild.core.support.i18n.LanguageBundle;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.Etag;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 多语言控制
 *
 * @author devezhao
 * @since 2019/11/29
 */
@Controller
@RequestMapping("/language/")
public class LanguageController extends BaseController {

    @GetMapping("use-bundle")
    public void useBundle(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType(ServletUtils.CT_JS);

        LanguageBundle bundle = AppUtils.getReuqestBundle(request);
        Etag etag = new Etag(bundle.getBundleHash(), response);

        if (etag.isNeedWrite(request)) {
            ServletUtils.write(response,
                    "window._LANGBUNDLE = " + bundle.toJSON().toJSONString());
        }
    }
}
