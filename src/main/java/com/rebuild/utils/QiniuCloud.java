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

package com.rebuild.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.rebuild.server.RebuildException;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.http4.HttpClientEx;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
public class QiniuCloud {

	private static final String ACCESS_KEY = "YCSTYJijko0gEoj84qx5NZjbshg2VzU7GE1l9FDe";
	private static final String SECRET_KEY = "u1keXSO5otlajgiOGyF0QRWhFIQfVDi1D5-yEXv4";
	
	private static final Auth AUTH = Auth.create(ACCESS_KEY, SECRET_KEY);
	private static final String BUCKET_NAME = "rb-cdn";
	
	private static final UploadManager UPLOAD_MANAGER = new UploadManager(new Configuration(Zone.autoZone()));

	/**
	 * 上传 Token
	 * 
	 * @return
	 */
	public static String getUpToken() {
		return AUTH.uploadToken(BUCKET_NAME);
	}
	
	/**
	 * 文件上传
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static String upload(File file) throws IOException {
		String key = String.format("rebuild/%s/%s", 
				CalendarUtils.getPlainDateFormat().format(CalendarUtils.now()), file.getName());
		Response resp = UPLOAD_MANAGER.put(file, key, getUpToken());
		System.out.println(resp.bodyString());
		return key;
	}
	
	/**
	 * 文件上传
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public static String upload(URL url) throws Exception {
		File tmp = AppUtils.getFileOfTemp("temp-" + System.currentTimeMillis());
		boolean success = download(url, tmp);
		if (!success) {
			throw new RebuildException("无法读取源文件:" + url);
		}
		try {
			return upload(tmp);
		} finally {
			tmp.delete();
		}
	}
	
	/**
	 * 下载文件
	 * 
	 * @param url
	 * @param dest
	 * @return
	 * @throws Exception
	 */
	public static boolean download(URL url, File dest) throws Exception {
		byte[] bs = HttpClientEx.instance().readBinary(url.toString(), 30 * 1000);
		FileUtils.writeByteArrayToFile(dest, bs);
		return true;
	}
}
