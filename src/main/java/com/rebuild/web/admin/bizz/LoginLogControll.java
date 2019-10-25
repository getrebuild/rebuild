/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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
import com.rebuild.utils.LocationUtils;
import com.rebuild.web.BaseEntityControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao-mac zhaofang123@gmail.com
 * @since 2019/02/16
 */
@Controller
@RequestMapping("/admin/bizuser/")
public class LoginLogControll extends BaseEntityControll {

	@RequestMapping("login-logs")
	public ModelAndView pageList(HttpServletRequest request) throws IOException {
		ID user = getRequestUser(request);
		ModelAndView mv = createModelAndView("/admin/bizuser/login-logs.jsp", "LoginLog", user);
		JSON config = DataListManager.instance.getFieldsLayout("LoginLog", user);
		mv.getModel().put("DataListConfig", JSON.toJSONString(config));
		return mv;
	}

	@RequestMapping("ip-location")
	public void getIpLocation(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String ip = getParameterNotNull(request, "ip");
		JSON location = LocationUtils.getLocation(ip);
		writeSuccess(response, location);
	}
}
