/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.utils.CommonsUtils;

/**
 * @author devezhao
 * @since 2023/12/1
 */
public class RecordHelper {

    /**
     * 更新指定字段值（无规则、无传播，一致则不更新）
     *
     * @param recordId
     * @param field
     * @param newValue
     * @return
     */
    public static boolean setValue(ID recordId, String field, Object newValue) {
        String pkName = MetadataHelper.getEntity(recordId.getEntityCode()).getPrimaryField().getName();
        Object[] o = Application.getQueryFactory().uniqueNoFilter(recordId, field, pkName);
        if (o == null) return false;
        if (CommonsUtils.isSame(o[0], newValue)) return false;

        Record r = EntityHelper.forUpdate(recordId, UserService.SYSTEM_USER, false);
        if (NullValue.isNull(newValue)) r.setNull(field);
        else r.setObjectValue(field, newValue);
        Application.getCommonsService().update(r, false);
        return true;
    }
}
