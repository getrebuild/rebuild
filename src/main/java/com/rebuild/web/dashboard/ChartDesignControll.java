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

package com.rebuild.web.dashboard;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.web.BaseControll;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;

/**
 * 
 * @author devezhao
 * @since 12/09/2018
 */
@Controller
@RequestMapping("/dashboard")
public class ChartDesignControll extends BaseControll {

	@RequestMapping("/chart-design")
	public ModelAndView pageHome(HttpServletRequest request) {
		ModelAndView mv = createModelAndView("/dashboard/chart-design.jsp");
		
		String entity = getParameterNotNull(request, "source");
		Entity entityMeta = MetadataHelper.getEntity(entity);
		putEntityMeta(mv, entityMeta);

		List<String[]> fields = new ArrayList<>();
		for (Field field : MetadataSorter.sortFields(entityMeta)) {
			EasyMeta easy = EasyMeta.valueOf(field);
			DisplayType dt = easy.getDisplayType();
			String type = "text";
			if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
				type = "date";
			} else if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
				type = "num";
			}
			fields.add(new String[] { easy.getName(), easy.getLabel(), type });
		}
		mv.getModel().put("fields", fields);
		
		return mv;
	}
	
}
