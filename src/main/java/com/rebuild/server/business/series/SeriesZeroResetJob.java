/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.series;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.metadata.entity.FieldExtConfigProps;
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
					String zeroFlag = easy.getExtraAttr(FieldExtConfigProps.SERIES_SERIESZERO);
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
