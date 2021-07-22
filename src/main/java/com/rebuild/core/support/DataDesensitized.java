/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import org.apache.commons.lang.StringUtils;

/**
 * 信息脱敏
 *
 * @author devezhao
 * @since 2020/12/22
 */
public class DataDesensitized {

    /**
     * 给敏感文本加星号/打码
     *
     * @param text
     * @return
     */
    public static String any(String text) {
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
                    + text.substring(textLen - len3 * 2);
        }
    }

    /**
     * @param phone
     * @return
     */
    public static String phone(String phone) {
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
    public static String email(String email) {
        if (StringUtils.isBlank(email)) return email;
        if (!email.contains("@")) return any(email);

        String[] nd = email.split("@");
        int nLen = nd[0].length();
        if (nd[0].length() <= 3) {
            nd[0] = nd[0].charAt(0) + StringUtils.repeat("*", nLen - 1);
        } else {
            nd[0] = nd[0].substring(0, 3) + StringUtils.repeat("*", Math.min(nLen - 3, 20));
        }
        return nd[0] + "@" + nd[1];
    }
}
