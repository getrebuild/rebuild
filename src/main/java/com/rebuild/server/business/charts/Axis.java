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
import com.rebuild.server.metadata.entity.EasyMeta;
import org.apache.commons.lang.StringUtils;

/**
 * 轴
 * 
 * @author devezhao
 * @since 12/14/2018
 */
public class Axis {
	
	private Field field;
	private FormatSort sort;
	private FormatCalc calc;
	protected String label;

	/**
	 * @param field
	 * @param sort
	 * @param calc
	 * @param label
	 */
	protected Axis(Field field, FormatSort sort, FormatCalc calc, String label) {
		this.field = field;
		this.calc = calc;
		this.sort = sort;
		this.label = label;
	}

	/**
	 * @return
	 */
	public Field getField() {
		return field;
	}
	
	/**
	 * @return
	 */
	public FormatSort getFormatSort() {
		return sort;
	}

	/**
	 * @return
	 */
	public FormatCalc getFormatCalc() {
		return calc;
	}
	
	/**
	 * @return
	 */
	public String getLabel() {
		if (StringUtils.isNotBlank(label)) {
			return label;
		}
		return EasyMeta.getLabel(field);
	}
	
	/**
	 * @return
	 */
	public String getSqlName() {
		return field.getName();
	}
}
