/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.task.TaskExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 表单配置
 *
 * @author devezhao-mbp
 * @since 2019/04/30
 */
@Service
@Slf4j
public class LayoutConfigService extends BaseConfigurationService {

    protected LayoutConfigService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.LayoutConfig;
    }

    @Override
    protected void cleanCache(ID cfgid) {
        BaseLayoutManager.instance.clean(cfgid);

        Object applyType = QueryHelper.queryFieldValue(cfgid, "applyType");
        if (BaseLayoutManager.TYPE_EASYACTION.equals(applyType)) {
            // NOTE 异步执行
            ThreadPool.exec(() -> {
                try {
                    EasyActionManager.instance.es5IfNeed(cfgid);
                } catch (Exception e) {
                    log.error("EasyActionManager ES5 error : {}", cfgid, e);
                }
            });
        }
    }
}
