/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.SetUser;
import com.rebuild.core.support.general.BarCodeSupport;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 报表生成 easyexcel
 * https://alibaba-easyexcel.github.io/quickstart/fill.html
 *
 * @author devezhao
 * @since 2020/2/24
 */
@Slf4j
public class EasyExcelGenerator extends SetUser {

    protected File template;
    private ID recordId;

    private boolean hasMain = false;

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
        File tmp = RebuildConfiguration.getFileOfTemp(String.format("RBREPORT-%d.%s",
                System.currentTimeMillis(), template.getName().endsWith(".xlsx") ? "xlsx" : "xls"));

        List<Map<String, Object>> datas = buildData();
        // 无数据
        if (datas.isEmpty()) return null;

        Map<String, Object> main = null;
        if (this.hasMain) {
            Iterator<Map<String, Object>> iter = datas.iterator();
            main = iter.next();
            iter.remove();
        }

        FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
        try (ExcelWriter excelWriter = EasyExcel.write(tmp).withTemplate(template).build()) {
            WriteSheet writeSheet = EasyExcel.writerSheet().registerWriteHandler(new FixsMergeStrategy()).build();

            // 有明细记录
            if (!datas.isEmpty()) {
                excelWriter.fill(datas, fillConfig, writeSheet);
            }

            // 主记录
            if (main != null) {
                excelWriter.fill(main, writeSheet);
            }
        }

