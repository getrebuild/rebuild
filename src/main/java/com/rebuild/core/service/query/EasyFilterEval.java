/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.FormsManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.trigger.aviator.AviatorUtils;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 简易版查询解析器
 *
 * @author devezhao
 * @since 1/7/2025
 * @see AdvFilterParser
 */
@Slf4j
public class EasyFilterEval {

    final private JSONArray formElements;
    private Entity filterEntity;

    /**
     * @param layoutId
     */
    public EasyFilterEval(ID layoutId) {
        ConfigBean cb = FormsManager.instance.getLayoutById(layoutId);
        this.formElements = (JSONArray) cb.getJSON("elements");
    }

    /**
     * @param data
     * @return
     */
    public JSON eval(JSONObject data) {
        JSONArray res = new JSONArray();
        for (Object o : formElements) {
            JSONObject el = (JSONObject) o;
            JSONObject item = JSONUtils.toJSONObject(
                    new String[]{"field"}, new String[]{el.getString("field")});

            JSONObject hiddenOnEasyFilter = el.getJSONObject("hiddenOnEasyFilter");
            JSONObject requiredOnEasyFilter = el.getJSONObject("requiredOnEasyFilter");
            JSONObject readonlyOnEasyFilter = el.getJSONObject("readonlyOnEasyFilter");

            if (ParseHelper.validAdvFilter(hiddenOnEasyFilter)) {
                item.put("hidden", evalInternal(data, hiddenOnEasyFilter));
            }
            if (ParseHelper.validAdvFilter(requiredOnEasyFilter)) {
                item.put("required", evalInternal(data, requiredOnEasyFilter));
            }
            if (ParseHelper.validAdvFilter(readonlyOnEasyFilter)) {
                item.put("readonly", evalInternal(data, readonlyOnEasyFilter));
            }

            if (item.size() > 1) res.add(item);
        }
        return res;
    }

    /**
     * @param data
     * @param easyFilter
     * @return
     */
    protected boolean evalInternal(JSONObject data, JSONObject easyFilter) {
        if (filterEntity == null) {
            filterEntity = MetadataHelper.getEntity(easyFilter.getString("entity"));
        }

        Map<Integer, Boolean> itemsPass = new HashMap<>();
        for (Object o : easyFilter.getJSONArray("items")) {
            JSONObject item = (JSONObject) o;
            itemsPass.put(item.getInteger("index"), evalFilterItem(data, item));
        }

        String equation = StringUtils.defaultIfBlank(easyFilter.getString("equation"), "OR");
        if ("AND".equalsIgnoreCase(equation)) {
            for (Boolean b : itemsPass.values()) {
                if (!b) return false;
            }
        }
        // OR
        for (Boolean b : itemsPass.values()) {
            if (b) return true;
        }

        // TODO 高级表达式
        return false;
    }

    /**
     * @param data
     * @param easyFilterItem
     * @return
     */
    protected boolean evalFilterItem(JSONObject data, JSONObject easyFilterItem) {
        final String field = easyFilterItem.getString("field");
        final String op = easyFilterItem.getString("op");
        String valueInEasy = easyFilterItem.getString("value");
        String valueInData = data.getString(field);

        EasyField easyField = EasyMetaFactory.valueOf(filterEntity.getField(field));
        DisplayType dt = easyField.getDisplayType();
        if (dt == DisplayType.FILE || dt == DisplayType.IMAGE) {
            if ("[]".equals(valueInData)) valueInData = null;
        }

        // 空
        if (ParseHelper.NL.equals(op)) {
            return StringUtils.isBlank(valueInData);
        }
        // 非空
        if (ParseHelper.NT.equals(op)) {
            return !StringUtils.isBlank(valueInData);
        }

        // 空值不继续了
        if (StringUtils.isBlank(valueInData)) return true;

        // 等于
        if (ParseHelper.EQ.equals(op)) {
            return StringUtils.equalsIgnoreCase(valueInEasy, valueInData);
        }
        // 不等于
        if (ParseHelper.NEQ.equals(op)) {
            return !StringUtils.equalsIgnoreCase(valueInEasy, valueInData);
        }
        // 包含
        if (ParseHelper.LK.equals(op)) {
            return StringUtils.containsIgnoreCase(valueInData, valueInEasy);
        }
        // 不包含
        if (ParseHelper.NLK.equals(op)) {
            return !StringUtils.containsIgnoreCase(valueInData, valueInEasy);
        }

        // 包含/不包含 IN/NIN
        if (ParseHelper.IN.equals(op) || ParseHelper.NIN.equals(op)) {
            boolean pass;
            if (dt == DisplayType.TAG) {
                pass = ArrayUtils.contains(valueInData.split(CommonsUtils.COMM_SPLITER_RE), valueInEasy);
            } else {
                // PICKLIST
                pass = StringUtils.containsIgnoreCase(valueInEasy, valueInData);
            }

            if (ParseHelper.NIN.equals(op)) return !pass;
            return pass;
        }

        // GT/LT/EQ/GE/LE
        boolean isDate = dt == DisplayType.DATE || dt == DisplayType.DATETIME || dt == DisplayType.TIME;
        if (dt == DisplayType.DECIMAL || dt == DisplayType.NUMBER || isDate) {
            String opReal = ParseHelper.convetOperation(op);
            Map<String, Object> map = new HashMap<>();
            if (isDate) {
                map.put("valueInData", parseDate(valueInData, dt));
                map.put("valueInEasy", parseDate(valueInEasy, dt));
            } else {
                map.put("valueInData", valueInData);
                map.put("valueInEasy", valueInEasy);
            }

            String aviatorString = String.format("valueInData %s valueInEasy", opReal);
            Object b = AviatorUtils.eval(aviatorString, map);
            return b instanceof Boolean ? (Boolean) b : false;
        }

        return false;
    }

    private Date parseDate(String date, DisplayType dt) {
        if (dt == DisplayType.TIME) {
            if (date.length() == 2) date = "2025-01-01 " + date + ":00:00";
            else if (date.length() == 5) date = "2025-01-01 " + date + ":00";
            else date = "2025-01-01 " + date;
        } else {
            if (date.length() == 4) date = date + "-01-01";
            else if (date.length() == 7) date = date + "-01";
        }
        return CalendarUtils.parse(date);
    }
}
