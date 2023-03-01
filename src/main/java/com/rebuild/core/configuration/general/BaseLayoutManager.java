/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.privileges.bizz.ZeroEntry;

/**
 * 基础布局管理
 *
 * @author Zixin (RB)
 * @since 09/15/2018
 */
public class BaseLayoutManager extends ShareToManager {

    public static final BaseLayoutManager instance = new BaseLayoutManager();

    protected BaseLayoutManager() {
    }

    // 导航
    public static final String TYPE_NAV = "NAV";
    // 表单
    public static final String TYPE_FORM = "FORM";
    // 列表
    public static final String TYPE_DATALIST = "DATALIST";
    // 列表-统计列
    public static final String TYPE_LISTSTATS = "LISTSTATS";
    // 列表-查询面板
    public static final String TYPE_LISTFILTERPANE = "LISTFILTERPANE";
    // 列表-图表
    public static final String TYPE_WCHARTS = "WCHARTS";
    // 视图-相关项
    public static final String TYPE_TAB = "TAB";
    // 视图-新建相关
    public static final String TYPE_ADD = "ADD";

    @Override
    protected String getConfigEntity() {
        return "LayoutConfig";
    }

    @Override
    protected String getConfigFields() {
        return "configId,shareTo,createdBy,config,configName";
    }

    /**
     * @param user
     * @param entity
     * @return
     */
    public ConfigBean getLayoutOfForm(ID user, String entity) {
        return getLayout(user, entity, TYPE_FORM, null);
    }

    /**
     * @param user
     * @param entity
     * @return
     */
    public ConfigBean getLayoutOfDatalist(ID user, String entity, String useSysFlag) {
        return getLayout(user, entity, TYPE_DATALIST, useSysFlag);
    }

    /**
     * @param user
     * @return
     */
    public ConfigBean getLayoutOfNav(ID user) {
        return getLayout(user, null, TYPE_NAV, null);
    }

    /**
     * @param user
     * @param belongEntity
     * @param applyType
     * @param useSysFlag
     * @return
     */
    protected ConfigBean getLayout(ID user, String belongEntity, String applyType, String useSysFlag) {
        // 221125 无权限不允许使用自有配置
        boolean firstUseSelf = true;
        if (TYPE_NAV.equals(applyType)) {
            firstUseSelf = Application.getPrivilegesManager().allow(user, ZeroEntry.AllowCustomNav);
        } else if (TYPE_DATALIST.equals(applyType)) {
            firstUseSelf = Application.getPrivilegesManager().allow(user, ZeroEntry.AllowCustomDataList);
        }

        ID detected = detectUseConfig(user, belongEntity, applyType, firstUseSelf, useSysFlag);
        // 无指定则使用默认
        if (detected == null && useSysFlag != null) {
            detected = detectUseConfig(user, belongEntity, applyType, firstUseSelf, null);
        }
        if (detected == null) return null;

        Object[][] cached = getAllConfig(belongEntity, applyType);
        return findConfigBean(cached, detected);
    }

    /**
     * @param cfgid
     * @return
     */
    public ConfigBean getLayoutById(ID cfgid) {
        Object[] o = Application.getQueryFactory().uniqueNoFilter(cfgid, "belongEntity,applyType");
        if (o == null) throw new ConfigurationException("No config found : " + cfgid);

        Object[][] cached = getAllConfig((String) o[0], (String) o[1]);
        return findConfigBean(cached, cfgid);
    }

    /**
     * @param uses
     * @param cfgid
     * @return
     */
    protected ConfigBean findConfigBean(Object[][] uses, ID cfgid) {
        for (Object[] c : uses) {
            if (c[0].equals(cfgid)) {
                return new ConfigBean()
                        .set("id", c[0])
                        .set("shareTo", c[1])
                        .set("config", JSON.parse((String) c[3]));
            }
        }
        return null;
    }

    @Override
    public void clean(Object layoutId) {
        cleanWithBelongEntity((ID) layoutId, true);
    }
}
