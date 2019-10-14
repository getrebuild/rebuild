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

package com.rebuild.server.business.charts;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;

import java.text.MessageFormat;

/**
 * 指标卡
 * 
 * @author devezhao
 * @since 12/14/2018
 */
public class IndexChart extends ChartData {
	
	protected IndexChart(JSONObject config) {
		super(config);
	}

	@Override
	public JSON build() {
		Numerical[] nums = getNumericals();

		Numerical axis = nums[0];
		Object[] dataRaw = createQuery(buildSql(axis)).unique();
		
		JSONObject index = JSONUtils.toJSONObject(
				new String[] { "data", "label" },
				new Object[] { wrapAxisValue(axis, dataRaw[0]), axis.getLabel() });

        return JSONUtils.toJSONObject("index", index);
	}
	
	protected String buildSql(Numerical axis) {
		String sql = "select {0} from {1} where {2}";
		String where = getFilterSql();
		
		sql = MessageFormat.format(sql, 
				axis.getSqlName(),
				getSourceEntity().getName(), where);
		return sql;
	}
}
