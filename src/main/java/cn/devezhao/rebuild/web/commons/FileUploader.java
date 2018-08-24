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

package cn.devezhao.rebuild.web.commons;

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

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.rebuild.utils.AppUtils;
import cn.devezhao.rebuild.utils.QiniuCloud;

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
				
				uploadName = CalendarUtils.now().getTime() + "__" + uploadName;
				File temp = AppUtils.getFileOfTemp(uploadName);
				item.write(temp);
				
				String cloud = req.getParameter("cloud");
				if ("true".equals(cloud) || "auto".equals(cloud)) {
					uploadName = QiniuCloud.upload(temp);
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
		File track = AppUtils.getFileOfTemp("track");
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
