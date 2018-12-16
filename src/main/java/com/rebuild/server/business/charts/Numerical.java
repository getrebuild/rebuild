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

import org.apache.commons.lang.StringUtils;

import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;

import cn.devezhao.persist4j.Field;

/**
 * 数值-轴
 * 
 * @author devezhao
 * @since 12/14/2018
 */
public class Numerical extends Axis {

	private int scale = 2;
	
	public Numerical(Field field, FormatSort sort, FormatCalc calc, String label, Integer scale) {
		super(field, sort, calc, label);
		if (scale != null) {
			this.scale = scale;
		}
	}
	
	public int getScale() {
		return scale;
	}
	
	@Override
	public String getLabel() {
		return StringUtils.defaultIfBlank(label, EasyMeta.getLabel(getField()) + getFormatCalc().getLabel());
	}
	
	@Override
	public String getSqlName() {
		EasyMeta meta = EasyMeta.valueOf(getField());
		if (meta.getDisplayType() == DisplayType.NUMBER || meta.getDisplayType() == DisplayType.DECIMAL) {
			return String.format("%s(%s)", getFormatCalc().name(), meta.getName());
		} else {
			return String.format("%s(%s)", FormatCalc.COUNT, meta.getName());
		}
	}
}
