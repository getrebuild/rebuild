/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.vector;

import com.rebuild.core.Application;
import com.rebuild.core.aibot2.AiBotException;
import com.rebuild.core.aibot2.ChatManager;
import com.rebuild.core.aibot2.Config;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.OkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文件
 *
 * @author Zixin
 * @since 2026/4/28
 */
@Slf4j
public class FileData implements VectorData {

    /**
     * AI 文件处理大小限制：50MB
     */
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    private final Object fileOrPath;

    public FileData(String filepath) {
        this.fileOrPath = filepath;
    }

    public FileData(File file) {
        this.fileOrPath = file;
    }

    @Override
    public String toVector() {
        final String filePath = fileOrPath.toString();
        final String fileKey = "FileData:" + filePath;
        String cached = Application.getCommonsCache().get(fileKey);
        if (cached != null) return cached;

        File file = null;
        if (fileOrPath instanceof File) {
            file = (File) fileOrPath;
        } else if (fileOrPath instanceof Path) {
            file = ((Path) fileOrPath).toFile();
        } else if (CommonsUtils.isExternalUrl(filePath)) {
            CommonsUtils.checkUrlSafe(filePath);
            try {
                file = OkHttpUtils.readBinary(filePath);
            } catch (IOException e) {
                log.error("Reading file error : {}", filePath, e);
            }
        } else {
            file = RebuildConfiguration.getFileOfTemp(filePath);
            if (!file.exists()) file = RebuildConfiguration.getFileOfData(filePath);
            if (!file.exists()) file = new File(filePath);
        }

        if (file == null || !file.isFile()) {
            throw new AiBotException("无法读取文件:" + filePath);
        }

        if (FileUtils.sizeOf(file) > MAX_FILE_SIZE) {
            throw new AiBotException("文件大小超过限制（50MB）");
        }

        String content;
        try {
            String mimeType = Config.TIKA.detect(file);

            if (mimeType != null && mimeType.startsWith("image/") && Config.availableAiBot()) {
                content = parseImageWithAI(file, mimeType);
            } else {
                content = Config.TIKA.parseToString(file.toPath());
                content = content.trim();

                if (StringUtils.isBlank(content)
                        && "application/pdf".equals(mimeType) && Config.availableAiBot()) {
                    content = parseImageWithAI(file, mimeType);
                }
            }
            content = StringUtils.trim(content);

            if (StringUtils.isBlank(content)) content = "无法识别文件";

        } catch (Throwable e) {
            throw new AiBotException("无法识别文件:" + e.getLocalizedMessage());
        }

        String name = QiniuCloud.parseFileName(filePath);
        String res = String.format("文件（%s）内容如下：", name)
                + NN + content + NN +
                String.format("文件（%s）内容结束", name);
        Application.getCommonsCache().put(fileKey, res);
        return res;
    }

    /**
     * 通过 AI 视觉能力识别图片内容（需要模型支持）
     *
     * @param file
     * @param mimeType
     * @return
     */
    private String parseImageWithAI(File file, String mimeType) {
        String askContent = "请详细描述这张图片中的内容，包括所有可见的文字、数据、表格、界面元素等信息";

        if (mimeType.startsWith("image/")) {
            return ChatManager.ask(askContent, null, Collections.singletonList(file));
        } else {
            List<File> pageImages = renderPdfToImages(file);
            if (pageImages.isEmpty()) return null;

            try {
                return ChatManager.ask(askContent, null, pageImages);
            } finally {
                pageImages.forEach(FileUtils::deleteQuietly);
            }
        }
    }

    /**
     * 将 PDF 页面渲染为图片文件
     *
     * @param pdfFile
     * @return
     */
    private List<File> renderPdfToImages(File pdfFile) {
        List<File> images = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 150, ImageType.RGB);
                File tempFile = RebuildConfiguration.getFileOfTemp(CommonsUtils.randomHex() + ".png");
                ImageIO.write(image, "png", tempFile);
                images.add(tempFile);
            }

        } catch (IOException e) {
            log.warn("Failed to render PDF to images : {}", pdfFile.getName(), e);
        }
        return images;
    }
}
