/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.core.support.RbvFunction;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author devezhao
 * @since 2020/10/27
 */
@Service
public class TransformConfigService extends BaseConfigurationService implements AdminGuard {

    protected TransformConfigService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.TransformConfig;
    }

    @Override
    protected void cleanCache(ID cfgid) {
        Object[] config = Application.createQueryNoFilter(
                "select config from TransformConfig where configId = ?")
                .setParameter(1, cfgid)
                .unique();
        if (config != null && StringUtils.isNotBlank((String) config[0])) {
            RbvFunction.call().validateJsonSchema("transform-config", JSON.parseObject((String) config[0]));
        }
        TransformManager.instance.clean(cfgid);
    }
}
