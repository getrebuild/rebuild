/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.hutool.core.io.FileUtil;
import cn.hutool.jwt.JWT;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.rebuild.core.support.ConfigurationItem.OnlyofficeJwt;
import static com.rebuild.core.support.ConfigurationItem.OnlyofficeServer;

/**
 * 文档预览
 *
 * @author devezhao
 * @see FileDownloader
 * @since 04/07/2025
 */
@Slf4j
@Controller
public class FilePreviewer extends BaseController {

    @GetMapping("/filex/preview/**")
    public ModelAndView ooPreview(HttpServletRequest request) {
        final String ooServer = RebuildConfiguration.get(OnlyofficeServer);
        final String ooJwt = RebuildConfiguration.get(OnlyofficeJwt);

        final String filepathRaw = request.getRequestURI().split("/filex/preview/")[1];
        final String filepath = CodecUtils.urlDecode(filepathRaw);
        String[] fs = filepath.split("/");
        String filename = fs[fs.length - 1];

        Map<String, Object> document = new HashMap<>();
        document.put("fileType", FileUtil.getSuffix(filename));
        document.put("key", "key-" + filename.hashCode());
        document.put("title", QiniuCloud.parseFileName(filename));
        // 外部地址
        if (CommonsUtils.isExternalUrl(filepath)) {
            document.put("url", filepath);
        } else {
            String fileUrl = String.format("/filex/download/%s?_csrfToken=%s",
                    filepathRaw,
                    AuthTokenManager.generateCsrfToken(90));
            fileUrl = RebuildConfiguration.getHomeUrl(fileUrl);
            document.put("url", fileUrl);
        }

        // Token
        String token = JWT.create()
                .setPayload("document", document)
                .setExpiresAt(CalendarUtils.add(15, Calendar.MINUTE))
                .setKey(ooJwt.getBytes())
                .sign();

        ModelAndView mv = createModelAndView("/common/oo-preview");
        mv.getModel().put(OnlyofficeServer.name(), ooServer);
        mv.getModel().put("_DocumentConfig", JSON.toJSON(document));
        mv.getModel().put("_Token", token);
        return mv;
    }
}
