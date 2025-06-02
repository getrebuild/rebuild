/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.OnlyOffice;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

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

    // https://api.onlyoffice.com/docs/docs-api/usage-api/config/
    @GetMapping("/commons/file-preview")
    public ModelAndView ooPreview(HttpServletRequest request) {
        final String src = getParameterNotNull(request, "src");
        final String mode = getParameter(request, "mode", "view");  // edit
        Object[] ps = OnlyOffice.buildPreviewParams(src);

        JSONObject ooConfig = new JSONObject();
        ooConfig.put("document", ps[0]);
        ooConfig.put("token", ps[1]);

        JSONObject editorConfig = JSONUtils.toJSONObject(
                new String[]{"mode", "lang", "toolbar", "menu"},
                new Object[]{"view", "zh", false, false});
        String[] user = new String[]{"REBUILD", "REBUILD"};
        ID userid = AppUtils.getRequestUser(request);
        if (userid != null) {
            user = new String[]{userid.toString(), UserHelper.getName(userid)};
        }
        editorConfig.put("user", JSONUtils.toJSONObject(new String[]{"id", "name"}, user));

        ModelAndView mv = createModelAndView("/common/oo-preview");
        mv.getModel().put(OnlyofficeServer.name(), OnlyOffice.getOoServer());

        // 编辑模式
        if ("edit".equals(mode)) {
            String callbackUrl = RebuildConfiguration.getHomeUrl("/commons/file-preview-forcesave");
            String fileKey = "/rb/" + src.split("/rb/")[1].split("\\?")[0];
            callbackUrl += "?fileKey=" + CodecUtils.urlEncode(fileKey);
            callbackUrl += "&_csrfToken=" + AuthTokenManager.generateCsrfToken(2 * 60 * 60);
            editorConfig.put("callbackUrl", callbackUrl);

            editorConfig.put("mode", "edit");
            editorConfig.put("toolbar", true);
            editorConfig.put("menu", true);
            ooConfig.put("type", "desktop");
            mv.getModel().put("title", Language.L("文档编辑"));
        } else {
            ooConfig.put("type", "embedded");
            mv.getModel().put("title", Language.L("文档预览"));
        }

        ooConfig.put("editorConfig", editorConfig);
        mv.getModel().put("_DocEditorConfig", ooConfig);
        return mv;
    }

    // https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/
    @RequestMapping("/commons/file-preview-forcesave")
    public void ooPreviewForcesave(HttpServletRequest request, HttpServletResponse response) {
        int status = getIntParameter(request, "status");
        // saving
        if (status == 2) {
            CommonsMultipartResolver resolver = new CommonsMultipartResolver(request.getServletContext());
            MultipartFile file = null;
            MultipartHttpServletRequest mp = resolver.resolveMultipart(request);
            for (MultipartFile t : mp.getFileMap().values()) {
                file = t;
                break;
            }
            if (file == null) {
                ServletUtils.writeJson(response, "{'error':'No file found'}");
                return;
            }

            String fileKey = getParameter(request, "fileKey");
            File dest;
            if (QiniuCloud.instance().available()) {
                dest = RebuildConfiguration.getFileOfTemp("oo-tmpfile-" + CommonsUtils.randomHex(true));
            } else {
                dest = RebuildConfiguration.getFileOfData(fileKey);
            }

            try {
                file.transferTo(dest);

                if (QiniuCloud.instance().available()) {
                    QiniuCloud.instance().upload(dest, fileKey);
                }
            } catch (Exception e) {
                log.error("Saving file error : {}", fileKey, e);
                ServletUtils.writeJson(response, "{'error':'Saving file error'}");
                return;
            }
        }

        // echo
        ServletUtils.writeJson(response, JSONUtils.toJSONObject("error", 0).toJSONString());
    }
}
