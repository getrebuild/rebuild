/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

/**
 * 部分触发器执行需要等到明细记录处理完成才能执行
 *
 * @author devezhao
 * @since 2023/11/11
 */
public interface LazyWaitDetailsFinished {

    String FLAG_LAZY = "lazy";

    // setLazy
    // isLazy
    // executeLazy
}
