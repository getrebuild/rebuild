/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.regex.Pattern;

/**
 * 通用工具类
 *
 * @author devezhao
 * @since 01/31/2019
 */
@Slf4j
public class CommonsUtils {

    private static final Pattern PATT_PLAINTEXT = Pattern.compile("[A-Za-z0-9_\\-\\u4e00-\\u9fa5]+");

    private static final char[] SPECIAL_CHARS = "`~!@#$%^&*()_+=-{}|[];':\",./<>?".toCharArray();

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
        if (StringUtils.isBlank(text)) return text;

        int textLen = text.length();
        if (textLen == 1) {
            return "*";
        } else if (textLen <= 3) {
            return text.charAt(0) + StringUtils.repeat("*", textLen - 1);
        } else {
            int len3 = Math.min(textLen / 3, 10);
            int starLen = textLen - len3 * 2;
            return text.substring(0, len3)
                    + StringUtils.repeat("*", Math.min(starLen, 20))
                    + text.substring(len3 * 2);
        }
    }

    /**
     * @param phone
     * @return
     */
    public static String starsPhone(String phone) {
        if (StringUtils.isBlank(phone)) return phone;

        if (phone.length() <= 7) {
            return phone.substring(0, 3) + StringUtils.repeat("*", phone.length() - 3);
        } else {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
    }

    /**
     * @param email
     * @return
     */
    public static String starsEmail(String email) {
        if (StringUtils.isBlank(email)) return email;

        String[] nd = email.split("@");
        int nLen = nd[0].length();
        if (nd[0].length() <= 3) {
            nd[0] = nd[0].charAt(0) + StringUtils.repeat("*", nLen - 1);
        } else {
            nd[0] = nd[0].substring(0, 3) + StringUtils.repeat("*", Math.min(nLen - 3, 20));
        }
        return nd[0] + "@" + nd[1];
    }

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
     * 获取 classpath 下的配置文件流
     *
     * @param file
     * @return
     * @see org.springframework.util.ResourceUtils#getFile(URI)
     */
    public static InputStream getStreamOfRes(String file) throws IOException {
        return new ClassPathResource(file).getInputStream();
    }

    /**
     * 获取 classpath 下的配置文件内容
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static String getStringOfRes(String file) {
        try (InputStream is = getStreamOfRes(file)) {
            return IOUtils.toString(is, "utf-8");
        } catch (IOException ex) {
            log.error("Cannot load file of res : " + file);
            return null;
        }
    }
}
