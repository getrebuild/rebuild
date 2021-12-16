/*
Copyright (c) Ruifang Tech <http://ruifang-tech.com/> and/or its owners. All rights reserved.
*/

package com.rebuild.core.support.setup;

import cn.devezhao.commons.CalendarUtils;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.CompressUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * 数据目录备份
 *
 * @author devezhao
 * @since 2021/12/15
 */
@Slf4j
public class DataFileBackup extends DatabaseBackup {

    /**
     * @return
     * @throws IOException
     */
    public File backup(File backups) throws IOException {
        File rbdata = RebuildConfiguration.getFileOfData("");

        String destName = "datafiles." + CalendarUtils.getPlainDateFormat().format(CalendarUtils.now()) + ".zip";
        File dest = new File(backups, destName);

        CompressUtils.forceZip(rbdata, dest, pathname -> {
            String name = pathname.getName();
            return !(name.equals("_backups") || name.equals("_log") || name.equals("temp") || name.equals("rebuild.pid"));
        });

        log.info("Backup succeeded : {} ({})", dest, FileUtils.byteCountToDisplaySize(dest.length()));

        return dest;
    }
}
