/*!
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
import java.io.IOException;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/08
 */
public class QiniuCloudTest extends TestSupport {

    @Test
    void testFormatKey() {
        String fileName = "imtestfile.txt";
        String fileKey = QiniuCloud.formatFileKey(fileName);
        System.out.println("File key ... " + fileKey);
        String fileName2 = QiniuCloud.parseFileName(fileKey);
        Assertions.assertEquals(fileName, fileName2);

        System.out.println("File2 key ... " + QiniuCloud.formatFileKey("_____1123++545#e+++?&&f  d  fefe.txt"));
        System.out.println("File2 key ... " + QiniuCloud.formatFileKey("1123++545#e+++?&&f  d / fefe%=.txt"));
        System.out.println("File3 key ... " + QiniuCloud.formatFileKey("_____1123++545#e+++?&&f  d  fefe.txt", false));
        System.out.println("File4 key ... " + QiniuCloud.formatFileKey("_____1123++545#e+++?&&f  d  fefe", false));
    }

    @Test
    void testUploadAndMakeUrl() throws Exception {
        if (!QiniuCloud.instance().available()) return;

        File file = ResourceUtils.getFile("classpath:classification-demo.xlsx");
        String uploadKey = QiniuCloud.instance().upload(file);
        System.out.println("upload key ... " + uploadKey);

        String downloadUrl = QiniuCloud.instance().makeUrl(uploadKey);
        System.out.println("download url ... " + downloadUrl);

        File temp = QiniuCloud.getStorageFile(uploadKey);
        System.out.println("downloaded file ... " + temp);

        QiniuCloud.instance().delete(uploadKey);
    }

    @Test
    void makeUrlCache() {
        if (!QiniuCloud.instance().available()) return;

        for (int i = 0; i < 10; i++) {
            System.out.println(i + " = " + QiniuCloud.instance().makeUrl("rb/20190830/170016833__0190815223938.png"));
            ThreadPool.waitFor(300);
        }
    }

    @Test
    void stats() {
        if (!QiniuCloud.instance().available()) return;

        System.out.println(QiniuCloud.instance().stats());
    }

    @Test
    void getStorageFile() throws IOException {
        File file = QiniuCloud.getStorageFile("https://cn.bing.com/th?id=OHR.LowerAntelopeAZ_ZH-CN4758496750_1920x1080.jpg&rf=LaDigue_1920x1080.jpg&pid=hp");
        System.out.println(file);
    }
}
