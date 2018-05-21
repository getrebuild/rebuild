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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
		String xtype = StringUtils.defaultIfBlank(req.getParameter("xtype"), "cloud");
		String uploadName = null;
		try {
			List<FileItem> fileItems = parseFileItem(req);
			for (FileItem item : fileItems) {
				uploadName = item.getName();
				if (uploadName == null) {
					continue;
				}
				
				File temp = AppUtils.getFileOfTemp(uploadName);
				item.write(temp);
				
				if ("cloud".equals(xtype)) {
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
			ServletUtils.writeJson(resp, String.format("{\"error_code\":0,\"url\":\"%s\"}", uploadName));
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
