/*!
Copyright (c) Ruifang Tech <http://ruifang-tech.com/> and/or its owners. All rights reserved.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;

/**
 * 自定义操作按钮
 *
 * @author Zixin (RB)
 * @since 6/5/2024
 */
public class EasyActionManager extends BaseLayoutManager {

    public static final EasyActionManager instance = new EasyActionManager();

    private EasyActionManager() {
    }

    /**
     * @param entity
     * @param user
     * @return
     */
    public JSON getEasyAction(String entity, ID user) {
        ConfigBean cb = getLayout(UserService.SYSTEM_USER, entity, TYPE_EASYACTION, null);
        if (cb == null) return null;

        JSONArray items = (JSONArray) cb.getJSON("config");
        if (items == null || items.isEmpty()) return null;

        JSONArray items4User = new JSONArray();
        for (Object item : items) {
            JSONObject itemObj = (JSONObject) item;

            JSONArray itemsL2 = itemObj.getJSONArray("items");
            boolean hasChild = itemsL2 != null && !itemsL2.isEmpty();
            if (hasChild) {
                JSONArray items4UserL2 = new JSONArray();
                for (Object itemL2 : itemsL2) {
                    JSONObject itemL2Obj = (JSONObject) itemL2;
                    String shareTo = itemL2Obj.getString("shareTo");
                    if (UserHelper.isAdmin(user) || isShareTo(shareTo, user)) {
                        items4UserL2.add(itemL2Obj);
                    }
                }

                // 是否可见由子元素确定
                if (!items4UserL2.isEmpty()) {
                    itemObj.put("items", items4UserL2);
                    items4User.add(itemObj);
                }

            } else {
                String shareTo = itemObj.getString("shareTo");
                if (UserHelper.isAdmin(user) || isShareTo(shareTo, user)) {
                    items4User.add(itemObj);
                }
            }
        }

        return items4User;
    }

    /**
     * @param entity
     * @return
     */
    public ConfigBean getEasyActionRaw(String entity) {
        return getLayout(UserService.SYSTEM_USER, entity, TYPE_EASYACTION, null);
    }
}
