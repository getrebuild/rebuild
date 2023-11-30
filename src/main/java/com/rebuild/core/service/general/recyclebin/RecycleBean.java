/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.recyclebin;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.List;

/**
 * 记录序列化为 JSON
 *
 * @author devezhao
 * @since 2019/8/19
 */
@Slf4j
public class RecycleBean implements Serializable {
    private static final long serialVersionUID = -1058552856844427594L;

    public static final String NAME_DETAILLIST = "$SLAVELIST$";

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
        if (queryed == null) {
            log.warn("Serialize record not exists : {}", this.recordId);
            return null;
        }

        JSONObject s = (JSONObject) queryed.serialize();

        Entity detailEntity = entity.getDetailEntity();
        if (detailEntity == null) return s;

        // v36 多明细
        JSONArray detailList = new JSONArray();
        for (Entity de : entity.getDetialEntities()) {
            String detailSql = buildBaseSql(de)
                    .append(MetadataHelper.getDetailToMainField(de).getName())
                    .append(" = ?")
                    .toString();
            List<Record> detailQueryed = Application.createQueryNoFilter(detailSql).setParameter(1, this.recordId).list();
            for (Record r : detailQueryed) {
                JSONObject item = (JSONObject) r.serialize();
                item.put(RestoreRecordCreator.META_FIELD, de.getName());
                detailList.add(item);
            }
        }
        s.put(NAME_DETAILLIST, detailList);

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
