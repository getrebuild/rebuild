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

package com.rebuild.utils;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 通用工具类
 * 
 * @author devezhao
 * @since 01/31/2019
 */
public class CommonsUtils {

	private static final Pattern PLAIN_PATTERN = Pattern.compile("[A-Za-z0-9_\\-\\u4e00-\\u9fa5]+");
	/**
	 * 不含特殊字符。不允许除 数字 字母 中文 及  _ - 以外的字符，包括空格
	 * 
	 * @param text
	 * @return
	 */
	public static boolean isPlainText(String text) {
		return !text.contains(" ") && PLAIN_PATTERN.matcher(text).matches();
	}
	
	/**
	 * 给敏感文本加星号/打码
	 * 
	 * @param text
	 * @return
	 */
	public static String stars(String text) {
		if (StringUtils.isBlank(text)) {
			return text;
		}
		
		int textLen = text.length();
		if (textLen <= 3) {
			return text.substring(0, 1) + "**";
		} else if (textLen <= 20) {
			return text.substring(0, 1) + "**" + text.substring(textLen - 1);
		} else if (textLen <= 30) {
			return text.substring(0, 2) + "****" + text.substring(textLen - 2);
		}  else if (textLen <= 40) {
			return text.substring(0, 4) + "**********" + text.substring(textLen - 4);
		} else {
			return text.substring(0, 4) + "********************" + text.substring(textLen - 4);
		}
	}

	private static OkHttpClient okHttpClient = null;
	/**
	 * @return
	 */
	public static OkHttpClient getHttpClient() {
		if (okHttpClient == null) {
			okHttpClient = new OkHttpClient.Builder()
					.retryOnConnectionFailure(false)
					.callTimeout(15, TimeUnit.SECONDS)
					.build();
		}
		return okHttpClient;
	}

	/**
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static String get(String url) throws IOException {
		Request request = new Request.Builder()
				.url(url)
				.build();

		try (Response response = getHttpClient().newCall(request).execute()) {
			return response.body().string();
		}
	}

	/**
	 * @param url
	 * @param data
	 * @return
	 * @throws IOException
	 */
	public static String post(String url, Map<String, Object> data) throws IOException {
		FormBody.Builder formBuilder = new FormBody.Builder();
		if (data != null && !data.isEmpty()) {
			for (Map.Entry<String, Object> e : data.entrySet()) {
				Object v = e.getValue();
				formBuilder.add(e.getKey(), v == null ? StringUtils.EMPTY : v.toString());
			}
		}

		Request request = new Request.Builder()
				.url(url)
				.post(formBuilder.build())
				.build();

		try (Response response = getHttpClient().newCall(request).execute()) {
			return response.body().string();
		}
	}
}
