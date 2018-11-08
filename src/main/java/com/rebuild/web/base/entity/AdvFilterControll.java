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

package com.rebuild.web.base.entity;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.query.AdvFilterParser;
import com.rebuild.web.BaseControll;
import com.rebuild.web.LayoutConfig;

import cn.devezhao.commons.web.ServletUtils;

/**
 * 高级查询
 * 
 * @author devezhao
 * @since 10/14/2018
 */
@Controller
@RequestMapping("/app/{entity}/")
public class AdvFilterControll extends BaseControll implements LayoutConfig {

	@Override
	public void sets(String entity, HttpServletRequest request, HttpServletResponse response) throws IOException {
	}
	
	@Override
	public void gets(String entity, HttpServletRequest request, HttpServletResponse response) throws IOException {
	}
	
	@RequestMapping("test-advfilter")
	public void testAdvfilter(HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSON advfilter = ServletUtils.getRequestJson(request);
		
		AdvFilterParser filterParser = new AdvFilterParser((JSONObject) advfilter);
		String sql = filterParser.toSqlWhere();
		writeSuccess(response, sql);
	}
}
