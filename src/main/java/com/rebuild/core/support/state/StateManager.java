/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.state;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.metadata.impl.FieldExtConfigProps;
import com.rebuild.core.metadata.impl.MetadataException;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.support.i18n.Language;
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

    private StateManager() {
    }

    /**
     * @param stateField
     * @return
     */
    public JSONArray getStateOptions(Field stateField) {
        String stateClass;
        if (EntityHelper.ApprovalState.equalsIgnoreCase(stateField.getName())) {
            stateClass = ApprovalState.class.getName();
        } else {
            stateClass = EasyMeta.valueOf(stateField).getExtraAttr(FieldExtConfigProps.STATE_STATECLASS);
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

        final String cKey = "STATECLASS-" + stateClass + "." + Language.getCurrentBundle().getLocale();
        JSONArray options = (JSONArray) Application.getCommonsCache().getx(cKey);
        if (options != null) {
            return (JSONArray) JSONUtils.clone(options);
        }

        Class<?> state = StateHelper.getSatetClass(stateClass);
        options = new JSONArray();
        for (Object c : state.getEnumConstants()) {
            StateSpec ss = (StateSpec) c;
            JSONObject item = JSONUtils.toJSONObject(
                    new String[]{"id", "text", "default"},
                    new Object[]{ss.getState(), Language.getLang(ss), ss.isDefault()});
            options.add(item);
        }

        Application.getCommonsCache().putx(cKey, options);
        return options;
    }

    /**
     * @param stateField
     * @param nv
     * @return
     * @throws MetadataException If not found
     */
    public StateSpec findState(Field stateField, Object nv) throws MetadataException {
        Class<?> stateClass = StateHelper.getSatetClass(stateField);
        for (Object c : stateClass.getEnumConstants()) {
            StateSpec s = (StateSpec) c;

            if (nv instanceof Integer && (Integer) nv == s.getState()) return s;
            if (s.getName().equalsIgnoreCase(nv.toString())
                    || ((Enum<?>) s).name().equalsIgnoreCase(nv.toString())) return s;
        }
        throw new RebuildException("Cannot found state : " + nv);
    }
}
