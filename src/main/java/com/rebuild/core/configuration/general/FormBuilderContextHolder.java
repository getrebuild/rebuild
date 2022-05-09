/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.engine.ID;

/**
 * 主/明细实体权限处理。创建明细实体必须指定主实体，以便验证权限
 *
 * @author devezhao
 * @since 2020/9/28
 */
public class FormBuilderContextHolder {

    private static final ThreadLocal<ID> MAINID_OF_DETAIL = new ThreadLocal<>();

    public static void setMainIdOfDetail(ID mainId) {
        MAINID_OF_DETAIL.set(mainId);
    }

    public static ID getMainIdOfDetail() {
        return MAINID_OF_DETAIL.get();
    }

    public static void clear() {
        MAINID_OF_DETAIL.remove();
    }
}
