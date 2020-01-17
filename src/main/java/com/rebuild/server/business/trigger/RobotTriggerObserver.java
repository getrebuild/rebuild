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

package com.rebuild.server.business.trigger;

import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.RobotTriggerManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.OperatingObserver;
import com.rebuild.server.service.TransactionManual;
import org.springframework.transaction.TransactionStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/27
 */
public class RobotTriggerObserver extends OperatingObserver {

    @Override
    protected void onCreate(OperatingContext context) {
        execAction(context, TriggerWhen.CREATE);
    }

    @Override
    protected void onUpdate(OperatingContext context) {
        execAction(context, TriggerWhen.UPDATE);
    }

    @Override
    protected void onAssign(OperatingContext context) {
        execAction(context, TriggerWhen.ASSIGN);
    }

    @Override
    protected void onShare(OperatingContext context) {
        execAction(context, TriggerWhen.SHARE);
    }

    @Override
    protected void onUnshare(OperatingContext context) {
        execAction(context, TriggerWhen.UNSHARE);
    }

    /**
     * @param context
     * @param when
     */
    protected void execAction(OperatingContext context, TriggerWhen when) {
        TriggerAction[] actions = RobotTriggerManager.instance.getActions(getEffectId(context), when);
        if (actions.length == 0) {
            return;
        }

        final boolean cleanSource = getTriggerSource() == null;
        // 设置原始触发源
        if (cleanSource) {
            setTriggerSource(context);
        }
        // 自己触发自己，避免无限执行
        else if (getTriggerSource().getAnyRecord().getPrimary().equals(context.getAnyRecord().getPrimary())) {
            return;
        }

        final ID currentUser = Application.getCurrentUser();
        try {
            for (TriggerAction action : actions) {

                if (action.useAsync()) {
                    // 异步执行

                    ThreadPool.exec(() -> {
                        Application.getSessionStore().set(currentUser);
                        try {
                            action.execute(context);
                        } catch (Exception ex) {
                            LOG.error("Failed Trigger : " + action + " << " + context, ex);
                        } finally {
                            Application.getSessionStore().clean();
                        }
                    });
                } else if (action.useNewTransaction()) {
                    // 手动开启一个新事物，不影响当前事物

                    TransactionStatus tx = TransactionManual.newTransaction();
                    try {
                        action.execute(context);
                        TransactionManual.commit(tx);
                    } catch (Exception ex) {
                        TransactionManual.rollback(tx);
                        LOG.error("Failed Trigger : " + action + " << " + context, ex);
                    }
                } else {

                    try {
                        action.execute(context);
                    } catch (DataSpecificationException ex) {
                        LOG.error("Failed Trigger : " + action + " << " + context, ex);
                        throw ex;
                    } catch (Exception ex) {
                        LOG.error("Failed Trigger : " + action + " << " + context, ex);
                    } finally {
                        if (cleanSource) {
                            action.clean();
                        }
                    }

                }
            }
        } finally {
            if (cleanSource) {
                setTriggerSource(null);
            }
        }
    }

    /**
     * 获取实际影响的记录。
     * 例如在共享时传入的 Record 是 ShareAccess，而实际影响的是其中的 recordId 记录
     *
     * @return
     */
    protected ID getEffectId(OperatingContext context) {
        ID effectId = context.getAnyRecord().getPrimary();
        if (effectId.getEntityCode() == EntityHelper.ShareAccess) {
            effectId = context.getAnyRecord().getID("recordId");
        }
        return effectId;
    }

    // 删除做特殊处理

    private static final Map<ID, TriggerAction[]> DELETE_ACTION_HOLDS = new ConcurrentHashMap<>();

    @Override
    protected void onDeleteBefore(OperatingContext context) {
        final ID primary = context.getAnyRecord().getPrimary();
        TriggerAction[] actions = RobotTriggerManager.instance.getActions(primary, TriggerWhen.DELETE);
        for (TriggerAction action : actions) {
            try {
                action.prepare(context);
            } catch (Exception ex) {
                LOG.error("Preparing trigger failure: " + action, ex);
            }
        }
        DELETE_ACTION_HOLDS.put(primary, actions);
    }

    @Override
    protected void onDelete(OperatingContext context) {
        final ID primary = context.getAnyRecord().getPrimary();
        TriggerAction[] holdActions = DELETE_ACTION_HOLDS.get(primary);
        if (holdActions == null) {
            LOG.warn("No action held for trigger of delete");
            return;
        }
        for (TriggerAction action : holdActions) {
            try {
                action.execute(context);
            } catch (Exception ex) {
                LOG.error("Executing trigger failure: " + action, ex);
            }
        }
        DELETE_ACTION_HOLDS.remove(primary);
    }

    // -- 当前线程触发源

    private static final ThreadLocal<OperatingContext> TRIGGER_SOURCE = new ThreadLocal<>();

    /**
     * 设置触发器触发源，供其他功能使用
     *
     * @param source
     */
    private static void setTriggerSource(OperatingContext source) {
        if (source == null) {
            TRIGGER_SOURCE.remove();
        } else {
            TRIGGER_SOURCE.set(source);
        }
    }

    /**
     * 获取当前（线程）触发源（如有），即原始触发记录
     *
     * @return
     */
    public static OperatingContext getTriggerSource() {
        return TRIGGER_SOURCE.get();
    }
}
