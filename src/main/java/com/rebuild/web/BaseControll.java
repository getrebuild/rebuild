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

package com.rebuild.web;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 基础 Controll 类
 * 
 * @author zhaofang123@gmail.com
 * @since 05/21/2017
 */
public abstract class BaseControll extends PageControll {
	
	public static final int CODE_OK = 0;
	public static final int CODE_FAIL = 1000;
	public static final int CODE_ERROR = 2000;
	
	protected static Log LOG = LogFactory.getLog(BaseControll.class);
	
	/**
	 * @param resp
	 */
	protected void writeSuccess(HttpServletResponse resp) {
		writeSuccess(resp, ObjectUtils.NULL);
	}

	/**
	 * @param resp
	 * @param record
	 */
	protected void writeSuccess(HttpServletResponse resp, Record record) {
		if (record == null) {
			writeFailure(resp, "无法找到记录");
			return;
		}
		
		Map<String, Object> data = new HashMap<String, Object>();
		for (Iterator<String> iter = record.getAvailableFieldIterator(); iter.hasNext(); ) {
			String f = iter.next();
			Object v = record.getObjectValue(f);
			if (v instanceof Date) {
				v = CalendarUtils.getUTCDateTimeFormat().format(v);
			} else if (v instanceof ID) {
				v = v.toString();
			}
			data.put(f, v);
		}
		writeSuccess(resp, data);
	}
	
	/**
	 * @param resp
	 * @param data
	 */
	protected void writeSuccess(HttpServletResponse resp, Object data) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("error_code", CODE_OK);
		map.put("error_msg", "调用成功");
		if (data != null && data != ObjectUtils.NULL) {
			// ID 类型不会 toString ???
			if (Map.class.isAssignableFrom(data.getClass())) {
				@SuppressWarnings("unchecked")
				Map<Object, Object> dataMap = (Map<Object, Object>) data;
				for (Object key : dataMap.keySet()) {
					Object value = dataMap.get(key);
					if (value != null && ID.class.isAssignableFrom(value.getClass())) {
						dataMap.put(key, value.toString());
					} else if (value != null && Date.class.isAssignableFrom(value.getClass())) {
						dataMap.put(key, CalendarUtils.getUTCDateTimeFormat().format(value));
					}
				}
			} else if (Object[][].class.isAssignableFrom(data.getClass())) {
				Object[][] array = (Object[][]) data;
				for (Object[] o : array) {
					for (int i = 0; i < o.length; i++) {
						Object value = o[i];
						if (value != null && ID.class.isAssignableFrom(value.getClass())) {
							o[i] = o[i].toString();
						} else if (value != null && Date.class.isAssignableFrom(value.getClass())) {
							o[i] = CalendarUtils.getUTCDateTimeFormat().format(o[i]);
						}
					}
				}
			} else if (Object[].class.isAssignableFrom(data.getClass())) {
				Object[] o = (Object[]) data;
				for (int i = 0; i < o.length; i++) {
					Object value = o[i];
					if (value != null && ID.class.isAssignableFrom(value.getClass())) {
						o[i] = o[i].toString();
					} else if (value != null && Date.class.isAssignableFrom(value.getClass())) {
						o[i] = CalendarUtils.getUTCDateTimeFormat().format(o[i]);
					}
				}
			}
			map.put("data", data);
		}
		writeJSON(resp, map);
	}
	
	/**
	 * @param resp
	 */
	protected void writeFailure(HttpServletResponse resp) {
		writeFailure(resp, null);
	}
	
	/**
	 * @param resp
	 * @param message
	 */
	protected void writeFailure(HttpServletResponse resp, String message) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("error_code", CODE_FAIL);
		map.put("error_msg", message == null ? "无效请求" : message);
		writeJSON(resp, map);
	}
	
	/**
	 * @param resp
	 * @param json
	 */
	protected void writeJSON(HttpServletResponse resp, Object json) {
		if (json == null) {
			throw new IllegalArgumentException();
		}
		
		String aJSONString = null;
		if (json instanceof String) {
			aJSONString = (String) json;
		} else {
			aJSONString = JSON.toJSONString(json);
		}
		ServletUtils.writeJson(resp, aJSONString);
	}
	
	/**
	 * @param req
	 * @param name
	 * @return
	 */
	protected String getParameter(HttpServletRequest req, String name) {
		return req.getParameter(name);
	}
	
	/**
	 * @param req
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	protected String getParameter(HttpServletRequest req, String name, String defaultValue) {
		return StringUtils.defaultIfBlank(getParameter(req, name), defaultValue);
	}
	
	/**
	 * @param req
	 * @param name
	 * @return
	 */
	protected String getParameterNotNull(HttpServletRequest req, String name) {
		String v = req.getParameter(name);
		if (StringUtils.isEmpty(v)) {
			throw new InvalidRequestException("无效参数 [" + name + "=" + v + "]");
		}
		return v;
	}
	
	/**
	 * @param req
	 * @param name
	 * @return
	 */
	protected Integer getIntParameter(HttpServletRequest req, String name) {
		String v = req.getParameter(name);
		if (v == null) {
			return null;
		}
		return NumberUtils.toInt(v);
	}
	
	/**
	 * @param req
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	protected Integer getIntParameter(HttpServletRequest req, String name, int defaultValue) {
		Integer v = getIntParameter(req, name);
		return v == null ? (defaultValue) : v;
	}
	
	/**
	 * @param req
	 * @param name
	 * @return
	 */
	protected ID getIdParameter(HttpServletRequest req, String name) {
		String v = req.getParameter(name);
		return ID.isId(v) ? ID.valueOf(v) : null;
	}
	
	/**
	 * @param req
	 * @param name
	 * @return
	 */
	protected ID getIdParameterNotNull(HttpServletRequest req, String name) {
		String v = req.getParameter(name);
		if (ID.isId(v)) {
			return ID.valueOf(v);
		}
		throw new InvalidRequestException("无效ID参数 [" + name + "=" + v + "]");
	}
}
