/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * 文件/图片上传工具
 * 支持 base64 或 URL 两种方式上传文件，返回 fileKey 供其他工具（如 CreateFeed）使用
 *
 * @author devezhao
 * @since 2026/7/24
 */
@Slf4j
public class UploadFile implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        JSONObject args = StringUtils.isBlank(arguments) ? new JSONObject() : JSON.parseObject(arguments);

        String base64 = args.getString("base64");
        String url = args.getString("url");
        String fileName = args.getString("fileName");

        if (StringUtils.isBlank(base64) && StringUtils.isBlank(url)) {
            throw new ToolException("请提供 base64 或 url 参数");
        }

        File tmpFile;
        String finalFileName;

        if (StringUtils.isNotBlank(base64)) {
            // base64 方式
            // 去除可能的前缀 data:xxx;base64,
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
            // URL 方式
            tmpFile = OkHttpUtils.readBinary(url);
            if (tmpFile == null || !tmpFile.exists()) {
                throw new ToolException("无法从 URL 下载文件: " + url);
            }

            if (StringUtils.isBlank(fileName)) {
                // 从 URL 中提取文件名
                String path = url.split("\\?")[0];
                fileName = path.substring(path.lastIndexOf("/") + 1);
                if (StringUtils.isBlank(fileName) || !fileName.contains(".")) {
                    fileName = "download-" + CommonsUtils.randomHex(true).substring(0, 8);
                }
            }
            finalFileName = fileName;
        }

        try {
            if (!tmpFile.exists() || FileUtils.sizeOf(tmpFile) == 0) {
                throw new ToolException("文件内容为空");
            }

            long fileSize = FileUtils.sizeOf(tmpFile);
            String fileKey = QiniuCloud.uploadFile(tmpFile, finalFileName);

            // 缓存文件大小
            FilesHelper.storeFileSize(fileKey, fileSize);

            String ext = FilenameUtils.getExtension(finalFileName);
            boolean isImage = isImageExt(ext);

            return JSONUtils.toJSONObject(
                    new String[]{"status", "fileKey", "fileName", "fileSize", "isImage", "message"},
                    new Object[]{"ok", fileKey, finalFileName, fileSize, isImage,
                            String.format("文件上传成功，fileKey: %s（%s）", fileKey,
                                    isImage ? "可作为图片使用" : "可作为附件使用")});

        } finally {
            FileUtils.deleteQuietly(tmpFile);
        }
    }

    /**
     * 判断是否为图片扩展名
     */
    private boolean isImageExt(String ext) {
        if (StringUtils.isBlank(ext)) return false;
        ext = ext.toLowerCase();
        return "jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext)
                || "gif".equals(ext) || "bmp".equals(ext) || "webp".equals(ext);
    }
}
