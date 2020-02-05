/*
rebuild - Building your business-systems freely.
Copyright (C) 2020 devezhao <zhaofang123@gmail.com>

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
For more information, please see <https://getrebuild.com>
*/

package com.rebuild.server.business.approval;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.configuration.portals.FormsBuilder;
import com.rebuild.server.metadata.MetadataHelper;

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
        FormsBuilder.instance.buildModelElements(elements, data.getEntity(), data, user, false);
        return elements;
    }
}
