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

import com.rebuild.server.helper.SystemConfiguration;
import com.rebuild.utils.StringsUtils;
import com.rebuild.web.BasePageControll;

/**
 * 系统配置
 * 
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
@Controller
@RequestMapping("/admin/")
public class SystemConfigurerContoll extends BasePageControll {

	@RequestMapping("systems")
	public ModelAndView pageSystems() {
		return createModelAndView("/admin/system-general.jsp");
	}
	
	@RequestMapping("plugins/storage")
	public ModelAndView pagePluginsStorage() {
		ModelAndView mv = createModelAndView("/admin/plugins/storage-qiniu.jsp");
		mv.getModel().put("storageAccount",
				starsAccount(SystemConfiguration.getStorageAccount(), 0, 1));
		return mv;
	}
	
	@RequestMapping("plugins/cache")
	public ModelAndView pagePluginsCache() {
		ModelAndView mv = createModelAndView("/admin/plugins/cache-redis.jsp");
		mv.getModel().put("cacheAccount", 
				starsAccount(SystemConfiguration.getCacheAccount(), 2));
		return mv;
	}
	
	@RequestMapping("plugins/submail")
	public ModelAndView pagePluginsMailSms() {
		ModelAndView mv = createModelAndView("/admin/plugins/submail.jsp");
		mv.getModel().put("smsAccount", 
				starsAccount(SystemConfiguration.getSmsAccount(), 1));
		mv.getModel().put("mailAccount", 
				starsAccount(SystemConfiguration.getMailAccount(), 1));
		return mv;
	}
	
	static String[] starsAccount(String account[], int ...index) {
		if (account == null) {
			return null;
		}
		for (int i : index) {
			account[i] = StringsUtils.stars(account[i]);
		}
		return account;
	}
}
