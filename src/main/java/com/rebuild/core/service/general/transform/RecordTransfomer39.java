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
import com.rebuild.core.configuration.general.AutoFillinManager;
import com.rebuild.core.configuration.general.FormsBuilderContextHolder;
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.general.GeneralEntityService;
import com.rebuild.core.service.query.FilterRecordChecker;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.CommonsLog;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * v3.9 新增转换为新记录或已存在记录
 *
 * @author Zixin
 * @since 2024/4/8
 */
@Slf4j
public class RecordTransfomer39 extends RecordTransfomer37 {

    private ID transid;
    // 转换到已存在记录
    private ID targetExistsRecordId;
    // 预览时生成的 Record
    volatile private List<Record> previewRecords;

    public RecordTransfomer39(ID transid) {
        super(transid);
        this.transid = transid;
    }

    @Override
    public ID transform(ID sourceRecordId) {
        return this.transform(sourceRecordId, null, null);
    }

    @Override
    public ID transform(ID sourceRecordId, ID specMainId) {
        return this.transform(sourceRecordId, specMainId, null);
    }

    /**
     * 转换
     *
     * @param sourceRecordId
     * @param specMainId
     * @param targetExistsRecordId
     * @return
     */
    public ID transform(ID sourceRecordId, ID specMainId, ID targetExistsRecordId) {
        this.targetExistsRecordId = targetExistsRecordId;
        // 直接转换做非空检查
        this.transConfig.put("checkNullable35", true);
        // 指定目标是明细
        if (targetExistsRecordId != null) specMainId = forceGetSpecMainId(targetExistsRecordId);

        ID theNewId = super.transform(sourceRecordId, specMainId);
        // #saveRecord afetr
        CommonsLog.createTransformLog(getUser(), sourceRecordId, theNewId, transid);
        return theNewId;
    }

    @Override
    protected ID saveRecord(Record record, List<Record> detailsList) {
        if (targetExistsRecordId == null) {
            return super.saveRecord(record, detailsList);
        }

        this.mergeExistsAndTarget(record);

        // 其下明细记录直接清空新建，因为没法一一对应去更新
        // 注意这会导致触发器触发动作不准
        JSONArray hasDetailsConf = transConfig.getJSONArray("fieldsMappingDetails");
        if (hasDetailsConf != null && !hasDetailsConf.isEmpty()) {
            List<ID> detailIds = QueryHelper.detailIdsNoFilter(targetExistsRecordId);
            if (!detailIds.isEmpty()) {
                Application.getCommonsService().delete(detailIds.toArray(new ID[0]), false);
            }
        }

        return super.saveRecord(record, detailsList);
    }

    /**
     * 预览
     *
     * @param sourceRecordId
     * @param specMainId
     * @param targetExistsRecordId
     * @return
     */
    public JSON preview(ID sourceRecordId, ID specMainId, ID targetExistsRecordId) {
        this.previewRecords = new ArrayList<>();
        this.targetExistsRecordId = targetExistsRecordId;
        // 预览不做非空检查
        this.transConfig.put("checkNullable35", false);
        // 指定目标是明细
        if (targetExistsRecordId != null) specMainId = forceGetSpecMainId(targetExistsRecordId);

        Entity sourceEntity = MetadataHelper.getEntity(sourceRecordId.getEntityCode());
        ConfigBean config = TransformManager.instance.getTransformConfig(transid, sourceEntity.getName());
        JSONObject transConfig = (JSONObject) config.getJSON("config");

        JSONObject fieldsMapping = transConfig.getJSONObject("fieldsMapping");
        JSONArray fieldsMappingDetails = transConfig.getJSONArray("fieldsMappingDetails");
        if (fieldsMapping == null || fieldsMapping.isEmpty()) {
            throw new ConfigurationException("INVALID TRANSFORM CONFIG");
        }

        Entity targetEntity = MetadataHelper.getEntity(config.getString("target"));
        Record tansTargetRecord = transformRecord(sourceEntity, targetEntity, fieldsMapping, sourceRecordId,
                null, false, false, false);

        if (targetExistsRecordId != null) {
            this.mergeExistsAndTarget(tansTargetRecord);
            // 此处强制回填，因为编辑时前端不会回填
            AutoFillinManager.instance.fillinRecord(tansTargetRecord, true);
        }

        this.previewRecords.add(tansTargetRecord);
        TransformerPreview37.fillLabelOfReference(tansTargetRecord);

        JSON formModel = UseFormsBuilder.buildFormWithRecord(targetEntity, tansTargetRecord, specMainId, getUser(), false);
        // fix:3.9.4 明细导入
        if (targetEntity.getDetailEntity() != null) {
            ((JSONObject) formModel).put("detailImports", buildDetailImports39(targetEntity));
        }

        if (fieldsMappingDetails == null || fieldsMappingDetails.isEmpty()) return formModel;

        // 有明细
        ID fakeMainId = EntityHelper.newUnsavedId(sourceEntity.getEntityCode());
        Map<String, Object> formModelDetailsMap = new HashMap<>();
        for (Object o : fieldsMappingDetails) {
            JSONObject fmd = (JSONObject) o;
            Entity[] fmdEntity = checkEntity(fmd);
            if (fmdEntity == null) continue;

            Entity dTargetEntity = fmdEntity[0];
            Entity dSourceEntity = fmdEntity[1];

            String querySourceSql = buildDetailsSourceSql(dSourceEntity, sourceRecordId);
            Object[][] dArray = Application.createQueryNoFilter(querySourceSql).array();
            // be:4.2.5 支持字段变量
            JSONObject transFilter = getTransFilter(fmd);
            FilterRecordChecker transChecker = transFilter != null ? new FilterRecordChecker(transFilter) : null;

            JSONArray formModelDetails = new JSONArray();
            FormsBuilderContextHolder.setMainIdOfDetail(fakeMainId);

            try {
                for (Object[] d : dArray) {
                    if (transChecker != null) {
                        if (!transChecker.check((ID) d[0])) continue;
                    }

                    Record dRecord = transformRecord(
                            dSourceEntity, dTargetEntity, fmd, (ID) d[0], null, true, false, false);

                    this.previewRecords.add(dRecord);
                    TransformerPreview37.fillLabelOfReference(dRecord);

                    JSON m = UseFormsBuilder.instance.buildNewForm(dTargetEntity, dRecord, fakeMainId, getUser());
                    formModelDetails.add(m);
                }
                formModelDetailsMap.put(dTargetEntity.getName(), formModelDetails);

            } finally {
                FormsBuilderContextHolder.getMainIdOfDetail(true);
            }

            // 删除已有的
            if (targetExistsRecordId != null) {
                List<ID> detailIds = QueryHelper.detailIdsNoFilter(targetExistsRecordId);
                if (!detailIds.isEmpty()) {
                    formModelDetailsMap.put(dTargetEntity.getName() + "$DELETED", detailIds);
                }
            }
        }

        ((JSONObject) formModel).put(GeneralEntityService.HAS_DETAILS, formModelDetailsMap);

        return formModel;
    }

