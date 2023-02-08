/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.ThreadPool;
import com.rebuild.core.privileges.bizz.InternalPermission;
import lombok.extern.slf4j.Slf4j;

import java.util.Observable;
import java.util.Observer;

/**
 * 记录操作观察者。子类复写需要关注的操作即可，**注意实现必须是无状态的**
 *
 * @author devezhao
 * @see ObservableService
 * @since 10/31/2018
 */
@Slf4j
public abstract class OperatingObserver implements Observer {

    protected OperatingObserver() {
        super();
    }

    @Override
    public void update(final Observable o, final Object arg) {
        final OperatingContext ctx = (OperatingContext) arg;
        if (isAsync()) {
            ThreadPool.exec(() -> {
                try {
                    updateByAction(ctx);
                } catch (Exception ex) {
                    log.error("OperateContext : " + ctx, ex);
                }
            });
        } else {
            updateByAction(ctx);
        }
    }

    /**
     * @param ctx
     */
    protected void updateByAction(final OperatingContext ctx) {
        if (ctx.getAction() == BizzPermission.CREATE) {
            onCreate(ctx);
        } else if (ctx.getAction() == BizzPermission.UPDATE) {
            onUpdate(ctx);
        } else if (ctx.getAction() == InternalPermission.DELETE_BEFORE) {
            onDeleteBefore(ctx);
        } else if (ctx.getAction() == BizzPermission.DELETE) {
            onDelete(ctx);
        } else if (ctx.getAction() == BizzPermission.ASSIGN) {
            onAssign(ctx);
        } else if (ctx.getAction() == BizzPermission.SHARE) {
            onShare(ctx);
        } else if (ctx.getAction() == EntityService.UNSHARE) {
            onUnshare(ctx);
        }
    }

    /**
     * 是否异步执行（异步执行请注意事物）
     *
     * @return
     */
    protected boolean isAsync() {
        return false;
    }

    // -- 根据需要复写以下方法

    /**
     * 新建时
     *
     * @param context
     */
    protected void onCreate(final OperatingContext context) {
    }

    /**
     * 更新时
     *
     * @param context
     */
    protected void onUpdate(final OperatingContext context) {
    }

    /**
     * 删除时
     *
     * @param context
     */
    protected void onDelete(final OperatingContext context) {
    }

    /**
     * 删除前处理
     *
     * @param context
     */
    protected void onDeleteBefore(final OperatingContext context) {
    }

    /**
     * 分配时
     *
     * @param context
     */
    protected void onAssign(final OperatingContext context) {
    }

    /**
     * 共享时
     *
     * @param context
     */
    protected void onShare(final OperatingContext context) {
    }

    /**
     * 取消共享时
     *
     * @param context
     */
    protected void onUnshare(final OperatingContext context) {
    }
}
