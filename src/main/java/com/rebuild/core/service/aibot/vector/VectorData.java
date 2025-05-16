/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.vector;

/**
 * 向模型推送的业务数据
 *
 * @author Zixin
 * @since 2025/4/18
 */
public interface VectorData {

    String N = "\n";
    String NN = "\n\n";

    /**
     * @return
     */
    String toVector();
}
