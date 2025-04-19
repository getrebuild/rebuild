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
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.general.BatchOperatorQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Zixin
 * @since 2025/4/29
 */
public class ListFilterData implements VectorData {

    private final JSONObject listFilter;

    public ListFilterData(JSONObject listFilter) {
        this.listFilter = listFilter;
    }

    @Override
    public String toVector() {
        int dataRange = listFilter.getIntValue("_dataRange");
        ID[] records = new BatchOperatorQuery(dataRange, listFilter).getQueryedRecordIds();

        String entityName = listFilter.getString("entity");
        Entity entity = MetadataHelper.getEntity(entityName);

        List<Field> fields = new ArrayList<>();
        MarkdownTable mdt = new MarkdownTable();
        for (Field field : entity.getFields()) {
            if (MetadataHelper.isSystemField(field)) continue;

            fields.add(field);
            mdt.addHead(EasyMetaFactory.getLabel(field));
        }

        for (ID id : records) {
            final Record record = QueryHelper.recordNoFilter(id);
            List<String> data = new ArrayList<>();
            for (Field field : fields) {
                String value = RecordData.clearedFieldValue(record.getObjectValue(field.getName()), field);
                data.add(value);
            }
            mdt.addRowData(data);
        }

        return mdt.toMdTable();
    }
}
