/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * APP 安装包下载（手机端 / 桌面端），免费版可用
 *
 * @author devezhao
 * @since 08/15/2026
 */
@Controller
public class AppDownloadController {

    @GetMapping("/app-download")
    public void appDownload(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean desktop = "desktop".equalsIgnoreCase(request.getParameter("type"));
        ConfigurationItem item = desktop ? ConfigurationItem.DesktopAppPath : ConfigurationItem.MobileAppPath;

        final String path = RebuildConfiguration.get(item);
        if (path == null) {
            response.sendRedirect(desktop ? "user/login" : "h5app/");
            return;
        }

        if (QiniuCloud.instance().available()) {
            String privateUrl = QiniuCloud.instance().makeUrl(path);
            response.sendRedirect(privateUrl);
        } else {
            FileDownloader.setDownloadHeaders(response, QiniuCloud.parseFileName(path), false);
            File app = RebuildConfiguration.getFileOfData(path);
            FileDownloader.writeLocalFile(app, response);
        }
    }
}
