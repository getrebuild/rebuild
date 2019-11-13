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

package com.rebuild.web.admin.bizz;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.configuration.portals.DataListManager;
import com.rebuild.web.BaseEntityControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * TODO
 *
 * @author devezhao
 * @since 2019/11/13
 */
@Controller
@RequestMapping("/admin/bizuser/")
public class TeamControll extends BaseEntityControll {

    @RequestMapping("teams")
    public ModelAndView pageList(HttpServletRequest request) throws IOException {
        ID user = getRequestUser(request);
        ModelAndView mv = createModelAndView("/admin/bizuser/team-list.jsp", "Team", user);
        JSON config = DataListManager.instance.getFieldsLayout("Team", user);
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));
        return mv;
    }
}
