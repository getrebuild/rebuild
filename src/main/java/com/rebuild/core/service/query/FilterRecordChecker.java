/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;

/**
 * 检查给定记录是否符合给定条件
 *
 * @author devezhao
 * @since 2020/10/30
 */
public class FilterRecordChecker {

    private JSONObject filterExpr;

    public FilterRecordChecker(JSONObject filterExpr) {
        this.filterExpr = filterExpr;
    }

    /**
     * @param recordId
     * @return
     */
    public boolean check(ID recordId) {
        if (filterExpr == null || filterExpr.isEmpty()
                || filterExpr.getJSONArray("items") == null || filterExpr.getJSONArray("items").isEmpty()) {
            return true;
        }

        Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());

        String sqlWhere = new AdvFilterParser(filterExpr, entity)
                .toSqlWhere();
        sqlWhere += String.format(" and (%s = '%s')", entity.getPrimaryField().getName(), recordId);

        String checkSql = String.format("select %s from %s where %s",
                entity.getPrimaryField().getName(), entity.getName(), sqlWhere);

        return Application.createQueryNoFilter(checkSql).unique() != null;
    }
}
