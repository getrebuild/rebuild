/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.hutool.core.io.FileUtil;
import cn.hutool.jwt.JWT;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.RebuildException;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.ExcelUtils;
import com.rebuild.utils.OkHttpUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.rebuild.core.support.ConfigurationItem.OnlyofficeJwt;
import static com.rebuild.core.support.ConfigurationItem.OnlyofficeServer;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * https://api.onlyoffice.com/docs/docs-api/additional-api/conversion-api/
 *
 * @author devezhao
 * @since 2025/2/26
 */
public class OnlyOffice {

    public static final String OO_PREVIEW_URL = "/commons/file-preview?src=";

    /**
     * @param path
     * @return
     * @throws IOException
     */
    public static Path convertPdf(Path path) throws IOException {
        String filename = path.getFileName().toString();
        // Excel 公式生效
        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            ExcelUtils.reSaveAndCalcFormula(path);
        }

        // 需要在临时目录下才可以，否则 oo 访问不到源文件
        File fileInTemp = RebuildConfiguration.getFileOfTemp(filename);
        if (!fileInTemp.equals(path.toFile())) {
            // 尝试父级目录
            String parent = path.getParent().getFileName().toString();
            fileInTemp = RebuildConfiguration.getFileOfTemp(parent + "/" + filename);
            if (fileInTemp.equals(path.toFile())) {
                filename = parent + "/" + filename;
            } else {
                FileUtils.deleteQuietly(fileInTemp);
                FileUtils.copyFile(path.toFile(), fileInTemp);
            }
        }

        String fileUrl = String.format("/filex/download/%s?_csrfToken=%s&temp=yes",
                filename, AuthTokenManager.generateCsrfToken(90));
        fileUrl = RebuildConfiguration.getHomeUrl(fileUrl);

        return convertPdf(path, fileUrl);
    }

    /**
     * @param path
     * @return
     * @throws IOException
     */
    public static Path convertPdf(Path path, String fileUrl) throws IOException {
        final String ooServer = getOoServer();
        final String ooJwt = RebuildConfiguration.get(OnlyofficeJwt);

        String filename = path.getFileName().toString();
        String filenameWithoutExt = filename.substring(0, filename.lastIndexOf("."));
        JSONObject document = new JSONObject(true);
        document.put("async", false);
        document.put("key", "key-" + CommonsUtils.randomHex(true));
        document.put("fileType", FileUtil.getSuffix(filename));
        document.put("outputType", "pdf");
        document.put("title", filenameWithoutExt);
        document.put("url", fileUrl);

        // Token
        String tokenIfNeed = StringUtils.isBlank(ooJwt) ? null : JWT.create()
                .addPayloads(document)
                .setKey(ooJwt.getBytes(UTF_8))
                .sign();

        Map<String, String> reqHeaders = new HashMap<>();
        reqHeaders.put("Content-Type", "application/json");
        if (tokenIfNeed != null) {
            reqHeaders.put("Authorization", "Bearer " + tokenIfNeed);
        }

        String res = OkHttpUtils.post(ooServer + "/converter", document, reqHeaders);
        JSONObject resJson = JSON.parseObject(res);

        String resFileUrl = resJson == null ? null : resJson.getString("fileUrl");
        if (resFileUrl != null) {
            File dest = new File(
                    RebuildConfiguration.getFileOfTemp(null), filenameWithoutExt + ".pdf");
            OkHttpUtils.readBinary(resFileUrl, dest, null);
            if (dest.exists()) return dest.toPath();
        }

        throw new RebuildException("Convert PDF fails (oo-ds) : " + res);
    }

    /**
     * @param filepath
     * @param editorConfig
     * @return
     * @see com.rebuild.web.commons.FileDownloader
     */
    public static Object[] buildPreviewParams(String filepath, JSONObject editorConfig) {
        getOoServer();
        final String ooJwt = RebuildConfiguration.get(OnlyofficeJwt);

        final String filepathDecode = CodecUtils.urlDecode(filepath);
        String[] fs = filepathDecode.split("/");
        String filename = fs[fs.length - 1].split("\\?")[0];

        JSONObject document = new JSONObject(true);
        document.put("fileType", FileUtil.getSuffix(filename));
        document.put("key", "key-" + EncryptUtils.toMD5Hex(filepath.split("\\?")[0]));
        document.put("title", QiniuCloud.parseFileName(filename));
        // 外部地址
        if (CommonsUtils.isExternalUrl(filepath)) {
            document.put("url", filepath);
        } else {
            if (filepath.startsWith("/")) filepath = filepath.substring(1);
            String fileUrl = String.format("/filex/download/%s?_csrfToken=%s",
                    filepath, AuthTokenManager.generateCsrfToken(90));
            fileUrl = RebuildConfiguration.getHomeUrl(fileUrl);
            document.put("url", fileUrl);
        }

        // 编辑需要
        if ("edit".equals(editorConfig.getString("mode"))) {
            JSONObject permissions = new JSONObject();
            permissions.put("edit", true);
            permissions.put("chat", false);
            document.put("permissions", permissions);
        }

        // Token
        String tokenIfNeed = StringUtils.isBlank(ooJwt) ? null : JWT.create()
                .setPayload("document", document)
                .setPayload("editorConfig", editorConfig)
                .setKey(ooJwt.getBytes(UTF_8))
                .sign();

        return new Object[]{document, tokenIfNeed};
    }

    /**
     * @return
     */
    public static String getOoServer() {
        String ooServer = RebuildConfiguration.get(OnlyofficeServer);
        Assert.notNull(ooServer, "[OnlyofficeServer] is not set");

        if (ooServer.endsWith("/")) ooServer = ooServer.substring(0, ooServer.length() - 1);
        return ooServer;
    }

    /**
     * @return
     */
    public static boolean isUseOoPreview() {
        if (RebuildConfiguration.get(OnlyofficeServer) == null) return false;
        String o = RebuildConfiguration.get(ConfigurationItem.PortalOfficePreviewUrl);
        return StringUtils.isBlank(o) || o.contains(OO_PREVIEW_URL);
    }

    /**
     * @return
     */
    public static String getBestPreviewUrl() {
        // v4.1
        if (OnlyOffice.isUseOoPreview()) {
            return AppUtils.getContextPath(OnlyOffice.OO_PREVIEW_URL);
        }
        // v4.0
        return StringUtils.defaultIfBlank(
                RebuildConfiguration.get(ConfigurationItem.PortalOfficePreviewUrl),
                "https://view.officeapps.live.com/op/view.aspx?src=");
    }
}
