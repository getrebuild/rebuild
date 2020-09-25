/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.util.support.QueryHelper;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.support.task.HeavyTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * 批量操作
 *
 * @author devezhao
 * @since 10/16/2018
 */
public abstract class BulkOperator extends HeavyTask<Integer> {

    protected static final Logger LOG = LoggerFactory.getLogger(BulkOperator.class);

    final protected BulkContext context;
    final protected GeneralEntityService ges;

    private ID[] records;

    /**
     * @param context
     * @param ges     可避免多次经由拦截器检查
     */
    protected BulkOperator(BulkContext context, GeneralEntityService ges) {
        super();
        this.context = context;
        this.ges = ges;
    }

    /**
     * 获取待操作记录
     *
     * @return
     */
    protected ID[] prepareRecords() {
        if (this.records != null) {
            return this.records;
        }

        if (context.getRecords() != null) {
            this.records = context.getRecords();
            setTotal(this.records.length);
            return this.records;
        }

        JSONObject asFilterExp = context.getCustomData();
        AdvFilterParser filterParser = new AdvFilterParser(asFilterExp);
        String sqlWhere = filterParser.toSqlWhere();
        // `(1=1)`.length < 10
        if (sqlWhere.length() < 10) {
            throw new SecurityException("Must specify filter items : " + sqlWhere);
        }

        Entity entity = MetadataHelper.getEntity(asFilterExp.getString("entity"));
        String sql = String.format("select %s from %s where (1=1) and %s",
                entity.getPrimaryField().getName(), entity.getName(), sqlWhere);

        // NOTE 注意没有分页
        Query query = Application.getQueryFactory().createQuery(sql, context.getOpUser());
        Object[][] array = QueryHelper.readArray(query);
        Set<ID> ids = new HashSet<>();
        for (Object[] o : array) {
            ids.add((ID) o[0]);
        }
        return ids.toArray(new ID[0]);
    }
}
