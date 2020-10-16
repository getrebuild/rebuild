package com.rebuild.core;

/**
 * 实现此接口，将在系统启动时调用 #init 执行初始化
 *
 * @author zhaofang123@gmail.com
 * @since 08/28/2020
 */
public interface Initialization {

    /**
     * 初始化
     *
     * @throws Exception
     */
    void init() throws Exception;

}
