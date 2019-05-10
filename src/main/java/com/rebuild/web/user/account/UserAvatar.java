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

package com.rebuild.web.user.account;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.rebuild.server.Application;
import com.rebuild.server.helper.QiniuCloud;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;

/**
 * 用户头像
 * 
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/08
 */
@RequestMapping("/account")
@Controller
public class UserAvatar extends BaseControll {
	
	@RequestMapping("/user-avatar")
	public void renderAvatat(HttpServletRequest request, HttpServletResponse response) throws IOException {
		renderUserAvatat(getRequestUser(request), response);
	}
	
	@RequestMapping("/user-avatar/{user}")
	public void renderAvatat(@PathVariable String user, HttpServletResponse response) throws IOException {
		renderUserAvatat(user, response);
	}
	
	/**
	 * @param user
	 * @param response
	 * @throws IOException
	 */
	protected void renderUserAvatat(Object user, HttpServletResponse response) throws IOException {
		User realUser = null;
		if (user instanceof ID) {
			realUser = Application.getUserStore().getUser((ID) user);
		} if (ID.isId(user)) {
			realUser = Application.getUserStore().getUser(ID.valueOf(user.toString()));
		} else if (Application.getUserStore().existsName((String) user)) {
			realUser = Application.getUserStore().getUserByName((String) user);
		} else if (Application.getUserStore().existsEmail((String) user)) {
			realUser = Application.getUserStore().getUserByEmail((String) user);
		}
		
		if (realUser == null) {
			response.sendError(404);
			return;
		}
		
		final int minutes = 15;
		ServletUtils.addCacheHead(response, minutes);
		
		String avatarUrl = realUser.getAvatarUrl();
		avatarUrl = QiniuCloud.encodeUrl(avatarUrl);
		if (avatarUrl != null) {
			avatarUrl = avatarUrl + "?imageView2/2/w/100/interlace/1/q/100";
			if (QiniuCloud.instance().available()) {
				avatarUrl = QiniuCloud.instance().url(avatarUrl, minutes * 60);
			} else {
				avatarUrl = AppUtils.getContextPath() + "/filex/img/" + avatarUrl;
			}
			response.sendRedirect(avatarUrl);
		} else {
			avatarUrl = AppUtils.getContextPath() + "/assets/img/avatar.png";
			response.sendRedirect(avatarUrl);
			
			// TODO 生成用户头像
			
//			ChineseCaptcha avatar = new ChineseCaptcha(200, 200, 1);
//			avatar.out(response.getOutputStream());
		}
	}
}
