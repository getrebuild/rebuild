/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
			Numerical d = new Numerical(getSourceEntity().getPrimaryField(), FormatSort.NONE, FormatCalc.COUNT, null, 0, null);
			return new Numerical[] { d };
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
