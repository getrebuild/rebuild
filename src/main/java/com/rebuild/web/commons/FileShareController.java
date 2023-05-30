/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.ShortUrls;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

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
    public JSON makeSharedFile(HttpServletRequest request) {
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
                                       HttpServletResponse response) throws IOException {
        if (!RebuildConfiguration.getBool(ConfigurationItem.FileSharable)) {
            response.sendError(403, Language.L("不允许分享文件"));
            return null;
        }

        String fileUrl = ShortUrls.retrieveUrl(shareKey);
        if (fileUrl == null) {
            response.sendError(403, Language.L("分享的文件已过期"));
            return null;
        }

        String publicUrl = makePublicUrl(fileUrl);
        return createModelAndView("/common/shared-file", Collections.singletonMap("publicUrl", publicUrl));
    }

    @GetMapping("/filex/all-make-share")
    public RespBody getAllShareFiles(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        String sql = "select shortKey,longUrl,expireTime,createdOn,createdBy,shortId" +
                " from ShortUrl where expireTime > ? and createdBy = ? order by createdOn desc";

        Object[][] array = Application.createQueryNoFilter(sql)
                .setParameter(1, CalendarUtils.now())
                .setParameter(2, user)
                .setLimit(1000)
                .array();
        for (Object[] o : array) {
            o[4] = UserHelper.getName((ID) o[4]);
        }

        return RespBody.ok(array);
    }

    @PostMapping("/filex/del-make-share")
    public RespBody delShareFile(@IdParam ID shortId) {
        Application.getCommonsService().delete(shortId, false);
        return RespBody.ok();
    }

    /**
     * @see FileDownloader#download(HttpServletRequest, HttpServletResponse)
     */
    private String makePublicUrl(String fileUrl) {
        String publicUrl;
        if (QiniuCloud.instance().available()) {
            publicUrl = QiniuCloud.instance().makeUrl(fileUrl, 5 * 60);
        } else {
            String e = CodecUtils.randomCode(40);
            Application.getCommonsCache().put(e, "rb", 5 * 60);

            publicUrl = "filex/access/" + fileUrl + "?e=" + e;
            publicUrl = RebuildConfiguration.getHomeUrl(publicUrl);
        }
        return publicUrl;
    }
}
