/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
	 * @param sort
	 * @param calc
	 * @param label
	 * @param scale
	 * @param parentField
	 */
	protected Numerical(Field field, FormatSort sort, FormatCalc calc, String label, Integer scale,
                        Field parentField) {
		super(field, sort, calc, label, parentField);
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
		return super.getLabel() + (StringUtils.isBlank(label) ? getFormatCalc().getLabel() : "");
	}
	
	@Override
	public String getSqlName() {
		if (FormatCalc.NONE == getFormatCalc()) {
			return super.getSqlName();
		}

		DisplayType dt = EasyMeta.getDisplayType(getField());
		if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
			return String.format("%s(%s)", getFormatCalc().name(), super.getSqlName());
		} else if (getFormatCalc() == FormatCalc.COUNT2) {
            return String.format("%s(DISTINCT %s)", FormatCalc.COUNT, super.getSqlName());
        } else {
            return String.format("%s(%s)", FormatCalc.COUNT, super.getSqlName());
        }
	}
}
