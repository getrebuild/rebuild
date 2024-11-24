/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.google.zxing.BarcodeFormat;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.general.BarCodeSupport;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        String content = getParameter(request, "t", "UNSET");
        int w = getIntParameter(request, "w", 0);

        BufferedImage bi;
        if (request.getRequestURI().endsWith("render-qr")) {
            bi = BarCodeSupport.createQRCode(content, w);
        } else {
            boolean showText = getBoolParameter(request, "b", true);
            String showFormat = getParameter(request, "format");
            BarcodeFormat specFormat = null;
            if (showFormat != null) {
                try {
                    specFormat = BarcodeFormat.valueOf(showFormat);
                } catch (Exception ex) {
                    log.warn("Bad format for BarCode : {}", specFormat);
                }
            }

            if (specFormat != null) {
                bi = BarCodeSupport.createBarCode(content, 0, w, showText, specFormat);
            } else {
                bi = BarCodeSupport.createBarCode(content, w, showText);
            }
        }

        // 6小时缓存
        ServletUtils.addCacheHead(response, 360);
        writeTo(bi, response);
    }

    private void writeTo(BufferedImage image, HttpServletResponse response) throws IOException {
        response.setContentType("image/png");
        ImageIO.write(image, "PNG", response.getOutputStream());
    }
}
