/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.transform;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.configuration.general.FormsBuilder;
import com.rebuild.core.configuration.general.FormsBuilderContextHolder;
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;

import java.util.Collections;
import java.util.List;

/**
 * 转换记录预览模式
 *
 * @author RB
 * @since 2022/7/19
 */
public class TransformerPreview {

    final protected ID configId;
    final protected ID sourceId;
    final protected ID user;

    final protected ID mainid;

    /**
     * @param previewid TRANSID.SOURCEID.[MAINID]
     * @param user
     */
    public TransformerPreview(String previewid, ID user) {
        String[] ids = previewid.split("\\.");
        this.configId = ID.valueOf(ids[0]);
        this.sourceId = ID.valueOf(ids[1]);
        this.mainid = ids.length > 2 ? ID.valueOf(ids[2]) : null;
        this.user = user;
    }

    /**
     * @return
     */
    public JSON buildForm() {
        return buildForm(null);
    }

    /**
     * @param detailName [获取明细]
     * @return
     */
    public JSON buildForm(String detailName) {
        Entity mainOrDetailEntity = MetadataHelper.getEntity(sourceId.getEntityCode());
        ConfigBean config = TransformManager.instance.getTransformConfig(configId, mainOrDetailEntity.getName());
        JSONObject transConfig = (JSONObject) config.getJSON("config");

        Entity targetEntity = MetadataHelper.getEntity(config.getString("target"));
        Entity sourceEntity = mainOrDetailEntity;

        RecordTransfomer transfomer = new RecordTransfomer37(targetEntity, transConfig, false);
        transfomer.setUser(this.user);

        // 获取明细
        if (detailName != null) {
            JSONObject fieldsMapping = transConfig.getJSONObject("fieldsMappingDetail");
            if (fieldsMapping == null || fieldsMapping.isEmpty()) {
                return JSONUtils.EMPTY_ARRAY;
            }

            List<ID> details;
            ID fakeMainid;
            // 源为明细
            if (sourceEntity.getMainEntity() != null) {
                details = Collections.singletonList(sourceId);
                fakeMainid = EntityHelper.newUnsavedId(sourceEntity.getMainEntity().getEntityCode());
            } else {
                details = QueryHelper.detailIdsNoFilter(sourceId);
                fakeMainid = EntityHelper.newUnsavedId(sourceEntity.getEntityCode());
            }
            if (details.isEmpty()) return JSONUtils.EMPTY_ARRAY;

            sourceEntity = sourceEntity.getMainEntity() != null ? sourceEntity : sourceEntity.getDetailEntity();
            targetEntity = targetEntity.getMainEntity() != null ? targetEntity : targetEntity.getDetailEntity();

            JSONArray detailModels = new JSONArray();
            FormsBuilderContextHolder.setMainIdOfDetail(fakeMainid);
            try {
                for (ID did : details) {
                    Record targetRecord = transfomer.transformRecord(
                            sourceEntity, targetEntity, fieldsMapping, did, null, true, false, false);

                    fillLabelOfReference(targetRecord);

                    JSON model = UseFormsBuilder.instance.buildNewForm(targetEntity, targetRecord, FormsBuilder.DV_MAINID, user);
                    detailModels.add(model);
                }
            } finally {
                FormsBuilderContextHolder.getMainIdOfDetail(true);
            }

            return detailModels;
        }

        // 检查主记录
        if (!transfomer.checkFilter(sourceId)) {
            throw new DataSpecificationException(Language.L("当前记录不符合转换条件"));
        }

        JSONObject fieldsMapping = transConfig.getJSONObject("fieldsMapping");
        if (fieldsMapping == null || fieldsMapping.isEmpty()) {
            throw new ConfigurationException("INVALID TRANSFORM CONFIG");
        }

        Record targetRecord = transfomer.transformRecord(
                sourceEntity, targetEntity, fieldsMapping, sourceId, null, true, false, false);
        fillLabelOfReference(targetRecord);

        // 转为明细
        if (mainid != null) {
            Field dtfField = MetadataHelper.getDetailToMainField(targetEntity);
            targetRecord.setID(dtfField.getName(), mainid);
            FormsBuilderContextHolder.setMainIdOfDetail(mainid);
        }

        try {
            return UseFormsBuilder.instance.buildNewForm(targetEntity, targetRecord, mainid, user);
        } finally {
            if (mainid != null) FormsBuilderContextHolder.getMainIdOfDetail(true);
        }
    }

    protected void fillLabelOfReference(Record record) {
        Entity entity = record.getEntity();
        for (String field : record.getAvailableFields()) {
            DisplayType dt = EasyMetaFactory.getDisplayType(entity.getField(field));

            if (dt == DisplayType.REFERENCE) {
                ID idVal = record.getID(field);
                if (NullValue.isNull(idVal) || idVal.getLabel() != null) continue;

                // Update ref
                idVal.setLabel(FieldValueHelper.getLabelNotry(idVal));
            }
        }
    }

    /**
     * @param newId
     * @return
     */
    public boolean fillback(ID newId) {
        return new RecordTransfomer37(this.configId).fillback(this.sourceId, newId);
    }
}
