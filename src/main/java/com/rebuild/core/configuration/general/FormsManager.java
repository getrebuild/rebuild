/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.utils.JSONUtils;

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
     * @param user
     * @return
     */
    public ConfigBean getFormLayout(String entity, ID user) {
        ConfigBean entry = getLayoutOfForm(user, entity);
        if (entry == null) {
            entry = new ConfigBean()
                    .set("elements", JSONUtils.EMPTY_ARRAY);
        } else {
            entry.set("elements", entry.getJSON("config"))
                    .set("config", null)
                    .set("shareTo", null);
        }
        return entry.set("entity", entity);
    }
}
