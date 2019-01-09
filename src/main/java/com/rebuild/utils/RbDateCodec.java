/*
rebuild - Building your system freely.
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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.serializer.DateCodec;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.util.TypeUtils;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.DateFormatUtils;

/**
 * @author devezhao
 * @since 01/09/2019
 */
public class RbDateCodec extends DateCodec {

	public final static RbDateCodec instance = new RbDateCodec();

	@Override
	public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features)
			throws IOException {
		SerializeWriter out = serializer.out;
		if (object == null) {
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

	@SuppressWarnings("unchecked")
	protected <T> T cast(DefaultJSONParser parser, Type clazz, Object fieldName, Object val) {
		if (val == null) {
			return null;
		}

		if (val instanceof java.util.Date) {
			return (T) val;
		} else if (val instanceof Number) {
			return (T) new java.util.Date(((Number) val).longValue());
		} else if (val instanceof String) {
			Date date = CalendarUtils.parse((String) val);
			return (T) date;
		}
		throw new JSONException("parse error");
	}
}
