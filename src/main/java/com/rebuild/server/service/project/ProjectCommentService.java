/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.project;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import com.rebuild.server.metadata.EntityHelper;

/**
 * @author devezhao
 * @since 2020/7/27
 */
public class ProjectCommentService extends AtUserAwareService {

    protected ProjectCommentService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.ProjectTaskComment;
    }

    @Override
    public Record create(Record record) {
        record = super.create(record);
        checkAtUserAndNotification(record, record.getString("content"));
        return record;
    }
}
