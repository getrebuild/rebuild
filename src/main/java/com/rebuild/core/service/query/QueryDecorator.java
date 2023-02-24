/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Filter;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.query.AjqlQuery;
import cn.devezhao.persist4j.query.Result;
import com.rebuild.core.metadata.MetadataHelper;

/**
 * @author RB
 * @since 2022/06/21
 */
public class QueryDecorator extends AjqlQuery {
    private static final long serialVersionUID = 1098465709501052707L;

    private Boolean be;
    private Result result;

    protected QueryDecorator(String ajql, PersistManagerFactory managerFactory, Filter filter) {
        super(ajql, managerFactory, filter);
    }

    @Override
    public Result result() {
        if (be == null) {
            Entity e = getRootEntity();
            be = MetadataHelper.isBusinessEntity(e) || MetadataHelper.isBizzEntity(e);
        }

        // 仅业务实体或BIZZ
        if (be) {
            if (result == null) result = new ResultDecorator(this);
            return result;
        } else {
            return super.result();
        }
    }
}
