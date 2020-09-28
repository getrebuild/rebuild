/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dataimport;

import cn.devezhao.persist4j.engine.ID;
import org.springframework.core.NamedThreadLocal;

/**
 * 导入时有些字段/数据无需加工
 *
 * @author devezhao
 * @since 2020/9/28
 */
public class DataImporterContextHolder {

    private static final ThreadLocal<ID> IMPORT_MODE = new NamedThreadLocal<>("Current user of data import");

    public static void setImportMode(ID importUser) {
        IMPORT_MODE.set(importUser);
    }

    public static boolean isImportMode(boolean clear) {
        ID user = IMPORT_MODE.get();
        if (user != null && clear) {
            IMPORT_MODE.remove();
        }
        return user != null;
    }

    public static void clear() {
        IMPORT_MODE.remove();
    }
}
