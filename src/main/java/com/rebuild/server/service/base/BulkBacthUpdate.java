/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.service.base;

import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.helper.datalist.BatchOperatorQuery;

/**
 * 批量修改
 *
 * @author devezhao
 * @since 2019/12/2
 */
public class BulkBacthUpdate extends BulkOperator {

    protected BulkBacthUpdate(BulkContext context, GeneralEntityService ges) {
        super(context, ges);
    }

    @Override
    protected Integer exec() throws Exception {
        final ID[] willUpdates = prepareRecords();
        this.setTotal(willUpdates.length);

        JSONArray updateContents = context.getCustomData().getJSONArray("updateContents");
        System.out.println(updateContents);

        int updates = 0;
        for (ID id : willUpdates) {
            ThreadPool.waitFor(500);
            this.addCompleted();
            updates++;
        }

        this.setCompleted(willUpdates.length);
        return updates;
    }

    @Override
    protected ID[] prepareRecords() {
        JSONObject customData = context.getCustomData();
        int dataRange = customData.getIntValue("_dataRange");
        BatchOperatorQuery query = new BatchOperatorQuery(dataRange, customData.getJSONObject("queryData"));
        return query.getQueryedRecords();
    }
}
