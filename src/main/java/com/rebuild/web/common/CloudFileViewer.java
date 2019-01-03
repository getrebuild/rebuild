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

package com.rebuild.web.common;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.rebuild.server.helper.QiniuCloud;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.web.ServletUtils;

/**
 * 
 * @author devezhao
 * @since 01/03/2019
 */
@Controller
public class CloudFileViewer extends BaseControll {
	
	@RequestMapping(value={ "/cloud/img/**" }, method=RequestMethod.GET)
	public void viewImg(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String filePath = request.getRequestURI();
		filePath = filePath.split("/cloud/img/")[1];
		String imageView2 = request.getQueryString();
		if (imageView2 != null && imageView2.startsWith("imageView2")) {
			filePath += "?" + imageView2;
		}
		
		int expires = 15;
		String privateUrl = QiniuCloud.instance().url(filePath, expires * 60);
		ServletUtils.addCacheHead(response, expires);
		response.sendRedirect(privateUrl);
	}
	
	@RequestMapping(value="/cloud/download/**", method=RequestMethod.GET)
	public void download(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String filePath = request.getRequestURI();
		filePath = filePath.split("/cloud/download/")[1];
		
		String privateUrl = QiniuCloud.instance().url(filePath);
		privateUrl += "&attname=" + parseFileName(filePath);
		response.sendRedirect(privateUrl);
	}
	
	/**
	 * @param filePath
	 * @return
	 */
	public static String parseFileName(String filePath) {
		String filePath_s[] = filePath.split("/");
		String fileName = filePath_s[filePath_s.length - 1];
		fileName = fileName.substring(fileName.indexOf("__") + 2);
		return fileName;
	}
}
