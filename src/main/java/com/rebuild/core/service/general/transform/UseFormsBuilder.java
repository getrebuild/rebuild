package com.rebuild.core.service.general.transform;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.general.FormsBuilder;
import com.rebuild.core.service.DataSpecificationException;

/**
 * 使用 FormsBuilder
 *
 * @author Zixin
 * @since 2024/1/10
 */
public class UseFormsBuilder extends FormsBuilder {

    protected static final UseFormsBuilder instance = new UseFormsBuilder();

    /**
     * @param entity
     * @param record
     * @param user
     * @return
     */
    public JSON buildNewForm(Entity entity, Record record, ID user) {
        JSON model = buildForm(entity.getName(), user, null);
        String hasError = ((JSONObject) model).getString("error");
        if (hasError != null) throw new DataSpecificationException(hasError);

        JSONArray elements = ((JSONObject) model).getJSONArray("elements");
        buildModelElements(elements, entity, record, user, false, true);
        return model;
    }

    /**
     * @param entity
     * @param record
     * @param user
     * @return
     */
    public static JSON buildNewFormWithRecord(Entity entity, Record record, ID user) {
        return instance.buildNewForm(entity, record, user);
    }
}
