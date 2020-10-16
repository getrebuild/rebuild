/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/24
 */
public class ActionFactory {

    /**
     * @return
     */
    public static ActionType[] getAvailableActions() {
        return ActionType.values();
    }

    /**
     * @param type
     * @return
     */
    public static TriggerAction createAction(String type) {
        return createAction(type, null);
    }

    /**
     * @param type
     * @param context
     * @return
     */
    public static TriggerAction createAction(String type, ActionContext context) {
        return createAction(ActionType.valueOf(type), context);
    }

    /**
     * @param type
     * @param context
     * @return
     */
    public static TriggerAction createAction(ActionType type, ActionContext context) {
        try {
            return type.newInstance(context);
        } catch (NoSuchMethodException ex) {
            throw new TriggerException("Unknown trigger type : " + type);
        }
    }
}
