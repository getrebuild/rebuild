/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.utils.JSONUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 表单布局管理
 *
 * @author Zixin (RB)
 * @since 08/30/2018
 */
public class FormsManager extends BaseLayoutManager {

    public static final FormsManager instance = new FormsManager();

    protected FormsManager() {
    }

    /**
     * @param entity
     * @return
     */
    public ConfigBean getNewFormLayout(String entity) {
        return getFormLayout(entity, null);
    }

    /**
     * @param entity
     * @param recordId
     * @return
     */
    public ConfigBean getFormLayout(String entity, ID recordId) {
        Object[][] cached = getAllConfig(entity, TYPE_FORM);
        for (Object[] o : cached) {
            ConfigBean cb = findConfigBean(cached, (ID) o[0]);
            ShareToAttr attr = new ShareToAttr(cb);
            if (recordId == null) {
                if (attr.isFallback()) return cb;
            } else {
                if (attr.isMatchUseFilter(recordId)) return cb;
            }
        }

        return useBlank(entity);
    }

    // 未配置, 无符合
    private ConfigBean useBlank(String entity) {
        return new ConfigBean()
                .set("entity", entity)
                .set("elements", JSONUtils.EMPTY_ARRAY);
    }

    // -- ADMIN

    /**
     * @param formConfigId
     * @param entity
     * @return
     */
    public ConfigBean getFormLayout(ID formConfigId, String entity) {
        Object[][] cached = getAllConfig(entity, TYPE_FORM);
        for (Object[] o : cached) {
            if (formConfigId == null) {
                return findConfigBean(cached, (ID) o[0]);
            }

            if (formConfigId.equals(o[0])) {
                return findConfigBean(cached, formConfigId);
            }
        }

        return useBlank(entity);
    }

    /**
     * @param entity
     * @return
     */
    public List<ConfigBean> getAllFormsAttr(String entity) {
        Object[][] alls = getAllConfig(entity, TYPE_FORM);

        List<ConfigBean> flist = new ArrayList<>();
        for (Object[] o : alls) {
            ConfigBean cb = findConfigBean(alls, (ID) o[0]).remove("config");
            cb.remove("elements");
            flist.add(cb);
        }
        return flist;
    }

    @Override
    protected ConfigBean findConfigBean(Object[][] uses, ID cfgid) {
        ConfigBean cb = super.findConfigBean(uses, cfgid);
        if (cb == null) return null;

        for (Object[] c : uses) {
            if (c[0].equals(cfgid)) {
                cb.set("name", c[4]);
                break;
            }
        }

        cb.set("elements", cb.getJSON("config"))
                .remove("config");

        // attrs
        String shareTo = cb.getString("shareTo");
        if (JSONUtils.wellFormat(shareTo)) {
            cb.set("shareTo", JSON.parse(shareTo));
        }
        return cb;
    }

    // ~
    static class ShareToAttr {

        private final JSONObject attrs;
        protected ShareToAttr(ConfigBean cb) {
            Object s = cb.getObject("shareTo");
            if (s instanceof JSON) {
                this.attrs = (JSONObject) s;
            } else {
                // shareTo=ALL
                this.attrs = JSONUtils.toJSONObject("fallback", true);
            }
        }

        // 默认
        boolean isFallback() {
            return this.attrs.getBooleanValue("fallback");
        }

        // 符合使用条件
        boolean isMatchUseFilter(ID recordId) {
            JSONObject filter = this.attrs.getJSONObject("filter");
            if (ParseHelper.validAdvFilter(filter)) {
                return QueryHelper.isMatchAdvFilter(recordId, filter);
            }
            return true;
        }
    }
}
