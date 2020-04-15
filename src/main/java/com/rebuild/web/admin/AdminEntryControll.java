/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.License;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.web.BasePageControll;
import com.rebuild.web.RequestWatchHandler;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao
 * @since 10/13/2018
 */
@Controller
public class AdminEntryControll extends BasePageControll {

	/**
	 * Admin 验证标志
	 */
	public static final String KEY_VERIFIED = WebUtils.KEY_PREFIX + "-AdminVerified";

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

    @RequestMapping("/user/admin-cancel")
    public void adminCancel(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ServletUtils.setSessionAttribute(request, KEY_VERIFIED, null);
        writeSuccess(response);
    }

	@RequestMapping("/user/admin-dangers")
	public void adminDangers(HttpServletResponse response)
			throws IOException {
		if (!SysConfiguration.getBool(ConfigurableItem.AdminDangers)) {
			writeSuccess(response);
			return;
		}

		List<String> dangers = new ArrayList<>();

		JSONObject ret = License.siteApi("api/authority/check-build", true);
		if (ret != null && ret.getIntValue("build") > Application.BUILD) {
			String buildUpdate = String.format(
					"有新版的 REBUILD (%s) 更新可用 <a target='_blank' href='%s' class='link'>(查看详情)</a>",
					ret.getString("version"), ret.getString("releaseUrl"));
			dangers.add(buildUpdate);
		}

		writeSuccess(response, dangers);
	}

	// -- CLI

	@RequestMapping("/admin/cli/console")
	public ModelAndView adminCliConsole() throws IOException {
		return createModelAndView("/admin/admin-cli.jsp");
	}

    @RequestMapping("/admin/cli/exec")
	public void adminCliExec(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String command = ServletUtils.getRequestString(request);
		if (StringUtils.isBlank(command)) {
			return;
		}

		String result = new AdminCli(command).exec();
		ServletUtils.write(response, result);
	}
}
