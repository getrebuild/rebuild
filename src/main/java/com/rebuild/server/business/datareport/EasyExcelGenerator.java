/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.datareport;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.DataReportManager;
import com.rebuild.server.helper.SetUser;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.fieldvalue.FieldValueWrapper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 报表生成 easyexcel
 * https://alibaba-easyexcel.github.io/quickstart/fill.html
 *
 * @author devezhao
 * @since 2020/2/24
 */
public class EasyExcelGenerator extends SetUser<EasyExcelGenerator> {

    private File template;
    private ID recordId;

    private boolean hasMaster = false;

    protected EasyExcelGenerator() { }

    /**
     * @param reportId
     * @param recordId
     */
    public EasyExcelGenerator(ID reportId, ID recordId) {
        this(DataReportManager.instance.getTemplateFile(
                MetadataHelper.getEntity(recordId.getEntityCode()), reportId), recordId);
    }

    /**
     * @param template
     * @param recordId
     */
    public EasyExcelGenerator(File template, ID recordId) {
        this.template = template;
        this.recordId = recordId;
    }

    /**
     * @return
     */
    public File generate() {
        String suffix = this.template.getName().endsWith(".xlsx") ? ".xlsx" : ".xls";
        File dest = SysConfiguration.getFileOfTemp("REPORT-" + System.currentTimeMillis() + suffix);

        List<Map<String, Object>> datas = buildData();
        if (datas.isEmpty()) {
            return  null;
        }

        Map<String, Object> master = null;
        if (this.hasMaster) {
            Iterator<Map<String, Object>> iter = datas.iterator();
            master = iter.next();
            iter.remove();
        }

        ExcelWriter excelWriter = null;
        FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
        try {
            excelWriter = EasyExcel.write(dest).withTemplate(template).build();
            WriteSheet writeSheet = EasyExcel.writerSheet().build();

            // 明细记录
            if (!datas.isEmpty()) {
                excelWriter.fill(datas, fillConfig, writeSheet);
            }

            // 主记录
            if (master != null) {
                excelWriter.fill(master, writeSheet);
            }

        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
        return dest;
    }

    /**
     * @return 第一个为主记录（若有）
     */
    protected List<Map<String, Object>> buildData() {
        Entity entity = MetadataHelper.getEntity(this.recordId.getEntityCode());

        TemplateExtractor templateExtractor = new TemplateExtractor(this.template, true);
        Map<String, String> varsMap = templateExtractor.transformVars(entity);

        List<String> fieldsOfMaster = new ArrayList<>();
        List<String> fieldsOfSlave = new ArrayList<>();

        for (Map.Entry<String, String> e : varsMap.entrySet()) {
            String validField = e.getValue();
            // 无效字段
            if (validField == null) {
                continue;
            }

            if (e.getKey().startsWith(TemplateExtractor.SLAVE_PREFIX)) {
                fieldsOfSlave.add(validField);
            } else {
                fieldsOfMaster.add(validField);
            }
        }

        if (fieldsOfMaster.isEmpty() && fieldsOfSlave.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Map<String, Object>> datas = new ArrayList<>();
        final String baseSql = "select %s from %s where %s = ?";

        if (!fieldsOfMaster.isEmpty()) {
            String sql = String.format(baseSql,
                    StringUtils.join(fieldsOfMaster, ","), entity.getName(), entity.getPrimaryField().getName());
            Record record = Application.createQuery(sql, this.getUser())
                    .setParameter(1, this.recordId)
                    .record();
            Assert.notNull(record, "No record found : " + this.recordId);

            Map<String, Object> data = buildData(record, varsMap, false);
            datas.add(data);
            this.hasMaster = true;
        }
        
        // 无明细
        if (fieldsOfSlave.isEmpty()) {
            return datas;
        }

        String sql = String.format(baseSql + " order by modifiedOn desc",
                StringUtils.join(fieldsOfSlave, ","),
                entity.getSlaveEntity().getName(),
                MetadataHelper.getSlaveToMasterField(entity.getSlaveEntity()).getName());
        List<Record> list = Application.createQuery(sql, this.getUser())
                .setParameter(1, this.recordId)
                .list();

        for (Record c : list) {
            datas.add(buildData(c, varsMap, true));
        }
        return datas;
    }

    /**
     * @param record
     * @param varsMap
     * @param isSlave
     * @return
     */
    protected Map<String, Object> buildData(Record record, Map<String, String> varsMap, boolean isSlave) {
        final Entity entity = record.getEntity();

        final Map<String, Object> data = new HashMap<>();
        // 无效字段填充
        for (Map.Entry<String, String> e : varsMap.entrySet()) {
            if (e.getValue() == null) {
                String varName = e.getKey();
                if (isSlave) {
                    if (varName.startsWith(TemplateExtractor.SLAVE_PREFIX)) {
                        data.put(varName.substring(1), "[无效字段]");
                    }
                } else {
                    data.put(varName, "[无效字段]");
                }
            }
        }

        for (Iterator<String> iter = record.getAvailableFieldIterator(); iter.hasNext(); ) {
            final String fieldName = iter.next();
            EasyMeta easyMeta = EasyMeta.valueOf(MetadataHelper.getLastJoinField(entity, fieldName));
            DisplayType dt = easyMeta.getDisplayType();
            if (dt == DisplayType.IMAGE || dt == DisplayType.AVATAR
                    || dt == DisplayType.FILE || dt == DisplayType.LOCATION) {
                data.put(fieldName, "[暂不支持" + dt.getDisplayName() + "]");
                continue;
            }

            // 替换成变量名
            String varName = fieldName;
            for (Map.Entry<String, String> e : varsMap.entrySet()) {
                if (fieldName.equalsIgnoreCase(e.getValue())) {
                    varName = e.getKey();
                    break;
                }
            }
            if (varName.startsWith(TemplateExtractor.SLAVE_PREFIX)) {
                varName = varName.substring(1);
            }

            Object fieldValue = record.getObjectValue(fieldName);
            if (fieldValue == null) {
                data.put(varName, StringUtils.EMPTY);
            } else {
                fieldValue = FieldValueWrapper.instance.wrapFieldValue(fieldValue, easyMeta, true);
                data.put(varName, fieldValue);
            }
        }
        return data;
    }
}
