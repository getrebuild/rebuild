/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.web.common;

import cn.devezhao.commons.CodecUtils;
import com.rebuild.server.Application;
import com.rebuild.server.helper.QiniuCloud;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
import org.springframework.stereotype.Controller;
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
public class FileShare extends BasePageControll {

    // Make public url
    @RequestMapping("/filex/make-url")
    public void makeUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fileUrl = getParameterNotNull(request, "url");
        if (!QiniuCloud.instance().available()) {
            writeFailure(response, "本地存储暂不支持");
            return;
        }

        String publicUrl = QiniuCloud.instance().url(fileUrl);
        writeSuccess(response, JSONUtils.toJSONObject("publicUrl", publicUrl));
    }

    // Make shared URL
    @RequestMapping("/filex/make-share")
    public void makeShareUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fileUrl = getParameterNotNull(request, "url");
        if (!QiniuCloud.instance().available()) {
            writeFailure(response, "本地存储暂不支持");
            return;
        }

        int minte = getIntParameter(request, "time", 5);
        String shareKey = CodecUtils.randomCode(40);
        Application.getCommonCache().put(shareKey, fileUrl, minte * 60);

        String shareUrl = SysConfiguration.getHomeUrl("s/" + shareKey);
        writeSuccess(response, JSONUtils.toJSONObject("shareUrl", shareUrl));
    }

    @RequestMapping("/s/{shareKey}")
    public ModelAndView makeShareUrl(@PathVariable String shareKey,
                                     HttpServletResponse response) throws IOException {
        String fileUrl = Application.getCommonCache().get(shareKey);
        if (fileUrl == null) {
            response.sendError(403, "文件已过期");
            return null;
        }

        String publicUrl = QiniuCloud.instance().url(fileUrl, 60);
        ModelAndView mv = createModelAndView("/commons/shared-file.jsp");
        mv.getModelMap().put("publicUrl", publicUrl);
        return mv;
    }
}
