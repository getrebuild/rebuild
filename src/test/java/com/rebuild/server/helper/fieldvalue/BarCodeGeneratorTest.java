/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper.fieldvalue;

import cn.devezhao.persist4j.Field;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;
import org.junit.Test;

/**
 * @author devezhao
 * @since 2020/6/5
 */
public class BarCodeGeneratorTest extends TestSupport {

    @Test
    public void getBarCodeContent() {
        Field barcodeField = MetadataHelper.getEntity(TEST_ENTITY).getField("barcode");
        System.out.println(BarCodeGenerator.getBarCodeContent(barcodeField, null));
    }

    @Test
    public void createBarCode() {
        System.out.println(BarCodeGenerator.createBarCode("CODE128"));
        System.out.println(BarCodeGenerator.createQRCode("123ABC支持中文"));
    }
}