/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.service;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
        checkAgentConfig(record);
        return super.create(record);
    }

    @Override
    public Record update(Record record) {
        checkAgentConfig(record);
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

    // AGENT 的代理用户校验，以及秘钥留空时保持原值
    private void checkAgentConfig(Record record) {
        if (!AibotConfigManager.TYPE_AGENT.equals(record.getString("type"))) return;

        JSONObject conf = (JSONObject) JSONUtils.parseSafe(record.getString("config"));
        if (conf == null) return;

        String bindUser = conf.getString("bindUser");
        if (StringUtils.isNotBlank(bindUser)) {
            ID bindUser2 = ID.isId(bindUser) ? ID.valueOf(bindUser) : null;
            if (bindUser2 == null || UserHelper.isSystemUser(bindUser2) || !UserHelper.isActive(bindUser2)) {
                throw new DataSpecificationException(Language.L("代理用户无效"));
            }
        }

        if (record.getPrimary() == null || StringUtils.isNotBlank(conf.getString("AibotDSSecret"))) return;

        Object[] o = Application.createQueryNoFilter("select config from AibotConfig where configId = ?")
                .setParameter(1, record.getPrimary())
                .unique();
        JSONObject old = o == null ? null : (JSONObject) JSONUtils.parseSafe((String) o[0]);
        if (old != null && StringUtils.isNotBlank(old.getString("AibotDSSecret"))) {
            conf.put("AibotDSSecret", old.getString("AibotDSSecret"));
            record.setString("config", conf.toJSONString());
        }
    }
}
