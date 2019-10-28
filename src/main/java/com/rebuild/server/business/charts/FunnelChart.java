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
 * 漏斗图
 * 
 * @author devezhao
 * @since 12/15/2018
 */
public class FunnelChart extends ChartData {

	protected FunnelChart(JSONObject config) {
		super(config);
	}

	@Override
	public JSON build() {
		Dimension[] dims = getDimensions();
		Numerical[] nums = getNumericals();
		
		JSONArray dataJson = new JSONArray();
		if (nums.length > 1) {
			Object[] dataRaw = createQuery(buildSql(nums)).unique();
			for (int i = 0; i < nums.length; i++) {
				JSONObject d = JSONUtils.toJSONObject(
						new String[] { "name", "value" },
						new Object[] { nums[i].getLabel(), wrapAxisValue(nums[i], dataRaw[i]) });
				dataJson.add(d);
			}
		} else if (nums.length >= 1 && dims.length >= 1) {
			Dimension dim1 = dims[0];
			Object[][] dataRaw = createQuery(buildSql(dim1, nums[0])).array();
			for (Object[] o : dataRaw) {
				JSONObject d = JSONUtils.toJSONObject(
						new String[] { "name", "value" },
						new Object[] { o[0] = wrapAxisValue(dim1, o[0]), wrapAxisValue(nums[0], o[1]) });
				dataJson.add(d);
			}
			
			if (dim1.getFormatSort() != FormatSort.NONE) {
				dataJson.sort((a, b) -> {
					String aName = ((JSONObject) a).getString("name");
					String bName = ((JSONObject) b).getString("name");
					if (dim1.getFormatSort() == FormatSort.ASC) {
						return aName.compareTo(bName);
					} else {
						return bName.compareTo(aName);
					}
				});
			}
		}
		
		JSONObject ret = JSONUtils.toJSONObject(
				new String[] { "data" },
				new Object[] { dataJson });
		if (nums.length >= 1 && dims.length >= 1) {
			ret.put("xLabel", nums[0].getLabel());
		}
		return ret;
	}
	
	protected String buildSql(Numerical[] nums) {
		List<String> numSqlItems = new ArrayList<>();
		for (Numerical num : nums) {
			numSqlItems.add(num.getSqlName());
		}
		
		String sql = "select {0} from {1} where {2}";
		sql = MessageFormat.format(sql,
				StringUtils.join(numSqlItems, ", "),
				getSourceEntity().getName(), getFilterSql());
		return sql;
	}
	
	protected String buildSql(Dimension dim, Numerical num) {
		String sql = "select {0},{1} from {2} where {3} group by {0}";
		sql = MessageFormat.format(sql, 
				dim.getSqlName(),
				num.getSqlName(),
				getSourceEntity().getName(), getFilterSql());
		return sql;
	}
}
