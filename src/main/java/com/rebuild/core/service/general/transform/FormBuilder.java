/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.transform;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.configuration.general.FormsBuilder;
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;

public class FormBuilder {

    final private ID sourceId;
    final private ID configId;
    final private ID user;

    /**
     * @param previewid ID.ID
     * @param user
     */
    public FormBuilder(String previewid, ID user) {
        String[] ids = previewid.split("\\.");
        this.sourceId = ID.valueOf(ids[0]);
        this.configId = ID.valueOf(ids[1]);
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
                throw new ConfigurationException("Invalid config of transform : " + transConfig);
            }

            sourceEntity = sourceEntity.getDetailEntity();
            targetEntity = targetEntity.getDetailEntity();

            JSONArray detailModels = new JSONArray();
            for (ID did : QueryHelper.detailIdsNoFilter(sourceId, 0)) {
                Record targetRecord = (Record) transfomer.transformRecord(
                        sourceEntity, targetEntity, fieldsMapping, did, null, false);
                fillLabelOfReference(targetRecord);
                detailModels.add(UseFormsBuilder.instance.buildPreviewForm(targetEntity, targetRecord, user));
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

        return UseFormsBuilder.instance.buildPreviewForm(targetEntity, targetRecord, user);
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
     */
    static class UseFormsBuilder extends FormsBuilder {
        public static final UseFormsBuilder instance = new UseFormsBuilder();

        protected JSON buildPreviewForm(Entity entity, Record record, ID user) {
            JSON newModel = buildForm(entity.getName(), user, null);
            JSONArray elements = ((JSONObject) newModel).getJSONArray("elements");
            buildModelElements(elements, entity, record, user, true);
            return newModel;
        }
    }
}
