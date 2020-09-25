/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.CalendarUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;

/**
 * @author ZHAO
 * @since 2020/2/25
 */
public class FileFilterByLastModified implements FileFilter {

    private long lessThisTime;

    /**
     * @param exceedDays 小于此时间的将被返回
     */
    public FileFilterByLastModified(int exceedDays) {
        this.lessThisTime = CalendarUtils.addDay(-exceedDays).getTime();
    }

    @Override
    public boolean accept(File file) {
        if (file.isHidden() || file.isDirectory() || file.lastModified() == 0) {
            return false;
        }
        return file.lastModified() < this.lessThisTime;
    }

    // --

    /**
     * 删除指定目录下的文件
     *
     * @param indir
     * @param keepDays 保留最近N天
     */
    public static void deletes(File indir, int keepDays) {
        File[] ds = indir.listFiles(new FileFilterByLastModified(keepDays));
        if (ds == null || ds.length == 0) {
            return;
        }

        for (File d : ds) {
            if (d.isFile()) FileUtils.deleteQuietly(d);
        }
    }
}
