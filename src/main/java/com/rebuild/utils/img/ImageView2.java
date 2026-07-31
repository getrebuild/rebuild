/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils.img;

import cn.devezhao.commons.ObjectUtils;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * 粗略图
 *
 * @author RB
 * @since 07/01/2022
 * @see Thumbnails
 */
@Slf4j
public class ImageView2 {

    // 默认压缩宽度
    public static final int ORIGIN_WIDTH = 1000;
    // 极限压缩大小 50M
    public static final long MAX_FILE_SIZE = 50 * 1024 * 1024L;;

    private String imageView2;
    private int width = -1;

    public ImageView2(String imageView2) {
        this.imageView2 = imageView2;
    }

    public ImageView2(int width) {
        this("/w/" + width);
    }

    /**
     * @return
     */
    public int getWidth() {
        if (width == -1) this.width = parseWidth();
        return width;
    }

    /**
     * @param img
     * @return
     * @throws IOException
     */
    public File thumb(File img) throws IOException {
        final String fileKey = formatFileKey(img);

        File thumb = RebuildConfiguration.getFileOfTemp(fileKey);
        if (thumb.exists()) return thumb;

        // 文件过大直接跳过避免 OOM
        if (FileUtils.sizeOf(img) > MAX_FILE_SIZE) {
            log.warn("Image file too large to thumbnail ({}) : {}", FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(img)), img);
            return null;
        }

        int wh = getWidth();
        BufferedImage bi = readSubsampled(img, wh);
        if (bi == null) {
            log.debug("Unsupportted image type : {}", img);
            return null;
        }

        Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(bi);
        if (bi.getWidth() > wh) {
            builder.size(wh, wh);
        } else {
            builder.scale(1.0);
        }

        builder.toFile(thumb);
        return thumb;
    }

    /**
     * 使用 Subsampled 读取图片，避免大图全量解码导致 OOM
     *
     * @param img
     * @param targetWidth
     * @return
     * @throws IOException
     */
    protected static BufferedImage readSubsampled(File img, int targetWidth) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(img)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) return null;

            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);

                int srcWidth = reader.getWidth(0);
                int srcHeight = reader.getHeight(0);

                ImageReadParam param = reader.getDefaultReadParam();
                // 计算 subsampling 步长，确保解码后宽度不超过目标的 2 倍
                int subsample = Math.max(1, srcWidth / (targetWidth * 2));
                if (subsample > 1) {
                    param.setSourceSubsampling(subsample, subsample, 0, 0);
                }

                return reader.read(0, param);
            } finally {
                reader.dispose();
            }
        }
    }

    // imageView2/2/w/300/interlace/1/q/100
    private int parseWidth() {
        if (imageView2.contains("/w/")) {
            String w = imageView2.split("/w/")[1].split("/")[0];
            return ObjectUtils.toInt(w, ORIGIN_WIDTH);
        } else {
            return ORIGIN_WIDTH;
        }
    }

    private String formatFileKey(File file) {
        return String.format("thumb%d.%s.%s", getWidth(),
                CommonsUtils.maxstr(file.getParentFile().getName(), 50), file.getName());
    }

    // --

    /**
     * 压缩图片大小
     * 
     * @param img
     * @return
     */
    public static File thumbQuietly(File img, int width) {
        try {
            File thumb = new ImageView2(width).thumb(img);
            return thumb != null && thumb.exists() ? thumb : img;
        } catch (Throwable ex) {
            log.warn("Image thumb failed : {}", img, ex);
        }
        return img;
    }
}
