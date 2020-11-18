/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import org.springframework.util.Assert;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyFile extends EasyField implements MixValue {
    private static final long serialVersionUID = -440245863103271478L;

    protected EasyFile(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        Assert.isTrue(targetField.getDisplayType() == getDisplayType(), "type-by-type is must");
        return value;
    }

    @Override
    public Object wrapValue(Object value) {
        return JSON.parseArray(value.toString());
    }

    @Override
    public Object unpackWrapValue(Object wrappedValue) {
        return wrappedValue.toString();
    }

    /**
     * 上传数量限制，如 3,6
     *
     * @return
     */
    public String attrUploadNumber() {
        return getExtraAttr(EasyFieldConfigProps.FILE_UPLOADNUMBER);
    }
}
