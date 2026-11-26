/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
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
            log.warn("Deleted existing file before creating new one : {}", destZip);
            FileUtils.deleteQuietly(destZip);
        }

        try {
            zip(Files.newOutputStream(destZip.toPath()), fileOrDir, filter);
        } catch (IOException | RuntimeException ex) {
            FileUtils.deleteQuietly(destZip);
            throw ex;
        }
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
        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(destZipOutputStream, 8192 * 10);
             ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(bufferedOutputStream)) {
            zipArchiveOutputStream.setUseZip64(Zip64Mode.AsNeeded);

            addFileToZip(zipArchiveOutputStream, fileOrDir, null, filter);
            zipArchiveOutputStream.finish();
        }
    }

    /**
     * @param destZip
     * @param files
     * @throws IOException
     */
    public static void forceZip(File destZip, File... files) throws IOException {
        if (destZip.exists()) {
            log.warn("Deleted existing file before creating new one : {}", destZip);
            FileUtils.deleteQuietly(destZip);
        }

        try (OutputStream destZipOutputStream = Files.newOutputStream(destZip.toPath());
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(destZipOutputStream);
             ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(bufferedOutputStream)) {

            for (File file : files) {
                addFileToZip(zipArchiveOutputStream, file, null, null);
            }
            zipArchiveOutputStream.finish();

        } catch (IOException | RuntimeException ex) {
            // 失败的归档不保留
            FileUtils.deleteQuietly(destZip);
            throw ex;
        }
    }

    private static void addFileToZip(ZipArchiveOutputStream zipArchiveOutputStream, File file, String path, FileFilter filter) throws IOException {
        String entryName = (path != null) ? path + file.getName() : file.getName();

        if (file.isFile()) {
            FileInputStream fInputStream;
            try {
                fInputStream = new FileInputStream(file);
            } catch (IOException ex) {
                log.warn("Skip file in zip (cannot open) : {} - {}", file, ex.toString());
                return;
            }

            try (FileInputStream in = fInputStream) {
                byte[] head = new byte[8192];
                int headLen = 0;
                try {
                    int n;
                    while (headLen < head.length && (n = in.read(head, headLen, head.length - headLen)) > 0) {
                        headLen += n;
                    }
                } catch (IOException ex) {
                    log.warn("Skip file in zip (cannot read) : {} - {}", file, ex.toString());
                    return;
                }

                zipArchiveOutputStream.putArchiveEntry(new ZipArchiveEntry(file, entryName));
                zipArchiveOutputStream.write(head, 0, headLen);
                IOUtils.copy(in, zipArchiveOutputStream);
                zipArchiveOutputStream.closeArchiveEntry();
            }

        } else {
            zipArchiveOutputStream.putArchiveEntry(new ZipArchiveEntry(file, entryName));
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
     */
    public static void copy(File source, File dest) throws IOException {
        FileUtils.copyFile(source, dest);
    }
}
