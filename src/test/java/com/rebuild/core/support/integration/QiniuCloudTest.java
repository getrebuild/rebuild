/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.integration;

import cn.devezhao.commons.ThreadPool;
import com.rebuild.TestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/08
 */
public class QiniuCloudTest extends TestSupport {

    @Test
    public void testUploadAndMakeUrl() throws Exception {
        if (!QiniuCloud.instance().available()) return;

        File file = ResourceUtils.getFile("classpath:classification-demo.xlsx");
        String uploadKey = QiniuCloud.instance().upload(file);
        System.out.println("uploadKey ... " + uploadKey);

        String downloadUrl = QiniuCloud.instance().url(uploadKey);
        System.out.println("downloadUrl ... " + downloadUrl);

        QiniuCloud.instance().delete(uploadKey);
    }

    @Test
    public void testFormatKey() {
        if (!QiniuCloud.instance().available()) return;

        String fileName = "imtestfile.txt";
        String fileKey = QiniuCloud.formatFileKey(fileName);
        System.out.println("File key ... " + fileKey);
        String fileName2 = QiniuCloud.parseFileName(fileKey);
        Assertions.assertEquals(fileName, fileName2);

        System.out.println("File2 key ... " + QiniuCloud.formatFileKey("_____1123++545#e+++?&&f  d  fefe.txt"));
        System.out.println("File3 key ... " + QiniuCloud.formatFileKey("_____1123++545#e+++?&&f  d  fefe.txt", false));
        System.out.println("File4 key ... " + QiniuCloud.formatFileKey("_____1123++545#e+++?&&f  d  fefe", false));
    }

    @Test
    public void testPrivateUrl() {
        if (!QiniuCloud.instance().available()) return;

        for (int i = 0; i < 10; i++) {
            System.out.println(i + " = " + QiniuCloud.instance().url("rb/20190830/170016833__0190815223938.png"));
            ThreadPool.waitFor(300);
        }
    }

    @Test
    public void stats() {
        if (!QiniuCloud.instance().available()) return;
        System.out.println(QiniuCloud.instance().stats());
    }
}
