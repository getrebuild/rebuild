/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.ServerStatus;
import com.rebuild.core.support.SysbaseSupport;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.OshiUtils;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Zixin (RB)
 * @see com.rebuild.web.RebuildWebConfigurer
 * @since 09/1/2020
 */
@Slf4j
@Controller
public class ErrorPageView extends BaseController {

    // Error Defined

    @GetMapping("/error/unsupported-browser")
    public ModelAndView pageUnsupportedBrowser() {
        return createErrorPage(
                Language.L("不支持 IE10 及以下的浏览器 [] 推荐使用 Edge、Chrome、Firefox 或 IE11"));
    }

    /**
     * @param msg
     * @return
     */
    public static ModelAndView createErrorPage(String msg) {
        ModelAndView mv = new ModelAndView("/error/error");
        mv.getModelMap().put("error_code", 400);
        mv.getModelMap().put("error_msg", msg);
        return mv;
    }

    // -- Status

    @GetMapping("/error/server-status")
    public ModelAndView pageServerStatus(HttpServletRequest request) {
        boolean realtime = "1".equals(request.getParameter("check"));

        ModelAndView mv = createModelAndView("/error/server-status");
        mv.getModel().put("ok", ServerStatus.isStatusOK() && Application.isReady());
        mv.getModel().put("status", ServerStatus.getLastStatus(realtime));
        mv.getModel().put("MemoryUsage", OshiUtils.getOsMemoryUsed());
        mv.getModel().put("MemoryUsageJvm", OshiUtils.getJvmMemoryUsed());
        mv.getModel().put("SystemLoad", OshiUtils.getSystemLoad());
        mv.getModelMap().put("isAdminVerified", AppUtils.isAdminVerified(request));
        return mv;
    }

    @GetMapping("/error/server-status.json")
    public void apiServerStatus(HttpServletRequest request, HttpServletResponse response) {
        boolean realtime = "1".equals(request.getParameter("check"));

        JSONObject s = new JSONObject();
        s.put("ok", ServerStatus.isStatusOK() && Application.isReady());
        s.put("uptime", System.currentTimeMillis() - ServerStatus.STARTUP_TIME.getTime());

        JSONObject status = new JSONObject();
        s.put("status", status);
        for (ServerStatus.Status item : ServerStatus.getLastStatus(realtime)) {
            status.put(item.name, item.success ? true : item.error);
        }
        status.put("MemoryUsageJvm", OshiUtils.getJvmMemoryUsed()[1]);
        status.put("MemoryUsage", OshiUtils.getOsMemoryUsed()[1]);
        status.put("SystemLoad", OshiUtils.getSystemLoad());

        ServletUtils.writeJson(response, s.toJSONString());
    }

    @GetMapping({"/gw/server-status", "/gw/server-status.json"})
    public String v1Fix(HttpServletRequest request) {
        if (request.getRequestURI().contains("server-status.json")) {
            return "redirect:/error/server-status.json";
        } else {
            return "redirect:/error/server-status";
        }
    }

    @GetMapping("/error/request-support")
    public void requestSupport(HttpServletResponse response) throws IOException {
        String tsid = null;
        try {
            tsid = new SysbaseSupport().submit();
        } catch (Exception e) {
            log.error(null, e);
        }
        response.sendRedirect(
                "https://getrebuild.com/report-issue?title=" + StringUtils.defaultIfBlank(tsid, "TS"));
    }
}
