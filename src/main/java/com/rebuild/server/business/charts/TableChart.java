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

package com.rebuild.server.business.charts;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.persist4j.engine.ID;

/**
 * 表格
 * 
 * @author devezhao
 * @since 12/15/2018
 */
public class TableChart extends ChartData {

	protected TableChart(JSONObject config, ID user) {
		super(config, user);
	}

	@Override
	public JSON build() {
		Dimension[] dims = getDimensions();
		Numerical[] nums = getNumericals();
		
		String sql = buildSql(dims, nums);
		Object[][] dataRaw = Application.createQuery(sql, user).array();
		
		String tableHtml = new TableBuilder(this, dataRaw).toHTML();
		
		JSONObject ret = JSONUtils.toJSONObject(
				new String[] { "html" },
				new Object[] { tableHtml });
		return ret;
	}
	
	protected String buildSql(Dimension[] dims, Numerical[] nums) {
		List<String> dimSqlItems = new ArrayList<>();
		for (Dimension dim : dims) {
			dimSqlItems.add(dim.getSqlName());
		}
		
		List<String> numSqlItems = new ArrayList<>();
		for (Numerical num : nums) {
			numSqlItems.add(num.getSqlName());
		}
		
		String sql = "select {0},{1} from {2} where {3} group by {0}";
		String where = getFilterSql();
		
		if (dimSqlItems.isEmpty()) {
			sql = "select {1} from {2} where {3}";
		}
		
		sql = MessageFormat.format(sql,
				StringUtils.join(dimSqlItems, ", "),
				StringUtils.join(numSqlItems, ", "),
				getSourceEntity().getName(), where);
		return sql;
	}
}
