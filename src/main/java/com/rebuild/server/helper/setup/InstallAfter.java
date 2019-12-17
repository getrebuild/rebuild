/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/
package com.rebuild.server.helper.setup;

import com.rebuild.server.Application;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.utils.AES;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * 标记接口。
 * 此接口用于标示系统在未完成安装时子类不能随意调用（直接或间接）Application 中的关于 SPRING 相关的方法，调用前应该判断安装状态。
 *
 * @author devezhao
 * @since 2019/12/17
 */
public interface InstallAfter {

    /**
     * 状态文件。位置 ～/.rebuild/.rebuild
     */
    String INSTALL_FILE = ".rebuild";

     /**
     * 检查安装状态
     *
     * @return
     */
    default boolean checkInstalled() {
        if (Application.devMode()) return true;  // for dev

        File file = SysConfiguration.getFileOfData(INSTALL_FILE);
        if (file.exists()) {
            try {
                Properties dbProps = PropertiesLoaderUtils.loadProperties(new FileSystemResource(file));
                String dbPasswd = (String) dbProps.remove("db.passwd.aes");
                if (dbPasswd != null) {
                    dbProps.put("db.passwd", AES.decrypt(dbPasswd));
                }

                for (Map.Entry<Object, Object> e : dbProps.entrySet()) {
                    System.setProperty((String) e.getKey(), (String) e.getValue());
                    if ("db.url".equals(e.getKey()) && ((String) e.getValue()).contains("jdbc:h2:")) {
                        Application.LOG.warn("Using QuickMode with H2 database!");
                    }
                }
            } catch (IOException e) {
                throw new SetupException(e);
            }

            return true;
        }
        return false;
    }
}
