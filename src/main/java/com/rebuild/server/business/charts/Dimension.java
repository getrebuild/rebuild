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

/**
 * 维度-轴
 * 
 * @author devezhao
 * @since 12/14/2018
 */
public class Dimension extends Axis {

	/**
	 * @param field
	 * @param sort
	 * @param calc
	 * @param label
	 */
	protected Dimension(Field field, FormatSort sort, FormatCalc calc, String label) {
		super(field, sort, calc, label);
	}
	
	@Override
	public String getSqlName() {
		EasyMeta meta = EasyMeta.valueOf(getField());
		if (meta.getDisplayType() == DisplayType.DATE || meta.getDisplayType() == DisplayType.DATETIME) {
			if (getFormatCalc() == FormatCalc.Y) {
				return String.format("DATE_FORMAT(%s,'%s')", meta.getName(), "%Y年");
			} else if (getFormatCalc() == FormatCalc.M) {
				return String.format("DATE_FORMAT(%s,'%s')", meta.getName(), "%Y年%m月");
			} else if (getFormatCalc() == FormatCalc.H) {
				return String.format("DATE_FORMAT(%s,'%s')", meta.getName(), "%Y年%m月%d日 %H时");
			} else {
				return String.format("DATE_FORMAT(%s,'%s')", meta.getName(), "%Y年%m月%d日");
			}
		} else {
			return meta.getName();
		}
	}
}
