/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.rebuild.web.common;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.rebuild.server.Startup;
import com.rebuild.web.PageControll;

/**
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
@Controller
public class SimplePageForward extends PageControll {

	@RequestMapping(value={ "/page/**/*", "/admin/page/**/*" }, method = RequestMethod.GET)
	public ModelAndView page(HttpServletRequest request) {
		String path = request.getRequestURI().toString();
		// remove `context path` and `/page/`
		path = path.substring(Startup.getContextPath().length());
		path = path.replace("/page/", "/");
		path = path + ".jsp";
		
		return createModelAndView(path);
	}
}