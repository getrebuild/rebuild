package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;

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
            String shareTo = itemObj.getString("shareTo");
            if (UserHelper.isAdmin(user) || isShareTo(shareTo, user)) {
                items4User.add(itemObj);
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
