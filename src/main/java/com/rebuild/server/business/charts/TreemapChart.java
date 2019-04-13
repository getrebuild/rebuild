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

package com.rebuild.server.business.charts;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.persist4j.engine.ID;

/**
 * 树图 TODO
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/13
 */
public class TreemapChart extends ChartData {

	protected TreemapChart(JSONObject config, ID user) {
		super(config, user);
	}

	@Override
	public JSON build() {
		Dimension[] dims = getDimensions();
		Numerical[] nums = getNumericals();
		
		Numerical num1 = nums[0];
		Object[][] dataRaw = Application.createQuery(buildSql(dims, num1), user).array();
		
		JSONArray treeJson = new JSONArray();
		double xAmount = 0d;
		for (Object[] o : dataRaw) {
			o[0] = warpAxisValue(dims[0], o[0]);
			o[1] = warpAxisValue(num1, o[1]);
			JSONObject d = JSONUtils.toJSONObject(new String[] { "name", "value" }, o);
			treeJson.add(d);
			
			double v = Double.parseDouble(((String) o[1]).replaceAll(",", ""));
			xAmount += v;
		}
		
		JSONObject ret = JSONUtils.toJSONObject(
				new String[] { "data", "xLabel", "xAmount" },
				new Object[] { treeJson, num1.getLabel(), xAmount });
		return ret;
	}
	
	@Override
	public Numerical[] getNumericals() {
		Numerical[] nums = super.getNumericals();
		if (nums.length == 0) {
			return new Numerical[] { new Numerical(getSourceEntity().getPrimaryField()) };
		}
		return nums;
	}
	
	protected String buildSql(Dimension[] dims, Numerical num) {
		List<String> dimSqlItems = new ArrayList<>();
		for (Dimension dim : dims) {
			dimSqlItems.add(dim.getSqlName());
		}
		
		String sql = "select {0},{1} from {2} where {3} group by {0}";
		String where = getFilterSql();
		
		sql = MessageFormat.format(sql, 
				StringUtils.join(dimSqlItems, ", "),
				num.getSqlName(),
				getSourceEntity().getName(),
				where);
		return sql;
	}
	
	private void buildTreeNode(Dimension[] dims, Numerical num, JSON parent) {
	}
}
