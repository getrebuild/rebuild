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

package com.rebuild.web.base.entity;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.helper.portals.AdvFilterManager;
import com.rebuild.server.helper.portals.SharableManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.query.AdvFilterParser;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import com.rebuild.web.PortalsConfiguration;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 高级查询
 * 
 * @author devezhao
 * @since 10/14/2018
 */
@Controller
@RequestMapping("/app/{entity}/")
public class AdvFilterControll extends BaseControll implements PortalsConfiguration {

	@RequestMapping("advfilter/post")
	@Override
	public void sets(@PathVariable String entity, 
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		ID filterId = getIdParameter(request, "id");
		if (filterId != null) {
			if (UserHelper.isAdmin(user) || AdvFilterManager.isSelf(user, filterId)) {
				// Okay
			} else {
				writeFailure(response, "无权修改此过滤项");
				return;
			}
		}
		
		String filterName = getParameter(request, "name");
		JSON filter = ServletUtils.getRequestJson(request);
		
		boolean toAll = getBoolParameter(request, "toAll", false);
		if (toAll) {
			toAll = UserHelper.isAdmin(user);
		}
		
		Record record = null;
		if (filterId == null) {
			record = EntityHelper.forNew(EntityHelper.FilterConfig, user);
			record.setString("belongEntity", entity);
			if (StringUtils.isBlank(filterName)) {
				filterName = "过滤项-" + CalendarUtils.getPlainDateFormat().format(CalendarUtils.now());
			}
		} else {
			record = EntityHelper.forUpdate(filterId, user);
		}
		
		if (StringUtils.isNotBlank(filterName)) {
			record.setString("filterName", filterName);
		}
		
		record.setString("config", filter.toJSONString());
		record.setString("shareTo", toAll ? SharableManager.SHARE_ALL : SharableManager.SHARE_SELF);
		Application.getCommonService().createOrUpdate(record);
		
		writeSuccess(response);
	}
	
	@RequestMapping("advfilter/get")
	@Override
	public void gets(@PathVariable String entity, 
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID filterId = getIdParameter(request, "id");
		Object[] filter = AdvFilterManager.getAdvFilter(filterId);
		if (filter == null) {
			writeFailure(response, "无效过滤条件");
		} else {
			JSON ret = JSONUtils.toJSONObject(
					new String[] { "id", "filter", "name", "shareTo" }, filter);
			writeSuccess(response, ret);
		}
	}
	
	@RequestMapping("advfilter/delete")
	public void delete(@PathVariable String entity, 
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		ID filterId = getIdParameter(request, "id");
		if (UserHelper.isAdmin(user) || AdvFilterManager.isSelf(user, filterId)) {
			// Okay
		} else {
			writeFailure(response, "无权删除此过滤项");
			return;
		}
		
		Application.getCommonService().delete(filterId);
		writeSuccess(response);
	}
	
	@RequestMapping("advfilter/list")
	public void list(@PathVariable String entity, 
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		Object[][] filters = AdvFilterManager.getAdvFilterList(entity, user);
		writeSuccess(response, filters);
	}
	
	@RequestMapping("advfilter/test-parse")
	public void testAdvfilter(HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSON advfilter = ServletUtils.getRequestJson(request);
		try {
			AdvFilterParser filterParser = new AdvFilterParser((JSONObject) advfilter);
			String sql = filterParser.toSqlWhere();
			writeSuccess(response, sql);
		} catch (Exception ex) {
			writeFailure(response, "语法错误:" + ex.getLocalizedMessage());
		}
	}
}
