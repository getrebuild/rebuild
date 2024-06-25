/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONAware;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import lombok.Getter;

import java.util.Collection;

/**
 * 触发器执行结果
 *
 * @author RB
 * @since 2022/09/26
 */
@Getter
public class TriggerResult implements JSONAware {

    // 状态 1=成功, 2=警告, 3=错误
    final private int level;
    // 消息
    final private String message;
    // 影响的记录
    final private Collection<ID> affected;

    private TriggerSource chain;
    private boolean breakNext;

    protected TriggerResult(int level, String message, Collection<ID> affected) {
        this.level = level;
        this.message = message;
        this.affected = affected;
    }

    public void setChain(TriggerSource chain) {
        this.chain = chain;
    }

    public void setBreakNext(boolean breakNext) {
        this.breakNext = breakNext;
    }

    public boolean hasAffected() {
        return affected != null && !affected.isEmpty();
    }

    @Override
    public String toJSONString() {
        JSONObject res = JSONUtils.toJSONObject("level", level);
        if (message != null) res.put("message", message);
        if (affected != null) res.put("affected", affected);
        if (chain != null) res.put("chain", chain.toString());
        if (breakNext) res.put("break", true);
        return res.toJSONString();
    }

    @Override
    public String toString() {
        return toJSONString();
    }

    // --

    /**
     * @param affected
     * @return
     */
    public static TriggerResult success(Collection<ID> affected) {
        return new TriggerResult(1, null, affected);
    }

    /**
     * @param message
     * @return
     */
    public static TriggerResult success(String message) {
        return new TriggerResult(1, message, null);
    }

    /**
     * @param message
     * @param affected
     * @return
     */
    public static TriggerResult success(String message, Collection<ID> affected) {
        return new TriggerResult(1, message, affected);
    }

    /**
     * @param message
     * @return
     */
    public static TriggerResult wran(String message) {
        return new TriggerResult(2, message, null);
    }

    /**
     * @param message
     * @return
     */
    public static TriggerResult error(String message) {
        return new TriggerResult(3, message, null);
    }

    /**
     * 无匹配记录
     *
     * @return
     */
    public static TriggerResult noMatching() {
        return wran("No matching");
    }

    /**
     * 仅可触发一次
     *
     * @return
     */
    public static TriggerResult triggerOnce() {
        return wran("Trigger once");
    }

    /**
     * 目标记录为空
     *
     * @return
     */
    public static TriggerResult targetEmpty() {
        return wran("Target are empty");
    }

    /**
     * 目标记录相等
     *
     * @return
     */
    public static TriggerResult targetSame() {
        return wran("Target are same");
    }

    /**
     * 目标记录不存在 [DELETED]
     *
     * @return
     */
    public static TriggerResult targetNotExists() {
        return wran("Target not exists");
    }

    /**
     * 无效配置
     *
     * @return
     */
    public static TriggerResult badConfig() {
        return wran("Bad config");
    }
}
