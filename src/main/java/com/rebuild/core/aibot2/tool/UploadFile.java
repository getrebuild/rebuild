/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.service.files.FilesHelper;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.OkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * 文件/图片上传工具
 *
 * @author devezhao
 * @since 2026/7/24
 */
@Slf4j
public class UploadFile implements Tool {

    /**
     * AI 文件处理大小限制：50MB
     */
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String base64 = args.getString("base64");
        String url = args.getString("url");
        String fileName = args.getString("fileName");

        if (StringUtils.isBlank(base64) && StringUtils.isBlank(url)) {
            throw new KnownToolException("请提供 base64 或 url 参数");
        }

        File tmpFile = null;
        String finalFileName;

        try {
            if (StringUtils.isNotBlank(base64)) {
                if (base64.contains(",")) {
                    base64 = base64.substring(base64.indexOf(",") + 1);
                }

                if (StringUtils.isBlank(fileName)) {
                    fileName = "upload-" + CommonsUtils.randomHex(true).substring(0, 8) + ".png";
                }
                finalFileName = fileName;

                tmpFile = RebuildConfiguration.getFileOfTemp("aibot-" + CommonsUtils.randomHex(true) + "-" + finalFileName);
                CommonsUtils.base64ToFile(base64, tmpFile);

            } else {
                CommonsUtils.checkUrlSafe(url);

                tmpFile = OkHttpUtils.readBinary(url);
                if (tmpFile == null || !tmpFile.exists()) {
                    throw new KnownToolException("无法从 URL 下载文件: " + url);
                }

                if (StringUtils.isBlank(fileName)) {
                    String path = url.split("\\?")[0];
                    fileName = path.substring(path.lastIndexOf("/") + 1);
                    if (StringUtils.isBlank(fileName) || !fileName.contains(".")) {
                        fileName = "download-" + CommonsUtils.randomHex(true).substring(0, 8);
                    }
                }
                finalFileName = fileName;
            }

            if (!tmpFile.exists() || FileUtils.sizeOf(tmpFile) == 0) {
                throw new KnownToolException("文件内容为空");
            }

            long fileSize = FileUtils.sizeOf(tmpFile);
            if (fileSize > MAX_FILE_SIZE) {
                throw new KnownToolException("文件大小超过限制（50MB），当前文件大小：" + (fileSize / 1024 / 1024) + "MB");
            }
            String fileKey = QiniuCloud.uploadFile(tmpFile, finalFileName);
            if (fileKey == null) {
                throw new KnownToolException("文件上传失败，请稍后重试");
            }

            FilesHelper.storeFileSize(fileKey, fileSize);

            boolean isImage = CommonsUtils.isImageFile(finalFileName);
            String message = String.format("文件上传成功，fileKey: %s（%s）", fileKey, isImage ? "可作为图片使用" : "可作为附件使用");

            return JSONUtils.toJSONObject(
                    new String[]{"status", "fileKey", "fileName", "fileSize", "isImage", "message"},
                    new Object[]{"ok", fileKey, finalFileName, fileSize, isImage, message});

        } finally {
            FileUtils.deleteQuietly(tmpFile);
        }
    }
}
