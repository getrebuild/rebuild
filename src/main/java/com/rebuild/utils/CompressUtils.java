/*!
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;

/**
 * @author devezhao
 * @since 2021/12/15
 */
@Slf4j
public class CompressUtils {

    /**
     * @param destZip
     * @param fileOrDir
     * @param filter
     * @throws IOException
     */
    public static void forceZip(File destZip, File fileOrDir, FileFilter filter) throws IOException {
        if (destZip.exists()) {
            log.warn("delete exists after create : {}", destZip);
            FileUtils.deleteQuietly(destZip);
        }

        zip(Files.newOutputStream(destZip.toPath()), fileOrDir, filter);
    }

    /**
     * Creates a zip output stream at the specified path with the contents of the specified directory.
     *
     * @param destZipOutputStream
     * @param fileOrDir
     * @param filter
     * @throws IOException
     */
    public static void zip(OutputStream destZipOutputStream, File fileOrDir, FileFilter filter) throws IOException {
        BufferedOutputStream bufferedOutputStream = null;
        ZipArchiveOutputStream zipArchiveOutputStream = null;

        try {
            bufferedOutputStream = new BufferedOutputStream(destZipOutputStream);
            zipArchiveOutputStream = new ZipArchiveOutputStream(bufferedOutputStream);

            addFileToZip(zipArchiveOutputStream, fileOrDir, null, filter);

        } finally {
            if (zipArchiveOutputStream != null) {
                zipArchiveOutputStream.finish();
                zipArchiveOutputStream.close();
            }

            IOUtils.closeQuietly(bufferedOutputStream);
            IOUtils.closeQuietly(destZipOutputStream);
        }
    }

    /**
     * @param destZip
     * @param files
     * @throws IOException
     */
    public static void forceZip(File destZip, File... files) throws IOException {
        if (destZip.exists()) {
            log.warn("delete exists after create : {}", destZip);
            FileUtils.deleteQuietly(destZip);
        }

        OutputStream destZipOutputStream = Files.newOutputStream(destZip.toPath());
        BufferedOutputStream bufferedOutputStream = null;
        ZipArchiveOutputStream zipArchiveOutputStream = null;

        try {
            bufferedOutputStream = new BufferedOutputStream(destZipOutputStream);
            zipArchiveOutputStream = new ZipArchiveOutputStream(bufferedOutputStream);

            for (File file : files) {
                addFileToZip(zipArchiveOutputStream, file, null, null);
            }

        } finally {
            if (zipArchiveOutputStream != null) {
                zipArchiveOutputStream.finish();
                zipArchiveOutputStream.close();
            }

            IOUtils.closeQuietly(bufferedOutputStream);
            IOUtils.closeQuietly(destZipOutputStream);
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

    /**
     * @param source
     * @param dest
     * @throws IOException
     * @see FileUtils#copyFile(File, File) Bad on unix!
     * @see IOUtils#copyLarge(Reader, Writer)
     */
    public static void copy(File source, File dest) throws IOException {
        FileUtils.copyFile(source, dest);
    }
}
