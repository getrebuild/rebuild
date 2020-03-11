/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.excel.Cell;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

	private static final char[] SPECIAL_CHARS = "`~!@#$%^&*()_+=-{}|[];':\",./<>?".toCharArray();
	/**
	 * 是否为特殊字符
	 *
	 * @param ch
	 * @return
	 */
	public static boolean isSpecialChar(char ch) {
		for (char c : SPECIAL_CHARS) {
			if (c == ch) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param text
	 * @param maxLength
	 * @return
	 */
	public static String maxstr(String text, int maxLength) {
		if (text.length() > maxLength) {
			return text.substring(0, maxLength);
		}
		return text;
	}

	private static OkHttpClient okHttpClient = null;
	/**
	 * @return
	 */
	public static OkHttpClient getHttpClient() {
		if (okHttpClient == null) {
			okHttpClient = new OkHttpClient.Builder()
					.connectTimeout(15, TimeUnit.SECONDS)
					.writeTimeout(45, TimeUnit.SECONDS)
					.readTimeout(45, TimeUnit.SECONDS)
					.retryOnConnectionFailure(true)
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
				.header("user-agent",  String.format("RB/%s (%s/%s)", Application.VER, SystemUtils.OS_NAME, SystemUtils.JAVA_SPECIFICATION_VERSION))
				.build();

		try (Response response = getHttpClient().newCall(request).execute()) {
			return Objects.requireNonNull(response.body()).string();
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
			return Objects.requireNonNull(response.body()).string();
		}
	}

	/**
	 * 读取二进制数据
	 *
	 * @param url
	 * @param dest
	 * @return
	 * @throws IOException
	 */
	public static boolean readBinary(String url, File dest) throws IOException {
		Request request = new Request.Builder()
				.url(url)
				.build();

		try (Response response = getHttpClient().newCall(request).execute()) {
			try (InputStream is = Objects.requireNonNull(response.body()).byteStream()) {
				try (BufferedInputStream bis = new BufferedInputStream(is)) {
					try (OutputStream os = new FileOutputStream(dest)) {
						byte[] chunk = new byte[1024];
						int count;
						while ((count = bis.read(chunk)) != -1) {
							os.write(chunk, 0, count);
						}
						os.flush();
					}
				}
			}
		}
		return true;
	}

	/**
	 * @param excel
	 * @return
	 */
	public static List<Cell[]> readExcel(File excel) {
		return readExcel(excel, -1, true);
	}

	/**
	 * @param excel
	 * @param maxRows
	 * @param hasHead
	 * @return
	 */
	public static List<Cell[]> readExcel(File excel, int maxRows, boolean hasHead) {
		final List<Cell[]> rows = new ArrayList<>();
		final AtomicInteger rowNo = new AtomicInteger(0);

		try (InputStream is = new FileInputStream(excel)) {
			try (BufferedInputStream bis = new BufferedInputStream(is)) {
				// noinspection rawtypes
				EasyExcel.read(bis, null, new AnalysisEventListener() {
					@Override
					public void invokeHeadMap(Map headMap, AnalysisContext context) {
						if (hasHead) {
							this.invoke(headMap, context);
						} else {
							rowNo.incrementAndGet();
						}
					}
					@Override
					public void invoke(Object data, AnalysisContext analysisContext) {
						if (maxRows > 0 && rows.size() >= maxRows) {
							return;
						}

						@SuppressWarnings("unchecked")
						Map<Integer, String> dataMap = (Map<Integer, String>) data;
						List<Cell> row = new ArrayList<>();
						for (int i = 0; i < dataMap.size(); i++) {
							row.add(new Cell(dataMap.get(i), rowNo.get(), i));
						}
						rows.add(row.toArray(new Cell[0]));
						rowNo.incrementAndGet();
					}
					@Override
					public void doAfterAllAnalysed(AnalysisContext analysisContext) {
					}
				}).sheet().doRead();
			}

		} catch (IOException e) {
			throw new RebuildException(e);
		}
		return rows;
	}

	/**
	 * @param text
	 * @return
	 * @see org.apache.commons.lang.StringEscapeUtils#escapeHtml(String)
	 */
	public static String escapeHtml(Object text) {
		if (text == null || StringUtils.isBlank(text.toString())) {
			return StringUtils.EMPTY;
		}
		return StringEscapeUtils.escapeHtml(text.toString());
	}

	/**
	 * ZIP 压缩（不支持目录压缩）
	 *
	 * @param file
	 * @param dest
	 */
	public static void zip(File file, File dest) throws IOException {
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        ZipOutputStream zos = null;

        try {
            fos = new FileOutputStream(dest);
            bos = new BufferedOutputStream(fos);
            zos = new ZipOutputStream(bos);

            zos.putNextEntry(new ZipEntry(file.getName()));

            FileInputStream fis = null;
            BufferedInputStream bis = null;

            try {
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);

                byte[] chunk = new byte[1024];
                int count;
                while((count = bis.read(chunk)) != -1) {
                    zos.write(chunk, 0, count);
                }

                zos.finish();

            } finally {
                IOUtils.closeQuietly(bis);
                IOUtils.closeQuietly(fis);
            }

        } finally {
            IOUtils.closeQuietly(zos);
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(fos);
        }
	}
}
