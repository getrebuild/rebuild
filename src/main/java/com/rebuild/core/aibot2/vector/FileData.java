/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.vector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import com.openai.models.audio.transcriptions.TranscriptionCreateResponse;
import com.rebuild.core.Application;
import com.rebuild.core.aibot2.AiBotException;
import com.rebuild.core.aibot2.AibotAgent;
import com.rebuild.core.aibot2.AibotContextHolder;
import com.rebuild.core.aibot2.ChatManager;
import com.rebuild.core.aibot2.Config;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.OkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * 语音识别大小限制：25MB（transcription 接口限制）
     */
    private static final long MAX_AUDIO_SIZE = 25 * 1024 * 1024;

    /**
     * 支持语音识别的音频扩展名
     */
    private static final Set<String> AUDIO_EXTS = new HashSet<>(Arrays.asList(
            "mp3", "m4a", "wav", "amr", "aac", "flac", "ogg", "opus", "wma", "webm"));

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
            if (!file.exists() && QiniuCloud.instance().available()) {
                try {
                    file = QiniuCloud.downloadFile(filePath);
                } catch (Exception e) {
                    log.warn("Cannot download file from cloud : {}", filePath);
                }
            }
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
            } else if (isAudioFile(file, mimeType) && StringUtils.isNotBlank(asrModel())) {
                content = parseAudioWithAI(file, filePath);
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
        String res = String.format("文件（%s，fileKey=%s）内容如下：", name, filePath)
                + NN + content + NN +
                String.format("文件（%s，fileKey=%s）内容结束", name, filePath);
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
            return ChatManager.ask(askContent, null, Collections.singletonList(file), "FileData");
        } else {
            List<File> pageImages = renderPdfToImages(file);
            if (pageImages.isEmpty()) return null;

            try {
                return ChatManager.ask(askContent, null, pageImages, "FileData");
            } finally {
                pageImages.forEach(FileUtils::deleteQuietly);
            }
        }
    }

    /**
     * 是否为音频文件（MIME 或扩展名判断）
     *
     * @param file
     * @param mimeType
     * @return
     */
    static boolean isAudioFile(File file, String mimeType) {
        if (mimeType != null && mimeType.startsWith("audio/")) return true;
        return AUDIO_EXTS.contains(FilenameUtils.getExtension(file.getName()).toLowerCase());
    }

    /**
     * 当前生效的语音识别模型（agent 优先回退系统配置，均未配置返回 null 即不启用）
     *
     * @return
     */
    private static String asrModel() {
        AibotAgent agent = AibotContextHolder.getAgent();
        return agent != null ? agent.asrModel() : Config.getAsrModel();
    }

    /**
     * 通过语音识别转写音频内容（agent 自有 API 连接时使用 agent 的网关）
     *
     * @param file
     * @param sourcePath
     * @return
     */
    private String parseAudioWithAI(File file, String sourcePath) {
        if (FileUtils.sizeOf(file) > MAX_AUDIO_SIZE) {
            throw new AiBotException("音频文件超过 25MB，无法转写");
        }

        AibotAgent agent = AibotContextHolder.getAgent();
        OpenAIClient client = agent != null ? agent.client() : Config.getClient();

        // 接口按文件名扩展名识别格式，下载的临时文件可能无扩展名，需用源路径的扩展名补名
        File asrFile = file;
        try {
            if (!AUDIO_EXTS.contains(FilenameUtils.getExtension(asrFile.getName()).toLowerCase())) {
                String sourceExt = FilenameUtils.getExtension(sourcePath).toLowerCase();
                if (AUDIO_EXTS.contains(sourceExt)) {
                    asrFile = RebuildConfiguration.getFileOfTemp("asr-" + CommonsUtils.randomHex() + "." + sourceExt);
                    FileUtils.copyFile(file, asrFile);
                }
            }

            TranscriptionCreateResponse resp = client.audio().transcriptions().create(
                    TranscriptionCreateParams.builder()
                            .file(asrFile.toPath())
                            .model(asrModel())
                            .build());

            if (resp.isTranscription()) {
                return StringUtils.trim(resp.asTranscription().text());
            }
            // 兼容非标准网关的响应结构
            JsonValue json = resp._json().orElse(null);
            if (json != null) {
                Map<String, Object> values = json.convert(new TypeReference<Map<String, Object>>() {});
                if (values != null && values.get("text") != null) {
                    return StringUtils.trim(String.valueOf(values.get("text")));
                }
            }
            throw new AiBotException("语音识别返回内容为空");

        } catch (IOException e) {
            throw new AiBotException("语音识别失败:" + e.getLocalizedMessage());
        } finally {
            if (asrFile != file) FileUtils.deleteQuietly(asrFile);
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
