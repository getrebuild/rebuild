/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.RebuildException;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.utils.JSONUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 高级过滤器
 *
 * @author devezhao
 * @since 09/30/2018
 */
public class AdvFilterManager extends ShareToManager {

    public static final AdvFilterManager instance = new AdvFilterManager();

    private AdvFilterManager() {
    }

    @Override
    protected String getConfigEntity() {
        return "FilterConfig";
    }

    @Override
    protected String getConfigFields() {
        return super.getConfigFields() + ",filterName";
    }

    /**
     * 获取高级查询列表
     *
     * @param entity
     * @param user
     * @return
     */
    public JSONArray getAdvFilterList(String entity, ID user) {
        Object[][] canUses = getUsesConfig(user, entity, null);

        List<ConfigBean> ces = new ArrayList<>();
        for (Object[] c : canUses) {
            ConfigBean e = new ConfigBean()
                    .set("id", c[0])
                    .set("editable", UserHelper.isSelf(user, (ID) c[2]))
                    .set("name", c[4]);
            ces.add(e);
        }

        ces.sort(Comparator.comparing(o -> o.getString("name")));
        return JSONUtils.toJSONArray(ces.toArray(new ConfigBean[0]));
    }

    /**
     * 获取高级查询
     *
     * @param cfgid
     * @return
     */
    public ConfigBean getAdvFilter(ID cfgid) {
        Object[] o = Application.createQueryNoFilter(
                "select belongEntity from FilterConfig where configId = ?")
                .setParameter(1, cfgid)
                .unique();
        if (o == null) {
            throw new RebuildException("No config found : " + cfgid);
        }

        Object[][] cached = getAllConfig((String) o[0], null);
        for (Object[] c : cached) {
            if (c[0].equals(cfgid)) {
                return new ConfigBean()
                        .set("id", c[0])
                        .set("shareTo", c[1])
                        .set("name", c[4])
                        .set("filter", JSON.parse((String) c[3]));
            }
        }
        return null;
    }

    @Override
    public void clean(Object filterId) {
        cleanWithBelongEntity((ID) filterId, false);
    }
}