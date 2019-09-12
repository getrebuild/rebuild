/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.utils;

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
		if (object == null || NullValue.is(object)) {
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
