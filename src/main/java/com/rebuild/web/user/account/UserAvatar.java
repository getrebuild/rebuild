/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
import com.rebuild.web.common.FileDownloader;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
		renderUserAvatar(getRequestUser(request), request, response);
	}
	
	@RequestMapping("/user-avatar/{user}")
	public void renderAvatat(@PathVariable String user, HttpServletRequest request, HttpServletResponse response) throws IOException {
		renderUserAvatar(user, request, response);
	}
	
	/**
	 * @param user
	 * @param response
	 * @throws IOException
	 */
	protected void renderUserAvatar(Object user, HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (user == null) {
			response.sendError(404);
			return;
		}

		User realUser = null;
		if (user instanceof ID) {
			realUser = Application.getUserStore().getUser((ID) user);
		} if (ID.isId(user)) {
			realUser = Application.getUserStore().getUser(ID.valueOf(user.toString()));
		} else if (Application.getUserStore().existsName(user.toString())) {
			realUser = Application.getUserStore().getUserByName(user.toString());
		} else if (Application.getUserStore().existsEmail(user.toString())) {
			realUser = Application.getUserStore().getUserByEmail(user.toString());
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
			File avatarFile;
			try {
			    String fullName = realUser.getFullName();
			    if (realUser.getId().equals(UserService.SYSTEM_USER) || realUser.getId().equals(UserService.ADMIN_USER)) {
			        fullName = "RB";
                }

				avatarFile = UserHelper.generateAvatar(fullName, false);

			} catch (IOException ex) {
				LOG.warn("Couldn't generate avatar", ex);

				avatarUrl = AppUtils.getContextPath() + "/assets/img/avatar.png";
				response.sendRedirect(avatarUrl);
				return;
			}

			FileDownloader.writeLocalFile(avatarFile, response);
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
		String[] xywh = params.split(",");
		int x = Integer.parseInt(xywh[0]);
		int y = Integer.parseInt(xywh[1]);
		int width = Integer.parseInt(xywh[2]);
		int height = Integer.parseInt(xywh[3]);

		Thumbnails.Builder<File> builder = Thumbnails.of(avatar)
				.sourceRegion(x, y, width, height);

		String destName = System.currentTimeMillis() + avatar.getName();
		File dest;
		if (QiniuCloud.instance().available()) {
			dest = SysConfiguration.getFileOfTemp(destName);
		} else {
			dest = SysConfiguration.getFileOfData(destName);
		}
		builder.scale(1.0).toFile(dest);

		if (QiniuCloud.instance().available()) {
			destName = QiniuCloud.instance().upload(dest);
		}
		return destName;
	}
}
