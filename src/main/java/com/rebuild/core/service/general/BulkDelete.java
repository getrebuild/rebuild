/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.trigger.impl.FieldAggregation;
import lombok.extern.slf4j.Slf4j;

/**
 * 删除
 *
 * @author devezhao
 * @since 10/16/2018
 */
@Slf4j
public class BulkDelete extends BulkOperator {

    public BulkDelete(BulkContext context, GeneralEntityService ges) {
        super(context, ges);
    }

    @Override
    public Integer exec() {
        final ID[] records = prepareRecords();
        this.setTotal(records.length);

        String lastError = null;
        for (ID id : records) {
            if (Application.getPrivilegesManager().allowDelete(context.getOpUser(), id)) {
                try {
                    ges.delete(id, context.getCascades());
                    this.addSucceeded();
                } catch (DataSpecificationException ex) {
                    lastError = ex.getLocalizedMessage();
                    log.warn("Cannot delete `{}` because : {}", id, lastError);

                } finally {
                    // 可能有级联触发器
                    Object ts = FieldAggregation.cleanTriggerChain();
                    if (ts != null) log.info("Clean current-loop : {}", ts);
                }

            } else {
                log.warn("No have privileges to DELETE : {} < {}", id, context.getOpUser());
            }
            this.addCompleted();
        }

        if (getSucceeded() == 0 && lastError != null) {
            throw new DataSpecificationException(lastError);
        }

        return getSucceeded();
    }
}
