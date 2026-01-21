/*!
Copyright (c) Ruifang Tech <http://ruifang-tech.com/> and/or its owners. All rights reserved.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.general.RecordBuilder;
import com.rebuild.rbv.frontjs.service.CodeBabel;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 自定义操作按钮
 *
 * @author Zixin (RB)
 * @since 6/5/2024
 */
@Slf4j
public class EasyActionManager extends BaseLayoutManager {

    public static final EasyActionManager instance = new EasyActionManager();

    private static final String TYPE_DATALIST = "datalist";
    private static final String TYPE_DATAROW = "datarow";
    private static final String TYPE_VIEW = "view";

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

        Object config = cb.getJSON("config");
        JSONObject configJson;
        if (config instanceof JSONArray) configJson = JSONUtils.toJSONObject(TYPE_DATALIST, config);
        else configJson = config == null ? null : (JSONObject) config;

        if (MapUtils.isEmpty(configJson)) return null;

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

    /**
     * @param entity
     * @param eeid
     * @return
     */
    public JSONObject findActionByEeid(String entity, String eeid) {
        ConfigBean cb = getLayout(UserService.SYSTEM_USER, entity, TYPE_EASYACTION, null);
        if (cb == null || eeid == null) return null;

        JSONObject conf = (JSONObject) cb.getJSON("config");
        JSONObject action = findActionByEeid(conf.getJSONArray(TYPE_DATALIST), eeid);
        if (action == null) action = findActionByEeid(conf.getJSONArray(TYPE_DATAROW), eeid);
        if (action == null) action = findActionByEeid(conf.getJSONArray(TYPE_VIEW), eeid);
        return action;
    }

    private JSONObject findActionByEeid(JSONArray typeItems, String eeid) {
        if (typeItems == null) return null;

        for (Object o : typeItems) {
            JSONObject item = (JSONObject) o;
            if (eeid.equals(item.getString("id"))) return item;

            // fix:4.1.5 二级菜单
            JSONArray itemsL2 = item.getJSONArray("items");
            if (CollectionUtils.isNotEmpty(itemsL2)) {
                for (Object oL2 : itemsL2) {
                    JSONObject itemL2 = (JSONObject) oL2;
                    if (eeid.equals(itemL2.getString("id"))) {
                        return itemL2;
                    }
                }
            }
        }
        return null;
    }

    /**
     * JS 支持 ES6 > ES5
     *
     * @param layoutId
     * @throws Exception
     */
    public void es5(ID layoutId) throws Exception {
        Object config = QueryHelper.queryFieldValue(layoutId, "config");
        JSONObject configJson = JSON.parseObject((String) config);
        if (MapUtils.isEmpty(configJson)) return;

        boolean es5Changed = false;
        for (String type : configJson.keySet()) {
            JSONArray items = (JSONArray) configJson.get(type);
            for (Object item : items) {
                JSONObject itemObj = (JSONObject) item;
                String es6 = itemObj.getString("op10Value");
                if (StringUtils.isNotBlank(es6)) {
                    itemObj.put("op10Value__es5", CodeBabel.es5(es6));
                    es5Changed = true;
                }

                JSONArray itemsL2 = itemObj.getJSONArray("items");
                if (CollectionUtils.isNotEmpty(itemsL2)) {
                    for (Object itemL2 : itemsL2) {
                        JSONObject itemL2Obj = (JSONObject) itemL2;
                        es6 = itemL2Obj.getString("op10Value");
                        if (StringUtils.isNotBlank(es6)) {
                            itemL2Obj.put("op10Value__es5", CodeBabel.es5(es6));
                            es5Changed = true;
                        }
                    }
                }
            }
        }

        if (es5Changed) {
            Record r = RecordBuilder.builder(layoutId)
                    .add("config", configJson.toJSONString())
                    .build(UserService.SYSTEM_USER);
            Application.getCommonsService().update(r, false);

            super.clean(layoutId);
            log.info("EasyActionManager es5 finished : {}", layoutId);
        }
    }

    @Override
    public void clean(Object layoutId) {
        super.clean(layoutId);
    }
}
