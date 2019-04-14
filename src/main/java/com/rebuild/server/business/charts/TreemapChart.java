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
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.commons.ObjectUtils;
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
		
		int lastIndex = dataRaw.length > 0 ? dataRaw[0].length - 1 : 0;
		double xAmount = 0d;
		for (int i = 0; i < dataRaw.length; i++) {
			Object o[] = dataRaw[i];
			double v = ID.isId(o[lastIndex]) ? 1d : ObjectUtils.toDouble(o[lastIndex]);
			o[lastIndex] = v;
			xAmount += v;
			
			for (int j = 0; j < o.length - 1; j++) {
				o[j] = warpAxisValue(dims[j], o[j]);
			}
		}
		
		TreeBuilder builder = new TreeBuilder(dataRaw, this);
		
		JSONObject ret = JSONUtils.toJSONObject(
				new String[] { "data", "xLabel", "xAmount" },
				new Object[] { builder.toJSON(), num1.getLabel(), xAmount });
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
}
