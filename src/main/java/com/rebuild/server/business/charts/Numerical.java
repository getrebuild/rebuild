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

import cn.devezhao.persist4j.Field;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import org.apache.commons.lang.StringUtils;

/**
 * 数值-轴
 * 
 * @author devezhao
 * @since 12/14/2018
 */
public class Numerical extends Axis {

	private int scale = 2;
	
	/**
	 * @param field
	 */
	protected Numerical(Field field) {
		this(field, FormatSort.NONE, FormatCalc.NONE, null, 0);
	}
	
	/**
	 * @param field
	 * @param sort
	 * @param calc
	 * @param label
	 * @param scale
	 */
	protected Numerical(Field field, FormatSort sort, FormatCalc calc, String label, Integer scale) {
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
		if (FormatCalc.NONE == getFormatCalc()) {
			return StringUtils.defaultIfBlank(label, "数值");
		}
		return StringUtils.defaultIfBlank(label, EasyMeta.getLabel(getField()) + getFormatCalc().getLabel());
	}
	
	@Override
	public String getSqlName() {
		if (FormatCalc.NONE == getFormatCalc()) {
			return getField().getName();
		}
		
		EasyMeta meta = EasyMeta.valueOf(getField());
		if (meta.getDisplayType() == DisplayType.NUMBER || meta.getDisplayType() == DisplayType.DECIMAL) {
			return String.format("%s(%s)", getFormatCalc().name(), meta.getName());
		} else {
			return String.format("%s(%s)", FormatCalc.COUNT, meta.getName());
		}
	}
}
