/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @author Zixin (RB)
 * @see com.rebuild.web.RebuildWebConfigurer
 * @since 09/20/2018
 */
@Controller
public class CommonPageView extends BaseController {

    @GetMapping("/")
    public void index(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (AppUtils.isMobile(request)) {
            response.sendRedirect(RebuildConfiguration.getMobileUrl("/"));
        } else {
            response.sendRedirect("user/login");
        }
    }
    
    @GetMapping("/*.txt")
    public void txtSuffix(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String url = request.getRequestURI();

        String name = url.substring(url.lastIndexOf("/") + 1);
        String content = null;

        // WXWORK
        if (name.startsWith("WW_verify_")) {
            String fileKey = RebuildConfiguration.get(ConfigurationItem.WxworkAuthFile);
            File file = RebuildConfiguration.getFileOfData(fileKey);
            if (file.exists() && file.isFile()) {
                content = FileUtils.readFileToString(file, AppUtils.UTF8);
            }
        }
        // OTHERS
        else {
            File file = RebuildConfiguration.getFileOfData(name);
            if (file.exists() && file.isFile()) {
                content = FileUtils.readFileToString(file, AppUtils.UTF8);
            } else {
                content = CommonsUtils.getStringOfRes("web/" + name);
            }
        }

        if (content == null) {
            response.sendError(HttpStatus.NOT_FOUND.value());
        } else {
            ServletUtils.setContentType(response, ServletUtils.CT_PLAIN);
            ServletUtils.write(response, content);
        }
    }

    @GetMapping("/p/**")
    public ModelAndView page(HttpServletRequest request) {
        String p = request.getRequestURI();
        p = p.split("/p/")[1];
        return createModelAndView("/" + p);
    }

    @GetMapping("/app/home")
    public void appHome(@RequestParam(name = "def", required = false) String def,
                        HttpServletResponse response) throws IOException {
        if (def != null && def.length() >= 20) {
            String[] defs = def.split(":");
            addCookie("AppHome.Nav", ID.isId(defs[0]) ? ID.valueOf(defs[0]) : null, response);
            addCookie("AppHome.Dash", defs.length > 1 && ID.isId(defs[1]) ? ID.valueOf(defs[1]) : null, response);
        }
        response.sendRedirect("../dashboard/home");
    }

    private void addCookie(String name, ID value, HttpServletResponse response) {
        Cookie cookie = new Cookie(name, value == null ? "N" : CodecUtils.urlEncode(value.toLiteral()));
        cookie.setPath("/");
        if (value == null) cookie.setMaxAge(0);
        else cookie.setMaxAge(60 * 60 * 24 * 30);  // 30d
        response.addCookie(cookie);
    }
}