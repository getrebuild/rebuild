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
import com.rebuild.core.configuration.general.FormBuilderContextHolder;
import com.rebuild.core.configuration.general.FormsBuilder;
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

import java.util.List;

public class TransformerPreview {

    final private ID sourceId;
    final private ID configId;
    final private ID user;

    final private ID mainid;

    /**
     * @param previewid SOURCEID.TRANSID.[MAINID]
     * @param user
     */
    public TransformerPreview(String previewid, ID user) {
        String[] ids = previewid.split("\\.");
        this.sourceId = ID.valueOf(ids[0]);
        this.configId = ID.valueOf(ids[1]);
        this.mainid = ids.length > 2 ? ID.valueOf(ids[2]) : null;
        this.user = user;
    }

    /**
     * @param isDetails
     * @return
     */
    public JSON buildForm(boolean isDetails) {
        ConfigBean config = TransformManager.instance.getTransformConfig(
                configId, MetadataHelper.getEntity(sourceId.getEntityCode()).getName());
        JSONObject transConfig = (JSONObject) config.getJSON("config");

        Entity targetEntity = MetadataHelper.getEntity(config.getString("target"));
        Entity sourceEntity = MetadataHelper.getEntity(sourceId.getEntityCode());

        RecordTransfomer transfomer = new RecordTransfomer(targetEntity, transConfig, false);

        // 明细
        if (isDetails) {
            JSONObject fieldsMapping = transConfig.getJSONObject("fieldsMappingDetail");
            if (fieldsMapping == null || fieldsMapping.isEmpty()) {
                return JSONUtils.EMPTY_ARRAY;
            }

            List<ID> ids = QueryHelper.detailIdsNoFilter(sourceId, 0);
            if (ids.isEmpty()) {
                return JSONUtils.EMPTY_ARRAY;
            }

            ID fakeMainid = EntityHelper.newUnsavedId(sourceEntity.getEntityCode());
            JSONObject initialVal = JSONUtils.toJSONObject(FormsBuilder.DV_MAINID, FormsBuilder.DV_MAINID);

            sourceEntity = sourceEntity.getDetailEntity();
            targetEntity = targetEntity.getDetailEntity();

            JSONArray detailModels = new JSONArray();
            FormBuilderContextHolder.setMainIdOfDetail(fakeMainid);
            try {
                for (ID did : ids) {
                    Record targetRecord = (Record) transfomer.transformRecord(
                            sourceEntity, targetEntity, fieldsMapping, did, null, false);
                    fillLabelOfReference(targetRecord);

                    JSON model = UseFormsBuilder.instance.buildNewForm(targetEntity, targetRecord, user);
                    UseFormsBuilder.instance.setFormInitialValue(targetEntity, model, initialVal);
                    detailModels.add(model);
                }
            } finally {
                FormBuilderContextHolder.getMainIdOfDetail(true);
            }

            return detailModels;
        }

        // 检查主记录
        if (!transfomer.checkFilter(sourceId)) {
            throw new DataSpecificationException(Language.L("当前记录不符合转换条件"));
        }

        JSONObject fieldsMapping = transConfig.getJSONObject("fieldsMapping");
        if (fieldsMapping == null || fieldsMapping.isEmpty()) {
            throw new ConfigurationException("Invalid config of transform : " + transConfig);
        }

        Record targetRecord = (Record) transfomer.transformRecord(
                sourceEntity, targetEntity, fieldsMapping, sourceId, null, false);
        fillLabelOfReference(targetRecord);

        // 转为明细
        if (mainid != null) {
            Field dtfField = MetadataHelper.getDetailToMainField(targetEntity);
            targetRecord.setID(dtfField.getName(), mainid);
            FormBuilderContextHolder.setMainIdOfDetail(mainid);
        }

        try {
            JSON model = UseFormsBuilder.instance.buildNewForm(targetEntity, targetRecord, user);
            if (mainid != null) {
                JSONObject initialVal = JSONUtils.toJSONObject(FormsBuilder.DV_MAINID, mainid);
                UseFormsBuilder.instance.setFormInitialValue(targetEntity, model, initialVal);
            }

            return model;

        } finally {
            if (mainid != null) FormBuilderContextHolder.getMainIdOfDetail(true);
        }
    }

    private void fillLabelOfReference(Record record) {
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
        return new RecordTransfomer(this.configId).fillback(this.sourceId, newId);
    }

    /**
     */
    static class UseFormsBuilder extends FormsBuilder {
        public static final UseFormsBuilder instance = new UseFormsBuilder();

        protected JSON buildNewForm(Entity entity, Record record, ID user) {
            JSON model = buildForm(entity.getName(), user, null);
            String hasError = ((JSONObject) model).getString("error");
            if (hasError != null) throw new DataSpecificationException(hasError);

            JSONArray elements = ((JSONObject) model).getJSONArray("elements");
            buildModelElements(elements, entity, record, user, true);
            return model;
        }
    }
}
