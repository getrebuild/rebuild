/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.ServerStatus;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author zhaofang123@gmail.com
 * @see com.rebuild.web.RebuildWebConfigurer
 * @since 09/1/2020
 */
@Controller
public class ErrorPageResolver extends BaseController {

    @GetMapping("/error/unsupported-browser")
    public ModelAndView pageUnsupportedBrowser(HttpServletRequest request) {
        ModelAndView mv = createModelAndView("/error/error");
        mv.getModelMap().put("error_code", 400);
        mv.getModelMap().put("error_msg", getLang(request, "UnsupportIE10"));
        return mv;
    }

    @GetMapping("/error/server-status")
    public ModelAndView pageServerStatus(HttpServletRequest request) {
        boolean realtime = "1".equals(request.getParameter("check"));

        ModelAndView mv = createModelAndView("/error/server-status");
        mv.getModel().put("ok", ServerStatus.isStatusOK() && Application.isReady());
        mv.getModel().put("status", ServerStatus.getLastStatus(realtime));

        mv.getModel().put("MemoryUsage", ServerStatus.getHeapMemoryUsed());
        mv.getModel().put("SystemLoad", ServerStatus.getSystemLoad());
        return mv;
    }

    @GetMapping("/error/server-status.json")
    public void apiServerStatus(HttpServletRequest request, HttpServletResponse response) {
        boolean realtime = "1".equals(request.getParameter("check"));

        JSONObject state = new JSONObject();
        state.put("ok", ServerStatus.isStatusOK());
        JSONArray stats = new JSONArray();
        state.put("status", stats);
        for (ServerStatus.Status s : ServerStatus.getLastStatus(realtime)) {
            stats.add(s.toJson());
        }

        stats.add(JSONUtils.toJSONObject("MemoryUsage", ServerStatus.getHeapMemoryUsed()[1]));
        stats.add(JSONUtils.toJSONObject("SystemLoad", ServerStatus.getSystemLoad()));
        ServletUtils.writeJson(response, state.toJSONString());
    }

    @GetMapping({"/gw/server-status", "/gw/server-status.json"})
    public String v1Fix(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.endsWith("/server-status.json")) return "redirect:/error/server-status.json";
        else return "redirect:/error/server-status";
    }
}
