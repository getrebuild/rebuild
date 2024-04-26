/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.series;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.support.distributed.DistributedJobLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;

/**
 * 数字系列归零。每日 00:00 执行
 *
 * @author devezhao
 * @since 12/25/2018
 */
@Slf4j
@Component
public class SeriesZeroResetJob extends DistributedJobLock {

    @Scheduled(cron = "0 0 0 * * ?")
    protected void executeJob() {
        if (!tryLock()) return;

        final Calendar now = CalendarUtils.getInstance();

        boolean isFirstDayOfYear = false;
        boolean isFirstDayOfMonth = false;
        if (now.get(Calendar.DAY_OF_MONTH) == 1) {
            isFirstDayOfMonth = true;
            if (now.get(Calendar.MONTH) == Calendar.JANUARY) {
                isFirstDayOfYear = true;
            }
        }

        // 周一
        boolean isFirstDayOfWeek = now.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY;

        for (Entity entity : MetadataHelper.getEntities()) {
            for (Field field : entity.getFields()) {
                EasyField easyField = EasyMetaFactory.valueOf(field);
                if (easyField.getDisplayType() == DisplayType.SERIES) {
                    String zeroMode = easyField.getExtraAttr(EasyFieldConfigProps.SERIES_ZERO);
                    if ("D".equalsIgnoreCase(zeroMode)) {
                        SeriesGeneratorFactory.zero(field);
                        log.info("Zero field by [D] : {}", field);
                    } else if ("M".equalsIgnoreCase(zeroMode) && isFirstDayOfMonth) {
                        SeriesGeneratorFactory.zero(field);
                        log.info("Zero field by [M] : {}", field);
                    } else if ("Y".equalsIgnoreCase(zeroMode) && isFirstDayOfYear) {
                        SeriesGeneratorFactory.zero(field);
                        log.info("Zero field by [Y] : {}", field);
                    } else if ("W".equalsIgnoreCase(zeroMode) && isFirstDayOfWeek) {
                        SeriesGeneratorFactory.zero(field);
                        log.info("Zero field by [W] : {}", field);
                    }
                }
            }
        }
    }
}
