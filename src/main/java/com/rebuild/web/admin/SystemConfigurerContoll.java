/*
rebuild - Building your system freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.rebuild.web.BaseControll;

/**
 * 系统配置
 * 
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
@Controller
@RequestMapping("/admin/")
public class SystemConfigurerContoll extends BaseControll {

	@RequestMapping("systems")
	public ModelAndView pageSystems() {
		return createModelAndView("/admin/system-general.jsp");
	}
	
	@RequestMapping("plugins/storage")
	public ModelAndView pagePluginsStorage() {
		return createModelAndView("/admin/plugins/storage-qiniu.jsp");
	}
	
	@RequestMapping("plugins/cache")
	public ModelAndView pagePluginsCache() {
		return createModelAndView("/admin/plugins/cache-redis.jsp");
	}
	
	@RequestMapping("plugins/sms")
	public ModelAndView pagePluginsSms() {
		return createModelAndView("/admin/plugins/sms-submail.jsp");
	}
	
	@RequestMapping("plugins/mail")
	public ModelAndView pagePluginsMail() {
		return createModelAndView("/admin/plugins/mail-submail.jsp");
	}
}
