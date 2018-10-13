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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.helper.QiniuCloud;
import com.rebuild.server.helper.SystemProps;
import com.rebuild.utils.AppUtils;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;

/**
 * 文件上传
 * 
 * @author zhaofang123@gmail.com
 * @since 11/06/2017
 */
public class FileUploader extends HttpServlet {
	private static final long serialVersionUID = 5264645972230896850L;
	
	private static final Log LOG = LogFactory.getLog(FileUploader.class);
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String uploadName = null;
		try {
			List<FileItem> fileItems = parseFileItem(req);
			for (FileItem item : fileItems) {
				uploadName = item.getName();
				if (uploadName == null) {
					continue;
				}
				
				if (uploadName.length() > 43) {
					uploadName = uploadName.substring(0, 20) + "..." + uploadName.substring(uploadName.length() - 20);
				}
				uploadName = CalendarUtils.getDateFormat("hhMMssSSS").format(CalendarUtils.now()) + "__" + uploadName;
				
				File temp = SystemProps.getFileOfTemp(uploadName);
				item.write(temp);
				
				String cloud = req.getParameter("cloud");
				if ("true".equals(cloud) || "auto".equals(cloud)) {
					uploadName = QiniuCloud.instance().upload(temp);
					if (temp.exists()) {
						temp.delete();
					}
				}
				break;
			}
			
		} catch (Exception e) {
			LOG.error(e);
			uploadName = null;
		}
		
		if (uploadName != null) {
			ServletUtils.writeJson(resp, AppUtils.formatClientMsg(0, uploadName));
		} else {
			ServletUtils.writeJson(resp, AppUtils.formatClientMsg(1000, "上传失败"));
		}
	}
	
	// ----
	
	private FileItemFactory fileItemFactory;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		File track = SystemProps.getFileOfTemp("track");
		if (!track.exists() || !track.isDirectory()) {
			boolean mked = track.mkdir();
			if (!mked) {
				throw new ExceptionInInitializerError("Could't mkdir track repository");
			}
		}
		fileItemFactory = new DiskFileItemFactory(
				DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD * 2/*20MB*/, track);
	}
	
	/*-
	 * 读取上传的文件列表
	 */
	private List<FileItem> parseFileItem(HttpServletRequest request) throws Exception {
		if (!ServletFileUpload.isMultipartContent(request)) {
			return Collections.<FileItem>emptyList();
		}
		
		ServletFileUpload upload = new ServletFileUpload(this.fileItemFactory);
		List<FileItem> files = null;
		try {
			files = upload.parseRequest(request);
		} catch (Exception ex) {
			if (ex instanceof IOException || ex.getCause() instanceof IOException) {
				LOG.warn("I/O, 传输意外中断, 客户端取消???", ex);
				return Collections.<FileItem>emptyList();
			}
			throw ex;
		}
		
		if (files == null || files.isEmpty()) {
			return Collections.<FileItem>emptyList();
		}
		return files;
	}
}
