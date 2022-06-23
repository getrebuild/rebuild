/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;

import java.text.MessageFormat;

/**
 * @author devezhao
 * @since 2021/8/25
 */
public class QueryHelper {

    /**
     * 指定记录是否符合过滤条件
     *
     * @param recordId
     * @param advFilter
     * @return
     */
    public static boolean isMatchAdvFilter(ID recordId, JSONObject advFilter) {
        if (!ParseHelper.validAdvFilter(advFilter)) return true;

        String filterSql = new AdvFilterParser(advFilter).toSqlWhere();
        if (filterSql != null) {
            Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
            String sql = MessageFormat.format(
                    "select {0} from {1} where {0} = ? and {2}",
                    entity.getPrimaryField().getName(), entity.getName(), filterSql);
            Object[] m = Application.createQueryNoFilter(sql).setParameter(1, recordId).unique();
            return m != null;
        }
        return true;
    }

    /**
     * @param sql
     * @param useEntity 决定是否使用权限查询
     * @return
     */
    public static Query createQuery(String sql, Entity useEntity) {
        if (MetadataHelper.hasPrivilegesField(useEntity) || useEntity.getMainEntity() != null) {
            return Application.createQuery(sql);
        }
        return Application.createQueryNoFilter(sql);
    }
}
