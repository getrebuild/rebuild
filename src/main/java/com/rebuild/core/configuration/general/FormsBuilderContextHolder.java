/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.engine.ID;
import org.springframework.core.NamedThreadLocal;

/**
 * 主/明细实体权限处理。创建明细实体必须指定主实体，以便验证权限
 *
 * @author devezhao
 * @since 2020/9/28
 */
public class FormsBuilderContextHolder {

    private static final ThreadLocal<ID> MAINID_OF_DETAIL = new NamedThreadLocal<>("MainId from details");

    private static final ThreadLocal<ID> SPEC_LAYOUT = new NamedThreadLocal<>("Layout using specified");

    private static final ThreadLocal<Boolean> FROM_PROTABLE = new NamedThreadLocal<>("From ProTable");

    /**
     * 明细指定主记录
     *
     * @param mainid
     */
    public static void setMainIdOfDetail(ID mainid) {
        MAINID_OF_DETAIL.set(mainid);
    }

    /**
     * @param once
     * @return
     */
    public static ID getMainIdOfDetail(boolean once) {
        ID id = MAINID_OF_DETAIL.get();
        if (id != null && once) MAINID_OF_DETAIL.remove();
        return id;
    }

    /**
     * 指定表单布局
     *
     * @param specLayout
     */
    public static void setSpecLayout(ID specLayout) {
        SPEC_LAYOUT.set(specLayout);
    }

    /**
     * @param once
     * @return
     */
    public static ID getSpecLayout(boolean once) {
        ID id = SPEC_LAYOUT.get();
        if (id != null && once) SPEC_LAYOUT.remove();
        return id;
    }

    /**
     * 来自 ProTable
     */
    public static void setFromProTable() {
        FROM_PROTABLE.set(true);
    }

    /**
     * @param once
     * @return
     */
    public static boolean isFromProTable(boolean once) {
        Boolean is = FROM_PROTABLE.get();
        if (is != null && once) FROM_PROTABLE.remove();
        return is != null && is;
    }
}
