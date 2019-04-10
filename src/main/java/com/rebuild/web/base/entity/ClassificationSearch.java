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

import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.web.BaseControll;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * TODO
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/03
 */
@Controller
@RequestMapping("/commons/search/")
public class ClassificationSearch extends BaseControll {
	
	@RequestMapping("classification")
	public void search(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		String field = getParameterNotNull(request, "field");
		ID parent = getIdParameter(request, "parent");
		
		Field fieldMeta = MetadataHelper.getEntity(entity).getField(field);
		EasyMeta fieldEasy = EasyMeta.valueOf(fieldMeta);
		JSONObject extConfig = fieldEasy.getFieldExtConfig();
		String dataId = extConfig.getString("classification");
		if (!ID.isId(dataId)) {
			writeFailure(response, "分类字段配置有误");
			return;
		}
		
		String sql = "select itemId,name from ClassificationData where dataId = ? and ";
		if (parent != null) {
			sql += "parent = '" + parent + "'";
		} else {
			sql += "parent is null";
		}
		sql += " order by code, name";
		Object[][] data = Application.createQueryNoFilter(sql).setParameter(1, ID.valueOf(dataId)).array();
		
		writeSuccess(response, data);
	}
}