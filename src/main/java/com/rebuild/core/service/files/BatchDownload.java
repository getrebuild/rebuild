/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.files;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.utils.CommonsUtils;
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
        final String tmpName = CommonsUtils.genPrettyName("RBFILE", true);
        File tmp = RebuildConfiguration.getFileOfTemp(tmpName);
        FileUtils.forceMkdir(tmp);

        File tmpZip = RebuildConfiguration.getFileOfTemp(tmpName + ".zip");

        for (String path : files) {
            if (StringUtils.isBlank(path)) continue;

            // v4.1 也可以是附件ID
            if (ID.isId(path)) {
                Object filePath = QueryHelper.queryFieldValue(ID.valueOf(path), "filePath");
                if (filePath == null) continue;
                path = filePath.toString();
            }

            // FIXME 太大的文件不适用于下载
            File dest = new File(tmp, QiniuCloud.parseFileName(path));
            try {
                if (QiniuCloud.instance().available()) {
                    QiniuCloud.instance().download(path, dest);
                } else {
                    File s = RebuildConfiguration.getFileOfData(path);
                    CompressUtils.copy(s, dest);
                }

            } catch (IOException ex) {
                log.error("Cannot read file. ignored : {}", path, ex);
            }
        }

        CompressUtils.forceZip(tmpZip, tmp, null);
        destZip = tmpZip.exists() ? tmpZip : null;

        return destZip;
    }

    /**
     * 压缩成功后的文件
     *
     * @return
     */
    public File getDestZip() {
        return destZip;
    }
}
