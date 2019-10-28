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

package com.rebuild.server.business.series;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自动编号
 * 
 * @author devezhao
 * @since 12/24/2018
 */
public class SeriesGenerator {
	
	final private Field field;
	final private JSONObject config;
	
	/**
	 * @param field
	 */
	public SeriesGenerator(EasyMeta field) {
		this.field = (Field) field.getBaseMeta();
		this.config = field.getFieldExtConfig();
	}
	
	/**
	 * @param field
	 * @param config
	 */
	public SeriesGenerator(Field field, JSONObject config) {
		this.field = field;
		this.config = config;
	}
	
	/**
	 * @return
	 */
	public String generate() {
		String seriesFormat = config.getString("seriesFormat");
		if (StringUtils.isBlank(seriesFormat)) {
			seriesFormat = DisplayType.SERIES.getDefaultFormat();
		}
		
		List<SeriesVar> vars = explainVars(seriesFormat);
		for (SeriesVar var : vars) {
			seriesFormat = seriesFormat.replace("{" + var.getSymbols() + "}", var.generate());
		}
		return seriesFormat;
	}
	
	/**
	 * @param format
	 * @return
	 */
	protected List<SeriesVar> explainVars(String format) {
		List<SeriesVar> vars = new ArrayList<>();
		Pattern varPattern = Pattern.compile("\\{(\\w+)\\}");
		Matcher varMatcher = varPattern.matcher(format);
		while (varMatcher.find()) {
			String s = varMatcher.group(1);
			if (s.substring(0, 1).equals("0")) {
				vars.add(new IncreasingVar(s, field, config.getString("seriesZero")));
			} else {
				vars.add(new TimeVar(s));
			}
		}
		return vars;
	}
}
