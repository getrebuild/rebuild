/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.files.FilesHelper;
import com.rebuild.core.support.OnlyOffice;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.general.RecordBuilder;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.OkHttpUtils;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

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

    @GetMapping("/commons/file-preview")
    public ModelAndView ooPreview(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return ooPreviewOrEditor(request, response, false);
    }

    @GetMapping("/commons/file-editor")
    public ModelAndView ooEditor(HttpServletRequest request, HttpServletResponse response) throws IOException {
        getRequestUser(request);  // check
        return ooPreviewOrEditor(request, response, true);
    }

    // 预览或编辑
    private ModelAndView ooPreviewOrEditor(HttpServletRequest request, HttpServletResponse response, boolean editor) throws IOException {
        String src = getParameterNotNull(request, "src");
        ID id = null;
        if (ID.isId(src)) {
            id = ID.valueOf(src);
            src = getFileById(id, getRequestUser(request));
        }

        // v4.0 兼容
        if (!OnlyOffice.isUseOoPreview()) {
            String fileUrl = src;
            if (!CommonsUtils.isExternalUrl(fileUrl)) {
                boolean temp = fileUrl.startsWith("/temp/");
                boolean data = fileUrl.startsWith("/data/");
                if (temp || data) fileUrl = fileUrl.substring(6);

                fileUrl = String.format("/filex/download/%s?temp=%s&data=%s&_onceToken=%s",
                        fileUrl, temp, data, AuthTokenManager.generateOnceToken(null));
                fileUrl = RebuildConfiguration.getHomeUrl(fileUrl);
            }

            String previewUrl = OnlyOffice.getBestPreviewUrl();
            previewUrl += CodecUtils.urlEncode(fileUrl);
            response.sendRedirect(previewUrl);
            return null;
        }

        // https://api.onlyoffice.com/docs/docs-api/usage-api/config/
        JSONObject editorConfig = JSONUtils.toJSONObject(
                new String[]{"mode", "lang", "toolbar", "menu"},
                new Object[]{"view", AppUtils.getReuqestLocale(request), false, false});
        String[] user = new String[]{"REBUILD", "REBUILD"};
        ID userid = AppUtils.getRequestUser(request);
        if (userid != null) {
            user = new String[]{userid.toString(), UserHelper.getName(userid)};
        }
        editorConfig.put("user", JSONUtils.toJSONObject(new String[]{"id", "name"}, user));

        JSONObject customization = new JSONObject();
        customization.put("uiTheme", "theme-dark");
        customization.put("about", false);
        customization.put("logo", JSONUtils.toJSONObject("visible", false));

        // 编辑模式
        if (editor) {
            String callbackUrl = RebuildConfiguration.getHomeUrl("/commons/file-editor-forcesave");
            String fileKey = src.split("\\?")[0];
            callbackUrl += "?fileKey=" + CodecUtils.urlEncode(fileKey);
            callbackUrl += "&_csrfToken=" + AuthTokenManager.generateCsrfToken(CommonsCache.TS_HOUR * 12);
            if (id != null) callbackUrl += "&id=" + id;

            editorConfig.put("callbackUrl", callbackUrl);
            editorConfig.put("mode", "edit");
            editorConfig.put("toolbar", true);
            editorConfig.put("menu", true);
            customization.put("forcesave", true);
        }
        editorConfig.put("customization", customization);

        Object[] ps = OnlyOffice.buildPreviewParams(src, editorConfig);

        JSONObject ooConfig = new JSONObject();
        ooConfig.put("document", ps[0]);
        ooConfig.put("token", ps[1]);
        ooConfig.put("editorConfig", editorConfig);

        ModelAndView mv = createModelAndView("/common/oo-preview");
        mv.getModel().put(OnlyofficeServer.name(), OnlyOffice.getOoServer());

        String fileName = ((JSONObject) ps[0]).getString("title");

        // 编辑模式
        if (editor) {
            ooConfig.put("type", "desktop");
            mv.getModel().put("title", fileName + " - " + Language.L("文档编辑"));
        } else {
            // https://api.onlyoffice.com/docs/docs-api/usage-api/config/#type
            String view = StringUtils.defaultIfBlank(getParameter(request, "view"), "embedded");
            ooConfig.put("type", view);
            mv.getModel().put("title", fileName + " - " + Language.L("文档预览"));
        }
        if (Application.devMode()) System.out.println("[dev] " + JSONUtils.prettyPrint(ooConfig));
        mv.getModel().put("_DocEditorConfig", ooConfig);
        return mv;
    }

    // https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/
    @PostMapping("/commons/file-editor-forcesave")
    public void ooEditorForcesave(HttpServletRequest request, HttpServletResponse response) {
        final JSONObject status = (JSONObject) ServletUtils.getRequestJson(request);
        if (Application.devMode()) {
            System.out.println("[dev] oo-callback : " + request.getQueryString() + "\n" + JSONUtils.prettyPrint(status));
        }

        // saving
        int statusVal = status.getIntValue("status");
        if (statusVal == 2 || statusVal == 6) {
            // 授权检测
            String csrfToken = getParameter(request, "_csrfToken");
            if (AuthTokenManager.verifyToken(csrfToken) == null) {
                ServletUtils.writeJson(response, "{\"error\":\"UNAUTHORIZED ACCESS\"}");
                return;
            }

            String fileKey = getParameter(request, "fileKey");
            boolean data41 = fileKey.startsWith("/data/");
            if (data41) fileKey = fileKey.substring(6);

            String hasId = getParameter(request, "id");
            if (ID.isId(hasId)) {
                String[] fileKey_s = fileKey.split("/");
                String fileName = QiniuCloud.parseFileName(fileKey);
                fileName = String.format("%s__%s",
                        CalendarUtils.getDateFormat("HHmmssSSS").format(CalendarUtils.now()), fileName);
                fileKey_s[fileKey_s.length - 1] = fileName;
                fileKey = StringUtils.join(fileKey_s, "/");
            }

            File dest;
            if (QiniuCloud.instance().available() && !data41) {
                dest = RebuildConfiguration.getFileOfTemp("oo-tmpfile-" + CommonsUtils.randomHex());
            } else {
                dest = RebuildConfiguration.getFileOfData(fileKey);
            }

            String changedUrl = status.getString("url");
            try {
                OkHttpUtils.readBinary(changedUrl, dest, null);

                if (QiniuCloud.instance().available() && !data41) {
                    QiniuCloud.instance().upload(dest, fileKey);
                    FileUtils.deleteQuietly(dest);
                }

                saveFileById(fileKey, hasId, status.getJSONArray("users"));

            } catch (Exception e) {
                log.error("Saving file error : {}", fileKey, e);
                ServletUtils.writeJson(response, "{\"error\":\"SAVING FILE ERROR\"}");
                return;
            }
        }

        // echo
        ServletUtils.writeJson(response, "{\"error\":0}");
    }

    /**
     * @param fileKey
     * @param id
     * @param users
     */
    static void saveFileById(String fileKey, String id, JSONArray users) {
        if (!ID.isId(id)) return;

        ID user = UserService.SYSTEM_USER;
        if (!CollectionUtils.isEmpty(users)) {
            if (ID.isId(users.getString(0))) {
                user = ID.valueOf(users.getString(0));
            }
        }

        ID fid = ID.valueOf(id);
        Record record = null;
        if (fid.getEntityCode() == EntityHelper.DataReportConfig) {
            record = RecordBuilder.builder(fid)
                    .add("templateFile", fileKey)
                    .build(user);
        } else if (fid.getEntityCode() == EntityHelper.Attachment) {
            record = RecordBuilder.builder(fid)
                    .add("filePath", fileKey)
                    .build(user);
        }
        if (record == null) return;

        ID o = UserContextHolder.setUser(user);
        try {
            Application.getService(fid.getEntityCode()).update(record);
        } finally {
            UserContextHolder.clearUser(o);
        }
    }

    /**
     * @param id
     * @param user
     * @return
     */
    static String getFileById(ID id, ID user) {
        Object[] e = null;
        // 报表
        if (id.getEntityCode() == EntityHelper.DataReportConfig) {
            RbAssert.is(UserHelper.isAdmin(user), "NOT ALLOWED");
            e = Application.getQueryFactory().uniqueNoFilter(id, "templateFile");
            if (e != null && e[0] != null) e[0] = "/data/" + e[0];
        }
        // 文件
        if (id.getEntityCode() == EntityHelper.Attachment) {
            RbAssert.is(FilesHelper.isFileManageable(user, id), "NOT ALLOWED");
            e = Application.getQueryFactory().uniqueNoFilter(id, "filePath");
        }

        RbAssert.is(e != null && e[0] != null, "FILE NOT FOUND:" + id);
        return (String) e[0];
    }
}
