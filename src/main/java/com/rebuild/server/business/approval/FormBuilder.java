/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.approval;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.configuration.portals.FormsBuilder;

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
        Record data = FormsBuilder.instance.findRecord(record, user, elements);
        FormsBuilder.instance.buildModelElements(elements, data.getEntity(), data, user);
        return elements;
    }
}
