/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import com.rebuild.core.Initialization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 系统启动时自动扫描并加载 RB 数据目录下 <code>_classes</code> 目录中的外部类。
 * <p>
 * 将编译好的 <b>.class</b> 文件按包结构放入数据目录下的 <code>_classes</code> 子目录即可，
 * 例如 <code>~/.rebuild/_classes/com/example/MyAction.class</code>。
 * <p>
 * 同时支持加载目录中的 <b>.jar</b> 包。
 *
 * @author devezhao
 * @see DynamicClassLoader
 * @since 2026/7/28
 */
@Slf4j
@Component
public class DynamicClassLoadingInitializer implements Initialization {

    @Override
    public void init() throws Exception {
        File classesDir = RebuildConfiguration.getFileOfData(DynamicClassLoader.CLASSES_DIR);

        if (!classesDir.exists() || !classesDir.isDirectory()) {
            log.info("No external classes directory found: {}", classesDir);
            return;
        }

        log.info("Scanning external classes from: {}", classesDir);
        DynamicClassLoader.init(classesDir);
    }

    @Override
    public int getOrder() {
        // 尽早加载，以便其他 Initialization 组件可以使用
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
