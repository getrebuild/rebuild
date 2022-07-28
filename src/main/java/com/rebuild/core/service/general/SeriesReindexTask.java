/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.util.support.QueryHelper;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.series.SeriesGeneratorFactory;
import com.rebuild.core.support.task.HeavyTask;

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
    public Integer exec() {
        if (EasyMetaFactory.getDisplayType(field) != DisplayType.SERIES) {
            throw new IllegalArgumentException("None SERIES field : " + field);
        }

        String sql = String.format("select %s from %s",
                field.getOwnEntity().getPrimaryField().getName(), field.getOwnEntity().getName());
        if (ONLY_REINDEX_BLANK) {
            sql += String.format(" where %s is null or %s = ''", field.getName(), field.getName());
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
                String series = SeriesGeneratorFactory.generate(
                        field, com.rebuild.core.service.query.QueryHelper.recordNoFilter((ID) o[0]));

                Record record = EntityHelper.forUpdate((ID) o[0], UserService.SYSTEM_USER, false);
                record.setString(field.getName(), series);
                Application.getCommonsService().update(record, false);
                this.addSucceeded();

            } finally {
                this.addCompleted();
            }
        }

        return this.getSucceeded();
    }
}
