/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.service.DataSpecificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 删除
 *
 * @author devezhao
 * @since 10/16/2018
 */
public class BulkDelete extends BulkOperator {

    private static final Logger LOG = LoggerFactory.getLogger(BulkDelete.class);

    public BulkDelete(BulkContext context, GeneralEntityService ges) {
        super(context, ges);
    }

    @Override
    protected Integer exec() {
        final ID[] records = prepareRecords();
        this.setTotal(records.length);

        for (ID id : records) {
            if (Application.getPrivilegesManager().allowDelete(context.getOpUser(), id)) {
                try {
                    ges.delete(id, context.getCascades());
                    this.addSucceeded();
                } catch (DataSpecificationException ex) {
                    LOG.warn("Cannot delete : " + id + " Ex : " + ex);
                }
            } else {
                LOG.warn("No have privileges to DELETE : " + context.getOpUser() + " > " + id);
            }
            this.addCompleted();
        }

        return getSucceeded();
    }
}
