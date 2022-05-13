/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.files;

import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.utils.CompressUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author ZHAO
 * @since 2022/4/22
 */
@Slf4j
public class BatchDownload extends HeavyTask<File> {

    final private List<String> files;

    private File destZip;

    public BatchDownload(List<String> files) {
        this.files = files;
    }

    @Override
    protected File exec() throws Exception {
        File tmp = RebuildConfiguration.getFileOfTemp(String.format("RBFILES-%d", System.currentTimeMillis()));
        FileUtils.forceMkdir(tmp);

        File tmpZip = RebuildConfiguration.getFileOfTemp(tmp.getName() + ".zip");

        for (String path : files) {
            if (StringUtils.isBlank(path)) continue;

            File dest = new File(tmp, QiniuCloud.parseFileName(path));

            try {
                if (QiniuCloud.instance().available()) {
                    QiniuCloud.instance().download(path, dest);
                } else {
                    File s = RebuildConfiguration.getFileOfData(path);
                    CompressUtils.copy(s, dest);
                }

            } catch (IOException ex) {
                log.error("Cannot read source file : {}", path, ex);
            }
        }

        CompressUtils.forceZip(tmp, tmpZip, null);
        destZip = tmpZip.exists() ? tmpZip : null;

        return destZip;
    }

    /**
     * @return
     */
    public File getDestZip() {
        return destZip;
    }
}
