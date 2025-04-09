/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import com.rebuild.utils.OnlyOfficeUtils;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

import static com.rebuild.core.support.ConfigurationItem.OnlyofficeServer;

/**
 * 文档预览
 *
 * @author devezhao
 * @see FileDownloader
 * @since 04/07/2025
 */
@Slf4j
@Controller
public class FilePreviewer extends BaseController {

    @GetMapping("/filex/preview/**")
    public ModelAndView ooPreview(HttpServletRequest request) {
        String filepath = request.getRequestURI().split("/filex/preview/")[1];
        Object[] ps = OnlyOfficeUtils.buildPreviewParams(filepath);

        ModelAndView mv = createModelAndView("/common/oo-preview");
        mv.getModel().put(OnlyofficeServer.name(), OnlyOfficeUtils.getOoServer());
        mv.getModel().put("_DocumentConfig", ps[0]);
        mv.getModel().put("_Token", ps[1]);
        return mv;
    }
}
