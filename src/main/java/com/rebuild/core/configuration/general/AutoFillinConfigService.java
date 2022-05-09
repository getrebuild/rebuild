/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.AdminGuard;
import org.springframework.stereotype.Service;

/**
 * 表单回填
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/18
 */
@Service
public class AutoFillinConfigService extends BaseConfigurationService implements AdminGuard {

    protected AutoFillinConfigService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.AutoFillinConfig;
    }

    @Override
    protected void cleanCache(ID cfgid) {
        Object[] cfg = Application.createQueryNoFilter(
                "select belongEntity,belongField from AutoFillinConfig where configId = ?")
                .setParameter(1, cfgid)
                .unique();
        if (cfg != null) {
            Field dest = MetadataHelper.getField((String) cfg[0], (String) cfg[1]);
            AutoFillinManager.instance.clean(dest);
        }
    }
}
