/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import com.rebuild.core.service.general.OperatingContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/24
 */
@Slf4j
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
        ActionType actionType = null;
        try {
            actionType = ActionType.valueOf(type);
            return actionType.newInstance(context);
        } catch (ClassNotFoundException rbv) {

            if (rbv.getLocalizedMessage().contains(".rbv.")) {
                return new NoRbv(actionType);
            } else {
                throw new TriggerException("Unknown trigger type : " + type, rbv);
            }

        } catch (ReflectiveOperationException ex) {
            throw new TriggerException("Unknown trigger type : " + type, ex);
        }
    }

    /**
     * RBV 功能
     */
    private static class NoRbv extends TriggerAction {

        private ActionType actionType;
        NoRbv(ActionType actionType) {
            super(null);
            this.actionType = actionType;
        }

        @Override
        public ActionType getType() {
            if (actionType == null) throw new UnsupportedOperationException();
            else return actionType;
        }

        @Override
        public ActionContext getActionContext() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void execute(OperatingContext operatingContext) throws TriggerException {
            log.warn("@rbv not attached");
        }
    }
}
