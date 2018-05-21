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

package cn.devezhao.rebuild.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.http4.HttpClientEx;
import cn.devezhao.rebuild.server.RebuildException;

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
		String key = String.format("/fs/%s/%s", 
				CalendarUtils.getPlainDateFormat().format(CalendarUtils.now()),
				UUID.randomUUID().toString().replace("-", ""));
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
