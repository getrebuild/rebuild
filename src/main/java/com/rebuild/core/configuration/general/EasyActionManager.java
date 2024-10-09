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
import com.rebuild.utils.JSONUtils;

/**
 * 自定义操作按钮
 *
 * @author Zixin (RB)
 * @since 6/5/2024
 */
public class EasyActionManager extends BaseLayoutManager {

    public static final EasyActionManager instance = new EasyActionManager();

    private static final String TYPE_DATALIST = "datalist";
    private static final String TYPE_DATAROW = "datarow";
    private static final String TYPE_VIEW = "view";

    private EasyActionManager() {}

    /**
     * @param entity
     * @param user
     * @return
     */
    public JSON getEasyAction(String entity, ID user) {
        ConfigBean cb = getLayout(UserService.SYSTEM_USER, entity, TYPE_EASYACTION, null);
        if (cb == null) return null;

        Object config = cb.getJSON("config");
        JSONObject configJson;
        if (config instanceof JSONArray) configJson = JSONUtils.toJSONObject(TYPE_DATALIST, config);
        else configJson = config == null ? null : (JSONObject) config;

        if (configJson == null || configJson.isEmpty()) return null;

        JSONObject action4User = new JSONObject();
        for (String type : configJson.keySet()) {
            final JSONArray items = (JSONArray) configJson.get(type);

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

            if (!items4User.isEmpty()) action4User.put(type, items4User);
        }
        return action4User;
    }

    /**
     * @param entity
     * @return
     */
    public ConfigBean getEasyActionRaw(String entity) {
        return getLayout(UserService.SYSTEM_USER, entity, TYPE_EASYACTION, null);
    }

    @Override
    public void clean(Object layoutId) {
        super.clean(layoutId);

        // TODO JS 支持 ES6 > ES5
    }
}
