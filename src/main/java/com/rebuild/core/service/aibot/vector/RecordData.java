/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.vector;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.general.FieldValueHelper;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * 记录
 *
 * @author Zixin
 * @since 2025/4/18
 */
public class RecordData implements VectorData {

    private final ID recordId;
    private final boolean hasDetails;

    public RecordData(ID recordId) {
        this(recordId, true);
    }

    public RecordData(ID recordId, boolean hasDetails) {
        this.recordId = recordId;
        this.hasDetails = hasDetails;
    }

    @Override
    public String toVector() {
        final Record r = Application.getQueryFactory().recordNoFilter(recordId);

        StringBuilder v = new StringBuilder();
        v.append("### ").append(EasyMetaFactory.getLabel(r.getEntity())).append(NN);
        for (String fieldName : r.getAvailableFields()) {
            Field field = r.getEntity().getField(fieldName);
            if (MetadataHelper.isSystemField(field)) continue;

            String label = EasyMetaFactory.getLabel(field);
            String value = clearedFieldValue(r.getObjectValue(fieldName), field);
            v.append(label).append(": ").append(value).append(N);
        }

        if (hasDetails) {
            for (Entity de : r.getEntity().getDetialEntities()) {
                v.append("\n#### ").append(EasyMetaFactory.getLabel(de)).append(N);
                List<ID> dids = QueryHelper.detailIdsNoFilter(recordId, de);
                String didsTable = new ListData(null).toVector(dids.toArray(new ID[0]), de);
                v.append(didsTable).append(N);
            }
        }

        return v.toString();
    }

    /**
     * @param value
     * @param field
     * @return
     */
    protected static String clearedFieldValue(Object value, Field field) {
        DisplayType dt = EasyMetaFactory.getDisplayType(field);
        if (dt == DisplayType.IMAGE || dt == DisplayType.FILE
                || dt == DisplayType.SIGN || dt == DisplayType.BARCODE
                || dt == DisplayType.AVATAR) return EMPTY;

        Object o = FieldValueHelper.wrapFieldValue(value, field, true);
        return o == null ? EMPTY : o.toString();
    }
}
