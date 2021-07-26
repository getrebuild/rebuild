/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.feeds;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.OperationDeniedException;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.general.recyclebin.RecycleStore;
import com.rebuild.core.support.i18n.Language;
import org.springframework.stereotype.Service;

import java.util.Set;

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
            throw new OperationDeniedException(Language.L("仅管理员可发布公告"));
        }

        return super.createOrUpdate(record);
    }

    @Override
    public int delete(ID recordId) {
        // 只有动态本身可以恢复
        final RecycleStore recycleBin = useRecycleStore(recordId);

        // 先删评论
        Object[][] comments = Application.createQueryNoFilter(
                "select commentId from FeedsComment where feedsId = ?")
                .setParameter(1, recordId)
                .array();
        for (Object[] c : comments) {
            Application.getBean(FeedsCommentService.class).delete((ID) c[0]);
        }

        int d = super.delete(recordId);

        if (recycleBin != null) recycleBin.store();
        return d;
    }

    /**
     * 刷新提及
     *
     * @param record
     * @return
     */
    public Set<ID> awareMentionCreate(Record record) {
        return super.awareMentionCreate(record);
    }
}
