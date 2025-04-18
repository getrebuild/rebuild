/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.vector;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.aibot.VectorData;
import com.rebuild.core.support.general.FieldValueHelper;

/**
 * @author Zixin
 * @since 2025/4/18
 */
public class RecordData implements VectorData {

    private final ID recordId;

    public RecordData(ID recordId) {
        this.recordId = recordId;
    }

    @Override
    public String toVector() {
        final Record r = Application.getQueryFactory().recordNoFilter(recordId);

        StringBuilder v = new StringBuilder();
        for (String fieldName : r.getAvailableFields()) {
            Field field = r.getEntity().getField(fieldName);
            String label = EasyMetaFactory.getLabel(field);
            Object value = FieldValueHelper.wrapFieldValue(r.getObjectValue(fieldName), field, true);
            v.append(label).append(":").append(value).append("\n");
        }
        return v.toString();
    }
}
