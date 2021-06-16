/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.RebuildException;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.CsrfToken;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 文件下载/查看
 *
 * @author devezhao
 * @since 01/03/2019
 */
@Controller
@RequestMapping("/filex/")
public class FileDownloader extends BaseController {

    @GetMapping("img/**")
    public void viewImg(HttpServletRequest request, HttpServletResponse response) throws IOException {
        RbAssert.isAllow(checkUser(request, true), "Unauthorized access");

        String filePath = request.getRequestURI();
        filePath = filePath.split("/filex/img/")[1];

        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            response.sendRedirect(filePath);
            return;
        }

        ServletUtils.addCacheHead(response, 60);

        final boolean temp = BooleanUtils.toBoolean(request.getParameter("temp"));
        String imageView2 = request.getQueryString();
        if (imageView2 != null && imageView2.contains("imageView2/")) {
            imageView2 = "imageView2/" + imageView2.split("imageView2/")[1].split("&")[0];
        } else {
            imageView2 = null;
        }

        // Local storage || temp
        if (!QiniuCloud.instance().available() || temp) {
            String fileName = QiniuCloud.parseFileName(filePath);
            String mimeType = request.getServletContext().getMimeType(fileName);
            if (mimeType != null) {
                response.setContentType(mimeType);
            }

            final int wh = imageView2 == null ? 0 : parseWidth(imageView2);

            // 原图
            if (wh <= 0 || wh >= 1000) {
                writeLocalFile(filePath, temp, response);
            }
            // 粗略图
            else {
                filePath = checkFilePath(filePath);
                File img = temp ? RebuildConfiguration.getFileOfTemp(filePath) : RebuildConfiguration.getFileOfData(filePath);
                if (!img.exists()) {
                    response.setHeader("Content-Disposition", StringUtils.EMPTY);  // Clean download
                    response.sendError(HttpStatus.NOT_FOUND.value());
                    return;
                }

                BufferedImage bi = ImageIO.read(img);
                Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(bi);

                if (bi.getWidth() > wh) {
                    builder.size(wh, wh);
                } else {
                    builder.scale(1.0);
                }

                builder
                        .outputFormat(mimeType != null && mimeType.contains("png") ? "png" : "jpg")
                        .toOutputStream(response.getOutputStream());
            }
        } else {
            if (imageView2 != null) {
                filePath += "?" + imageView2;
            }

            String privateUrl = QiniuCloud.instance().url(filePath, 30 * 60);
            response.sendRedirect(privateUrl);
        }
    }

    /**
     * 宽度参数
     *
     * @param imageView2
     * @return
     */
    private int parseWidth(String imageView2) {
        if (!imageView2.contains("/w/")) {
            return 1000;
        }

        String w = imageView2.split("/w/")[1].split("/")[0];
        return Integer.parseInt(w);
    }

    @GetMapping(value = {"download/**", "access/**"})
    public void download(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String filePath = request.getRequestURI();

        // 共享查看
        if (request.getRequestURI().contains("/filex/access/")) {
            String e = getParameter(request, "e");
            if (StringUtils.isBlank(e) || Application.getCommonsCache().get(e) == null) {
                response.sendError(HttpStatus.FORBIDDEN.value(), Language.L("分享的文件已过期"));
                return;
            }

            filePath = filePath.split("/filex/access/")[1];
        } else {
            RbAssert.isAllow(checkUser(request, false), "Unauthorized access");
            filePath = filePath.split("/filex/download/")[1];
        }

        boolean temp = getBoolParameter(request, "temp");
        String attname = getParameter(request, "attname");
        if (StringUtils.isBlank(attname)) attname = QiniuCloud.parseFileName(filePath);

        ServletUtils.setNoCacheHeaders(response);

        // Local storage || temp
        if (!QiniuCloud.instance().available() || temp) {
            setDownloadHeaders(request, response, attname);
            writeLocalFile(filePath, temp, response);
        } else {
            String privateUrl = QiniuCloud.instance().url(filePath);
            privateUrl += "&attname=" + CodecUtils.urlEncode(attname);
            response.sendRedirect(privateUrl);
        }
    }

    private boolean checkUser(HttpServletRequest request, boolean allowCsrf) {
        ID user = AppUtils.getRequestUser(request);
        // authToken
        if (user == null) {
            String authToken = request.getParameter(AppUtils.URL_AUTHTOKEN);
            user = AuthTokenManager.verifyToken(authToken, false);
        }
        // csrfToken
        if (user == null && allowCsrf) {
            if (CsrfToken.verify(request, false)) {
                user = UserService.SYSTEM_USER;
            }
        }
        return user != null;
    }

    // --

    /**
     * 本地文件下载
     *
     * @param filePath
     * @param temp
     * @param response
     * @return
     * @throws IOException
     */
    public static boolean writeLocalFile(String filePath, boolean temp, HttpServletResponse response) throws IOException {
        filePath = checkFilePath(filePath);
        File file = temp ? RebuildConfiguration.getFileOfTemp(filePath) : RebuildConfiguration.getFileOfData(filePath);
        return writeLocalFile(file, response);
    }

    /**
     * 本地文件下载
     *
     * @param file
     * @param response
     * @return
     * @throws IOException
     */
    public static boolean writeLocalFile(File file, HttpServletResponse response) throws IOException {
        if (!file.exists()) {
            response.setHeader("Content-Disposition", StringUtils.EMPTY);  // Clean download
            response.sendError(HttpStatus.NOT_FOUND.value());
            return false;
        }

        long size = FileUtils.sizeOf(file);
        response.setHeader("Content-Length", String.valueOf(size));

        try (InputStream fis = new FileInputStream(file)) {
            return writeStream(fis, response);
        }
    }

    /**
     * @param is
     * @param response
     * @return
     * @throws IOException
     */
    public static boolean writeStream(InputStream is, HttpServletResponse response) throws IOException {
        response.setContentLength(is.available());

        OutputStream os = response.getOutputStream();
        int count;
        byte[] buffer = new byte[1024 * 1024];
        while ((count = is.read(buffer)) != -1) {
            os.write(buffer, 0, count);
        }
        os.flush();
        return true;
    }

    /**
     * 设置下载 Headers
     *
     * @param request
     * @param response
     * @param attname
     */
    public static void setDownloadHeaders(HttpServletRequest request, HttpServletResponse response, String attname) {
        // 特殊字符处理
        attname = attname.replace(" ", "-");
        attname = attname.replace("%", "-");

        // 火狐 Safari 中文名乱码问题
        String UA = StringUtils.defaultIfBlank(request.getHeader("user-agent"), "").toUpperCase();
        if (UA.contains("FIREFOX") || UA.contains("SAFARI")) {
            attname = CodecUtils.urlDecode(attname);
            attname = new String(attname.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        }

        response.setHeader("Content-Disposition", "attachment;filename=" + attname);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    /**
     * @param filepath
     * @return
     */
    private static String checkFilePath(String filepath) {
        filepath = CodecUtils.urlDecode(filepath);
        filepath = filepath.replace("\\", "/");

        if (filepath.contains("../")
                || filepath.startsWith("_log/") || filepath.contains("/_log/")
                || filepath.startsWith("_backups/") || filepath.contains("/_backups/")) {
            throw new RebuildException("Attack path detected : " + filepath);
        }
        return filepath;
    }
}
