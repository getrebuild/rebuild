/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.impl.DynamicMetadataContextHolder;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.core.support.task.TaskExecutors;

/**
 * TODO 从商业库导入业务模块等
 *
 * @author devezhao
 * @since 2020/9/29
 */
public class BusinessModelImporter extends HeavyTask<Integer> {

    private String[] entities;

    public BusinessModelImporter(String[] entities) {
        this.entities = entities;
    }

    @Override
    protected Integer exec() throws Exception {
        DynamicMetadataContextHolder.setSkipRefentityCheck();
        DynamicMetadataContextHolder.setSkipLanguageRefresh();
        this.setTotal(entities.length);

        for (String e : entities) {
            JSONObject data = (JSONObject) RBStore.fetchBusinessModel(e);
            String created = (String) TaskExecutors.exec(new MetaschemaImporter(data));
            LOG.info("Entity created : " + created);
            this.addCompleted();
        }

        return entities.length;
    }

    @Override
    protected void completedAfter() {
        super.completedAfter();

        DynamicMetadataContextHolder.isSkipRefentityCheck(true);
        DynamicMetadataContextHolder.isSkipLanguageRefresh(true);
    }
}
