/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.CompressUtils;
import com.rebuild.utils.PdfConverter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 导出报表打包
 *
 * @author devezhao
 * @since 2025/7/19
 */
public class ReportsFile extends File {
    private static final long serialVersionUID = -8876458376733911086L;

    @Setter
    private String reportName;

    private List<File> files = new ArrayList<>();

    /**
     * @param parent
     * @param fileName
     */
    public ReportsFile(File parent, String fileName) {
        super(parent, fileName);
    }

    /**
     * @param fileName
     * @param reportName
     */
    public ReportsFile(String fileName, String reportName) {
        this(RebuildConfiguration.getFileOfTemp("/"), fileName);
        this.reportName = reportName;
    }

    /**
     * @param file
     * @return
     * @throws IOException
     */
    public ReportsFile addFile(File file) throws IOException {
        if (!this.exists()) FileUtils.forceMkdir(this);

        String fileName = (files.size() + 1) + "-" + reportName;
        File dest = new File(this, fileName);
        FileUtils.moveFile(file, dest);
        files.add(dest);
        return this;
    }

    /**
     * @return
     * @throws IOException
     */
    public File toZip() throws IOException {
        return toZip(false);
    }

    /**
     * @param makePdf
     * @return
     * @throws IOException
     */
    public File toZip(boolean makePdf) throws IOException {
        FileFilter filter = null;
        if (makePdf) {
            for (File file : files) {
                Path pdf = PdfConverter.convert(file.toPath(), PdfConverter.TYPE_PDF);
                File pdfFile = pdf.toFile();
                FileUtils.copyFile(pdfFile, new File(this, pdfFile.getName()));
            }
            filter = file -> file.getName().endsWith(".pdf");
        }

        File zipFile = RebuildConfiguration.getFileOfTemp(this.getName() + ".zip");
        try {
            CompressUtils.forceZip(zipFile, this, filter);
            return zipFile;
        } catch (IOException ex) {
            throw new ReportsException("Cannot make zip for reports", ex);
        }
    }
}
