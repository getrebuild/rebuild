/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.configuration.general.BaseLayoutManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 导航菜单
 *
 * @author zhaofang123@gmail.com
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

    // 分隔符
    public static final String NAV_DIVIDER = "$DIVIDER$";

    public static final NavManager instance = new NavManager();

    protected NavManager() {
    }

    /**
     * @param user
     * @return
     */
    public JSON getNavLayout(ID user) {
        ConfigBean config = getLayoutOfNav(user);
        return config == null ? null : config.toJSON();
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
}
