/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.ObjectUtils;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 粗略图
 *
 * @author RB
 * @since 07/01/2022
 * @see Thumbnails
 */
@Slf4j
public class ImageView2 {

    public static final int ORIGIN_WIDTH = 1000;

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
     */
    public File thumbQuietly(File img) {
        try {
            File thumb = thumb(img);
            return thumb != null && thumb.exists() ? thumb : img;
        } catch (Exception ex) {
            log.warn("Image thumb failed : {}", img, ex);
        }
        return img;
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

        BufferedImage bi = ImageIO.read(img);
        if (bi == null) {
            log.debug("Unsupportted image type : {}", img);
            return null;
        }

        int wh = getWidth();
        Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(bi);
        if (bi.getWidth() > wh) {
            builder.size(wh, wh);
        } else {
            builder.scale(1.0);
        }

        builder.toFile(thumb);
        return thumb;
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
}
