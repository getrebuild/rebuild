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

    private static final ThreadLocal<ID> LAYOUT_SPEC_RECORD = new NamedThreadLocal<>("Layout using specified record");

    /**
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
        ID mainid = MAINID_OF_DETAIL.get();
        if (mainid != null && once) MAINID_OF_DETAIL.remove();
        return mainid;
    }

    /**
     * @param specRecordId
     */
    public static void setLayoutSpecRecord(ID specRecordId) {
        LAYOUT_SPEC_RECORD.set(specRecordId);
    }

    /**
     * @param once
     * @return
     */
    public static ID getLayoutSpecRecord(boolean once) {
        ID specRecordId = LAYOUT_SPEC_RECORD.get();
        if (specRecordId != null && once) LAYOUT_SPEC_RECORD.remove();
        return specRecordId;
    }
}
