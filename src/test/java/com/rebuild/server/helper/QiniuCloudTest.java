/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.helper;

import cn.devezhao.commons.ThreadPool;
import com.rebuild.server.TestSupport;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/08
 */
public class QiniuCloudTest extends TestSupport {

    @Test
    public void testUploadAndMakeUrl() throws Exception {
        URL fileUrl = QiniuCloudTest.class.getClassLoader().getResource("approval-flow.json");
        File file = new File(fileUrl.toURI());
        String uploadKey = QiniuCloud.instance().upload(file);
        System.out.println("uploadKey ... " + uploadKey);

        String downloadUrl = QiniuCloud.instance().url(uploadKey);
        System.out.println("downloadUrl ... " + downloadUrl);

        QiniuCloud.instance().delete(uploadKey);
    }

    @Test
    public void testFormatKey() throws Exception {
        String fileName = "imtestfile.txt";
        String fileKey = QiniuCloud.formatFileKey(fileName);
        System.out.println("File key ... " + fileKey);
        String fileName2 = QiniuCloud.parseFileName(fileKey);
        assertEquals(fileName, fileName2);

        System.out.println("File2 key ... " + QiniuCloud.formatFileKey("_____1123++545#e+++?&&f  d  fefe.txt"));
        System.out.println("File3 key ... " + QiniuCloud.formatFileKey("_____1123++545#e+++?&&f  d  fefe.txt", false));
        System.out.println("File4 key ... " + QiniuCloud.formatFileKey("_____1123++545#e+++?&&f  d  fefe", false));
    }

    @Test
    public void testPrivateUrl() {
        for (int i = 0; i < 20; i++) {
            System.out.println(i + " = " + QiniuCloud.instance().url("rb/20190830/170016833__0190815223938.png"));
            ThreadPool.waitFor(300);
        }
    }
}
