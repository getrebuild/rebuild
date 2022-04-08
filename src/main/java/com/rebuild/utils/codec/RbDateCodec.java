/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils.codec;

import cn.devezhao.commons.DateFormatUtils;
import cn.devezhao.persist4j.engine.NullValue;
import com.alibaba.fastjson.serializer.DateCodec;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.util.TypeUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;

/**
 * {@link Date} JSON 编码
 *
 * @author devezhao
 * @since 01/09/2019
 */
public class RbDateCodec extends DateCodec {

    public final static RbDateCodec instance = new RbDateCodec();

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features)
            throws IOException {
        SerializeWriter out = serializer.out;
        if (NullValue.isNull(object)) {
            out.writeNull();
            return;
        }

        Date date;
        if (object instanceof Date) {
            date = (Date) object;
        } else {
            date = TypeUtils.castToDate(object);
        }

        String text = DateFormatUtils.getUTCDateTimeFormat().format(date);
        out.writeString(text);
    }
}
