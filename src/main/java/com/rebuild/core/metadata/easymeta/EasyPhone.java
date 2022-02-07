/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;

import java.util.regex.Pattern;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyPhone extends EasyText {
    private static final long serialVersionUID = 347745040979570855L;

    protected EasyPhone(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Pattern getPattern() {
        Pattern patt = super.getPattern();
        return patt == null ? PATT_PHONE : patt;
    }

    // --

    // 兼容电话、手机，国际区号
    public static Pattern PATT_PHONE = Pattern.compile(
            "((\\(\\d{1,5}\\))?(\\d{3,4}-)?\\d{7,8}(-\\d{1,6})?)|(1[356789]\\d{9})");

    /**
     * @param phone
     * @return
     */
    public static boolean isPhone(String phone) {
        return phone != null && PATT_PHONE.matcher(phone).matches();
    }
}
