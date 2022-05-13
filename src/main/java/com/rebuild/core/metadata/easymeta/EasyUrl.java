/*!
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
public class EasyUrl extends EasyText {
    private static final long serialVersionUID = -1196837593872219949L;

    protected EasyUrl(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Pattern getPattern() {
        Pattern patt = super.getPattern();
        return patt == null ? RegexUtils.URL_PATTERN : patt;
    }

    /**
     * @param url
     * @return
     */
    public static boolean isUrl(String url) {
        return url != null && RegexUtils.URL_PATTERN.matcher(url).matches();
    }
}
