/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;

/**
 * 取消共享
 *
 * @author devezhao
 * @since 12/19/2018
 */
public class BulkUnshare extends BulkOperator {

    public BulkUnshare(BulkContext context, GeneralEntityService ges) {
        super(context, ges);
    }

    @Override
    public Integer exec() {
        final ID[] records = prepareRecords();
        this.setTotal(records.length);

        final ID realTarget = context.getTargetRecord();

        // 只需要验证记录权限
        if (!Application.getPrivilegesManager().allowShare(context.getOpUser(), realTarget)) {
            this.setCompleted(records.length);
            return 0;
        }

        // 这里的取消实际是删除了共享表记录
        for (ID id : records) {
            int a = ges.unshare(realTarget, id);
            if (a > 0) {
                this.addSucceeded();
            }
            this.addCompleted();
        }

        return getSucceeded();
    }
}
