/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import com.rebuild.core.metadata.impl.DynamicMetadataContextHolder;
import com.rebuild.core.support.task.HeavyTask;

/**
 * TODO 从商业库导入业务模块等
 *
 * @author devezhao
 * @since 2020/9/29
 */
public class BusinessModelImporter extends HeavyTask<Void> {

    private String[] entities;

    public BusinessModelImporter(String[] entities) {
        this.entities = entities;
    }

    @Override
    protected Void exec() throws Exception {
        DynamicMetadataContextHolder.setSkipRefentityCheck();
        DynamicMetadataContextHolder.setSkipLanguageRefresh();


        return null;
    }

    @Override
    protected void completedAfter() {
        super.completedAfter();

        DynamicMetadataContextHolder.isSkipRefentityCheck(true);
        DynamicMetadataContextHolder.isSkipLanguageRefresh(true);
    }
}
