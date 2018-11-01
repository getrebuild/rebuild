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

package com.rebuild.web.userprofile;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.rebuild.web.BaseControll;

/**
 * 
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
public class UserProfileControll extends BaseControll {

	@RequestMapping("/profile/view")
	public ModelAndView pageView(HttpServletRequest request) throws IOException {
		return createModelAndView("/user-profile/account.jsp", "User", getRequestUser(request));
	}
	
	@RequestMapping("/profile/edit")
	public ModelAndView pageEdit(HttpServletRequest request) throws IOException {
		return createModelAndView("/user-profile/edit.jsp", "User", getRequestUser(request));
	}
	
	@RequestMapping("/people/{user}")
	public ModelAndView pagePeople(@PathVariable String user, HttpServletRequest request) throws IOException {
		return createModelAndView("/user-profile/edit.jsp", "User", getRequestUser(request));
	}
}
