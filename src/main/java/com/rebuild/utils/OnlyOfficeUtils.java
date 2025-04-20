/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.hutool.core.io.FileUtil;
import cn.hutool.jwt.JWT;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.RebuildException;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.web.admin.ConfigurationController;
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
 * @author devezhao
 * @since 2025/2/26
 */
public class OnlyOfficeUtils {

    /**
     * OnlyOffice PDF
     *
     * @param path
     * @return
     * @throws PdfConverterException
     */
    public static Path convertPdf(Path path) throws IOException {
        final String ooServer = getOoServer();
        final String ooJwt = RebuildConfiguration.get(OnlyofficeJwt);

        String filename = path.getFileName().toString();
        String filenameWithoutExt = filename.substring(0, filename.lastIndexOf("."));
        JSONObject document = new JSONObject(true);
        document.put("async", false);
        document.put("key", "key-" + EncryptUtils.toMD5Hex(filename));
        document.put("fileType", FileUtil.getSuffix(filename));
        document.put("outputType", "pdf");
        document.put("title", filenameWithoutExt);

        String fileUrl = String.format("/filex/download/%s?_csrfToken=%s&temp=yes",
                filename, AuthTokenManager.generateCsrfToken(90));
        fileUrl = RebuildConfiguration.getHomeUrl(fileUrl);
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

        throw new RebuildException("Convert PDF fails (oo-ds) : " + resJson);
    }

    /**
     * @param filepath
     * @return
     */
    public static Object[] buildPreviewParams(String filepath) {
        final String ooJwt = RebuildConfiguration.get(OnlyofficeJwt);

        final String filepathDecode = CodecUtils.urlDecode(filepath);
        String[] fs = filepathDecode.split("/");
        String filename = fs[fs.length - 1].split("\\?")[0];

        JSONObject document = new JSONObject(true);
        document.put("fileType", FileUtil.getSuffix(filename));
        document.put("key", "key-" + EncryptUtils.toMD5Hex(filename));
        document.put("title", QiniuCloud.parseFileName(filename));
        // 外部地址
        if (CommonsUtils.isExternalUrl(filepath)) {
            document.put("url", filepath);
        } else {
            String fileUrl = String.format("/filex/download/%s?_csrfToken=%s",
                    filepath,
                    AuthTokenManager.generateCsrfToken(90));
            fileUrl = RebuildConfiguration.getHomeUrl(fileUrl);
            document.put("url", fileUrl);
        }

        // Token
        String tokenIfNeed = StringUtils.isBlank(ooJwt) ? null : JWT.create()
                .setPayload("document", document)
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
        return StringUtils.contains(
                RebuildConfiguration.get(ConfigurationItem.PortalOfficePreviewUrl),
                ConfigurationController.OO_PREVIEW_URL);
    }
}
