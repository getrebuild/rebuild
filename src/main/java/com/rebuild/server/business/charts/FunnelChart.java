/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.charts;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;

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
}
