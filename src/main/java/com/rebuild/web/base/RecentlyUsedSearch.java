/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.base;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.helper.cache.RecentlyUsedCache;
import com.rebuild.server.helper.fieldvalue.FieldValueWrapper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 最近搜索（针对引用字段）。
 * 非自动，需要调用 <tt>recently-add</tt> 方法手动添加方可用，后期考虑自动化
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/25
 */
@Controller
@RequestMapping("/commons/search/")
public class RecentlyUsedSearch extends BaseControll {
	
	@RequestMapping("recently")
	public void fetchRecently(HttpServletRequest request, HttpServletResponse response) {
		String entity = getParameterNotNull(request, "entity");
		String type = getParameter(request, "type");
		ID[] recently = cache().gets(getRequestUser(request), entity, type);
		writeSuccess(response, formatSelect2(recently, "最近使用"));
	}
	
	@RequestMapping("recently-add")
	public void addRecently(HttpServletRequest request, HttpServletResponse response) {
		ID id = getIdParameterNotNull(request, "id");
		String type = getParameter(request, "type");
		cache().addOne(getRequestUser(request),id, type);
		writeSuccess(response);
	}
	
	@RequestMapping("recently-clean")
	public void cleanRecently(HttpServletRequest request, HttpServletResponse response) {
		String entity = getParameterNotNull(request, "entity");
		String type = getParameter(request, "type");
		cache().clean(getRequestUser(request),entity, type);
		writeSuccess(response);
	}
	
	/**
	 * 格式化成前端 select2 组件数据格式
	 * 
	 * @param idLabels
	 * @param groupName select2 分组 null 表示无分组
	 * @return
	 */
	protected static JSONArray formatSelect2(ID[] idLabels, String groupName) {
		JSONArray data = new JSONArray();
		for (ID id : idLabels) {
			data.add(JSONUtils.toJSONObject(
					new String[] { "id", "text" }, 
					new String[] { id.toLiteral(), 
							StringUtils.defaultIfBlank(id.getLabel(), FieldValueWrapper.NO_LABEL_PREFIX + id.toLiteral().toUpperCase()) }));
		}
		
		if (groupName != null) {
			JSONObject group = new JSONObject();
			group.put("text", groupName);
			group.put("children", data);
			
			JSONArray array = new JSONArray();
			array.add(group);
			data = array;
		}
		return data;
	}

	/**
	 * @return
	 */
	protected static RecentlyUsedCache cache() {
		return Application.getBean(RecentlyUsedCache.class);
	}
}
