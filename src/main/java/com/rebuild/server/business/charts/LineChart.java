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
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 曲线图
 * 
 * @author devezhao
 * @since 12/15/2018
 */
public class LineChart extends ChartData {

	protected LineChart(JSONObject config) {
		super(config);
	}

	@Override
	public JSON build() {
		Dimension[] dims = getDimensions();
		Numerical[] nums = getNumericals();
		
		Dimension dim1 = dims[0];
		Object[][] dataRaw = createQuery(buildSql(dim1, nums)).array();
		
		List<String> dimAxis = new ArrayList<>();
		Object[] numsAxis = new Object[nums.length];
		for (Object[] o : dataRaw) {
			dimAxis.add(wrapAxisValue(dim1, o[0]));
			
			for (int i = 0; i < nums.length; i++) {
				@SuppressWarnings("unchecked")
				List<String> numAxis = (List<String>) numsAxis[i];
				if (numAxis == null) {
					numAxis = new ArrayList<>();
					numsAxis[i] = numAxis;
				}
				numAxis.add(wrapAxisValue(nums[i], o[i + 1]));
			}
		}
		
		JSONArray yyyAxis = new JSONArray();
		for (int i = 0; i < nums.length; i++) {
			Numerical axis = nums[i];
			@SuppressWarnings("unchecked")
			List<String> data = (List<String>) numsAxis[i];
			
			JSONObject map = new JSONObject();
			map.put("name", axis.getLabel());
			map.put("data", data);
			yyyAxis.add(map);
		}
		
		JSONObject ret = JSONUtils.toJSONObject(
				new String[] { "xAxis", "yyyAxis" },
				new Object[] { JSON.toJSON(dimAxis), JSON.toJSON(yyyAxis) });
		return ret;
	}
	
	protected String buildSql(Dimension dim, Numerical[] nums) {
		List<String> numSqlItems = new ArrayList<>();
		for (Numerical num : nums) {
			numSqlItems.add(num.getSqlName());
		}
		
		String sql = "select {0},{1} from {2} where {3} group by {0}";
		sql = MessageFormat.format(sql, 
				dim.getSqlName(),
				StringUtils.join(numSqlItems, ", "),
				getSourceEntity().getName(), getFilterSql());
		
		String sorts = getSortSql();
		if (sorts != null) {
			sql += " order by " + sorts;
		}
		return sql;
	}
}
