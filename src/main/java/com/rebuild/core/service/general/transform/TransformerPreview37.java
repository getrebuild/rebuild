/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.transform;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.configuration.general.FormsBuilder;
import com.rebuild.core.configuration.general.FormsBuilderContextHolder;
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.query.FilterRecordChecker;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Zixin
 * @since 2024/4/8
 */
@Slf4j
public class TransformerPreview37 extends TransformerPreview {

    public TransformerPreview37(String previewid, ID user) {
        super(previewid, user);
    }

    @Override
    public JSON buildForm(String detailName) {
        Entity sourceEntity = MetadataHelper.getEntity(sourceId.getEntityCode());
        ConfigBean config = TransformManager.instance.getTransformConfig(configId, sourceEntity.getName());
        JSONObject transConfig = (JSONObject) config.getJSON("config");

        JSONObject fieldsMapping = transConfig.getJSONObject("fieldsMapping");
        if (fieldsMapping == null || fieldsMapping.isEmpty()) {
            throw new ConfigurationException("INVALID TRANSFORM CONFIG");
        }

        // 兼容
        if (detailName == null) return super.buildForm(null);
        if (fieldsMapping.get("_") == null) return super.buildForm(detailName);

        JSONArray fieldsMappingDetails = transConfig.getJSONArray("fieldsMappingDetails");
        if (fieldsMappingDetails == null || fieldsMappingDetails.isEmpty()) {
            // 兼容:源为明细
            if (sourceEntity.getMainEntity() != null) return super.buildForm(detailName);
            else return JSONUtils.EMPTY_ARRAY;
        }

        Entity targetEntity = MetadataHelper.getEntity(config.getString("target"));
        RecordTransfomer transfomer = new RecordTransfomer37(targetEntity, transConfig, false);
        transfomer.setUser(this.user);

        JSONArray detailModels = new JSONArray();

        // 明细
        for (Object o : fieldsMappingDetails) {
            JSONObject fmd = (JSONObject) o;
            Entity[] fmdEntity = RecordTransfomer37.checkEntity(fmd);
            if (fmdEntity == null) continue;

            Entity dTargetEntity = fmdEntity[0];
            Entity dSourceEntity = fmdEntity[1];

            // ND 指定明细的
            if (!detailName.equalsIgnoreCase(dTargetEntity.getName())) continue;

            String querySourceSql = RecordTransfomer37.buildDetailsSourceSql(dSourceEntity, sourceId);
            Object[][] dArray = Application.createQueryNoFilter(querySourceSql).array();
            // be:4.2.5 支持字段变量
            JSONObject transFilter = RecordTransfomer37.getTransFilter(fmd);
            FilterRecordChecker transChecker = transFilter != null ? new FilterRecordChecker(transFilter) : null;

            ID fakeMainid = EntityHelper.newUnsavedId(sourceEntity.getEntityCode());
            FormsBuilderContextHolder.setMainIdOfDetail(fakeMainid);

            try {
                for (Object[] d : dArray) {
                    if (transChecker != null) {
                        if (!transChecker.check((ID) d[0])) continue;
                    }

                    Record targetRecord = transfomer.transformRecord(
                            dSourceEntity, dTargetEntity, fmd, (ID) d[0], null, true, false, false);

                    fillLabelOfReference(targetRecord);

                    JSON model = UseFormsBuilder.instance.buildNewForm(dTargetEntity, targetRecord, FormsBuilder.DV_MAINID, user);
                    detailModels.add(model);
                }

            } finally {
                FormsBuilderContextHolder.getMainIdOfDetail(true);
            }
        }

        return detailModels;
    }
}
