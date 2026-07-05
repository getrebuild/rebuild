/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.vector;

import com.rebuild.core.Application;
import com.rebuild.core.service.aibot.AiBotException;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.OkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 文件
 *
 * @author Zixin
 * @since 2026/4/28
 */
@Slf4j
public class FileData implements VectorData {

    static final Tika TIKA = new Tika();
    static {
        TIKA.setMaxStringLength(1024 * 1024 * 50);  // 50M
    }

    private final Object fileOrPath;

    public FileData(String filepath) {
        this.fileOrPath = filepath;
    }

    public FileData(File file) {
        this.fileOrPath = file;
    }

    @Override
    public String toVector() {
        final String filePath = fileOrPath.toString();
        final String fileKey = "FileData:" + filePath;
        String cached = Application.getCommonsCache().get(fileKey);
        if (cached != null) return cached;

        File file = null;
        if (fileOrPath instanceof File) {
            file = (File) fileOrPath;
        } else if (fileOrPath instanceof Path) {
            file = ((Path) fileOrPath).toFile();
        } else if (CommonsUtils.isExternalUrl(filePath)) {
            try {
                file = OkHttpUtils.readBinary(filePath);
            } catch (IOException e) {
                log.error("Reading file error : {}", filePath, e);
            }
        } else {
            file = RebuildConfiguration.getFileOfTemp(filePath);
            if (!file.exists()) file = RebuildConfiguration.getFileOfData(filePath);
            if (!file.exists()) file = new File(filePath);
        }

        if (file == null || !file.isFile()) {
            throw new AiBotException("无法读取文件:" + filePath);
        }

        String content;
        try {
            content = TIKA.parseToString(file.toPath());
            content = content.trim();

            if (StringUtils.isBlank(content)) content = "无法识别文件";

        } catch (Throwable e) {
            throw new AiBotException("无法识别文件:" + e.getLocalizedMessage());
        }

        String name = QiniuCloud.parseFileName(filePath);
        String res = String.format("文件（%s）内容如下：", name)
                + NN + content + NN +
                String.format("文件（%s）内容结束", name);
        Application.getCommonsCache().put(fileKey, res);
        return res;
    }
}
