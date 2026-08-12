/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.service;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.configuration.general.CommonsConfigManager;
import com.rebuild.core.metadata.EntityHelper;
import org.springframework.stereotype.Service;

/**
 * AI 定时任务专用配置服务。不实现 AdminGuard，普通用户可操作自己的定时任务
 *
 * @author devezhao
 * @since 2026/8/9
 * @see com.rebuild.core.configuration.general.CommonsConfigService
 */
@Service
public class AiBotScheduleConfigService extends BaseConfigurationService {

    protected AiBotScheduleConfigService(PersistManagerFactory aPMFactory) {
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
