/*
rebuild - Building your business-systems freely.
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

import com.alibaba.fastjson.JSON;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.portals.AutoFillinManager;
import com.rebuild.web.BaseControll;

import cn.devezhao.persist4j.engine.ID;

/**
 * 表单功能扩展
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/20
 */
@Controller
@RequestMapping("/app/entity/extras/")
public class FormExtrasControll extends BaseControll {
	
	// --
	// -- AUTOFILLIN
	
	@RequestMapping("fillin-value")
	public void getFillinValue(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		String field = getParameterNotNull(request, "field");
		ID source = getIdParameterNotNull(request, "source");
		
		JSON ret = AutoFillinManager.getFillinValue(MetadataHelper.getField(entity, field), source);
		writeSuccess(response, ret);
	}
}
