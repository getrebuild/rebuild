/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Field;
import com.google.zxing.BarcodeFormat;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;

/**
 * @author devezhao
 * @since 2020/6/5
 */
public class BarCodeSupportTest extends TestSupport {

    @Test
    void getBarCodeContent() {
        Field barcodeField = MetadataHelper.getEntity(TestAllFields).getField("barcode");
        System.out.println(BarCodeSupport.getBarCodeContent(barcodeField, null));
    }

    @Test
    void saveBarCode() {
        System.out.println(BarCodeSupport.saveCode(BarCodeSupport.createBarCodeImage("123ABC支持中文", BarcodeFormat.QR_CODE, 0, 0)));
        System.out.println(BarCodeSupport.saveCode(BarCodeSupport.createBarCodeImage("CODE128", BarcodeFormat.CODE_128, 0, 0)));
    }

    @Test
    void decode() throws Exception {
        File qrcode = ResourceUtils.getFile("classpath:qrcode_getrebuild.com.png").getAbsoluteFile();
        System.out.println(BarCodeSupport.decode(qrcode));

        File barcode = ResourceUtils.getFile("classpath:barcode.png").getAbsoluteFile();
        System.out.println(BarCodeSupport.decode(barcode));
    }
}