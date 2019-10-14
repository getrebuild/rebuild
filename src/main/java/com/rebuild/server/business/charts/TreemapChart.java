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

package com.rebuild.server.business.charts;

import cn.devezhao.commons.ObjectUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 树图
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/13
 */
public class TreemapChart extends ChartData {

	protected TreemapChart(JSONObject config) {
		super(config);
	}

	@Override
	public JSON build() {
		Dimension[] dims = getDimensions();
		Numerical[] nums = getNumericals();
		
		Numerical num1 = nums[0];
		Object[][] dataRaw = createQuery(buildSql(dims, num1)).array();
		
		int lastIndex = dataRaw.length > 0 ? dataRaw[0].length - 1 : 0;
		double xAmount = 0d;
        for (Object[] o : dataRaw) {
            double v = ObjectUtils.toDouble(o[lastIndex]);
            o[lastIndex] = v;
            xAmount += v;

            for (int j = 0; j < o.length - 1; j++) {
                o[j] = wrapAxisValue(dims[j], o[j]);
            }
        }
		
		TreeBuilder builder = new TreeBuilder(dataRaw, this);

        return JSONUtils.toJSONObject(
                new String[] { "data", "xLabel", "xAmount" },
                new Object[] { builder.toJSON(), num1.getLabel(), xAmount });
	}
	
	@Override
	public Numerical[] getNumericals() {
		Numerical[] nums = super.getNumericals();
		if (nums.length == 0) {
			return new Numerical[] {
					new Numerical(getSourceEntity().getPrimaryField(), FormatSort.NONE, FormatCalc.COUNT, null, 0) };
		}
		return nums;
	}
	
	protected String buildSql(Dimension[] dims, Numerical num) {
		List<String> dimSqlItems = new ArrayList<>();
		for (Dimension dim : dims) {
			dimSqlItems.add(dim.getSqlName());
		}
		
		String sql = "select {0},{1} from {2} where {3} group by {0}";
		sql = MessageFormat.format(sql, 
				StringUtils.join(dimSqlItems, ", "),
				num.getSqlName(),
				getSourceEntity().getName(),
				getFilterSql());
		return sql;
	}
}
