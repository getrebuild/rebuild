/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.business.recyclebin;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.List;

/**
 * 记录序列化为 JSON
 *
 * @author devezhao
 * @since 2019/8/19
 */
public class RecycleBean implements Serializable {
    private static final long serialVersionUID = -1058552856844427594L;

    public static final String NAME_SLAVELIST = "$SLAVELIST$";

    final private ID recordId;

    /**
     * @param recordId
     */
    public RecycleBean(ID recordId) {
        this.recordId = recordId;
    }

    /**
     * 序列化（含明细）
     *
     * @return
     */
    public JSON serialize() {
        Entity entity = MetadataHelper.getEntity(this.recordId.getEntityCode());

        String sql = buildBaseSql(entity)
                .append(entity.getPrimaryField().getName())
                .append(" = ?")
                .toString();
        Record queryed = Application.createQueryNoFilter(sql).setParameter(1, this.recordId).record();
        JSONObject s = (JSONObject) queryed.serialize();

        Entity slaveEntity = entity.getSlaveEntity();
        if (slaveEntity == null) {
            return s;
        }

        // 明细
        String slaveSql = buildBaseSql(slaveEntity)
                .append(MetadataHelper.getSlaveToMasterField(slaveEntity).getName())
                .append(" = ?")
                .toString();
        List<Record> slaveQueryed = Application.createQueryNoFilter(slaveSql).setParameter(1, this.recordId).list();
        JSONArray slaveList = new JSONArray();
        for (Record r : slaveQueryed) {
            slaveList.add(r.serialize());
        }
        s.put(NAME_SLAVELIST, slaveList);

        return s;
    }

    private StringBuffer buildBaseSql(Entity entity) {
        StringBuffer sql = new StringBuffer("select ");
        sql.append(StringUtils.join(entity.getFieldNames(), ","))
                .append(" from ")
                .append(entity.getName())
                .append(" where ");
        return sql;
    }
}
