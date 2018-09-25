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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.rebuild.web.BaseControll;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
@Controller
public class UrlSafe extends BaseControll {

	@RequestMapping(value="/common/url-safe", method=RequestMethod.GET)
	public void safeRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String url = getParameterNotNull(request, "url");
		
		// TODO 检查 URL 安全
		
		response.sendRedirect(url);
	}
}
