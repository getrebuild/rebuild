/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.web.ServletUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * @author Zixin (RB)
 * @since 2025/9/18
 */
@Controller
public class MermaidController extends BaseController {

    @RequestMapping("/commons/mermaid")
    public ModelAndView renderMermaid(HttpServletRequest request) {
        ModelAndView mv = createModelAndView("/common/mermaid-chart");
        // in POST
        String data = ServletUtils.getRequestString(request);
        if (StringUtils.isBlank(data)) {
            // in URL
            data = request.getParameter("data");
            if (data != null) data = CodecUtils.urlDecode(data);
        }

        data = Objects.requireNonNull(data).trim();
        mv.getModel().put("mermaidData", data);
        return mv;
    }
}
