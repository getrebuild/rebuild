/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.general.BarCodeSupport;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * BARCODE QRCODE
 *
 * @author devezhao
 * @since 2020/6/5
 */
@Controller
public class BarCodeGeneratorController extends BaseController {

    @GetMapping("/commons/barcode/generate")
    public void generateAndRender(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String entity = getParameterNotNull(request, "entity");
        String field = getParameterNotNull(request, "field");
        if (!MetadataHelper.checkAndWarnField(entity, field)) {
            response.sendRedirect(AppUtils.getContextPath("/assets/img/s.gif"));
            return;
        }

        Field barcodeField = MetadataHelper.getField(entity, field);
        ID record = getIdParameterNotNull(request, "id");

        ServletUtils.setNoCacheHeaders(response);
        writeTo(BarCodeSupport.getBarCodeImage(barcodeField, record), response);
    }

    @GetMapping({"/commons/barcode/render-qr", "/commons/barcode/render"})
    public void render(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String content = getParameterNotNull(request, "t");

        // 4小时缓存
        ServletUtils.addCacheHead(response, 240);

        if (request.getRequestURI().endsWith("render-qr")) {
            writeTo(BarCodeSupport.createQRCode(content), response);
        } else {
            writeTo(BarCodeSupport.createBarCode(content), response);
        }
    }

    private void writeTo(BufferedImage image, HttpServletResponse response) throws IOException {
        response.setContentType("image/png");
        ImageIO.write(image, "PNG", response.getOutputStream());
    }
}
