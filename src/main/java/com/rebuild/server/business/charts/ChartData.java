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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.helper.manager.FieldValueWrapper;
import com.rebuild.server.helper.manager.PickListManager;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.server.query.AdvFilterParser;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * 图表数据
 * 
 * @author devezhao
 * @since 12/14/2018
 */
public abstract class ChartData {
	
	protected JSONObject config;
	
	/**
	 * @param config
	 */
	protected ChartData(JSONObject config) {
		this.config = config;
	}
	
	/**
	 * 源实体
	 * 
	 * @return
	 */
	public Entity getSourceEntity() {
		String e = config.getString("entity");
		return MetadataHelper.getEntity(e);
	}
	
	/**
	 * 标题
	 * 
	 * @return
	 */
	public String getTitle() {
		return StringUtils.defaultIfBlank(config.getString("title"), "未命名图表");
	}
	
	/**
	 * 维度轴
	 * 
	 * @return
	 */
	public Dimension[] getDimensions() {
		JSONObject axis = config.getJSONObject("axis");
		JSONArray items = axis.getJSONArray("dimension");
		if (items == null || items.isEmpty()) {
			return new Dimension[0];
		}
		
		List<Dimension> list = new ArrayList<>();
		for (Object o : items) {
			JSONObject item = (JSONObject) o;
			String field = item.getString("field");
			FormatSort sort = FormatSort.NONE;
			FormatCalc calc = FormatCalc.NONE;
			if (StringUtils.isNotBlank(item.getString("sort"))) {
				sort = FormatSort.valueOf(item.getString("sort"));
			}
			if (StringUtils.isNotBlank(item.getString("calc"))) {
				calc = FormatCalc.valueOf(item.getString("calc"));
			}
			Dimension dim = new Dimension(getSourceEntity().getField(field), sort, calc, item.getString("label"));
			list.add(dim);
		}
		return list.toArray(new Dimension[list.size()]);
	}
	
	/**
	 * 数值轴
	 * 
	 * @return
	 */
	public Numerical[] getNumericals() {
		JSONObject axis = config.getJSONObject("axis");
		JSONArray items = axis.getJSONArray("numerical");
		if (items == null || items.isEmpty()) {
			return new Numerical[0];
		}
		
		List<Numerical> list = new ArrayList<>();
		for (Object o : items) {
			JSONObject item = (JSONObject) o;
			String field = item.getString("field");
			FormatSort sort = FormatSort.NONE;
			FormatCalc calc = FormatCalc.NONE;
			if (StringUtils.isNotBlank(item.getString("sort"))) {
				sort = FormatSort.valueOf(item.getString("sort"));
			}
			if (StringUtils.isNotBlank(item.getString("calc"))) {
				calc = FormatCalc.valueOf(item.getString("calc"));
			}
			
			Numerical num = new Numerical(getSourceEntity().getField(field), sort, calc, item.getString("label"), item.getInteger("scale"));
			list.add(num);
		}
		return list.toArray(new Numerical[list.size()]);
	}
	
	/**
	 * 获取过滤 SQL
	 * 
	 * @return
	 */
	protected String getFilterSql() {
		JSONObject filterExp = config.getJSONObject("filter");
		if (filterExp == null) {
			return "(1=1)";
		}
		
		AdvFilterParser filterParser = new AdvFilterParser(filterExp);
		return filterParser.toSqlWhere();
	}
	
	/**
	 * 格式化值
	 * 
	 * @param axis
	 * @param value
	 * @return
	 */
	protected String warpAxisValue(Axis axis, Object value) {
		if (value == null) {
			return (axis instanceof Dimension) ? "无" : "0";
		}
		
		if (axis instanceof Numerical) {
			Numerical num = (Numerical) axis;
			String format = "###";
			if (num.getScale() > 0) {
				format = "##0.";
				format = StringUtils.rightPad(format, format.length() + num.getScale(), "0");
			}
			return new DecimalFormat(format).format(value);
		}
		
		EasyMeta axisField = EasyMeta.valueOf(axis.getField());
		DisplayType axisType = axisField.getDisplayType();
		
		String label = null;
		if (axisType == DisplayType.PICKLIST) {
			label = PickListManager.getLabel((ID) value);
		} else if (axisType == DisplayType.REFERENCE) {
			label = FieldValueWrapper.getLabel((ID) value);
		} else if (axisType == DisplayType.DATE || axisType == DisplayType.DATETIME) {
			label = value.toString();
		} else if (axisType == DisplayType.BOOL) {
			label = (String) FieldValueWrapper.wrapBool(value, axisField);
		} else {
			label = value.toString();
		}
		
		return label;
	}
	
	/**
	 * 构建数据
	 * 
	 * @return
	 */
	abstract public JSON build();
	
}
