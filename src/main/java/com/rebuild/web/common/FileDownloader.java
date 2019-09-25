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

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.web.ServletUtils;
import com.rebuild.server.helper.QiniuCloud;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.web.BaseControll;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 文件下载/查看
 * 
 * @author devezhao
 * @since 01/03/2019
 */
@RequestMapping("/filex/")
@Controller
public class FileDownloader extends BaseControll {
	
	@RequestMapping(value = "img/**", method = RequestMethod.GET)
	public void viewImg(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String filePath = request.getRequestURI();
		filePath = filePath.split("/filex/img/")[1];
		
		final int minutes = 60 * 24;
		ServletUtils.addCacheHead(response, minutes);

		if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
			response.sendRedirect(filePath);
			return;
		}

		boolean temp = BooleanUtils.toBoolean(request.getParameter("temp"));
		// Local storage || temp
		if (!QiniuCloud.instance().available() || temp) {
			String fileName = QiniuCloud.parseFileName(filePath);
			String mimeType = request.getServletContext().getMimeType(fileName);
			if (mimeType != null) {
				response.setContentType(mimeType);
			}
			
			writeLocalFile(filePath, temp, response);
			return;
		}
		
		String imageView2 = request.getQueryString();
		if (imageView2 != null && imageView2.startsWith("imageView2")) {
			filePath += "?" + imageView2;
		}
		String privateUrl = QiniuCloud.instance().url(filePath, minutes * 60);
		response.sendRedirect(privateUrl);
	}
	
	@RequestMapping(value = "download/**", method = RequestMethod.GET)
	public void download(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String filePath = request.getRequestURI();
		filePath = filePath.split("/filex/download/")[1];

		boolean temp = BooleanUtils.toBoolean(request.getParameter("temp"));
		String fileName = QiniuCloud.parseFileName(filePath);

		ServletUtils.setNoCacheHeaders(response);

		// Local storage || temp
		if (!QiniuCloud.instance().available() || temp) {
			setDownloadHeaders(response, fileName);
			writeLocalFile(filePath, temp, response);
		} else {
			String privateUrl = QiniuCloud.instance().url(filePath);
			privateUrl += "&attname=" + fileName;
			response.sendRedirect(privateUrl);
		}
	}

	// --

	/**
	 * 文件下载
	 *
	 * @param filePath
	 * @param temp
	 * @param response
	 * @return
	 * @throws IOException
	 */
	public static boolean writeLocalFile(String filePath, boolean temp, HttpServletResponse response) throws IOException {
		filePath = CodecUtils.urlDecode(filePath);
		File file = temp ? SysConfiguration.getFileOfTemp(filePath) : SysConfiguration.getFileOfData(filePath);
		if (!file.exists()) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return false;
		}

		return writeLocalFile(file, response);
	}

	/**
	 * 文件下载
	 *
	 * @param file
	 * @param response
	 * @return
	 * @throws IOException
	 */
	public static boolean writeLocalFile(File file, HttpServletResponse response) throws IOException {
		long size = FileUtils.sizeOf(file);
		response.setHeader("Content-Length", String.valueOf(size));

		try (InputStream fis = new FileInputStream(file)) {
			response.setContentLength(fis.available());

			OutputStream os = response.getOutputStream();
			int count;
			byte[] buffer = new byte[1024 * 1024];
			while ((count = fis.read(buffer)) != -1) {
				os.write(buffer, 0, count);
			}
			os.flush();
			return true;
		}
	}

	/**
	 * @param response
	 * @param attname
	 */
	public static void setDownloadHeaders(HttpServletResponse response, String attname) {
		response.setHeader("Content-Disposition", "attachment;filename=" + attname);
		response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
	}
}
