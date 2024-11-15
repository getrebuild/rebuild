package com.rebuild.core.service.general.transform;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.general.FormsBuilder;
import com.rebuild.core.configuration.general.FormsBuilderContextHolder;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.utils.JSONUtils;

/**
 * 使用 FormsBuilder
 *
 * @author Zixin
 * @since 2024/1/10
 */
public class UseFormsBuilder extends FormsBuilder {

    protected static final UseFormsBuilder instance = new UseFormsBuilder();

    /**
     * 构建新表单，使用指定记录（数据）
     *
     * @param entity
     * @param record
     * @param mainid
     * @param user
     * @return
     */
    public JSON buildNewForm(Entity entity, Record record, Object mainid, ID user) {
        return buildForm(entity, record, mainid, user, false);
    }

    /**
     * @param entity
     * @param record
     * @param mainid
     * @param user
     * @param isNew
     * @return
     */
    public JSON buildForm(Entity entity, Record record, Object mainid, ID user, boolean isNew) {
        JSON model = buildForm(entity.getName(), user, isNew ? null : record.getPrimary());
        String hasError = ((JSONObject) model).getString("error");
        if (hasError != null) throw new DataSpecificationException(hasError);

        // 明细带主ID
        if (mainid != null) {
            JSONObject initialVal = JSONUtils.toJSONObject(FormsBuilder.DV_MAINID, mainid);
            setFormInitialValue(entity, model, initialVal);
        }

        JSONArray elements = ((JSONObject) model).getJSONArray("elements");
        buildModelElements(elements, entity, record, user, false, true);
        return model;
    }

    /**
     * @param entity
     * @param record
     * @param mainid 针对明细
     * @param user
     * @return
     */
    public static JSON buildNewFormWithRecord(Entity entity, Record record, ID mainid, ID user) {
        return buildFormWithRecord(entity, record, mainid, user, true);
    }

    /**
     * @param entity
     * @param record
     * @param mainid 针对明细
     * @param user
     * @param isNew
     * @return
     */
    public static JSON buildFormWithRecord(Entity entity, Record record, ID mainid, ID user, boolean isNew) {
        if (mainid != null) FormsBuilderContextHolder.setMainIdOfDetail(mainid);

        try {
            return instance.buildForm(entity, record, mainid, user, isNew);
        } finally {
            if (mainid != null) FormsBuilderContextHolder.getMainIdOfDetail(true);
        }
    }
}
