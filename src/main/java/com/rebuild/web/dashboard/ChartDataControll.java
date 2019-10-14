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

package com.rebuild.web.dashboard;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.business.charts.ChartData;
import com.rebuild.server.business.charts.ChartsException;
import com.rebuild.server.business.charts.ChartsFactory;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 
 * @author devezhao
 * @since 12/15/2018
 */
@Controller
@RequestMapping("/dashboard")
public class ChartDataControll extends BaseControll {

	@RequestMapping("/chart-data")
	public void data(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID chartid = getIdParameterNotNull(request, "id");
		JSON data = null;
		try {
			ChartData chart = ChartsFactory.create(chartid);
			data = chart.build();
		} catch (ChartsException ex) {
			writeFailure(response, ex.getLocalizedMessage());
			return;
		}
		
		writeSuccess(response, data);
	}
}
