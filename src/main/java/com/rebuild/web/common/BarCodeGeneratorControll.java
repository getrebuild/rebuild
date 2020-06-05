/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.common;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.helper.fieldvalue.BarCodeGenerator;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * BARCODE QRCODE
 *
 * @author devezhao
 * @since 2020/6/5
 */
@Controller
public class BarCodeGeneratorControll extends BaseControll {

    @RequestMapping("/commons/barcode/generate")
    public void generate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Field barcodeField = getFieldOfBarCode(request);
        ID record = getIdParameterNotNull(request, "id");

        File codeImg = BarCodeGenerator.getBarCodeImage(barcodeField, record);
        FileDownloader.writeLocalFile(codeImg, response);
    }

    @RequestMapping("/commons/barcode/contents")
    public void contents(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Field barcodeField = getFieldOfBarCode(request);
        ID record = getIdParameterNotNull(request, "id");

        String codeContents = BarCodeGenerator.getBarCodeContent(barcodeField, record);
        writeSuccess(response, codeContents);
    }

    @RequestMapping({ "/commons/barcode/render-qr", "/commons/barcode/render-bar" })
    public void render(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String content = getParameter(request, "t", "RB");

        File codeImg;
        if (request.getRequestURI().endsWith("render-qr")) {
            codeImg = BarCodeGenerator.createQRCode(content);
        } else {
            codeImg = BarCodeGenerator.createBarCode(content);
        }
        FileDownloader.writeLocalFile(codeImg, response);
    }

    private Field getFieldOfBarCode(HttpServletRequest request) {
        String entity = getParameterNotNull(request, "entity");
        String field = getParameterNotNull(request, "field");
        return MetadataHelper.getField(entity, field);
    }
}
