/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.task;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.support.SetUser;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * 耗时操作可通过此类进行，例如大批量删除/修改等。此类提供了进度相关的约定，如总计执行条目，已完成条目/百分比等。
 * 继承此类应该处理线程的 `isInterrupted` 方法，以便任务可以被终止。
 * 使用此类应该总是使用 TaskExecutors 调用。
 * 前端使用可通过 HeavyTaskController 进行一些公共操作。
 *
 * @author devezhao
 * @see TaskExecutors
 * @see com.rebuild.web.commons.HeavyTaskController
 * @since 09/29/2018
 */
@Slf4j
public abstract class HeavyTask<T> extends SetUser implements Runnable {

    volatile private boolean interruptState = false;

    /**
     * @see SetUser
     */
    private ID threadUser;

    // 要处理的数据总数量
    private int total = -1;
    // 完成数量
    private int completed = 0;
    // 成功数量
    private int succeeded = 0;

    final private Date beginTime;
    private Date completedTime;

    protected String errorMessage;

    protected HeavyTask() {
        this.beginTime = CalendarUtils.now();
    }

    @Override
    public SetUser setUser(ID user) {
        this.threadUser = user;
        return super.setUser(user);
    }

    protected void setTotal(int total) {
        this.total = total;
    }

    protected void setCompleted(int completed) {
        this.completed = completed;
    }

    protected void addCompleted() {
        this.completed++;
    }

    protected Date getCompletedTime() {
        return completedTime;
    }

    protected void addSucceeded() {
        succeeded++;
    }

    /**
     * 任务已耗时（ms）
     *
     * @return
     */
    public long getElapsedTime() {
        if (getCompletedTime() != null) {
            return getCompletedTime().getTime() - beginTime.getTime();
        } else {
            return CalendarUtils.now().getTime() - beginTime.getTime();
        }
    }

    /**
     * 总计数量
     *
     * @return
     */
    public int getTotal() {
        return total;
    }

    /**
     * 完成数量
     *
     * @return
     */
    public int getCompleted() {
        return completed;
    }

    /**
     * 完成进度百分比
     *
     * @return
     */
    public double getCompletedPercent() {
        if (total == -1 || completed == 0) {
            return 0;
        }
        if (completed >= total) {
            return 1;
        }
        return completed * 1d / total;
    }

    /**
     * 是否完成
     *
     * @return
     */
    public boolean isCompleted() {
        return getCompletedTime() != null || (total != -1 && getCompleted() >= getTotal());
    }

    /**
     * 成功数量
     *
     * @return
     */
    public int getSucceeded() {
        return succeeded;
    }

    /**
     * 错误消息（如有）
     *
     * @return
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 执行结果（如有）
     *
     * @return
     */
    public JSON getExecResults() {
        return null;
    }

    // 中断处理。是否允许中断由子类决定（实现）

    /**
     * 设置中断
     */
    public void setInterruptState() {
        this.interruptState = true;
    }

    /**
     * 是否中断（含手动中断与VM中断）
     *
     * @return
     */
    public boolean isInterruptState() {
        if (this.interruptState) return true;
        if (Thread.currentThread().isInterrupted()) {
            log.warn("Current thread is interrupted (by VM) : {}", Thread.currentThread());
            setInterruptState();
        }
        return interruptState;
    }

    @Override
    final public void run() {
        if (this.threadUser != null) {
            UserContextHolder.setUser(this.threadUser);
        }

        try {
            exec();
        } catch (Exception ex) {
            log.error("Exception during task execute", ex);
            this.errorMessage = ex.getLocalizedMessage();
        } finally {
            completedAfter();
        }
    }

    /**
     * 子类复写此方法进行实际的任务执行。
     * 不建议直接调用此方法，如直接调用请处理好线程用户问题
     *
     * @return
     * @throws Exception
     */
    abstract protected T exec() throws Exception;

    /**
     * 子类应该在执行完毕后调用此方法。任何情况下，都应保证此方法被调用！
     */
    protected void completedAfter() {
        this.completedTime = CalendarUtils.now();
        if (this.threadUser != null) {
            UserContextHolder.clearUser();
        }
    }

    @Override
    public String toString() {
        return "HeavyTask#" + getClass();
    }
}
