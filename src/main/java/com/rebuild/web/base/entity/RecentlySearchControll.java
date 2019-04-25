/*
rebuild - Building your system freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.web.base.entity;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;

import cn.devezhao.persist4j.engine.ID;

/**
 * 最近搜索（针对引用字段）。
 * 非自动，需要调用 <tt>recently-add</tt> 方法手动添加方可用，后期考虑自动化
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/25
 */
@Controller
@RequestMapping("/commons/search/")
public class RecentlySearchControll extends BaseControll {
	
	@RequestMapping("recently")
	public void fetchRecently(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		String type = getParameter(request, "type");
		ID recently[] = Application.getRecentlySearchCache().gets(getRequestUser(request), entity, type);
		writeSuccess(response, formatSelect2(recently, true));
	}
	
	@RequestMapping("recently-add")
	public void addRecently(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID id = getIdParameterNotNull(request, "id");
		String type = getParameter(request, "type");
		Application.getRecentlySearchCache().addOne(getRequestUser(request),id, type);
		writeSuccess(response);
	}
	
	@RequestMapping("recently-clean")
	public void cleanRecently(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		String type = getParameter(request, "type");
		Application.getRecentlySearchCache().clean(getRequestUser(request),entity, type);
		writeSuccess(response);
	}
	
	/**
	 * 格式化成前端 select2 组件数据格式
	 * 
	 * @param recently
	 * @param useGroup
	 * @return
	 */
	protected static JSONArray formatSelect2(ID[] recently, boolean useGroup) {
		JSONArray data = new JSONArray();
		for (ID id : recently) {
			data.add(JSONUtils.toJSONObject(
					new String[] { "id", "text" }, 
					new String[] { id.toLiteral(), id.getLabel() }));
		}
		
		if (useGroup) {
			JSONObject group = new JSONObject();
			group.put("text", "最近使用");
			group.put("children", data);
			
			JSONArray array = new JSONArray();
			array.add(group);
			data = array;
		}
		
		return data;
	}
}
