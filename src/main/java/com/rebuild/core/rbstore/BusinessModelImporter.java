/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.DynamicMetadataContextHolder;
import com.rebuild.core.support.task.HeavyTask;

/**
 * 批量导入业务模块
 *
 * @author devezhao
 * @since 2020/9/29
 */
public class BusinessModelImporter extends HeavyTask<Integer> {

    private final String[] modelFiles;

    public BusinessModelImporter(String[] modelFiles) {
        this.modelFiles = modelFiles;
    }

    @Override
    protected Integer exec() {
        DynamicMetadataContextHolder.setSkipRefentityCheck();
        DynamicMetadataContextHolder.setSkipLanguageRefresh();
        this.setTotal(modelFiles.length);

        for (String fileUrl : modelFiles) {
            JSONObject data;
            if (fileUrl.startsWith("http")) {
                data = (JSONObject) RBStore.fetchRemoteJson(fileUrl);
            } else {
                data = (JSONObject) RBStore.fetchMetaschema(fileUrl);
            }

            String created = new MetaschemaImporter(data).exec();
            LOG.info("Entity created : " + created);
            this.addCompleted();
            this.addSucceeded();
        }

        return modelFiles.length;
    }

    @Override
    protected void completedAfter() {
        super.completedAfter();

        DynamicMetadataContextHolder.isSkipRefentityCheck(true);
        DynamicMetadataContextHolder.isSkipLanguageRefresh(true);

        MetadataHelper.getMetadataFactory().refresh(false);
    }
}
