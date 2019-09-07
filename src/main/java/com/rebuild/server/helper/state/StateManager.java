/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.helper.state;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.approval.ApprovalState;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 状态管理
 *
 * @author devezhao
 * @since 2019/9/6
 */
public class StateManager {

    public static final StateManager instance = new StateManager();
    private StateManager() { }

    /**
     * @param stateField
     * @return
     */
    public JSONArray getStateOptions(Field stateField) {
        String stateClass;
        if (EntityHelper.ApprovalState.equalsIgnoreCase(stateField.getName())) {
            stateClass = ApprovalState.class.getName();
        } else {
            stateClass = EasyMeta.valueOf(stateField).getFieldExtConfig().getString("stateClass");
        }
        return getStateOptions(stateClass);
    }

    /**
     * @param stateClass
     * @return
     */
    public JSONArray getStateOptions(String stateClass) {
        if (StringUtils.isBlank(stateClass)) {
            return JSONUtils.EMPTY_ARRAY;
        }

        final String cKey = "STATECLASS-" + stateClass;
        JSONArray options = (JSONArray) Application.getCommonCache().getx(cKey);
        if (options != null) {
            return options;
        }

        Class<?> state = StateHelper.getSatetClass(stateClass);
        options = new JSONArray();
        for (Object c : state.getEnumConstants()) {
            JSONObject item = JSONUtils.toJSONObject(
                    new String[] { "id", "text", "default" },
                    new Object[] { ((StateSpec) c).getState(), ((StateSpec) c).getName(), ((StateSpec) c).isDefault() });
            options.add(item);
        }
        Application.getCommonCache().putx(cKey, options);
        return options;
    }

    /**
     * @param stateField
     * @param state
     * @return
     * @see #getName(Field, int)
     */
    public String getLabel(Field stateField, int state) {
        return getName(stateField, state);
    }

    /**
     * @param stateField
     * @param state
     * @return
     */
    public String getName(Field stateField, int state) {
        Class<?> stateClass = StateHelper.getSatetClass(stateField);
        for (Object c : stateClass.getEnumConstants()) {
            if (((StateSpec) c).getState() == state) return ((StateSpec) c).getName();
        }
        return null;
    }

    /**
     * @param stateField
     * @param name
     * @return
     */
    public Integer getState(Field stateField, String name) {
        Class<?> stateClass = StateHelper.getSatetClass(stateField);
        for (Object c : stateClass.getEnumConstants()) {
            StateSpec s = (StateSpec) c;
            if (s.getName().equalsIgnoreCase(name) || ((Enum<?>) s).name().equalsIgnoreCase(name)) return s.getState();
        }
        return null;
    }
}
