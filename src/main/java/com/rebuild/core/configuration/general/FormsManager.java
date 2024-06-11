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
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 表单布局管理
 *
 * @author Zixin (RB)
 * @since 08/30/2018
 */
@Slf4j
public class FormsManager extends BaseLayoutManager {

    public static final FormsManager instance = new FormsManager();

    protected FormsManager() {}

    // 表单布局适用于
    public static int APPLY_ONNEW = 1;
    public static int APPLY_ONEDIT = 2;
    public static int APPLY_ONVIEW = 4;

    /**
     * @param entity
     * @return
     */
    public ConfigBean getNewFormLayout(String entity) {
        return getFormLayout(entity, null, APPLY_ONNEW);
    }

    /**
     * @param entity
     * @param recordOrLayoutId 指定布局
     * @param applyType
     * @return
     */
    public ConfigBean getFormLayout(String entity, ID recordOrLayoutId, int applyType) {
        final Object[][] alls = getAllConfig(entity, TYPE_FORM);

        // TODO `applyType` 暂未用

        ConfigBean use = null;

        // 1.指定布局
        if (recordOrLayoutId != null && recordOrLayoutId.getEntityCode() == EntityHelper.LayoutConfig) {
            for (Object[] o : alls) {
                if (recordOrLayoutId.equals(o[0])) {
                    use = findConfigBean(alls, (ID) o[0]);;
                    break;
                }
            }

            if (use == null) {
                log.warn("Spec layout not longer exists : {}", recordOrLayoutId);
                recordOrLayoutId = null;
            }
        }

        // 2.使用布局
        if (use == null) {
            for (Object[] o : alls) {
                ConfigBean cb = findConfigBean(alls, (ID) o[0]);
                ShareToAttr attr = new ShareToAttr(cb);
                if (recordOrLayoutId == null) {
                    if (attr.isFallback()) {
                        use = cb;
                        break;
                    }
                } else {
                    if (attr.isMatchUseFilter(recordOrLayoutId)) {
                        use = cb;
                        break;
                    }
                }
            }
        }

        // 3.默认布局（fallback）
        if (use == null && recordOrLayoutId != null) {
            for (Object[] o : alls) {
                ConfigBean cb = findConfigBean(alls, (ID) o[0]);
                ShareToAttr attr = new ShareToAttr(cb);
                if (attr.isFallback()) {
                    use = cb;
                    break;
                }
            }
        }

        if (use != null) {
            use.set("entity", entity)
                    .remove("shareTo").remove("name");
            return use;
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
        final Object[][] alls = getAllConfig(entity, TYPE_FORM);
        for (Object[] o : alls) {
            if (formConfigId == null) {
                return findConfigBean(alls, (ID) o[0]);
            }

            if (formConfigId.equals(o[0])) {
                return findConfigBean(alls, formConfigId);
            }
        }

        return useBlank(entity);
    }

    /**
     * @param entity
     * @return
     */
    public List<ConfigBean> getAllFormsAttr(String entity) {
        final Object[][] alls = getAllConfig(entity, TYPE_FORM);

        List<ConfigBean> flist = new ArrayList<>();
        for (Object[] o : alls) {
            ConfigBean cb = findConfigBean(alls, (ID) o[0]).remove("config");
            flist.add(cb.remove("elements"));
        }

        // A-Z
        flist.sort((o1, o2) -> {
            String name1 = Objects.toString(o1.getString("name"), "0");
            String name2 = Objects.toString(o2.getString("name"), "0");
            return name1.compareTo(name2);
        });

        return flist;
    }

    @Override
    protected ConfigBean findConfigBean(Object[][] uses, ID cfgid) {
        ConfigBean cb = super.findConfigBean(uses, cfgid);
        if (cb == null) return null;

        // 补充信息
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
            return false;
        }
    }
}
