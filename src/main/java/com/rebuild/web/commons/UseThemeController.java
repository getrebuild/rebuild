/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.KVStorage;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.Etag;
import com.rebuild.web.BaseController;
import com.rebuild.web.admin.ConfigurationController;
import com.rebuild.web.user.signup.LoginController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * @author devezhao
 * @since 2020/12/24
 */
@Slf4j
@Controller
@RequestMapping("commons/theme/")
public class UseThemeController extends BaseController {

    // 支持的主题
    public static final String[] THEMES = {
            "default", "dark", "red", "green", "blue", "blue2", "purple"
    };

    @GetMapping("use-theme")
    public void useTheme(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String theme = "default";

        String referer = ServletUtils.getReferer(request);
        if (StringUtils.isBlank(referer)
                || referer.contains("/admin/") || referer.contains("/admin-")) {
            // No theme provided. Use default

        } else {
            theme = (String) ServletUtils.getSessionAttribute(request, LoginController.SK_USER_THEME);
            if (theme == null) {
                theme = KVStorage.getCustomValue("THEME." + AppUtils.getRequestUser(request));
                if ("_".equalsIgnoreCase(theme)) {
                    theme = THEMES[RandomUtils.nextInt(THEMES.length)];
                }

                theme = StringUtils.defaultIfBlank(theme, THEMES[0]);
                ServletUtils.setSessionAttribute(request, LoginController.SK_USER_THEME, theme);

            } else if ("_".equalsIgnoreCase(theme)) {
                theme = THEMES[RandomUtils.nextInt(THEMES.length)];
                ServletUtils.setSessionAttribute(request, LoginController.SK_USER_THEME, theme);
            }
        }

        theme = String.format("web/assets/css/theme-%s.css", theme);

        String themeHash = EncryptUtils.toMD5Hex(theme);
        Etag etag = new Etag(themeHash, response);
        if (!etag.isNeedWrite(request)) return;

        InputStream is = CommonsUtils.getStreamOfRes(theme);
        try {
            FileDownloader.writeStream(is, response);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @RequestMapping("set-use-theme")
    public void setUseTheme(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        String theme = getParameter(request, "theme", "_");

        KVStorage.setCustomValue("THEME." + user, theme);
        ServletUtils.setSessionAttribute(request, LoginController.SK_USER_THEME, theme);
        writeSuccess(response);
    }

    // -- LOGO

    @GetMapping({ "use-logo", "use-logo-white" })
    public void useLogo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String dimgHash = getDimgHash(ConfigurationController.ETAG_DIMGLOGOTIME);
        Etag etag = new Etag(dimgHash, response);
        if (!etag.isNeedWrite(request)) return;

        boolean isWhite = request.getRequestURI().contains("-white");
        String logo = RebuildConfiguration.get(isWhite ? ConfigurationItem.LOGOWhite : ConfigurationItem.LOGO);

        InputStream is = null;
        if (logo != null) {
            File file = RebuildConfiguration.getFileOfData(logo);
            if (file.exists()) {
                is = Files.newInputStream(file.toPath());
            }
        }

        if (is == null) {
            is = CommonsUtils.getStreamOfRes(
                    isWhite ? "web/assets/img/logo-white.png" : "web/assets/img/logo.png");
        }

        try {
            FileDownloader.writeStream(is, response);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    // -- BGIMG

    @GetMapping("use-bgimg")
    public void useBgimg(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String dimgHash = getDimgHash(ConfigurationController.ETAG_DIMGBGIMGTIME);
        Etag etag = new Etag(dimgHash, response);
        if (!etag.isNeedWrite(request)) return;

        String bgimg = RebuildConfiguration.get(ConfigurationItem.CustomWallpaper);

        InputStream is = null;
        if (bgimg != null) {
            File file = RebuildConfiguration.getFileOfData(bgimg);
            if (file.exists()) {
                is = Files.newInputStream(file.toPath());
            }
        }

        if (is == null) {
            is = CommonsUtils.getStreamOfRes("web/assets/img/bg.jpg");
        }

        try {
            FileDownloader.writeStream(is, response);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private String getDimgHash(String name) {
        String dimgHash = Application.getCommonsCache().get(name);
        if (dimgHash == null) {
            dimgHash = CommonsUtils.randomHex(true);
            Application.getCommonsCache().put(name, dimgHash, CommonsCache.TS_WEEK);
        }
        return dimgHash;
    }
}
