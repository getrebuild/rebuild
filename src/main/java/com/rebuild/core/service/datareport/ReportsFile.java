/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.CompressUtils;
import com.rebuild.utils.PdfConverter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

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

    private List<File> files = new ArrayList<>();

    public ReportsFile(File parent, String fileName) {
        super(ObjectUtils.defaultIfNull(parent, RebuildConfiguration.getFileOfTemp("/")),
                StringUtils.defaultIfBlank(fileName, "RBREPORT-" + System.currentTimeMillis()));
    }

    public ReportsFile() {
        this(null, null);
    }

    public ReportsFile addFile(File file, String reportName) throws IOException {
        if (!this.exists()) FileUtils.forceMkdir(this);

        if (reportName == null) reportName = file.getName();
        String fileName = (files.size() + 1) + "-" + reportName;

        File dest = new File(this, fileName);
        FileUtils.moveFile(file, dest);
        files.add(dest);
        return this;
    }

    public File[] getFiles() {
        return files.toArray(new File[0]);
    }

    public File toZip(boolean makePdf) throws IOException {
        return toZip(makePdf, false);
    }

    public File toZip(boolean makePdf, boolean keepOrigin) throws IOException {
        FileFilter filter = null;
        if (makePdf) {
            for (File file : files) {
                File pdfFile = convertPdf(file);
                FileUtils.copyFile(pdfFile, new File(this, pdfFile.getName()));
            }

            filter = file -> file.getName().endsWith(".pdf");
            if (!keepOrigin) filter = null;
        }

        File zipFile = RebuildConfiguration.getFileOfTemp(this.getName() + ".zip");
        try {
            CompressUtils.forceZip(zipFile, this, filter);
            return zipFile;
        } catch (IOException ex) {
            throw new ReportsException("Cannot make zip for reports", ex);
        }
    }

    public File convertPdf(File file) {
        Path pdf = PdfConverter.convert(file.toPath(), PdfConverter.TYPE_PDF);
        return pdf.toFile();
    }
}
