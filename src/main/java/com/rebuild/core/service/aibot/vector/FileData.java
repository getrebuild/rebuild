/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.vector;

import com.rebuild.core.service.aibot.AiBotException;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.Tika;

import java.io.File;

/**
 * 文件
 *
 * @author Zixin
 * @since 2026/4/28
 */
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
        File file = RebuildConfiguration.getFileOfTemp(filepath);

        String content;
        try {
            content = TIKA.parseToString(file.toPath());
            content = content.trim();

            if (StringUtils.isBlank(content)) content = "无法识别文件";

        } catch (Throwable e) {
            throw new AiBotException("无法识别文件:" + e.getLocalizedMessage());
        }

        String name = QiniuCloud.parseFileName(filepath);
        return String.format("文件（%s）内容如下：", name)
                + NN + content + NN +
                String.format("文件（%s）内容结束", name);
    }
}
