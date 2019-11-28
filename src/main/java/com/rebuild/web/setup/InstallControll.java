/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.web.setup;

import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.setup.Installer;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * @author devezhao
 * @since 2019/11/25
 */
@Controller
@RequestMapping("/setup/")
public class InstallControll extends BasePageControll {

    @RequestMapping("install")
    public ModelAndView pageIndex(HttpServletResponse response) throws IOException {
        if (Application.serversReady()) {
            response.sendError(404);
            return null;
        }

        ModelAndView mv = createModelAndView("/setup/install.jsp");
        mv.getModel().put("defaultDataDirectory", SysConfiguration.getFileOfData(null).getAbsolutePath().replace("\\", "/"));
        mv.getModel().put("defaultAppName", SysConfiguration.get(ConfigurableItem.AppName));
        mv.getModel().put("defaultHomeURL", SysConfiguration.get(ConfigurableItem.HomeURL));
        return mv;
    }

    @RequestMapping("test-connection")
    public void testConnection(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject dbProps = (JSONObject) ServletUtils.getRequestJson(request);

        try (Connection conn = new Installer(JSONUtils.toJSONObject("databaseProps", dbProps)).getConnection(null)) {
            DatabaseMetaData dmd = conn.getMetaData();
            String msg = String.format("连接成功 : %s %s", dmd.getDatabaseProductName(), dmd.getDatabaseProductVersion());
            writeSuccess(response, msg);
        } catch (SQLException e) {
            if (e.getLocalizedMessage().contains("Unknown database")) {
                writeSuccess(response, "连接成功 : 数据库不存在，将自动创建");
            } else {
                writeFailure(response, "连接错误 : " + e.getLocalizedMessage());
            }
        }
    }

    @RequestMapping("test-directory")
    public void testDirectory(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String dir = getParameterNotNull(request, "dir");
        File file = new File(dir);
        if (file.exists()) {
            if (!file.isDirectory()) {
                file = null;
            }
        } else {
            try {
                FileUtils.forceMkdir(file);
                if (file.exists()) {
                    FileUtils.deleteDirectory(file);
                } else {
                    file = null;
                }
            } catch (IOException ex) {
                file = null;
            }
        }

        if (file == null) {
            writeFailure(response);
        } else {
            writeSuccess(response, file.getAbsolutePath());
        }
    }

    @RequestMapping("install-rebuild")
    public void installExec(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject installProps = (JSONObject) ServletUtils.getRequestJson(request);
        try {
            new Installer(installProps).install();
            writeSuccess(response);
        } catch (Exception e) {
            e.printStackTrace();
            writeFailure(response, "出现错误 : " + e.getLocalizedMessage());
        }
    }
}
