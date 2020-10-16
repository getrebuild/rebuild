/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.general.QuickCodeReindexTask;

/**
 * @author devezhao
 * @since 01/04/2019
 */
public class BaseServiceImpl extends BaseService {

    public BaseServiceImpl(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return 0;
    }

    @Override
    public Record create(Record record) {
        setQuickCodeValue(record);
        return super.create(record);
    }

    @Override
    public Record update(Record record) {
        setQuickCodeValue(record);
        return super.update(record);
    }

    /**
     * 设置助记码
     *
     * @param record
     */
    private void setQuickCodeValue(Record record) {
        // 已设置了则不再设置
        if (record.hasValue(EntityHelper.QuickCode)) {
            return;
        }
        // 无助记码字段
        if (!record.getEntity().containsField(EntityHelper.QuickCode)) {
            return;
        }

        String quickCode = QuickCodeReindexTask.generateQuickCode(record);
        if (quickCode != null) {
            record.setString(EntityHelper.QuickCode, quickCode);
        }
    }
}