        return tmp;
    }

    /**
     * @return 第一个为主记录（若有）
     */
    protected List<Map<String, Object>> buildData() {
        Entity entity = MetadataHelper.getEntity(this.recordId.getEntityCode());

        TemplateExtractor templateExtractor = new TemplateExtractor(this.template, true);
        Map<String, String> varsMap = templateExtractor.transformVars(entity);

        Map<String, String> varsMapOfMain = new HashMap<>();
        Map<String, String> varsMapOfDetail = new HashMap<>();
        Map<String, String> varsMapOfApproval = new HashMap<>();

        List<String> fieldsOfMain = new ArrayList<>();
        List<String> fieldsOfDetail = new ArrayList<>();
        List<String> fieldsOfApproval = new ArrayList<>();

        for (Map.Entry<String, String> e : varsMap.entrySet()) {
            final String field = e.getKey();

            if (field.startsWith(TemplateExtractor.APPROVAL_PREFIX)) {
                varsMapOfApproval.put(field, e.getValue());
            } else if (field.startsWith(TemplateExtractor.NROW_PREFIX)) {
                varsMapOfDetail.put(field, e.getValue());
            } else {
                varsMapOfMain.put(field, e.getValue());
            }

            String validField = e.getValue();
            // 无效字段
            if (validField == null) {
                log.warn("Invalid field `{}` in template : {}", e.getKey(), this.template);
                continue;
            }

            if (field.startsWith(TemplateExtractor.APPROVAL_PREFIX)) {
                fieldsOfApproval.add(validField);
            } else if (field.startsWith(TemplateExtractor.NROW_PREFIX)) {
                fieldsOfDetail.add(validField);
            } else {
                fieldsOfMain.add(validField);
            }
        }

        if (fieldsOfMain.isEmpty() && fieldsOfDetail.isEmpty() && fieldsOfApproval.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Map<String, Object>> datas = new ArrayList<>();
        final String baseSql = "select %s,%s from %s where %s = ?";

        if (!fieldsOfMain.isEmpty()) {
            String sql = String.format(baseSql,
                    StringUtils.join(fieldsOfMain, ","),
                    entity.getPrimaryField().getName(), entity.getName(), entity.getPrimaryField().getName());

            Record record = Application.createQuery(sql, this.getUser())
                    .setParameter(1, this.recordId)
                    .record();
            Assert.notNull(record, "No record found : " + this.recordId);

            datas.add(buildData(record, varsMapOfMain));
            this.hasMain = true;
        }

        // 明细
        if (!fieldsOfDetail.isEmpty()) {
            String sql = String.format(baseSql + " order by modifiedOn desc",
                    StringUtils.join(fieldsOfDetail, ","),
                    entity.getDetailEntity().getPrimaryField().getName(),
                    entity.getDetailEntity().getName(),
                    MetadataHelper.getDetailToMainField(entity.getDetailEntity()).getName());

            List<Record> list = Application.createQuery(sql, this.getUser())
                    .setParameter(1, this.recordId)
                    .list();

            for (Record c : list) {
                datas.add(buildData(c, varsMapOfDetail));
            }
        }

        // 审批
        if (!fieldsOfApproval.isEmpty()) {
            String sql = String.format(
                    "select %s,stepId from RobotApprovalStep where recordId = ? and isWaiting = 'F' and isCanceled = 'F' order by createdOn",
                    StringUtils.join(fieldsOfApproval, ","));

            List<Record> list = Application.createQueryNoFilter(sql)
                    .setParameter(1, this.recordId)
                    .list();

            for (Record c : list) {
                datas.add(buildData(c, varsMapOfApproval));
            }
        }

        return datas;
    }

    /**
     * @param record
     * @param varsMap
     * @return
     */
    protected Map<String, Object> buildData(Record record, Map<String, String> varsMap) {
        final Entity entity = record.getEntity();

        final String invalidFieldTip = Language.L("[无效字段]");
        final String unsupportFieldTip = Language.L("[暂不支持]");

        final Map<String, Object> data = new HashMap<>();

        // 无效字段填充
        for (Map.Entry<String, String> e : varsMap.entrySet()) {
            if (e.getValue() == null) {
                String varName = e.getKey();
                if (varName.startsWith(TemplateExtractor.NROW_PREFIX)) {
                    varName = varName.substring(1);
                }
                data.put(varName, invalidFieldTip);
            }
        }

        for (final String fieldName : varsMap.values()) {
            if (fieldName == null) continue;

            EasyField easyField = EasyMetaFactory.valueOf(
                    Objects.requireNonNull(MetadataHelper.getLastJoinField(entity, fieldName)));
            DisplayType dt = easyField.getDisplayType();

            // 替换成变量名
            String varName = fieldName;
            for (Map.Entry<String, String> e : varsMap.entrySet()) {
                if (fieldName.equalsIgnoreCase(e.getValue())) {
                    varName = e.getKey();
                    break;
                }
            }
            if (varName.startsWith(TemplateExtractor.NROW_PREFIX)) {
                varName = varName.substring(1);
            }

            if (!dt.isExportable()) {
                data.put(varName, unsupportFieldTip);
                continue;
            }

            Object fieldValue = record.getObjectValue(fieldName);

            if (dt == DisplayType.BARCODE) {
                data.put(varName, buildBarcodeData(easyField.getRawMeta(), record.getPrimary()));
            } else if (fieldValue == null) {
                data.put(varName, StringUtils.EMPTY);
            } else {

                if (dt == DisplayType.SIGN) {
                    fieldValue = buildSignData((String) fieldValue);
                }  else {
                    fieldValue = FieldValueHelper.wrapFieldValue(fieldValue, easyField, true);

                    if (FieldValueHelper.isUseDesensitized(easyField, this.getUser())) {
                        fieldValue = FieldValueHelper.desensitized(easyField, fieldValue);
                    } else if (record.getEntity().getEntityCode() == EntityHelper.RobotApprovalStep
                            && "state".equalsIgnoreCase(fieldName)) {
                        fieldValue = Language.L(ApprovalState.valueOf(ObjectUtils.toInt(fieldValue)));
                    }
                }
                data.put(varName, fieldValue);
            }
        }
        return data;
    }

    private byte[] buildSignData(String base64img) {
        // data:image/png;base64,xxx
        return Base64Utils.decodeFromString(base64img.split("base64,")[1]);
    }

    private byte[] buildBarcodeData(Field barcodeField, ID recordId) {
        BufferedImage bi = BarCodeSupport.getBarCodeImage(barcodeField, recordId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bi, "png", baos);

            String base64 = Base64.encodeBase64String(baos.toByteArray());
            return buildSignData("base64," + base64);

        } catch (IOException e) {
            log.error("Cannot encode image of barcode : {}", recordId, e);
        }
        return null;
    }
}
