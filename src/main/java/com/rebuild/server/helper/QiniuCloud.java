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

package com.rebuild.server.helper;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Region;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.rebuild.server.RebuildException;
import com.rebuild.utils.CommonsUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

/**
 * 七牛云存储
 * 
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
public class QiniuCloud {

	private static final Log LOG = LogFactory.getLog(QiniuCloud.class);
	
	private final Configuration CONFIGURATION = new Configuration(Region.autoRegion());
	private final UploadManager UPLOAD_MANAGER = new UploadManager(CONFIGURATION);
	
	private Auth auth;
	private String bucketName;

	private QiniuCloud() {
		String[] account = SysConfiguration.getStorageAccount();
		if (account != null) {
			this.auth = Auth.create(account[0], account[1]);
			this.bucketName = account[2];
		} else {
			LOG.warn("No QiniuCloud configuration! Using local storage.");
		}
	}
	
	/**
	 * @return
	 */
	public boolean available() {
//		return false;  // TEST
		return this.auth != null;
	}
	
	/**
	 * 文件上传
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public String upload(File file) throws IOException {
		Assert.notNull(auth, "云存储账户未配置");
		String key = formatFileKey(file.getName()); 
		Response resp = UPLOAD_MANAGER.put(file, key, auth.uploadToken(bucketName));
		if (resp.isOK()) {
			return key;
		} else {
			LOG.error("文件上传失败 : " + resp);
			return null;
		}
	}
	
	/**
	 * 从 URL 上传文件
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public String upload(URL url) throws Exception {
		Assert.notNull(auth, "云存储账户未配置");
		File tmp = SysConfiguration.getFileOfTemp("download." + System.currentTimeMillis());
		boolean success = CommonsUtils.readBinary(url.toString(), tmp);
		if (!success) {
			throw new RebuildException("无法从 URL 读取文件 : " + url);
		}
		
		try {
			return upload(tmp);
		} finally {
			tmp.delete();
		}
	}
	
	/**
	 * 生成访问 URL
	 * 
	 * @param filePath
	 * @return
	 */
	public String url(String filePath) {
		return url(filePath, 60 * 15);
	}

	/**
	 * 生成访问 URL
	 * 
	 * @param filePath
	 * @param seconds 有效期
	 * @return
	 */
	public String url(String filePath, int seconds) {
		Assert.notNull(auth, "云存储账户未配置");
		String baseUrl = SysConfiguration.getStorageUrl() + filePath;
		// default use HTTPS
		if (baseUrl.startsWith("//")) {
			baseUrl = "https:" + baseUrl;
		}

		long deadline = System.currentTimeMillis() / 1000 + seconds;
		// Use http cache
		seconds /= 2;
		deadline = deadline / seconds * seconds;
		return auth.privateDownloadUrlWithDeadline(baseUrl, deadline);
	}
	
	/**
	 * 删除文件
	 * 
	 * @param key
	 * @return
	 */
	protected boolean delete(String key) {
		Assert.notNull(auth, "云存储账户未配置");
		BucketManager bucketManager = new BucketManager(auth, CONFIGURATION);
		Response resp = null;
		try {
			resp = bucketManager.delete(this.bucketName, key);
			if (resp.isOK()) {
				return true;
			} else {
				throw new RebuildException("删除文件失败 : " + this.bucketName + " < " + key + " : " + resp.bodyString());
			}
		} catch (QiniuException e) {
			throw new RebuildException("删除文件失败 : " + this.bucketName + " < " + key, e);
		}
	}

	/**
	 * @param fileKey
	 * @return
	 * @see #formatFileKey(String)
	 */
	public String getUploadToken(String fileKey) {
		// 上传策略参见 https://developer.qiniu.com/kodo/manual/1206/put-policy
		StringMap policy = new StringMap().put("fsizeLimit", 1024 * 1024 * 20);  // 20M
		return auth.uploadToken(bucketName, fileKey, 120, policy);
	}
	
	// --
	
	/**
	 * @param fileName
	 * @return
	 * @see #parseFileName(String)
	 */
	public static String formatFileKey(String fileName) {
		return formatFileKey(fileName, true);
	}
	
	/**
	 * @param fileName
	 * @param keepName
	 * @return
	 * @see #parseFileName(String)
	 */
	public static String formatFileKey(String fileName, boolean keepName) {
		if (!keepName) {
			String fileName_s[] = fileName.split("\\.");
			fileName = UUID.randomUUID().toString().replace("-", "");
			if (fileName_s.length > 1 && StringUtils.isNotBlank(fileName_s[fileName_s.length - 1])) {
				fileName += "." + fileName_s[fileName_s.length - 1];
			}
		} else {
			while (fileName.contains("__")) {
				fileName = fileName.replace("__", "_");
			}
			if (fileName.contains("+")) {
				fileName = fileName.replace("+", "");
			}
			if (fileName.contains("#")) {
				fileName = fileName.replace("#", "");
			}
			if (fileName.contains("?")) {
				fileName = fileName.replace("?", "");
			}
			if (fileName.length() > 41) {
				fileName = fileName.substring(0, 20) + "-" + fileName.substring(fileName.length() - 20);
			}
		}
		
		String datetime = CalendarUtils.getDateFormat("yyyyMMddHHmmssSSS").format(CalendarUtils.now());
		fileName = String.format("rb/%s/%s__%s", datetime.substring(0, 8), datetime.substring(8), fileName);
		return fileName;
	}
	
	/**
	 * 解析上传文件名称
	 * 
	 * @param filePath
	 * @return
	 * @see #formatFileKey(String)
	 */
	public static String parseFileName(String filePath) {
		String filePath_s[] = filePath.split("/");
		String fileName = filePath_s[filePath_s.length - 1];
		fileName = fileName.substring(fileName.indexOf("__") + 2);
		return fileName;
	}
	
	/**
	 * URL 编码（中文或特殊字符）
	 * 
	 * @param url
	 * @return
	 */
	public static String encodeUrl(String url) {
		if (StringUtils.isBlank(url)) {
			return url;
		}
		
		String url_s[] = url.split("/");
		for (int i = 0; i < url_s.length; i++) {
			String e = CodecUtils.urlEncode(url_s[i]);
			if (e.contains("+")) {
				e = e.replace("+", "%20");
			}
			url_s[i] = e;
		}
		return StringUtils.join(url_s, "/");
	}
	
	private static final QiniuCloud INSTANCE = new QiniuCloud();
	/**
	 * @return
	 */
	public static QiniuCloud instance() {
		return INSTANCE;
	}
}
