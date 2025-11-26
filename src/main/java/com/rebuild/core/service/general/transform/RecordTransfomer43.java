/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.transform;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.query.QueryHelper;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * v4.3 支持 `one2n` 模式
 *
 * @author Zixin
 * @since 2025/11/23
 */
public class RecordTransfomer43 extends RecordTransfomer39 {

    private ID sourceRecordId;
    private String one2nModeField;

    private ID[] fillbackReadyIds = null;

    public RecordTransfomer43(ID transid) {
        super(transid);
    }

    @Override
    public ID transform(ID sourceRecordId, ID specMainId, ID targetExistsRecordId) {
        boolean one2nMode = transConfig.getBooleanValue("one2nMode");
        this.one2nModeField = one2nMode ? transConfig.getString("one2nModeField") : null;
        this.sourceRecordId = sourceRecordId;

        return super.transform(sourceRecordId, specMainId, targetExistsRecordId);
    }

    @Override
    protected ID saveRecord(Record record, List<Record> detailsList) {
        if (StringUtils.isBlank(one2nModeField)) {
            return super.saveRecord(record, detailsList);
        }

        // 需要保存为多条
        // TODO 如果多条主记录，明细（如有）都会带着

        Object multiValue = QueryHelper.queryFieldValue(sourceRecordId, one2nModeField);
        if (multiValue == null) {
            return super.saveRecord(record, detailsList);
        }

        List<Object> multiValueSplit = new ArrayList<>();
        // TAG, N2NREF
        if (multiValue instanceof Object[]) {
            Collections.addAll(multiValueSplit, (Object[]) multiValue);
        } else if (multiValue instanceof Number) {
            // MULTISELECT
            EasyField one2nModeFieldEasy = EasyMetaFactory.valueOf(
                    MetadataHelper.getEntity(sourceRecordId.getEntityCode()).getField(one2nModeField));
            if (one2nModeFieldEasy.getDisplayType() == DisplayType.MULTISELECT) {
                multiValue = MultiSelectManager.instance.getLabels((Long) multiValue, one2nModeFieldEasy.getRawMeta());
                Collections.addAll(multiValueSplit, (Object[]) multiValue);
            }
            // NUMBER
            else {
                for (int i = 0; i < ObjectUtils.toInt(multiValue); i++) {
                    multiValueSplit.add(1);
                }
            }
        }

        if (multiValueSplit.size() <= 1) {
            return super.saveRecord(record, detailsList);
        }

        // `转换依据字段` 未映射则不保存此字段值
        Object hm = transConfig.getJSONObject("fieldsMapping").get(one2nModeField);
        DisplayType hasMappingTargetType = null;
        if (hm != null) {
            if (hm instanceof JSONArray) {
                // 使用固定值
            } else {
                hasMappingTargetType = EasyMetaFactory.getDisplayType(record.getEntity().getField(hm.toString()));
            }
        }

        List<ID> theNewId = new ArrayList<>();
        for (Object value : multiValueSplit) {
            Record clone = record.clone();
            if (hasMappingTargetType != null) {
                if (hasMappingTargetType == DisplayType.N2NREFERENCE) {
                    clone.setObjectValue(one2nModeField, new ID[]{(ID) value});
                } else {
                    clone.setObjectValue(one2nModeField, value);
                }
            }

            theNewId.add(super.saveRecord(clone, detailsList));
        }

        // 回填
        fillbackReadyIds = theNewId.toArray(new ID[0]);

        // 返回最先那条
        return fillbackReadyIds[0];
    }

    @Override
    protected boolean fillback(ID sourceRecordId, ID newId) {
        if (fillbackReadyIds == null) return true;
        return super.fillback(sourceRecordId, fillbackReadyIds);
    }

    // --

    /**
     * @param transid
     * @return
     */
    public static RecordTransfomer39 create(ID transid) {
        return new RecordTransfomer43(transid);
    }
}
