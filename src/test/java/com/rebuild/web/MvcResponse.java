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

package com.rebuild.web;

import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 
 * @author devezhao
 * @since 01/14/2019
 */
public class MvcResponse {
	
	final private JSONObject response;

	protected MvcResponse(String content) {
		this.response = JSONObject.parseObject(content);
	}
	
	protected MvcResponse(ModelAndView view) {
		this.response = JSONObject.parseObject("{ error_code:0 }");
		this.response.put("error_msg", view);
	}
	
	public boolean isSuccess() {
		return response.getInteger("error_code") == 0;
	}
	
	public String getErrorMsg() {
		return response.getString("error_msg");
	}
	
	public Object getDataSimple() {
		return response.get("data");
	}
	
	public JSONArray getDataJSONArray() {
		return response.getJSONArray("data");
	}
	
	public JSONObject getDataJSONObject() {
		return response.getJSONObject("data");
	}
	
	@Override
	public String toString() {
		return response.toJSONString();
	}
}
