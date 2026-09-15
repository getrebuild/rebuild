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
import com.rebuild.api.RespBody;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.general.BarCodeSupport;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * BARCODE QRCODE
 *
 * @author devezhao
 * @since 2020/6/5
 */
@Slf4j
@RestController
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

        BufferedImage bi = BarCodeSupport.getBarCodeImage(barcodeField, record);
        if (bi == null) {
            response.sendRedirect(AppUtils.getContextPath("/assets/img/s.gif"));
        } else {
            ServletUtils.setNoCacheHeaders(response);
            writeTo(bi, response);
        }
    }

    @GetMapping({"/commons/barcode/render-qr", "/commons/barcode/render", "/commons/barcode/render-auto"})
    public void render(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String content = getParameter(request, "t", "UNSET");
        int w = getIntParameter(request, "w");

        // v4.2 根据前缀渲染 QR: BC:
        boolean renderQr = request.getRequestURI().endsWith("render-qr");
        if (request.getRequestURI().endsWith("render-auto")) {
            renderQr = content.startsWith("QR:");
            content = content.substring(3);
        }

        BufferedImage bi;
        if (renderQr) {
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

        // 24小时缓存
        ServletUtils.addCacheHead(response, 60 * 24);
        writeTo(bi, response);
    }

    private void writeTo(BufferedImage image, HttpServletResponse response) throws IOException {
        response.setContentType("image/png");
        ImageIO.write(image, "PNG", response.getOutputStream());
    }

    @PostMapping("/commons/barcode/decode")
    public RespBody decode(HttpServletRequest request) throws IOException {
        String data = ServletUtils.getRequestString(request);
        // 去掉 data:image/...;base64, 前缀
        if (data != null && data.contains("base64,")) {
            data = data.substring(data.indexOf("base64,") + 7);
        }
        if (StringUtils.isBlank(data)) {
            return RespBody.errorl("无效的图片数据");
        }

        byte[] bytes = Base64.decodeBase64(data);
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
        if (bufferedImage == null) {
            return RespBody.errorl("无法解析图片");
        }

        String result = BarCodeSupport.decode(bufferedImage);
        if (result == null) {
            return RespBody.errorl("无法识别");
        }
        return RespBody.ok(result);
    }
}
