/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author devezhao
 * @since 2020/11/17
 */
@Slf4j
public class EasyText extends EasyField {
    private static final long serialVersionUID = -244546170269555223L;

    protected EasyText(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    /**
     * 文本字段正则表达式
     *
     * @return
     */
    public Pattern getPattern() {
        String patt = getExtraAttr(EasyFieldConfigProps.ADV_PATTERN);
        if (StringUtils.isNotBlank(patt)) {
            try {
                return Pattern.compile(patt);
            } catch (PatternSyntaxException ex) {
                log.warn("Invalid pattern syntax : {}. Will be ignored", patt);
            }
        }
        return null;
    }

    /**
     * 信息脱敏
     * 
     * @return
     */
    public boolean isDesensitized() {
        return BooleanUtils.toBoolean(getExtraAttr(EasyFieldConfigProps.ADV_DESENSITIZED));
    }
}
