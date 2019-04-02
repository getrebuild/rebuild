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

package com.rebuild.web.admin.entityhub;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 分类数据管理
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/27
 */
@Controller
@RequestMapping("/admin/")
public class ClassificationControll extends BasePageControll {
	
	@RequestMapping("classifications")
	public ModelAndView pageList(HttpServletRequest request) throws IOException {
		Object[][] array = Application.createQuery(
				"select dataId,name,description from Classification order by name")
				.array();
		
		ModelAndView mv = createModelAndView("/admin/entityhub/classification/list.jsp");
		mv.getModel().put("classifications", array);
		return mv;
	}
	
	@RequestMapping("classification/{id}")
	public ModelAndView pageData(@PathVariable String id,
			HttpServletRequest request, HttpServletResponse resp) throws IOException {
		Object[] data = Application.createQuery(
				"select name,openLevel from Classification where dataId = ?")
				.setParameter(1, ID.valueOf(id))
				.unique();
		if (data == null) {
			resp.sendError(404, "分类数据不存在");
			return null;
		}
		
		ModelAndView mv = createModelAndView("/admin/entityhub/classification/editor.jsp");
		mv.getModel().put("dataId", id);
		mv.getModel().put("name", data[0]);
		mv.getModel().put("openLevel", data[1]);
		return mv;
	}
	
	@RequestMapping("classification/list")
	public void list(HttpServletRequest request, HttpServletResponse resp) throws IOException {
		Object[][] array = Application.createQuery(
				"select dataId,name,description from Classification where isDisabled = 'F' order by name")
				.array();
		JSON ret = JSONUtils.toJSONArray(new String[] { "dataId", "name", "description" }, array);
		writeSuccess(resp, ret);
	}
	
	@RequestMapping("classification/info")
	public void info(HttpServletRequest request, HttpServletResponse resp) throws IOException {
		ID dataId = getIdParameterNotNull(request, "id");
		Object[] data = Application.createQuery(
				"select name from Classification where dataId = ?")
				.setParameter(1, dataId)
				.unique();
		if (data == null) {
			writeFailure(resp, "分类数据不存在");
			return;
		}
		writeSuccess(resp, JSONUtils.toJSONObject("name", data[0]));
	}
	
	@RequestMapping("classification/save-data-item")
	public void saveDataItem(HttpServletRequest request, HttpServletResponse resp) throws IOException {
		ID user = getRequestUser(request);
		ID itemId = getIdParameter(request, "item_id");
		ID dataId = getIdParameter(request, "data_id");
		ID parent = getIdParameter(request, "parent");
		
		Record item = null;
		if (itemId != null) {
			item = EntityHelper.forUpdate(itemId, user);
		} else if (dataId != null) {
			item = EntityHelper.forNew(EntityHelper.ClassificationData, user);
			item.setID("dataId", dataId);
			if (parent != null) {
				item.setID("parent", parent);
			}
		} else {
			writeFailure(resp, "无效参数");
			return;
		}
		
		String code = getParameter(request, "code");
		String name = getParameter(request, "name");
		
		if (StringUtils.isNotBlank(code)) {
			item.setString("code", code);
		}
		if (StringUtils.isNotBlank(name)) {
			item.setString("name", name);
		}
		item = Application.getCommonService().createOrUpdate(item);
		
		writeSuccess(resp, item.getPrimary());
	}
	
	@RequestMapping("classification/load-data-items")
	public void loadDataItems(HttpServletRequest request, HttpServletResponse resp) throws IOException {
		ID dataId = getIdParameter(request, "data_id");
		ID parent = getIdParameter(request, "parent");
		
		Object[][] child = null;
		if (parent != null) {
			child = Application.createQuery(
					"select itemId,name,code from ClassificationData where parent = ? order by name")
					.setParameter(1, parent)
					.array();
		} else if (dataId != null) {
			child = Application.createQuery(
					"select itemId,name,code from ClassificationData where dataId = ? and parent is null order by name")
					.setParameter(1, dataId)
					.array();
		} else {
			writeFailure(resp, "无效参数");
			return;
		}
		
		writeSuccess(resp, child);
	}
}
