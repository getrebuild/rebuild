/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.vector;

import com.rebuild.core.service.aibot.AiBotException;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import org.apache.tika.Tika;

import java.io.File;

/**
 * 文件
 *
 * @author Zixin
 * @since 2026/4/28
 */
public class FileData implements VectorData {

    static Tika tika = new Tika();
    static {
        tika.setMaxStringLength(1024 * 1024 * 50);  // 50M
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
            content = tika.parseToString(file.toPath());
            content = content.trim();
        } catch (Throwable e) {
            throw new AiBotException("无法识别文件:" + e.getLocalizedMessage());
        }

        String s = String.format("文件（%s）内容如下：", QiniuCloud.parseFileName(filepath));
        return s + NN + "```" + N + content + N + "```" + N;
    }
}
