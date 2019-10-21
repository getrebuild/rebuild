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

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.helper.cache.EhcacheTemplate;
import com.rebuild.server.helper.cache.JedisCacheTemplate;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.web.BasePageControll;
import com.rebuild.web.RequestWatchHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao
 * @since 10/13/2018
 */
@Controller
public class AdminEntryControll extends BasePageControll {

	@RequestMapping("/user/admin-entry")
	public ModelAndView pageAdminEntry(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		boolean pass = RequestWatchHandler.verfiyPass(request, response);
		if (!pass) {
			return null;
		}
		
		ID adminId = getRequestUser(request);
		User admin = Application.getUserStore().getUser(adminId);
		if (admin.isAdmin()) {
			return createModelAndView("/admin/admin-entry.jsp");
		} else {
			response.sendError(403, "当前登录用户非管理员");
			return null;
		}
	}
	
	@RequestMapping("/user/admin-verify")
	public void adminVerify(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		ID adminId = getRequestUser(request);
		String passwd = getParameterNotNull(request, "passwd");
		
		Object[] foundUser = Application.createQueryNoFilter(
				"select password from User where userId = ?")
				.setParameter(1, adminId)
				.unique();
		if (foundUser[0].equals(EncryptUtils.toSHA256Hex(passwd))) {
			ServletUtils.setSessionAttribute(request, KEY_VERIFIED, CalendarUtils.now());
			writeSuccess(response);
		} else {
			ServletUtils.setSessionAttribute(request, KEY_VERIFIED, null);
			writeFailure(response, "密码不正确");
		}
	}

	// ----
	
	private static final String KEY_VERIFIED = WebUtils.KEY_PREFIX + "-AdminVerified";
	/**
	 * @param request
	 * @return
	 */
	public static boolean isAdminVerified(HttpServletRequest request) {
		Object verified = ServletUtils.getSessionAttribute(request, KEY_VERIFIED);
		return verified != null;
	}

	/**
	 * @param request
	 */
	public static void cleanAdminVerified(HttpServletRequest request) {
		ServletUtils.setSessionAttribute(request, KEY_VERIFIED, null);
	}

	// ---- CLI

	@RequestMapping("/admin/cli/{command}")
	public void adminCLI(@PathVariable String command,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		// 清缓存
		if ("CLEANCACHE".equals(command)) {
			if (Application.getCommonCache().isUseRedis()) {
				try (Jedis jedis = ((JedisCacheTemplate) Application.getCommonCache().getCacheTemplate()).getJedisPool().getResource()) {
					jedis.flushAll();
				}
			} else {
				((EhcacheTemplate) Application.getCommonCache().getCacheTemplate()).cache().clear();
			}
			ServletUtils.write(response, "command:CLEANCACHE");
		}
		else {
			response.sendRedirect("../systems");
		}
	}

}
