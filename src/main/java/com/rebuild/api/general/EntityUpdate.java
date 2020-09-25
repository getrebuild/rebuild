/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.ApiContext;
import com.rebuild.api.ApiInvokeException;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityRecordCreator;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;

/**
 * 更新记录
 *
 * @author devezhao
 * @since 2020/5/21
 */
public class EntityUpdate extends EntityCreate {

    @Override
    protected String getApiName() {
        return "entity/update";
    }

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        final Entity useEntity = getUseEntity(context);
        if (!useEntity.isQueryable() || !useEntity.isUpdatable()) {
            throw new ApiInvokeException("Unsupportted operation for entity : " + useEntity.getName());
        }

        Record recordUpdate = new EntityRecordCreator(
                useEntity, (JSONObject) context.getPostData(), context.getBindUser(), true)
                .create();
        if (recordUpdate.getPrimary() == null) {
            return formatFailure("Non-updatable record");
        }

        Collection<String> repeatedFields = checkRepeated(recordUpdate);
        if (!repeatedFields.isEmpty()) {
            return formatFailure(
                    "There are duplicate field values : " + StringUtils.join(repeatedFields, "/"),
                    ApiInvokeException.ERR_DATASPEC);
        }

        recordUpdate = Application.getService(useEntity.getEntityCode()).update(recordUpdate);

        return formatSuccess(JSONUtils.toJSONObject("id", recordUpdate.getPrimary()));
    }
}
