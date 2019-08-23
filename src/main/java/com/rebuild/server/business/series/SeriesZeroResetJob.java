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

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Calendar;

/**
 * 数字系列归零。每日 00:00 执行
 * 
 * @author devezhao
 * @since 12/25/2018
 */
public class SeriesZeroResetJob extends QuartzJobBean {

	private static final Log LOG = LogFactory.getLog(SeriesZeroResetJob.class);

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		LOG.info("SeriesZeroResetJob running ...");

		boolean isFirstDayOfYear = false;
		boolean isFirstDayOfMonth = false;
		final Calendar now = CalendarUtils.getInstance();
		if (now.get(Calendar.DAY_OF_MONTH) == 1) {
			isFirstDayOfMonth = true;
			if (now.get(Calendar.MONTH) == Calendar.JANUARY) {
				isFirstDayOfYear = true;
			}
		}
		
		for (Entity entity : MetadataHelper.getEntities()) {
			for (Field field : entity.getFields()) {
				EasyMeta easy = EasyMeta.valueOf(field);
				if (easy.getDisplayType() == DisplayType.SERIES) {
					String zeroFlag = easy.getFieldExtConfig().getString("seriesZero");
					if ("D".equalsIgnoreCase(zeroFlag)) {
						SeriesGeneratorFactory.zero(field);
						LOG.info("Zero field by [D] : " + field);
					} else if ("M".equalsIgnoreCase(zeroFlag) && isFirstDayOfMonth) {
						SeriesGeneratorFactory.zero(field);
						LOG.info("Zero field by [M] : " + field);
					} else if ("Y".equalsIgnoreCase(zeroFlag) && isFirstDayOfYear) {
						SeriesGeneratorFactory.zero(field);
						LOG.info("Zero field by [Y] : " + field);
					}
				}
			}
		}
	}
}
