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

package com.rebuild.web.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.rebuild.server.helper.SysConfiguration;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.web.ServletUtils;

/**
 * 本地文件下载
 * 
 * @author zhaofang123@gmail.com
 * @since 08/23/2018
 */
public class FileDownloader extends HttpServlet {
	private static final long serialVersionUID = -8907494228567119962L;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String fileName = req.getRequestURI();
		String fileName_s[] = fileName.split("/");
		fileName = fileName_s[fileName_s.length - 1];
		if (fileName.contains("%")) {
			fileName = CodecUtils.urlDecode(fileName);
		}
		if (fileName.contains("%")) {
			fileName = CodecUtils.urlDecode(fileName);
		}
		
		File temp = SysConfiguration.getFileOfTemp(fileName);
		if (!temp.exists() && !temp.isDirectory()) {
			resp.sendError(404, "未找到文件 : " + fileName);
			return;
		}
		
		InputStream fis = null;
		try {
			fis = new FileInputStream(temp);
			byte[] buffer = new byte[fis.available()];
			IOUtils.read(fis, buffer);
			
			ServletUtils.setNoCacheHeaders(resp);
			IOUtils.write(buffer, resp.getWriter());
		} catch (Exception ex) {
			resp.sendError(500, "无法下载文件 : " + fileName);
		} finally {
			IOUtils.closeQuietly(fis);
		}
	}
}
