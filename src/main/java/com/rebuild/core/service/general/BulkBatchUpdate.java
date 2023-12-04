/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.record.JsonRecordCreator;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.trigger.impl.FieldAggregation;
import com.rebuild.core.support.general.BatchOperatorQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

/**
 * 批量修改
 *
 * @author devezhao
 * @since 2019/12/2
 */
@Slf4j
public class BulkBatchUpdate extends BulkOperator {

    /**
     * 修改为
     */
    protected static final String OP_SET = "SET";
    /**
     * 置空
     */
    protected static final String OP_NULL = "NULL";

    public BulkBatchUpdate(BulkContext context, GeneralEntityService ges) {
        super(context, ges);
    }

    @Override
    public Integer exec() {
        final ID[] willUpdates = prepareRecords();
        this.setTotal(willUpdates.length);

        if (isInterruptState()) return getSucceeded();

        JSONArray updateContents = ((JSONObject) context.getExtraParams().get("customData"))
                .getJSONArray("updateContents");

        // 转化成标准 FORM 格式
        JSONObject formJson = new JSONObject();
        for (Object o : updateContents) {
            JSONObject item = (JSONObject) o;
            String field = item.getString("field");
            String op = item.getString("op");
            String value = item.getString("value");

            if (OP_NULL.equalsIgnoreCase(op)) {
                formJson.put(field, StringUtils.EMPTY);
            } else if (OP_SET.equalsIgnoreCase(op)) {
                formJson.put(field, value);
            }
        }

        JSONObject metadata = new JSONObject();
        metadata.put("entity", context.getMainEntity().getName());
        formJson.put(JsonRecordCreator.META_FIELD, metadata);
        if (log.isDebugEnabled()) {
            log.debug("Conversion to : {}", formJson);
        }

        GeneralEntityServiceContextHolder.setRepeatedCheckMode(GeneralEntityServiceContextHolder.RCM_CHECK_ALL);
        for (ID id : willUpdates) {
            if (isInterruptState()) break;

            if (Application.getPrivilegesManager().allowUpdate(context.getOpUser(), id)) {
                // 更新记录
                formJson.getJSONObject(JsonRecordCreator.META_FIELD).put("id", id.toLiteral());

                GeneralEntityServiceContextHolder.setRepeatedCheckMode(GeneralEntityServiceContextHolder.RCM_CHECK_MAIN);
                try {
                    Record record = EntityHelper.parse(formJson, context.getOpUser());
                    ges.createOrUpdate(record);
                    this.addSucceeded();

                } catch (Exception ex) {
                    log.warn("Cannot update `{}` because : {}", id, ex.getLocalizedMessage());

                } finally {
                    GeneralEntityServiceContextHolder.getRepeatedCheckModeOnce();

                    // 可能有级联触发器
                    Object ts = FieldAggregation.cleanTriggerChain();
                    if (ts != null) log.info("Clean current-loop : {}", ts);
                }

            } else {
                log.warn("No have privileges to UPDATE : {} < {}", id, context.getOpUser());
            }
            this.addCompleted();
        }

        return getSucceeded();
    }

    @Override
    protected ID[] prepareRecords() {
        JSONObject customData = (JSONObject) context.getExtraParams().get("customData");
        int dataRange = customData.getIntValue("_dataRange");
        BatchOperatorQuery query = new BatchOperatorQuery(dataRange, customData.getJSONObject("queryData"));
        return query.getQueryedRecordIds();
    }
}
