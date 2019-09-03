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

import cn.devezhao.commons.web.ServletUtils;
import com.rebuild.server.helper.QiniuCloud;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.utils.AppUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 文件上传
 * 
 * @author zhaofang123@gmail.com
 * @since 11/06/2017
 */
@RequestMapping("/filex/")
@Controller
public class FileUploader {
	
	private static final Log LOG = LogFactory.getLog(FileUploader.class);
	
	@RequestMapping(value = "upload", method = RequestMethod.POST)
	public void upload(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String uploadName = null;
		try {
			List<FileItem> fileItems = parseFileItem(request);
			for (FileItem item : fileItems) {
				uploadName = item.getName();
				if (uploadName == null) {
					continue;
				}

				uploadName = QiniuCloud.formatFileKey(uploadName);
				File file = null;
				// 上传临时文件
				if (BooleanUtils.toBoolean(request.getParameter("temp"))) {
					uploadName = uploadName.split("/")[2];
					file = SysConfiguration.getFileOfTemp(uploadName);
				} else {
					file = SysConfiguration.getFileOfData(uploadName);
					FileUtils.forceMkdir(file.getParentFile());
				}
				
				item.write(file);
				if (!file.exists()) {
					ServletUtils.writeJson(response, AppUtils.formatControllMsg(1000, "上传失败"));
					return;
				}

				break;
			}
			
		} catch (Exception e) {
			LOG.error(null, e);
			uploadName = null;
		}
		
		if (uploadName != null) {
			ServletUtils.writeJson(response, AppUtils.formatControllMsg(0, uploadName));
		} else {
			ServletUtils.writeJson(response, AppUtils.formatControllMsg(1000, "上传失败"));
		}
	}

	// ----
	
	private static FileItemFactory fileItemFactory;
	static {
		File track = SysConfiguration.getFileOfTemp("track");
		if (!track.exists() || !track.isDirectory()) {
			if (!track.mkdirs()) {
				throw new ExceptionInInitializerError("Could't mkdir track repository");
			}
		}
		fileItemFactory = new DiskFileItemFactory(
				DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD * 2/*20MB*/, track);
	}
	
	/**
	 * 读取上传的文件列表
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	private static List<FileItem> parseFileItem(HttpServletRequest request) throws Exception {
		if (!ServletFileUpload.isMultipartContent(request)) {
			return Collections.emptyList();
		}
		
		ServletFileUpload upload = new ServletFileUpload(fileItemFactory);
		List<FileItem> files = null;
		try {
			files = upload.parseRequest(request);
		} catch (Exception ex) {
			if (ex instanceof IOException || ex.getCause() instanceof IOException) {
				LOG.warn("传输意外中断", ex);
				return Collections.emptyList();
			}
			throw ex;
		}
		
		if (files == null || files.isEmpty()) {
			return Collections.emptyList();
		}
		return files;
	}
}
