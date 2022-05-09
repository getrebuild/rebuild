/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.AdminGuard;
import org.springframework.stereotype.Service;

/**
 * 数据报表
 *
 * @author devezhao
 * @since 2019/8/13
 */
@Service
public class DataReportConfigService extends BaseConfigurationService implements AdminGuard {

    protected DataReportConfigService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.DataReportConfig;
    }

    @Override
    protected void cleanCache(ID cfgid) {
        Object[] c = Application.createQueryNoFilter(
                "select belongEntity from DataReportConfig where configId = ?")
                .setParameter(1, cfgid)
                .unique();
        Entity entity = MetadataHelper.getEntity((String) c[0]);
        DataReportManager.instance.clean(entity);
    }
}
