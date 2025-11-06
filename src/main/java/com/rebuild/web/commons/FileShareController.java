/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.dashboard.ChartManager;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.ShortUrls;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import org.apache.commons.io.FileUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 文件共享
 *
 * @author ZHAO
 * @since 2019/9/26
 */
@RestController
public class FileShareController extends BaseController {

    // URL for public
    @GetMapping("/filex/make-url")
    public JSON makeUrl(HttpServletRequest request) {
        String fileUrl = getParameterNotNull(request, "url");
        return JSONUtils.toJSONObject("publicUrl", makePublicUrl(fileUrl));
    }

    // URL for share
    @GetMapping("/filex/make-share")
    public JSON makeShareFile(HttpServletRequest request) {
        Assert.isTrue(
                RebuildConfiguration.getBool(ConfigurationItem.FileSharable),
                Language.L("不允许分享文件"));

        String shareUrl4del = getParameter(request, "shareUrl");
        if (shareUrl4del != null && shareUrl4del.contains("/s/")) ShortUrls.invalid(shareUrl4del.split("/s/")[1]);

        String fileUrl = getParameterNotNull(request, "url");
        int time = getIntParameter(request, "time", 5);
        String shareKey = ShortUrls.make(fileUrl, time * 60, getRequestUser(request));

        String shareUrl = RebuildConfiguration.getHomeUrl("s/" + shareKey);
        return JSONUtils.toJSONObject("shareUrl", shareUrl);
    }

    @GetMapping("/s/{shareKey}")
    public ModelAndView viewSharedFile(@PathVariable String shareKey,
                                       HttpServletRequest request,
                                       HttpServletResponse response) throws IOException {
        if (!RebuildConfiguration.getBool(ConfigurationItem.FileSharable)) {
            response.sendError(403, Language.L("系统不允许分享"));
            return null;
        }

        String fileUrl = ShortUrls.retrieveUrl(shareKey);
        if (fileUrl == null) {
            response.sendError(403, Language.L("分享已过期"));
            return null;
        }

        // v4.2
        final ID folderOrDash42 = ID.isId(fileUrl) ? ID.valueOf(fileUrl) : null;
        // 分享仪表盘
        if (folderOrDash42 != null && folderOrDash42.getEntityCode() == EntityHelper.DashboardConfig) {
            String dashName = FieldValueHelper.getLabelNotry(folderOrDash42);
            if (Objects.equals(dashName, FieldValueHelper.MISS_REF_PLACE)) {
                response.sendError(403, Language.L("分享不存在"));
                return null;
            }

            Object[] o = Application.getQueryFactory().uniqueNoFilter(folderOrDash42, "config");
            JSONArray dashConfig = JSON.parseArray(o[0].toString());
            ChartManager.instance.richingCharts(dashConfig, UserService.SYSTEM_USER);

            Map<String, Object> map = new HashMap<>();
            map.put("dashName", dashName);
            map.put("dashId", folderOrDash42);
            map.put("dashConfig", dashConfig);
            map.put("shareKey", shareKey);
            map.put("csrfToken", "sk:" + shareKey);
            return createModelAndView("/common/shared-dash", map);

        }
        // 分享目录
        else if (folderOrDash42 != null && folderOrDash42.getEntityCode() == EntityHelper.AttachmentFolder) {
            String viewFile = getParameter(request, "file");
            // 查看目录内文件
            if (ID.isId(viewFile)) {
                fileUrl = (String) QueryHelper.queryFieldValue(ID.valueOf(viewFile), "filePath");
            } else {
                String folderName = FieldValueHelper.getLabelNotry(folderOrDash42);
                if (Objects.equals(folderName, FieldValueHelper.MISS_REF_PLACE)) {
                    response.sendError(403, Language.L("分享不存在"));
                    return null;
                }

                Map<String, Object> map = new HashMap<>();
                map.put("folderName", folderName);

                Object[][] array = Application.createQueryNoFilter(
                        "select attachmentId,fileName,fileSize,fileType from Attachment where inFolder = ? and isDeleted <> 'T' order by fileName")
                        .setParameter(1, folderOrDash42)
                        .array();
                List<String[]> files = new ArrayList<>();
                for (Object[] o : array) {
                    files.add(new String[]{
                            shareKey + "?file=" + o[0],
                            (String) o[1],
                            FileUtils.byteCountToDisplaySize(ObjectUtils.toLong(o[2])),
                            (String) o[3]
                    });
                }
                map.put("folderFiles", files);

                return createModelAndView("/common/shared-folder", map);
            }
        }

        // v4.2 检查文件是否存在
        Object[] e = Application.createQueryNoFilter(
                "select attachmentId from Attachment where isDeleted <> 'T' and filePath = ?")
                .setParameter(1, fileUrl).unique();
        if (e == null) {
            response.sendError(403, Language.L("分享的文件不存在"));
            return null;
        }

        String publicUrl = makePublicUrl(fileUrl);
        return createModelAndView("/common/shared-file", Collections.singletonMap("publicUrl", publicUrl));
    }

    @GetMapping("/filex/all-make-share")
    public RespBody allSharedFiles(HttpServletRequest request) {
        String sql = "select shortKey,longUrl,expireTime,createdOn,createdBy,shortId" +
                " from ShortUrl where (expireTime > ? or expireTime is null) and (1=1) order by createdOn desc";

        // 管理员可见全部
        final ID user = getRequestUser(request);
        if (!UserHelper.isAdmin(user)) {
            sql = sql.replace("(1=1)", "createdBy = '" + user + "'");
        }

        Object[][] array = Application.createQueryNoFilter(sql)
                .setParameter(1, CalendarUtils.now())
                .setLimit(1000)
                .array();
        for (Object[] o : array) {
            o[0] = RebuildConfiguration.getHomeUrl("s/" + o[0]);
            o[4] = UserHelper.getName((ID) o[4]);

            final ID folderOrDash42 = ID.isId(o[1]) ? ID.valueOf((String) o[1]) : null;
            if (folderOrDash42 != null
                    && (folderOrDash42.getEntityCode() == EntityHelper.AttachmentFolder || folderOrDash42.getEntityCode() == EntityHelper.DashboardConfig)) {
                o[1] = o[1] + "/" + FieldValueHelper.getLabel(ID.valueOf((String) o[1]));
            }
        }

        return RespBody.ok(array);
    }

    @PostMapping("/filex/del-make-share")
    public RespBody delSharedFile(@IdParam ID shortId) {
        Application.getCommonsService().delete(shortId, false);
        return RespBody.ok();
    }

    @PostMapping("/filex/is-make-share")
    public RespBody isSharedFile(HttpServletRequest request) {
        final String longUrl = getParameterNotNull(request, "longUrl");

        Object[] has = Application.createQueryNoFilter(
                "select shortKey from from ShortUrl where longUrl = ? and expireTime > ?")
                .setParameter(1, longUrl)
                .setParameter(2, CalendarUtils.now())
                .unique();
        return RespBody.ok(has == null ? null : has[0]);
    }

    /**
     * @see FileDownloader#download(HttpServletRequest, HttpServletResponse)
     */
    private String makePublicUrl(String fileUrl) {
        String publicUrl;
        if (QiniuCloud.instance().available()) {
            publicUrl = QiniuCloud.instance().makeUrl(fileUrl, 15 * 60);
        } else {
            String e = CodecUtils.randomCode(40);
            Application.getCommonsCache().put(e, "rb", 5 * 60);

            publicUrl = "filex/access/" + fileUrl + "?e=" + e;
            publicUrl = RebuildConfiguration.getHomeUrl(publicUrl);
        }
        return publicUrl;
    }
}
