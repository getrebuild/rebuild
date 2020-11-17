/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.series;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.impl.FieldExtConfigProps;
import com.rebuild.core.support.distributed.DistributedJobLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;

/**
 * 数字系列归零。每日 00:00 执行
 *
 * @author devezhao
 * @since 12/25/2018
 */
@Component
public class SeriesZeroResetJob extends DistributedJobLock {

    @Scheduled(cron = "0 0 0 * * ?")
    protected void executeJob() {
        if (!tryLock()) return;

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
                EasyField easy = EasyMetaFactory.valueOf(field);
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
