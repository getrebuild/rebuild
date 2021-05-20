/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.feeds;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.OperationDeniedException;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.i18n.Language;
import org.springframework.stereotype.Service;

/**
 * 动态
 *
 * @author devezhao
 * @since 2019/11/4
 */
@Service
public class FeedsService extends BaseFeedsService {

    protected FeedsService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.Feeds;
    }

    @Override
    public Record createOrUpdate(Record record) {
        Integer type = record.getInt("type");
        if (type != null && type == FeedsType.ANNOUNCEMENT.getMask()
                && !UserHelper.isAdmin(UserContextHolder.getUser())) {
            throw new OperationDeniedException(Language.$L("仅管理员可发布公告"));
        }

        return super.createOrUpdate(record);
    }
}
