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

import cn.devezhao.commons.web.ServletUtils;
import com.rebuild.server.helper.language.Languages;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author ZHAO
 * @since 2019/10/31
 */
@Controller
public class LanguageControll extends BaseControll {

    @RequestMapping(value = "language/bundle", method = RequestMethod.GET)
    public void getLanguageBundle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String bundle = Languages.instance.getDefaultBundle().toJSON().toJSONString();
        response.setContentType(ServletUtils.CT_JS);
        ServletUtils.write(response, "__LANG__ = " + bundle);
    }
}
