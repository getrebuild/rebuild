/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
	 * @param parentField
	 */
	protected Dimension(Field field, FormatSort sort, FormatCalc calc, String label, Field parentField) {
		super(field, sort, calc, label, parentField);
	}
	
	@Override
	public String getSqlName() {
		DisplayType dt = EasyMeta.getDisplayType(getField());
		if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
			if (getFormatCalc() == FormatCalc.Y) {
				return String.format("DATE_FORMAT(%s,'%s')", super.getSqlName(), "%Y年");
			} else if (getFormatCalc() == FormatCalc.M) {
				return String.format("DATE_FORMAT(%s,'%s')", super.getSqlName(), "%Y年%m月");
			} else if (getFormatCalc() == FormatCalc.H) {
				return String.format("DATE_FORMAT(%s,'%s')", super.getSqlName(), "%Y年%m月%d日 %H时");
			} else {
				return String.format("DATE_FORMAT(%s,'%s')", super.getSqlName(), "%Y年%m月%d日");
			}
		} else {
			return super.getSqlName();
		}
	}
}
