/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.aibot;

import com.rebuild.core.support.i18n.Language;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao
 * @since 2025/4/12
 */
@Slf4j
@RestController
@RequestMapping("/aibot")
public class AiBotController extends BaseController {

    @GetMapping("chat")
    public ModelAndView chatIndex() {
        ModelAndView mv = createModelAndView("/aibot/chat-view");
        mv.getModelMap().put("pageFooter", Language.L("由 REBUILD AI 助手强力驱动"));
        return mv;
    }

    @GetMapping("redirect")
    public void chatRedirect(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = req.getParameter("id");
        resp.sendRedirect("../");
    }
}
