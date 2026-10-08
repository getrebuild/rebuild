/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.service;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.i18n.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author devezhao
 * @since 2026/8/12
 */
@Service
@Slf4j
public class AibotConfigService extends BaseConfigurationService {

    protected AibotConfigService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.AibotConfig;
    }

    @Override
    public Record create(Record record) {
        checkTypeGuard(record.getString("type"));
        return super.create(record);
    }

    @Override
    public Record update(Record record) {
        checkTypeGuard(record.getString("type"));
        return super.update(record);
    }

    @Override
    protected void throwIfNotSelf(ID cfgid) throws DataSpecificationException {
        if (!UserHelper.isAdmin(UserContextHolder.getUser())) {
            Object type = QueryHelper.queryFieldValue(cfgid, "type");
            checkTypeGuard(type == null ? null : type.toString());
        }
        super.throwIfNotSelf(cfgid);
    }

    // SKILL/KNOWLEDGE/AGENT 仅管理员可操作
    private void checkTypeGuard(String type) {
        if (UserHelper.isAdmin(UserContextHolder.getUser())) return;

        if (AibotConfigManager.TYPE_SKILL.equals(type)
                || AibotConfigManager.TYPE_KNOWLEDGE.equals(type)
                || AibotConfigManager.TYPE_AGENT.equals(type)) {
            throw new DataSpecificationException(Language.L("权限不足，访问被阻止"));
        }
    }

    @Override
    protected void cleanCache(ID cfgid) {
        AibotConfigManager.instance.clean(cfgid);
    }
}
