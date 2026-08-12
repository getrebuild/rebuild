/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.metadata.EntityHelper;
import org.springframework.stereotype.Service;

/**
 * @author devezhao
 * @since 2026/8/12
 */
@Service
public class AibotCommonsConfigService extends BaseConfigurationService {

    protected AibotCommonsConfigService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.AibotCommonsConfig;
    }

    @Override
    protected void cleanCache(ID cfgid) {
        AibotCommonsConfigManager.instance.clean(cfgid);
    }
}
