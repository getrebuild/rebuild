/*
rebuild - Building your business-systems freely.
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

import com.rebuild.server.Application;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.QiniuCloud;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.web.BasePageControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * 系统配置
 * 
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 * @see SysConfiguration
 */
@Controller
@RequestMapping("/admin/")
public class SysConfigControll extends BasePageControll {

	@RequestMapping("systems")
	public ModelAndView pageSystems() {
		ModelAndView mv = createModelAndView("/admin/system-general.jsp");
		for (ConfigurableItem item : ConfigurableItem.values()) {
			mv.getModel().put(item.name(), SysConfiguration.get(item));
		}
		return mv;
	}
	
	@RequestMapping("integration/storage")
	public ModelAndView pageIntegrationStorage() {
		ModelAndView mv = createModelAndView("/admin/integration/storage-qiniu.jsp");
		mv.getModel().put("storageAccount",
				starsAccount(SysConfiguration.getStorageAccount(), 0, 1));
		mv.getModel().put("storageStatus", QiniuCloud.instance().available());
		return mv;
	}
	
	@RequestMapping("integration/cache")
	public ModelAndView pageIntegrationCache() {
		ModelAndView mv = createModelAndView("/admin/integration/cache-redis.jsp");
		mv.getModel().put("cacheAccount", 
				starsAccount(SysConfiguration.getCacheAccount(), 2));
		mv.getModel().put("cacheStatus", Application.getCommonCache().isUseRedis());
		return mv;
	}
	
	@RequestMapping("integration/submail")
	public ModelAndView pageIntegrationSubmail() {
		ModelAndView mv = createModelAndView("/admin/integration/submail.jsp");
		mv.getModel().put("smsAccount", 
				starsAccount(SysConfiguration.getSmsAccount(), 1));
		mv.getModel().put("mailAccount", 
				starsAccount(SysConfiguration.getMailAccount(), 1));
		return mv;
	}
	
	/**
	 * @param account
	 * @param index
	 * @return
	 */
	private String[] starsAccount(String[] account, int ...index) {
		if (account == null) {
			return null;
		}
		for (int i : index) {
			account[i] = CommonsUtils.stars(account[i]);
		}
		return account;
	}
}
