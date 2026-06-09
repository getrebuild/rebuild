/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.AdminGuard;
import org.springframework.stereotype.Service;

/**
 * @author ZHAO
 * @since 4/15/2026
 */
@Service
public class CommonsConfigService  extends BaseConfigurationService implements AdminGuard {

    protected CommonsConfigService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.CommonsConfig;
    }

    @Override
    protected void cleanCache(ID cfgid) {
        CommonsConfigManager.instance.clean(cfgid);
    }
}