    // 合并目标
    private void mergeExistsAndTarget(Record transTargetRecord) {
        JSONObject fieldsMapping = transConfig.getJSONObject("fieldsMapping");
        Entity targetEntity = transTargetRecord.getEntity();

        transTargetRecord.setObjectValue(targetEntity.getPrimaryField().getName(), targetExistsRecordId);
        transTargetRecord.removeValue(EntityHelper.CreatedBy);
        transTargetRecord.removeValue(EntityHelper.CreatedOn);
        if (!fieldsMapping.containsKey(EntityHelper.OwningUser)) {
            transTargetRecord.removeValue(EntityHelper.OwningUser);
            transTargetRecord.removeValue(EntityHelper.OwningDept);
        }
    }

    // 获取主记录（如果是明细的话）
    private ID forceGetSpecMainId(ID targetRecordId) {
        Entity targetEntity = MetadataHelper.getEntity(targetRecordId.getEntityCode());
        if (targetEntity.getMainEntity() != null) {
            return QueryHelper.getMainIdByDetail(targetRecordId);
        }
        return null;
    }

    /**
     * 获取预览后产生的 Record
     *
     * @return
     * @see #preview(ID, ID, ID)
     */
    public List<Record> getPreviewRecords() {
        return previewRecords;
    }

    // --

    /**
     * 构造明细导入配置
     *
     * @param mainEntity
     * @return
     */
    public static List<Object> buildDetailImports39(Entity mainEntity) {
        List<Object> importsConfs = new ArrayList<>();
        for (Entity de : mainEntity.getDetialEntities()) {
            List<ConfigBean> cb = TransformManager.instance.getDetailImports(de.getName());
            if (!cb.isEmpty()) {
                for (ConfigBean c : cb) {
                    JSONObject trans = (JSONObject) EasyMetaFactory.valueOf(c.getString("source")).toJSON();
                    trans.put("transid", c.getID("id"));
                    trans.put("transName", c.getString("name"));

                    int ifAuto = ((JSONObject) c.getJSON("config")).getIntValue("importsMode2Auto");
                    if (ifAuto > 0) {
                        JSONArray importsFilter = ((JSONObject) c.getJSON("config")).getJSONArray("importsFilter");
                        Set<String> autoFields = new HashSet<>();
                        for (Object o : importsFilter) {
                            String name = ((JSONArray) o).getString(0);
                            autoFields.add(name.split("\\.")[1]);
                        }

                        if (!autoFields.isEmpty()) {
                            trans.put("auto", ifAuto);
                            trans.put("autoFields", autoFields);
                        }
                    }

                    trans.put("detailName", de.getName());
                    importsConfs.add(trans);
                }

            }
        }
        return importsConfs;
    }
}
