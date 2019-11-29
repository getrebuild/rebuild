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
import cn.devezhao.commons.web.ServletUtils;
import com.rebuild.server.helper.language.LanguageBundle;
import com.rebuild.server.helper.language.Languages;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.BaseControll;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.rebuild.utils.AppUtils.SK_LOCALE;

/**
 * 语言控制
 *
 * @author devezhao
 * @since 2019/11/29
 */
@Controller
@RequestMapping("/language/")
public class LanguagesControll extends BaseControll {

    @RequestMapping(value = "bundle", method = RequestMethod.GET)
    public void getLanguageBundle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final LanguageBundle bundle = getBundle(request);
        // HTTP Headers
        response.addHeader("Cache-Control", "max-age=60, must-revalidate");
        response.addHeader("ETag", "W/" + bundle.getBundleHash());
        response.setContentType(ServletUtils.CT_JS);

        ServletUtils.write(response, "__LANGBUNDLE__ = " + bundle.toJSON().toJSONString());
    }

    @RequestMapping("select")
    public void selectLanguage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String locale = request.getParameter("locale");
        if (locale != null && Languages.instance.isAvailable(locale)) {
            if (AppUtils.devMode()) Languages.instance.reset();
            ServletUtils.setSessionAttribute(request, SK_LOCALE, locale);
        }

        if (ServletUtils.isAjaxRequest(request)) {
            writeSuccess(response);
        } else {
            String nexturl = StringUtils.defaultIfBlank(request.getParameter("nexturl"), AppUtils.getContextPath());
            response.sendRedirect(CodecUtils.urlDecode(nexturl));
        }
    }
}
