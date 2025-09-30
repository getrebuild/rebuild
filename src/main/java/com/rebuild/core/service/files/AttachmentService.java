/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.files;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.ObservableService;
import com.rebuild.core.service.general.recyclebin.RecycleStore;
import org.springframework.stereotype.Service;

/**
 * Only for 文件, Not 附件.
 * NOTE: 只具备回收站功能, 无变更历史记录
 *
 * @author ZHAO
 * @since 2025/7/14
 * @see AttachmentAwareObserver
 */
@Service
public class AttachmentService extends ObservableService {

    protected AttachmentService(PersistManagerFactory aPMFactory) {
        super(aPMFactory, false, false);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.Attachment;
    }

    @Override
    public int delete(ID recordId) {
        final RecycleStore recycleBin = useRecycleStore(recordId);

        // v4.2 标记删除
        Record d = EntityHelper.forUpdate(recordId, UserService.SYSTEM_USER, false);
        d.setBoolean(EntityHelper.IsDeleted, true);
        d.setDate(EntityHelper.ModifiedOn, CalendarUtils.now());
        Application.getCommonsService().update(d, false);

        if (recycleBin != null) recycleBin.store();
        return 1;
    }
}
