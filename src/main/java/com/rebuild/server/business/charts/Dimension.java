/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.charts;

import cn.devezhao.persist4j.Field;
import com.rebuild.server.configuration.portals.ClassificationManager;
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
			switch (getFormatCalc()) {
				case Y:
					return String.format("DATE_FORMAT(%s,'%s')", super.getSqlName(), "%Y");
				case M:
					return String.format("DATE_FORMAT(%s,'%s')", super.getSqlName(), "%Y-%m");
				case H:
					return String.format("DATE_FORMAT(%s,'%s')", super.getSqlName(), "%Y-%m-%d %H时");
				default:
					return String.format("DATE_FORMAT(%s,'%s')", super.getSqlName(), "%Y-%m-%d");
			}

		} else if (dt == DisplayType.CLASSIFICATION
                && getFormatCalc() != null && getFormatCalc().name().startsWith("L")) {
			int useLevel = ClassificationManager.instance.getOpenLevel(getField()) + 1;
			int selectLevel = Integer.parseInt(getFormatCalc().name().substring(1));
			// Last
			if (selectLevel >= useLevel || selectLevel == 4) {
				return super.getSqlName();
			}

			String sqlName = super.getSqlName();
			for (int i = 0; i < useLevel - selectLevel; i++) {
				sqlName += ".parent";
			}
			return sqlName;

		} else {
			return super.getSqlName();
		}
	}
}
