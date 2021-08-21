/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.commons.RegexUtils;
import cn.devezhao.persist4j.Field;

import java.util.regex.Pattern;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyEmail extends EasyText {
    private static final long serialVersionUID = -3601935952056036314L;

    protected EasyEmail(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Pattern getPattern() {
        Pattern patt = super.getPattern();
        return patt == null ? RegexUtils.EMAIL_PATTERN : patt;
    }

    /**
     * @param email
     * @return
     */
    public static boolean isEmail(String email) {
        return email != null && RegexUtils.EMAIL_PATTERN.matcher(email).matches();
    }
}
