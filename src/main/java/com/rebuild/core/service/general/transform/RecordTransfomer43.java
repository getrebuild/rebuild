/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.transform;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.TransactionManual;
import com.rebuild.core.service.query.QueryHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.TransactionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * v4.3 支持 `one2n` 模式
 *
 * @author Zixin
 * @since 2025/11/23
 */
@Slf4j
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
        // 如果多条主记录，明细（如有）都会带着

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
        JSONObject fieldsMapping = transConfig.getJSONObject("fieldsMapping");
        String hmField = null;
        DisplayType hmType = null;
        for (Map.Entry<String, Object> e : fieldsMapping.entrySet()) {
            if (one2nModeField.equals(e.getValue())) {
                hmField = e.getKey();
                hmType = EasyMetaFactory.getDisplayType(record.getEntity().getField(hmField));
                break;
            }
        }

        List<ID> theNewId = new ArrayList<>();
        TransactionStatus tx = TransactionManual.newTransaction();
        try {
            for (Object value : multiValueSplit) {
                Record clone = record.clone();
                if (hmType != null) {
                    if (hmType == DisplayType.N2NREFERENCE) {
                        clone.setObjectValue(hmField, new ID[]{(ID) value});
                    } else {
                        clone.setObjectValue(hmField, value);
                    }
                }

                List<Record> detailsListClone = null;
                if (CollectionUtils.isNotEmpty(detailsList)) {
                    detailsListClone = new ArrayList<>();
                    for (Record d : detailsList) {
                        detailsListClone.add(d.clone());
                    }
                }

                theNewId.add(super.saveRecord(clone, detailsListClone));
            }

            TransactionManual.commit(tx);

        } catch (Exception ex) {
            TransactionManual.rollback(tx);

            log.error("Saving `one2nModeField` error : {}", transid, ex);
            throw ex;
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

    /**
     * 获取转换后的多条记录
     *
     * @return
     */
    public ID[] getTheNewIds() {
        return fillbackReadyIds;
    }

    // --

    /**
     * @param transid
     * @return
     */
    public static RecordTransfomer43 create(ID transid) {
        return new RecordTransfomer43(transid);
    }
}
