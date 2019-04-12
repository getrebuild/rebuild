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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.rebuild.server.RebuildException;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.http4.HttpClientEx;

/**
 * 七牛云存储
 * 
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
public class QiniuCloud {

	private static final Log LOG = LogFactory.getLog(QiniuCloud.class);
	
	private final Configuration CONFIGURATION = new Configuration(Zone.autoZone());
	
	private final UploadManager UPLOAD_MANAGER = new UploadManager(CONFIGURATION);
	
	private Auth auth;
	private String bucketName;

	private QiniuCloud() {
		init();
	}
	
	/**
	 * 初始化
	 */
	synchronized public void init() {
		String[] account = SystemConfig.getStorageAccount();
		if (account != null) {
			this.auth = Auth.create(account[0], account[1]);
			this.bucketName = account[2];
		} else {
			LOG.error("云存储账户未配置，文件上传功能不可用");
		}
	}
	
	/**
	 * 文件上传
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public String upload(File file) throws IOException {
		if (auth == null) {
			return null;
		}
		
		String key = String.format("rebuild/%s/%s", 
				CalendarUtils.getPlainDateFormat().format(CalendarUtils.now()), file.getName());
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
		File tmp = SystemConfig.getFileOfTemp("temp-" + System.currentTimeMillis());
		boolean success = download(url, tmp);
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
		return url(filePath, 60 * 5);
	}
	
	/**
	 * 生成访问 URL
	 * 
	 * @param filePath
	 * @param seconds 过期时间
	 * @return
	 */
	public String url(String filePath, int seconds) {
		String baseUrl = SystemConfig.getStorageUrl() + filePath;
		// default use HTTP
		if (baseUrl.startsWith("//")) {
			baseUrl = "http:" + baseUrl;
		}
		
		long deadline = System.currentTimeMillis() / 1000 + seconds;
		// Use http cache
		seconds /= 1.5;
		deadline = deadline / seconds * seconds;
		return auth.privateDownloadUrlWithDeadline(baseUrl, deadline);
	}
	
	/**
	 * 下载文件
	 * 
	 * @param url
	 * @param dest
	 * @return
	 * @throws Exception
	 */
	public boolean download(URL url, File dest) throws Exception {
		byte[] bs = HttpClientEx.instance().readBinary(url.toString(), 60 * 1000);
		FileUtils.writeByteArrayToFile(dest, bs);
		return true;
	}
	
	/**
	 * 删除文件
	 * 
	 * @param key
	 * @return
	 */
	protected boolean delete(String key) {
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
	
	// --
	
	private static final QiniuCloud INSTANCE = new QiniuCloud();
	/**
	 * @return
	 */
	public static QiniuCloud instance() {
		return INSTANCE;
	}
}
