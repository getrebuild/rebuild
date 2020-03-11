/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper.setup;

import com.rebuild.server.Application;
import com.rebuild.server.helper.SysConfiguration;

import java.io.File;

/**
 * 安装状态。
 * 此接口主要用于警示子类在未安装时不能随意调用（直接或间接）Application 中关于 SPRING 的相关方法，调用前应该判断安装状态。
 *
 * @author devezhao
 * @since 2019/12/17
 */
public interface InstallState {

    /**
     * 状态文件位置 ～/.rebuild/.rebuild
     */
    String INSTALL_FILE = ".rebuild";

     /**
     * 检查安装状态
     *
     * @return
     */
    default boolean checkInstalled() {
        if (Application.devMode()) {
            return true;  // for dev
        }
        File file = SysConfiguration.getFileOfData(INSTALL_FILE);
        return file != null && file.exists();
    }
}
