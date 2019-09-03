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

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.helper.QiniuCloud;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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
		renderUserAvatat(getRequestUser(request), request, response);
	}
	
	@RequestMapping("/user-avatar/{user}")
	public void renderAvatat(@PathVariable String user, HttpServletRequest request, HttpServletResponse response) throws IOException {
		renderUserAvatat(user, request, response);
	}
	
	/**
	 * @param user
	 * @param response
	 * @throws IOException
	 */
	protected void renderUserAvatat(Object user, HttpServletRequest request, HttpServletResponse response) throws IOException {
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
			int w = getIntParameter(request, "w", 100);
			avatarUrl = avatarUrl + "?imageView2/2/w/" + w + "/interlace/1/q/100";

			if (QiniuCloud.instance().available()) {
				avatarUrl = QiniuCloud.instance().url(avatarUrl, minutes * 60);
			} else {
				avatarUrl = AppUtils.getContextPath() + "/filex/img/" + avatarUrl;
			}
			response.sendRedirect(avatarUrl);
		} else {
			BufferedImage avatarBi = null;
			try {
				File avatarFile = UserHelper.generateAvatar(realUser.getFullName(), false);
				avatarBi = ImageIO.read(avatarFile);
			} catch (IOException ex) {
				LOG.warn("Couldn't generate avatar", ex);
				avatarUrl = AppUtils.getContextPath() + "/assets/img/avatar.png";
				response.sendRedirect(avatarUrl);
				return;
			}

			ImageIO.write(avatarBi, "png", response.getOutputStream());
		}
	}

	@RequestMapping("/user-avatar-update")
	public void avatarUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String avatarRaw = getParameterNotNull(request, "avatar");
		String xywh = getParameterNotNull(request, "xywh");

		File avatarFile = SysConfiguration.getFileOfTemp(avatarRaw);
		String uploadName = avatarCrop(avatarFile, xywh);

		ID user = getRequestUser(request);
		Record record = EntityHelper.forUpdate(user, user);
		record.setString("avatarUrl", uploadName);
		Application.getBean(UserService.class).update(record);

		writeSuccess(response, uploadName);
	}

	/**
	 * 头像裁剪
	 *
	 * @param avatar
	 * @param params x,y,width,height
	 * @return
	 * @throws IOException
	 */
	private String avatarCrop(File avatar, String params) throws IOException {
		String xywh[] = params.split(",");
		BufferedImage bi = ImageIO.read(avatar);
		int x = Integer.parseInt(xywh[0]);
		int y = Integer.parseInt(xywh[1]);
		int width = Integer.parseInt(xywh[2]);
		int height = Integer.parseInt(xywh[3]);

		if (x + width > bi.getWidth()) {
			width = bi.getWidth() - x;
		}
		if (y + height > bi.getHeight()) {
			height = bi.getHeight() - y;
		}

		bi = bi.getSubimage(Math.max(x, 0), Math.max(y, 0), width, height);

		String destName = System.currentTimeMillis() + avatar.getName();
		File dest = null;
		if (QiniuCloud.instance().available()) {
			dest = SysConfiguration.getFileOfTemp(destName);
		} else {
			dest = SysConfiguration.getFileOfData(destName);
		}
		ImageIO.write(bi, "png", dest);

		if (QiniuCloud.instance().available()) {
			destName = QiniuCloud.instance().upload(dest);
		}
		return destName;
	}
}
