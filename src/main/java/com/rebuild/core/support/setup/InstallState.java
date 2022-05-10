/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import com.rebuild.core.Application;
import com.rebuild.core.support.RebuildConfiguration;

import java.io.File;

/**
 * 安装状态。
 * 此接口主要用于警示子类在未安装时不能随意调用（直接或间接）RebuildApplication 中关于 SPRING 的相关方法，调用前应该判断安装状态。
 *
 * @author devezhao
 * @since 2019/12/17
 */
public interface InstallState {

    /**
     * 安装文件位置 ～/.rebuild/.rebuild
     */
    String INSTALL_FILE = ".rebuild";

    /**
     * 检查安装状态
     *
     * @return
     */
    default boolean checkInstalled() {
        return Application.devMode() || getInstallFile().exists();
    }

    /**
     * 获取安装文件
     *
     * @return
     */
    default File getInstallFile() {
        return RebuildConfiguration.getFileOfData(INSTALL_FILE);
    }
}
