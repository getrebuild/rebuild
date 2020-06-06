/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.common;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.helper.fieldvalue.BarCodeGenerator;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.AppUtils;
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
        try {
            String entity = getParameterNotNull(request, "entity");
            String field = getParameterNotNull(request, "field");
            Field barcodeField = MetadataHelper.getField(entity, field);
            ID record = getIdParameterNotNull(request, "id");

            File codeImg = BarCodeGenerator.getBarCodeImage(barcodeField, record);

            ServletUtils.setNoCacheHeaders(response);
            FileDownloader.writeLocalFile(codeImg, response);

        } catch (Exception ex) {
            LOG.error("Generate BarCode failed : " + request.getQueryString(), ex);
            response.sendRedirect(AppUtils.getContextPath() + "/assets/img/s.gif");
        }
    }

    @RequestMapping({ "/commons/barcode/render-qr", "/commons/barcode/render" })
    public void render(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String content = getParameter(request, "t", "RB");

        File codeImg;
        if (request.getRequestURI().endsWith("render-qr")) {
            codeImg = BarCodeGenerator.createQRCode(content);
        } else {
            codeImg = BarCodeGenerator.createBarCode(content);
        }

        ServletUtils.addCacheHead(response, 120);
        FileDownloader.writeLocalFile(codeImg, response);
    }
}
