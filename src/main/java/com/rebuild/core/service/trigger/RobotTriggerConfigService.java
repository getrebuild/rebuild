/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

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
 * 触发器
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/24
 */
@Service
public class RobotTriggerConfigService extends BaseConfigurationService implements AdminGuard {

    protected RobotTriggerConfigService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.RobotTriggerConfig;
    }

    @Override
    protected boolean hasLock() {
        return true;
    }

    @Override
    protected void cleanCache(ID cfgid) {
        Object[] cfg = Application.createQueryNoFilter(
                "select belongEntity from RobotTriggerConfig where configId = ?")
                .setParameter(1, cfgid)
                .unique();
        Entity entity = MetadataHelper.getEntity((String) cfg[0]);
        RobotTriggerManager.instance.clean(entity);
    }
}
