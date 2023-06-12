/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.excel.exception.ExcelRuntimeException;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rebuild.core.service.datareport.TemplateExtractor.APPROVAL_PREFIX;
import static com.rebuild.core.service.datareport.TemplateExtractor.NROW_PREFIX;
import static com.rebuild.core.service.datareport.TemplateExtractor33.DETAIL_PREFIX;

/**
 * V33
 *
 * @author devezhao
 * @since 2023/4/5
 */
@Slf4j
public class EasyExcelGenerator33 extends EasyExcelGenerator {

    protected EasyExcelGenerator33(File template, ID recordId) {
        super(template, recordId);
    }

    @Override
    protected List<Map<String, Object>> buildData() {
        final Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
        final Map<String, String> varsMap = new TemplateExtractor33(template).transformVars(entity);

        // 变量
        Map<String, String> varsMapOfMain = new HashMap<>();
        Map<String, Map<String, String>> varsMapOfRefs = new HashMap<>();
        // 字段
        List<String> fieldsOfMain = new ArrayList<>();
        Map<String, List<String>> fieldsOfRefs = new HashMap<>();

        for (Map.Entry<String, String> e : varsMap.entrySet()) {
            final String varName = e.getKey();
            final String fieldName = e.getValue();

            String refName = null;
            if (varName.startsWith(NROW_PREFIX)) {
                if (varName.startsWith(APPROVAL_PREFIX)) {
                    refName = APPROVAL_PREFIX;
                } else if (varName.startsWith(DETAIL_PREFIX)) {
                    refName = DETAIL_PREFIX;
                } else {
                    // .AccountId.SalesOrder.SalesOrderName
                    String[] split = varName.substring(1).split("\\.");
                    if (split.length < 2) throw new ExcelRuntimeException("Bad REF (Miss .detail prefix?) : " + varName);
                    
                    String refName2 = split[0] + split[1];
                    refName = varName.substring(0, refName2.length() + 2 /* dots */);
                }

                Map<String, String> varsMapOfRef = varsMapOfRefs.getOrDefault(refName, new HashMap<>());
                varsMapOfRef.put(varName, fieldName);
                varsMapOfRefs.put(refName, varsMapOfRef);

            } else {
                varsMapOfMain.put(varName, fieldName);
            }

            // 占位字段
            if (TemplateExtractor33.isPlaceholder(varName)) continue;
            // 无效字段
            if (fieldName == null) {
                log.warn("Invalid field `{}` in template : {}", e.getKey(), template);
                continue;
            }

            if (varName.startsWith(NROW_PREFIX)) {
                List<String> fieldsOfRef = fieldsOfRefs.getOrDefault(refName, new ArrayList<>());
                fieldsOfRef.add(fieldName);
                fieldsOfRefs.put(refName, fieldsOfRef);
            } else {
                fieldsOfMain.add(fieldName);
            }
        }

        if (fieldsOfMain.isEmpty() && fieldsOfRefs.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Map<String, Object>> datas = new ArrayList<>();
        final String baseSql = "select %s,%s from %s where %s = ?";

        if (!fieldsOfMain.isEmpty()) {
            String sql = String.format(baseSql,
                    StringUtils.join(fieldsOfMain, ","),
                    entity.getPrimaryField().getName(), entity.getName(), entity.getPrimaryField().getName());

            Record record = Application.createQuery(sql, this.getUser())
                    .setParameter(1, recordId)
                    .record();
            Assert.notNull(record, "No record found : " + recordId);

            this.hasMain = true;
            datas.add(buildData(record, varsMapOfMain));
        }

        for (Map.Entry<String, List<String>> e : fieldsOfRefs.entrySet()) {
            final String refName = e.getKey();

            String querySql = baseSql;
            if (refName.startsWith(APPROVAL_PREFIX)) {
                querySql += " and isWaiting = 'F' and isCanceled = 'F' order by createdOn";
                querySql = String.format(querySql, StringUtils.join(e.getValue(), ","),
                        "stepId", "RobotApprovalStep", "recordId");
            } else if (refName.startsWith(DETAIL_PREFIX)) {
                Entity de = entity.getDetailEntity();
                querySql += " order by autoId asc";
                querySql = String.format(querySql, StringUtils.join(e.getValue(), ","),
                        de.getPrimaryField().getName(), de.getName(), MetadataHelper.getDetailToMainField(de).getName());
            } else {
                String[] split = refName.substring(1).split("\\.");
                Field ref2Field = MetadataHelper.getField(split[1], split[0]);
                Entity ref2Entity = ref2Field.getOwnEntity();

                querySql += " order by createdOn";
                querySql = String.format(querySql, StringUtils.join(e.getValue(), ","),
                        ref2Entity.getPrimaryField().getName(), ref2Entity.getName(), split[0]);
            }

            List<Record> list = Application.createQuery(querySql, getUser())
                    .setParameter(1, recordId)
                    .list();

            phNumber = 1;
            for (Record c : list) {
                datas.add(buildData(c, varsMapOfRefs.get(refName)));
                phNumber++;
            }
        }

        return datas;
    }
}
