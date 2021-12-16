/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;

/**
 * @author devezhao
 * @since 2021/12/15
 */
@Slf4j
public class CompressUtils {

    /**
     * @param fileOrDir
     * @param destZip delete after create
     * @param filter
     * @throws IOException
     */
    public static void forceZip(File fileOrDir, File destZip, FileFilter filter) throws IOException {
        if (destZip.exists()) {
            log.warn("delete exists after create : {}", destZip);
            FileUtils.deleteQuietly(destZip);
        }

        zip(fileOrDir, Files.newOutputStream(destZip.toPath()), filter);
    }

    /**
     * Creates a zip output stream at the specified path with the contents of the specified directory.
     *
     * @param fileOrDir
     * @param zipOutputStream
     * @param filter
     * @throws IOException
     */
    public static void zip(File fileOrDir, OutputStream zipOutputStream, FileFilter filter) throws IOException {
        BufferedOutputStream bufferedOutputStream = null;
        ZipArchiveOutputStream zipArchiveOutputStream = null;

        try {
            bufferedOutputStream = new BufferedOutputStream(zipOutputStream);
            zipArchiveOutputStream = new ZipArchiveOutputStream(bufferedOutputStream);

            addFileToZip(zipArchiveOutputStream, fileOrDir, null, filter);

        } finally {
            if (zipArchiveOutputStream != null) {
                zipArchiveOutputStream.finish();
                zipArchiveOutputStream.close();
            }

            IOUtils.closeQuietly(bufferedOutputStream);
            IOUtils.closeQuietly(zipOutputStream);
        }
    }

    private static void addFileToZip(ZipArchiveOutputStream zipArchiveOutputStream, File file, String path, FileFilter filter) throws IOException {
        // at first call it is the folder, otherwise is the relative path
        String entryName = (path != null) ? path + file.getName() : file.getName();
        ZipArchiveEntry zipEntry = new ZipArchiveEntry(file, entryName);
        zipArchiveOutputStream.putArchiveEntry(zipEntry);

        // if is a file, add the content to zip file
        if (file.isFile()) {
            FileInputStream fInputStream = null;
            try {
                fInputStream = new FileInputStream(file);
                IOUtils.copy(fInputStream, zipArchiveOutputStream);
                zipArchiveOutputStream.closeArchiveEntry();
            } finally {
                IOUtils.closeQuietly(fInputStream);
            }
        } else {
            // is a directory so it calls recursively all files in folder
            zipArchiveOutputStream.closeArchiveEntry();
            File[] children = file.listFiles(filter);
            if (children != null) {
                for (File child : children) {
                    addFileToZip(zipArchiveOutputStream, child, entryName + "/", filter);
                }
            }
        }
    }
}
