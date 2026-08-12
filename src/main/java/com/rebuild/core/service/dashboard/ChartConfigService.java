/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.aibot2.JsonSchemaValidator;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.RbvFunction;
import org.springframework.stereotype.Service;

/**
 * 图表
 *
 * @author devezhao-mbp
 * @since 2019/06/04
 */
@Service
public class ChartConfigService extends BaseConfigurationService {

    protected ChartConfigService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.ChartConfig;
    }

    @Override
    protected void cleanCache(ID cfgid) {
        Object c = QueryHelper.queryFieldValue(cfgid, "config");
        RbvFunction.call().validateJsonSchema(JsonSchemaValidator.CHART_CONFIG, c);

        ChartManager.instance.clean(cfgid);
    }
}
