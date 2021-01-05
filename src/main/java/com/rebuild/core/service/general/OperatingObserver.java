/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Observable;
import java.util.Observer;

/**
 * 记录操作观察者。子类复写需要关注的操作即可，**注意实现必须是无状态的**
 *
 * @author devezhao
 * @see ObservableService
 * @since 10/31/2018
 */
public abstract class OperatingObserver implements Observer {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

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
                    LOG.error("OperateContext : " + ctx, ex);
                }
            });
        } else {
            updateByAction(ctx);
        }
    }

    /**
     * @param ctx
     */
    protected void updateByAction(OperatingContext ctx) {
        if (ctx.getAction() == BizzPermission.CREATE) {
            onCreate(ctx);
        } else if (ctx.getAction() == BizzPermission.UPDATE) {
            onUpdate(ctx);
        } else if (ctx.getAction() == ObservableService.DELETE_BEFORE) {
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
     * 是否异步执行
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
     * 分派时
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
