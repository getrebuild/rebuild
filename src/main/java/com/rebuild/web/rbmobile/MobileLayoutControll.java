/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.rbmobile;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.configuration.portals.NavManager;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;

/**
 * 移动端布局
 *
 * @author devezhao
 * @since 2020/3/10
 */
@Controller
@RequestMapping("/mobile/layout/")
@RbMobile
public class MobileLayoutControll extends BaseControll {

    @RequestMapping("navigation")
    public void navigation(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 注意顺序
        JSONArray topNav = new JSONArray();
        topNav.add(buildNavItem("Home", "首页", null));
        topNav.add(buildNavItem("Feeds", "动态", null));
        JSONObject worksNav = buildNavItem("Works", "工作", null);
        topNav.add(worksNav);
        topNav.add(buildNavItem("Files", "文件", null));
        topNav.add(buildNavItem("Me", "我的", null));

        final ID user = getRequestUser(request);
        JSONArray worksChild = NavManager.instance.getNavForPortal(user);

        // 移除已有模块
        for (Iterator<Object> iter = worksChild.iterator(); iter.hasNext(); ) {
            String value = ((JSONObject) iter.next()).getString("value");
            if (NavManager.NAV_FEEDS.equals(value) || NavManager.NAV_FILEMRG.equals(value)) {
                iter.remove();
            }
        }
        worksNav.put("child", worksChild);

        writeSuccess(response, topNav);
    }

    private JSONObject buildNavItem(String value, String text, String icon) {
        return JSONUtils.toJSONObject(
                new String[] { "value", "text", "icon" },
                new Object[] { value, text, icon });
    }

}
