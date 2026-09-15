/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.configuration.general.BaseLayoutManager;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.utils.JSONUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 导航菜单
 *
 * @author Zixin (RB)
 * @since 09/20/2018
 */
public class NavManager extends BaseLayoutManager {

    // 父菜单
    public static final String NAV_PARENT = "$PARENT$";
    // 文件
    public static final String NAV_FILEMRG = "$FILEMRG$";
    // 动态
    public static final String NAV_FEEDS = "$FEEDS$";
    // 项目
    public static final String NAV_PROJECT = "$PROJECT$";
    // 通讯录
    public static final String NAV_CONTACT = "$CONTACT$";
    // 仪表盘
    public static final String NAV_DASHBOARD = "$DASHBOARD$";
    // 审批中心
    public static final String NAV_APPROVAL = "$APPROVAL$";

    // 分栏
    public static final String NAV_DIVIDER = "$DIVIDER$";

    // 导航菜单属性
    protected static final String[] NAV_ITEM_PROPS = new String[]{"icon", "text", "type", "value"};
    // 默认导航
    protected static final JSONArray NAVS_DEFAULT = JSONUtils.toJSONObjectArray(
            NAV_ITEM_PROPS,
            new Object[][]{
                    new Object[]{"chart-donut", "动态", "BUILTIN", NAV_FEEDS},
                    new Object[]{"folder", "文件", "BUILTIN", NAV_FILEMRG},
                    new Object[]{"account-box-phone", "通讯录", "BUILTIN", NAV_CONTACT},
                    new Object[]{"mdi-progress-check", "审批中心", "BUILTIN", NAV_APPROVAL},
                    new Object[]{"shape", "项目", "BUILTIN", NAV_PROJECT},
            });

    public static final NavManager instance = new NavManager();

    protected NavManager() {
    }

    /**
     * @param user
     * @return
     */
    public JSON getNavLayout(ID user) {
        ConfigBean config = getLayoutOfNav(user);
        return config == null ? JSONUtils.toJSONObject("config", useBlankNav(user)) : config.toJSON();
    }

    /**
     * @param cfgid
     * @return
     */
    public JSON getNavLayoutById(ID cfgid) {
        ConfigBean config = getLayoutById(cfgid);
        return config == null ? null : config.toJSON();
    }

    /**
     * 获取可用导航ID
     *
     * @param user
     * @return
     */
    public ID[] getUsesNavId(ID user) {
        Object[][] uses = getUsesConfig(user, null, TYPE_NAV);
        List<ID> array = new ArrayList<>();
        for (Object[] c : uses) {
            array.add((ID) c[0]);
        }
        return array.toArray(new ID[0]);
    }

    // 无配置时使用默认导航
    protected JSONArray useBlankNav(ID user) {
        JSONArray useDefault = (JSONArray) JSONUtils.clone(NAVS_DEFAULT);
        // v4.5 未配置时使用全部实体
        for (Entity e : MetadataSorter.sortEntities(user, false, false)) {
            EasyEntity easyEntity = EasyMetaFactory.valueOf(e);
            useDefault.add(JSONUtils.toJSONObject(
                    NAV_ITEM_PROPS,
                    new Object[]{easyEntity.getIcon(), easyEntity.getLabel(), "ENTITY", easyEntity.getName()}));
        }
        return useDefault;
    }

    @Override
    protected String getConfigFields() {
        return "configId,shareTo,createdBy,config,configName,createdBy.roleId";
    }
}
