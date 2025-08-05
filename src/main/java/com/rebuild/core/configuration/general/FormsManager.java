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
import org.springframework.util.Assert;

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
    public static int APPLY_NEW = 1;
    public static int APPLY_EDIT = 2;
    public static int APPLY_VIEW = 4;

    /**
     * @param entity
     * @return
     */
    public ConfigBean getNewFormLayout(String entity) {
        return getFormLayout(entity, null, APPLY_NEW);
    }

    /**
     * @param entity
     * @param recordOrLayoutId 指定布局
     * @param applyType
     * @return
     */
    public ConfigBean getFormLayout(String entity, ID recordOrLayoutId, int applyType) {
        Assert.notNull(entity, "[entity] cannot be null");
        final Object[][] allConfs = getAllConfig(entity, TYPE_FORM);

        // TODO `applyType` 暂未用

        ConfigBean use = null;

        // 1.指定布局
        if (recordOrLayoutId != null && recordOrLayoutId.getEntityCode() == EntityHelper.LayoutConfig) {
            use = findConfigBean(allConfs, recordOrLayoutId);
            if (use == null) {
                log.warn("Spec form-layout not longer exists : {}", recordOrLayoutId);
                recordOrLayoutId = null;
            }
        }

        // 2.查找布局
        if (use == null) {
            // 优先使用条件匹配的
            for (Object[] o : allConfs) {
                ConfigBean cb = findConfigBean(allConfs, (ID) o[0]);
                ShareToAttr attr = new ShareToAttr(cb);
                if (recordOrLayoutId == null) {
                    if (attr.isFallback() || attr.isForNew()) {
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

            // 默认优先级
            if (recordOrLayoutId == null) {
                use = findDefault(allConfs);
            }
        }

        // 3.默认布局
        if (use == null && recordOrLayoutId != null) {
            for (Object[] o : allConfs) {
                ConfigBean cb = findConfigBean(allConfs, (ID) o[0]);
                ShareToAttr attr = new ShareToAttr(cb);
                if (attr.isFallback()) {
                    use = cb;
                    break;
                }
            }
        }

        if (use != null) {
            Object shareTo = use.getObject("shareTo");
            if (shareTo instanceof JSONObject) {
                JSONObject shareTo4Attr = (JSONObject) shareTo;
                Object o;
                if ((o = shareTo4Attr.get("verticalLayout")) != null) use.set("verticalLayout", o);
                if ((o = shareTo4Attr.get("detailsFromsAttr")) != null) use.set("detailsFromsAttr", o);
            }

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

    // 默认优先级布局
    private ConfigBean findDefault(Object[][] alls) {
        for (Object[] o : alls) {
            ConfigBean cb = findConfigBean(alls, (ID) o[0]);
            ShareToAttr attr = new ShareToAttr(cb);
            if (attr.isFallback() && attr.isForNew()) return cb;
        }
        return null;
    }

    // -- ADMIN

    /**
     * @param formConfigId
     * @param entity
     * @return
     */
    public ConfigBean getFormLayout(ID formConfigId, String entity) {
        final Object[][] alls = getAllConfig(entity, TYPE_FORM);

        // 高优先级
        if (formConfigId == null) {
            ConfigBean best = findDefault(alls);
            if (best != null) return best;
        }

        // 次优先级
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
        return getAllFormsAttr(entity, false);
    }

    /**
     * @param entity
     * @param forNew 仅新建布局
     * @return
     */
    public List<ConfigBean> getAllFormsAttr(String entity, boolean forNew) {
        final Object[][] alls = getAllConfig(entity, TYPE_FORM);

        List<ConfigBean> faList = new ArrayList<>();
        for (Object[] o : alls) {
            ConfigBean cb = findConfigBean(alls, (ID) o[0]).remove("config");
            cb.remove("elements");
            if (forNew) {
                if (new ShareToAttr(cb).isForNew()) faList.add(cb.remove("shareTo"));
            } else {
                faList.add(cb);
            }
        }

        // A-Z
        faList.sort((o1, o2) -> {
            String name1 = Objects.toString(o1.getString("name"), "0");
            String name2 = Objects.toString(o2.getString("name"), "0");
            return name1.compareTo(name2);
        });

        return faList;
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
            JSONObject shareTo2 = (JSONObject) JSON.parse(shareTo);
            cb.set("shareTo", shareTo2);
        }
        return cb;
    }

    // ~
    static class ShareToAttr {

        private final JSONObject attrs;
        private final boolean sysDefault;
        protected ShareToAttr(ConfigBean cb) {
            Object s = cb.getObject("shareTo");
            if (s instanceof JSON) {
                this.attrs = (JSONObject) s;
            } else {
                // shareTo=ALL
                this.attrs = JSONUtils.toJSONObject("fallback", true);
            }
            this.sysDefault = cb.getString("name") == null;  // 系统默认的
        }

        // 默认
        boolean isFallback() {
            return this.sysDefault || this.attrs.getBooleanValue("fallback");
        }

        // 新建
        boolean isForNew() {
            return this.sysDefault || this.attrs.getBooleanValue("fornew");
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
