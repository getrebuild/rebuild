/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.DefinedException;
import com.rebuild.core.configuration.general.FormsBuilder;
import com.rebuild.core.support.i18n.Language;

/**
 * 审批可修改字段表单
 *
 * @author devezhao
 * @since 2020/2/5
 */
public class FormBuilder {

    final private ID record;
    final private ID user;

    /**
     * @param record
     * @param user
     */
    public FormBuilder(ID record, ID user) {
        this.record = record;
        this.user = user;
    }

    /**
     * @param elements
     * @return
     */
    public JSONArray build(JSONArray elements) {
        Record data = UseFormsBuilder.instance.findRecord(record, user, elements);
        if (data == null) {
            throw new DefinedException(403, Language.L("无法读取审批记录"));
        }

        UseFormsBuilder.instance.buildModelElements(elements, data.getEntity(), data, user);
        return elements;
    }

    /**
     */
    static class UseFormsBuilder extends FormsBuilder {
        public static final UseFormsBuilder instance = new UseFormsBuilder();

        protected void buildModelElements(JSONArray elements, Entity entity, Record data, ID user) {
            super.buildModelElements(elements, entity, data, user, false);
        }

        protected Record findRecord(ID id, ID user, JSONArray elements) {
            return super.findRecord(id, user, elements);
        }
    }
}
