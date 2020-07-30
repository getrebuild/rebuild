/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用工具类
 * 
 * @author devezhao
 * @since 01/31/2019
 */
public class CommonsUtils {

	private static final Pattern PATT_PLAINTEXT = Pattern.compile("[A-Za-z0-9_\\-\\u4e00-\\u9fa5]+");
	/**
	 * 不含特殊字符。不允许除 数字 字母 中文 及  _ - 以外的字符，包括空格
	 * 
	 * @param text
	 * @return
	 */
	public static boolean isPlainText(String text) {
		return !text.contains(" ") && PATT_PLAINTEXT.matcher(text).matches();
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

	/**
	 * @param text
	 * @return
	 * @see org.apache.commons.lang.StringEscapeUtils#escapeHtml(String)
	 */
	public static String escapeHtml(Object text) {
		if (text == null || StringUtils.isBlank(text.toString())) {
			return StringUtils.EMPTY;
		}
		String escape = StringEscapeUtils.escapeHtml(text.toString());
		return escape.replace("&gt;", ">");  // `>` for MD
	}

	private static final Pattern PATT_VAR = Pattern.compile("\\{([0-9a-zA-Z._]+)}");
	/**
	 * 提取内容中的变量 {xxx}
	 * @param content
	 * @return
	 */
	public static Set<String> matchsVars(String content) {
		if (StringUtils.isBlank(content)) return Collections.emptySet();

		Set<String> vars = new HashSet<>();
		Matcher m = PATT_VAR.matcher(content);
		while (m.find()) {
			String varName = m.group(1);
			if (StringUtils.isNotBlank(varName)) vars.add(varName);
		}
		return vars;
	}
}
