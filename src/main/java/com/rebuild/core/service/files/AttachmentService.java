/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.files;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.general.ObservableService;
import com.rebuild.core.service.general.recyclebin.RecycleStore;
import org.springframework.stereotype.Service;

/**
 * @author ZHAO
 * @since 2025/7/14
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

        int d = super.delete(recordId);

        if (recycleBin != null) recycleBin.store();
        return d;
    }
}
