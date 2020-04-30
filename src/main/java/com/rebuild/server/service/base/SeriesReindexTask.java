/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.base;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.util.support.QueryHelper;
import com.rebuild.server.Application;
import com.rebuild.server.business.series.SeriesGeneratorFactory;
import com.rebuild.server.helper.task.HeavyTask;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.bizz.UserService;

/**
 * 自动编号字段值重建
 *
 * @author devezhao
 * @since 2020/4/30
 */
public class SeriesReindexTask extends HeavyTask<Integer> {

    // 仅空字段
    static final boolean ONLY_REINDEX_BLANK = true;

    final private Field field;

    public SeriesReindexTask(Field field) {
        this.field = field;
    }

    @Override
    public Integer exec() throws Exception {
        if (EasyMeta.getDisplayType(field) != DisplayType.SERIES) {
            throw new IllegalArgumentException("None SERIES field : " + field);
        }

        String sql = String.format("select %s from %s",
                field.getOwnEntity().getPrimaryField().getName(), field.getOwnEntity().getName());
        if (ONLY_REINDEX_BLANK) {
            sql += String.format(" where %s is null or %s = ''",  field.getName(), field.getName());
        }
        Query query = Application.createQueryNoFilter(sql);
        Object[][] array = QueryHelper.readArray(query);

        setTotal(array.length);
        for (Object[] o : array) {
            if (this.isInterrupt()) {
                this.setInterrupted();
                break;
            }

            try {
                Record record = EntityHelper.forUpdate((ID) o[0], UserService.SYSTEM_USER, false);
                String series = SeriesGeneratorFactory.generate(field);
                record.setString(field.getName(), series);
                Application.getCommonService().update(record, false);
                this.addSucceeded();

            } finally {
                this.addCompleted();
            }
        }

        return this.getSucceeded();
    }
}
