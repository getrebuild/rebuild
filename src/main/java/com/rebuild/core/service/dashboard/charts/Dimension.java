/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.persist4j.Field;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;

import java.text.MessageFormat;

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
                case Q:
                    return MessageFormat.format("CONCAT(YEAR({0}),'' Q'',QUARTER({0}))", super.getSqlName());
                case M:
                    return String.format("DATE_FORMAT(%s,'%s')", super.getSqlName(), "%Y-%m");
                case H:
                    return String.format("DATE_FORMAT(%s,'%s')", super.getSqlName(), "%Y-%m-%d %HH");
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

            StringBuilder sqlName = new StringBuilder(super.getSqlName());
            for (int i = 0; i < useLevel - selectLevel; i++) {
                sqlName.append(".parent");
            }
            return sqlName.toString();

        } else {
            return super.getSqlName();
        }
    }
}
