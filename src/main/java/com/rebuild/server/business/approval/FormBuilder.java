/*
rebuild - Building your business-systems freely.
Copyright (C) 2020 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
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
        FormsBuilder.instance.buildModelElements(
                elements, MetadataHelper.getEntity(record.getEntityCode()), data, user, false);
        return elements;
    }
}
