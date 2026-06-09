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

    private final String filepath;

    public FileData(String filepath) {
        this.filepath = filepath;
    }

    @Override
    public String toVector() {
        final String fileKey = "FileData:" + filepath;
        String cached = Application.getCommonsCache().get(fileKey);
        if (cached != null) return cached;

        File file = null;
        if (CommonsUtils.isExternalUrl(filepath)) {
            try {
                file = OkHttpUtils.readBinary(filepath);
            } catch (IOException e) {
                log.error("Reading file error : {}", filepath, e);
            }
        } else {
            file = RebuildConfiguration.getFileOfTemp(filepath);
            if (!file.exists()) file = RebuildConfiguration.getFileOfData(filepath);
        }

        if (file == null || !file.isFile()) {
            throw new AiBotException("无法读取文件:" + filepath);
        }

        String content;
        try {
            content = TIKA.parseToString(file.toPath());
            content = content.trim();

            if (StringUtils.isBlank(content)) content = "无法识别文件";

        } catch (Throwable e) {
            throw new AiBotException("无法识别文件:" + e.getLocalizedMessage());
        }

        String name = QiniuCloud.parseFileName(filepath);
        String res = String.format("文件（%s）内容如下：", name)
                + NN + content + NN +
                String.format("文件（%s）内容结束", name);
        Application.getCommonsCache().put(fileKey, res);
        return res;
    }
}
