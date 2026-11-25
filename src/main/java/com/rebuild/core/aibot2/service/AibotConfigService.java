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
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.Config;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.configuration.general.ShareToManager;
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
        String type = record.getString("type");
        checkTypeGuard(type);
        checkPublishBindUser(record);
        // AGENT/MINAPP 默认共享给全部用户
        if (StringUtils.isBlank(record.getString("shareTo"))
                && (AibotConfigManager.TYPE_AGENT.equals(type) || AibotConfigManager.TYPE_MINAPP.equals(type))) {
            record.setString("shareTo", ShareToManager.SHARE_ALL);
        }
        return super.create(record);
    }

    @Override
    public Record update(Record record) {
        checkTypeGuard(record.getString("type"));
        checkPublishBindUser(record);
        record = keepStoredSecret(record);
        checkApiConnectivity(record);
        return super.update(record);
    }

    // 模型密钥不再下发浏览器，保存时未携带则沿用已存储的密钥
    private Record keepStoredSecret(Record record) {
        if (record.getPrimary() == null) return record;

        JSONObject conf = (JSONObject) JSONUtils.parseSafe(record.getString("config"));
        if (conf == null || StringUtils.isNotBlank(conf.getString("AibotDSSecret"))) return record;

        Object type = record.getString("type");
        if (type == null) type = QueryHelper.queryFieldValue(record.getPrimary(), "type");
        if (!AibotConfigManager.TYPE_MODEL.equals(String.valueOf(type))) return record;

        Object stored = QueryHelper.queryFieldValue(record.getPrimary(), "config");
        JSONObject storedConf = stored == null ? null : (JSONObject) JSONUtils.parseSafe(stored.toString());
        String secret = storedConf == null ? null : storedConf.getString("AibotDSSecret");
        if (StringUtils.isBlank(secret)) return record;

        conf.put("AibotDSSecret", secret);
        record.setString("config", conf.toJSONString());
        return record;
    }

    // AGENT 的 API 配置（地址、秘钥、模型）有变更时测试连通性
    private void checkApiConnectivity(Record record) {
        JSONObject conf = (JSONObject) JSONUtils.parseSafe(record.getString("config"));
        if (conf == null) return;

        String type = record.getString("type");
        if (type == null && record.getPrimary() != null) {
            Object t = QueryHelper.queryFieldValue(record.getPrimary(), "type");
            type = t == null ? null : t.toString();
        }
        // 引用模型配置的 AGENT 或未自带 API 配置的 AGENT，不测试智能体自身连通性
        if (AibotConfigManager.TYPE_AGENT.equals(type)
                && (StringUtils.isNotBlank(conf.getString("modelConfigId")) || !hasLegacyApiConfig(conf))) {
            return;
        }

        Object stored = QueryHelper.queryFieldValue(record.getPrimary(), "config");
        JSONObject storedConf = stored == null ? null : (JSONObject) JSONUtils.parseSafe(stored.toString());
        if (sameApiConfig(conf, storedConf)) return;

        String error = Config.testApiConnectivity(
                conf.getString("AibotDSUrl"), conf.getString("AibotDSSecret"), conf.getString("AibotBaseDefModel"));
        if (error != null) throw new DataSpecificationException(error);
    }

    private static boolean hasLegacyApiConfig(JSONObject conf) {
        return StringUtils.isNotBlank(conf.getString("AibotDSUrl"))
                || StringUtils.isNotBlank(conf.getString("AibotDSSecret"))
                || StringUtils.isNotBlank(conf.getString("AibotBaseDefModel"));
    }

    // 地址/秘钥/模型是否均无变更（仅 AGENT 配置使用这些字段，其他类型恒无变更）
    private static boolean sameApiConfig(JSONObject conf, JSONObject stored) {
        String[] keys = {"AibotDSUrl", "AibotDSSecret", "AibotBaseDefModel"};
        for (String key : keys) {
            String now = StringUtils.trimToEmpty(conf.getString(key));
            String old = stored == null ? "" : StringUtils.trimToEmpty(stored.getString(key));
            if (!now.equals(old)) return false;
        }
        return true;
    }

    @Override
    protected void throwIfNotSelf(ID cfgid) throws DataSpecificationException {
        if (!UserHelper.isAdmin(UserContextHolder.getUser())) {
            Object type = QueryHelper.queryFieldValue(cfgid, "type");
            checkTypeGuard(type == null ? null : type.toString());
        }
        super.throwIfNotSelf(cfgid);
    }

    // SKILL/KNOWLEDGE/AGENT/MINAPP 仅管理员可操作
    private void checkTypeGuard(String type) {
        if (UserHelper.isAdmin(UserContextHolder.getUser())) return;

        if (AibotConfigManager.TYPE_SKILL.equals(type)
                || AibotConfigManager.TYPE_KNOWLEDGE.equals(type)
                || AibotConfigManager.TYPE_AGENT.equals(type)
                || AibotConfigManager.TYPE_MINAPP.equals(type)
                || AibotConfigManager.TYPE_MODEL.equals(type)) {
            throw new DataSpecificationException(Language.L("权限不足，访问被阻止"));
        }
    }

    private void checkPublishBindUser(Record record) {
        String type = record.getString("type");
        if (type == null && record.getPrimary() != null) {
            // 更新记录可能未携带 type
            Object t = QueryHelper.queryFieldValue(record.getPrimary(), "type");
            type = t == null ? null : t.toString();
        }
        if (!AibotConfigManager.TYPE_AGENT.equals(type) && !AibotConfigManager.TYPE_MINAPP.equals(type)) return;

        String config = record.getString("config");
        if (StringUtils.isBlank(config)) return;

        JSONObject conf = (JSONObject) JSONUtils.parseSafe(config);
        if (conf == null) return;

        JSONObject publish = conf.getJSONObject("publish");
        if (publish == null || !publish.getBooleanValue("enabled")) return;

        String bindUser = conf.getString("bindUser");
        if (!ID.isId(bindUser) || !UserHelper.isActive(ID.valueOf(bindUser))) {
            throw new DataSpecificationException(Language.L("公开访问必须绑定有效用户"));
        }
        // 公开访问会以绑定用户身份调用接口与工具，管理员绑定等同于对外放开管理权限
        if (UserHelper.isAdmin(ID.valueOf(bindUser))) {
            throw new DataSpecificationException(Language.L("公开访问绑定的用户不能是管理员，请改用专用账号"));
        }
    }

    @Override
    protected void cleanCache(ID cfgid) {
        AibotConfigManager.instance.clean(cfgid);
    }
}
