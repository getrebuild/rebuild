package com.rebuild.core;

import org.springframework.core.Ordered;

/**
 * 实现此接口，将在系统启动时调用 #init 执行初始化
 *
 * @author Zixin (RB)
 * @since 08/28/2020
 */
public interface Initialization extends Ordered {

    /**
     * 初始化
     *
     * @throws Exception
     */
    void init() throws Exception;

    @Override
    default int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
