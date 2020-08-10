/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.ApiContext;
import com.rebuild.api.ApiInvokeException;
import com.rebuild.api.BaseApi;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.ExtRecordCreator;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * 新建记录
 *
 * @author devezhao
 * @since 2020/5/21
 */
public class EntityCreate extends BaseApi {

    @Override
    protected String getApiName() {
        return "entity/create";
    }

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        final Entity useEntity = getUseEntity(context);
        if (!useEntity.isQueryable() || !useEntity.isCreatable()) {
            throw new ApiInvokeException(ApiInvokeException.ERR_BIZ, "Unsupportted operation for entity : " + useEntity.getName());
        }

        Record recordNew = new ExtRecordCreator(
                useEntity, (JSONObject) context.getPostData(), context.getBindUser(), true)
                .create();
        if (recordNew.getPrimary() != null) {
            return formatFailure("非可新建记录");
        }

        Collection<String> repeatedFields = checkRepeated(recordNew);
        if (!repeatedFields.isEmpty()) {
            return formatFailure("新建字段 " + StringUtils.join(repeatedFields, "/") + " 中存在重复值",
                    ApiInvokeException.ERR_DATASPEC);
        }

        recordNew = Application.getService(useEntity.getEntityCode()).create(recordNew);

        return formatSuccess(JSONUtils.toJSONObject("id", recordNew.getPrimary()));
    }

    /**
     * @param context
     * @return
     */
    protected Entity getUseEntity(ApiContext context) {
        JSONObject data = (JSONObject) context.getPostData();
        JSONObject metadata = data.getJSONObject(ExtRecordCreator.META_FIELD);

        final String useEntity = metadata == null ? null : metadata.getString("entity");
        if (metadata == null || useEntity == null) {
            throw new ApiInvokeException(ApiInvokeException.ERR_BADPARAMS, "Invalid post/data : `metadata` element not be empty");
        }

        if (!MetadataHelper.containsEntity(useEntity)) {
            throw new ApiInvokeException(ApiInvokeException.ERR_BADPARAMS, "Invalid post/data : Unknow entity : " + useEntity);
        }

        Entity entity = MetadataHelper.getEntity(useEntity);
        if (!entity.isQueryable() || MetadataHelper.isBizzEntity(entity.getEntityCode())) {
            throw new ApiInvokeException(ApiInvokeException.ERR_BADPARAMS, "Unsupportted operator for entity : " + useEntity);
        }
        return entity;
    }

    /**
     * 检查重复值
     * @param record
     */
    protected Collection<String> checkRepeated(Record record) {
        List<Record> repeated = Application.getGeneralEntityService().getCheckRepeated(record, 1);
        if (repeated.isEmpty()) return Collections.emptySet();

        // 重复字段
        Set<String> repeatedFields = new HashSet<>();
        for (Iterator<String> iter = repeated.get(0).getAvailableFieldIterator(); iter.hasNext(); ) {
            String fieldName = iter.next();
            Field field = record.getEntity().getField(fieldName);
            if (field.getType() == FieldType.PRIMARY) continue;

            repeatedFields.add(EasyMeta.getLabel(field));
        }
        return repeatedFields;
    }
}
