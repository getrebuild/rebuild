/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

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
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityRecordCreator;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyMeta;
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
            throw new ApiInvokeException("Unsupportted operation for entity : " + useEntity.getName());
        }

        Record recordNew = new EntityRecordCreator(
                useEntity, (JSONObject) context.getPostData(), context.getBindUser(), true)
                .create();
        if (recordNew.getPrimary() != null) {
            return formatFailure("Non-creatable record");
        }

        Collection<String> repeatedFields = checkRepeated(recordNew);
        if (!repeatedFields.isEmpty()) {
            return formatFailure(
                    "There are duplicate field values : " + StringUtils.join(repeatedFields, "/"),
                    ApiInvokeException.ERR_DATASPEC);
        }

        recordNew = Application.getEntityService(useEntity.getEntityCode()).create(recordNew);

        return formatSuccess(JSONUtils.toJSONObject("id", recordNew.getPrimary()));
    }

    /**
     * @param context
     * @return
     */
    protected Entity getUseEntity(ApiContext context) {
        JSONObject data = (JSONObject) context.getPostData();
        JSONObject metadata = data.getJSONObject(EntityRecordCreator.META_FIELD);

        final String useEntity = metadata == null ? null : metadata.getString("entity");
        if (metadata == null || useEntity == null) {
            throw new ApiInvokeException(ApiInvokeException.ERR_BADPARAMS, "Invalid post/data : [metadata] element not be null");
        }

        if (!MetadataHelper.containsEntity(useEntity)) {
            throw new ApiInvokeException(ApiInvokeException.ERR_BADPARAMS, "Invalid post/data : Unknown entity : " + useEntity);
        }

        Entity entity = MetadataHelper.getEntity(useEntity);
        if (!entity.isQueryable() || MetadataHelper.isBizzEntity(entity.getEntityCode())) {
            throw new ApiInvokeException(ApiInvokeException.ERR_BADPARAMS, "Unsupportted operator for entity : " + useEntity);
        }
        return entity;
    }

    /**
     * 检查重复值
     *
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

            repeatedFields.add(EasyMetaFactory.getLabel(field));
        }
        return repeatedFields;
    }
}
