/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.CodecUtils;
import com.rebuild.core.Application;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.QiniuCloud;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 文件共享
 *
 * @author ZHAO
 * @since 2019/9/26
 */
@Controller
public class FileShareControl extends BaseController {

    // URL of public
    @RequestMapping("/filex/make-url")
    public void makeUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fileUrl = getParameterNotNull(request, "url");
        String publicUrl = genPublicUrl(fileUrl);
        writeSuccess(response, JSONUtils.toJSONObject("publicUrl", publicUrl));
    }

    // URL of share
    @RequestMapping("/filex/make-share")
    public void makeShareUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Assert.isTrue(RebuildConfiguration.getBool(ConfigurationItem.FileSharable), "不允许分享文件");

        String fileUrl = getParameterNotNull(request, "url");
        int minte = getIntParameter(request, "time", 5);

        String shareKey = CodecUtils.randomCode(40);
        Application.getCommonsCache().put(shareKey, fileUrl, minte * 60);

        String shareUrl = RebuildConfiguration.getHomeUrl("s/" + shareKey);
        writeSuccess(response, JSONUtils.toJSONObject("shareUrl", shareUrl));
    }

    @RequestMapping("/s/{shareKey}")
    public ModelAndView makeShareUrl(@PathVariable String shareKey,
                                     HttpServletResponse response) throws IOException {
        String fileUrl;
        if (!RebuildConfiguration.getBool(ConfigurationItem.FileSharable)
                || (fileUrl = Application.getCommonsCache().get(shareKey)) == null) {
            response.sendError(403, "分享的文件已过期");
            return null;
        }

        String publicUrl = genPublicUrl(fileUrl);
        ModelAndView mv = createModelAndView("/commons/shared-file");
        mv.getModelMap().put("publicUrl", publicUrl);
        return mv;
    }

    /**
     * @param fileUrl
     * @return
     * @see FileDownloader#download(HttpServletRequest, HttpServletResponse)
     */
    private String genPublicUrl(String fileUrl) {
        String publicUrl;
        if (QiniuCloud.instance().available()) {
            publicUrl = QiniuCloud.instance().url(fileUrl, 120);
        } else {
            // @see FileDownloader#download
            String e = CodecUtils.randomCode(40);
            Application.getCommonsCache().put(e, "rb", 120);

            publicUrl = "filex/access/" + fileUrl + "?e=" + e;
            publicUrl = RebuildConfiguration.getHomeUrl(publicUrl);
        }
        return publicUrl;
    }
}
