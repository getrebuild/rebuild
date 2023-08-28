/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.DefinedException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.MultiValue;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.SetUser;
import com.rebuild.core.support.general.BarCodeSupport;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.ImageView2;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.rebuild.core.service.datareport.TemplateExtractor.APPROVAL_PREFIX;
import static com.rebuild.core.service.datareport.TemplateExtractor.NROW_PREFIX;
import static com.rebuild.core.service.datareport.TemplateExtractor.PH__CURRENTBIZUNIT;
import static com.rebuild.core.service.datareport.TemplateExtractor.PH__CURRENTDATE;
import static com.rebuild.core.service.datareport.TemplateExtractor.PH__CURRENTDATETIME;
import static com.rebuild.core.service.datareport.TemplateExtractor.PH__CURRENTUSER;
import static com.rebuild.core.service.datareport.TemplateExtractor.PH__KEEP;
import static com.rebuild.core.service.datareport.TemplateExtractor.PH__NUMBER;
import static com.rebuild.core.service.datareport.TemplateExtractor.PLACEHOLDER;

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
    protected Integer writeSheetAt = null;
    protected ID recordId;

    protected boolean hasMain = false;
    protected int phNumber = 1;
    protected Map<String, Object> phValues = new HashMap<>();

    /**
     * @param template
     * @param recordId
     */
    protected EasyExcelGenerator(File template, ID recordId) {
        this.template = getFixTemplate(template);
        this.recordId = recordId;
    }

    /**
     * 生成报表
     *
     * @return
     */
    public File generate() {
        final File target = getTargetFile();

        List<Map<String, Object>> datas = buildData();
        if (datas.isEmpty()) throw new DefinedException(Language.L("暂无数据"));

        Map<String, Object> main = null;
        if (this.hasMain) {
            Iterator<Map<String, Object>> iter = datas.iterator();
            main = iter.next();
            iter.remove();
        }

        FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();

        try (ExcelWriter excelWriter = EasyExcel.write(target).withTemplate(template).build()) {
            WriteSheet writeSheet = EasyExcel.writerSheet(writeSheetAt)
                    .registerWriteHandler(new FixsMergeStrategy())
                    .registerWriteHandler(new FormulaCellWriteHandler())
                    .build();

            // 有明细记录
            if (!datas.isEmpty()) {
                excelWriter.fill(datas, fillConfig, writeSheet);
            }

            // 主记录
            if (main != null) {
                excelWriter.fill(main, writeSheet);
            } else {
                // PH 变量
                if (!phValues.isEmpty()) {
                    excelWriter.fill(phValues, writeSheet);
                }
            }

            // 强制公式生效
            Workbook wb = excelWriter.writeContext().writeWorkbookHolder().getWorkbook();
            wb.setForceFormulaRecalculation(true);
            wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
        }

        return target;
    }

    /**
     * @return
     */
    protected File getTargetFile() {
        return RebuildConfiguration.getFileOfTemp(String.format("RBREPORT-%d.%s",
                System.currentTimeMillis(), template.getName().endsWith(".xlsx") ? "xlsx" : "xls"));
    }

    /**
     * 构建报表数据
     *
     * @return 第一个为主记录（若有）
     */
    protected List<Map<String, Object>> buildData() {
        Entity entity = MetadataHelper.getEntity(this.recordId.getEntityCode());

        TemplateExtractor templateExtractor = new TemplateExtractor(this.template);
        Map<String, String> varsMap = templateExtractor.transformVars(entity);

        Map<String, String> varsMapOfMain = new HashMap<>();
        Map<String, String> varsMapOfDetail = new HashMap<>();
        Map<String, String> varsMapOfApproval = new HashMap<>();

        List<String> fieldsOfMain = new ArrayList<>();
        List<String> fieldsOfDetail = new ArrayList<>();
        List<String> fieldsOfApproval = new ArrayList<>();

        for (Map.Entry<String, String> e : varsMap.entrySet()) {
            final String varName = e.getKey();

            if (varName.startsWith(APPROVAL_PREFIX)) {
                varsMapOfApproval.put(varName, e.getValue());
            } else if (varName.startsWith(NROW_PREFIX)) {
                varsMapOfDetail.put(varName, e.getValue());
            } else {
                varsMapOfMain.put(varName, e.getValue());
            }

            String validField = e.getValue();

            // 占位字段
            if (varName.startsWith(PLACEHOLDER)
                    || varName.startsWith(NROW_PREFIX + PLACEHOLDER)) {
                continue;
            }
            // 无效字段
            else if (validField == null) {
                log.warn("Invalid field `{}` in template : {}", e.getKey(), this.template);
                continue;
            }

            if (varName.startsWith(APPROVAL_PREFIX)) {
                fieldsOfApproval.add(validField);
            } else if (varName.startsWith(NROW_PREFIX)) {
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
            String sql = String.format(baseSql + " order by autoId asc",
                    StringUtils.join(fieldsOfDetail, ","),
                    entity.getDetailEntity().getPrimaryField().getName(),
                    entity.getDetailEntity().getName(),
                    MetadataHelper.getDetailToMainField(entity.getDetailEntity()).getName());

            List<Record> list = Application.createQuery(sql, this.getUser())
                    .setParameter(1, this.recordId)
                    .list();

            phNumber = 1;
            for (Record c : list) {
                datas.add(buildData(c, varsMapOfDetail));
                phNumber++;
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

            phNumber = 1;
            for (Record c : list) {
                datas.add(buildData(c, varsMapOfApproval));
                phNumber++;
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

        // 非实体字段填充
        for (Map.Entry<String, String> e : varsMap.entrySet()) {
            String varName = e.getKey();

            if (e.getValue() == null) {
                if (varName.startsWith(NROW_PREFIX)) {
                    varName = varName.substring(1);
                }

                Object phValue = getPhValue(varName);
                if (phValue != null) {
                    data.put(varName, phValue);
                } else {
                    data.put(varName, invalidFieldTip);
                }
            }
        }

        for (final String fieldName : varsMap.values()) {
            if (fieldName == null) continue;

            EasyField easyField = EasyMetaFactory.valueOf(MetadataHelper.getLastJoinField(entity, fieldName));
            DisplayType dt = easyField.getDisplayType();

            // 替换成变量名
            String varName = fieldName;
            for (Map.Entry<String, String> e : varsMap.entrySet()) {
                if (fieldName.equalsIgnoreCase(e.getValue())) {
                    varName = e.getKey();
                    break;
                }
            }

            if (varName.startsWith(NROW_PREFIX)) {
                varName = varName.substring(1);
            }

            // FIXME v3.2 图片仅支持导出第一张
            if (!dt.isExportable() && dt != DisplayType.IMAGE) {
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
                } else if (dt == DisplayType.IMAGE) {
                    fieldValue = buildImageData((String) fieldValue);
                } else {

                    if (dt == DisplayType.NUMBER) {
                        // Keep Type
                    } else if (dt == DisplayType.DECIMAL) {
                        String format = easyField.getExtraAttr(EasyFieldConfigProps.DECIMAL_FORMAT);
                        int scale = StringUtils.isBlank(format) ? 2 : format.split("\\.")[1].length();
                        // Keep Type
                        fieldValue = ObjectUtils.round(((BigDecimal) fieldValue).doubleValue(), scale);
                    } else {
                        fieldValue = FieldValueHelper.wrapFieldValue(fieldValue, easyField, Boolean.TRUE);
                    }

                    if (record.getEntity().getEntityCode() == EntityHelper.RobotApprovalStep && "state".equalsIgnoreCase(fieldName)) {
                        fieldValue = Language.L(ApprovalState.valueOf(ObjectUtils.toInt(fieldValue)));
                    } else if (FieldValueHelper.isUseDesensitized(easyField, this.getUser())) {
                        fieldValue = FieldValueHelper.desensitized(easyField, fieldValue);
                    }
                    // v3.1.4
                    else if (dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE) {

                        Field refNameField = easyField.getRawMeta().getReferenceEntity().getNameField();
                        EasyField easyNameField = EasyMetaFactory.valueOf(refNameField);

                        if (FieldValueHelper.isUseDesensitized(easyNameField, this.getUser())) {
                            if (dt == DisplayType.N2NREFERENCE) {
                                List<String> fieldValueList = new ArrayList<>();
                                for (String s : fieldValue.toString().split(MultiValue.MV_SPLIT)) {
                                    fieldValueList.add((String) FieldValueHelper.desensitized(easyNameField, s));
                                }
                                fieldValue = StringUtils.join(fieldValueList, MultiValue.MV_SPLIT);
                            } else {
                                fieldValue = FieldValueHelper.desensitized(easyNameField, fieldValue);
                            }
                        }
                    }
                }
                data.put(varName, fieldValue);
            }
        }
        return data;
    }

    private byte[] buildImageData(String fieldValue) {
        JSONArray paths = JSON.parseArray(fieldValue);
        if (paths == null || paths.isEmpty()) return null;

        try {
            for (Object path : paths) {
                File temp = QiniuCloud.getStorageFile((String) path);
                File img1000 = new ImageView2(1000).thumbQuietly(temp);

                byte[] b = FileUtils.readFileToByteArray(img1000);
                String base64 = Base64.encodeBase64String(b);
                return buildSignData("base64," + base64);
            }

        } catch (IOException ex) {
            log.error("Build image data error : {}", fieldValue, ex);
        }
        return null;
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

    /**
     * @param phName
     * @return
     */
    protected Object getPhValue(String phName) {
        if (!phName.contains("__")) return null;

        // detail.__KEEP > __KEEP
        phName = PLACEHOLDER + phName.split("__")[1];

        // {.__KEEP:块}
        if (phName.startsWith(PH__KEEP)) {
            return phName.length() > PH__KEEP.length()
                    ? phName.substring(PH__KEEP.length() + 1) : "";
        }
        // 列表序号
        if (phName.equals(PH__NUMBER)) {
            return phNumber;
        }
        if (phName.equals(PH__CURRENTUSER)) {
            return UserHelper.getName(getUser());
        }
        if (phName.equals(PH__CURRENTBIZUNIT)) {
            return Objects.requireNonNull(UserHelper.getDepartment(getUser())).getName();
        }
        if (phName.equals(PH__CURRENTDATE)) {
            return CalendarUtils.getUTCDateFormat().format(CalendarUtils.now());
        }
        if (phName.equals(PH__CURRENTDATETIME)) {
            return CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now());
        }
        return null;
    }

    // 修复模板
    private File getFixTemplate(File template) {
        return template;

        // v34 暂不启用
//        String fixName = template.getName();
//        fixName = fixName.substring(0, fixName.lastIndexOf(".")) + "__FIX34" + fixName.substring(fixName.lastIndexOf("."));
//        File fixTemplate = new File(template.getParent(), fixName);
//        if (fixTemplate.exists()) return fixTemplate;
//
//        // Use copy
//        try {
//            FileUtils.copyFile(template, fixTemplate);
//        } catch (IOException e) {
//            throw new ReportsException(e);
//        }
//
//        // 替换 `.` > `$`
//        try (Workbook wb = WorkbookFactory.create(Files.newInputStream(template.toPath()))) {
//            Sheet sheet = wb.getSheetAt(0);
//            for (Iterator<Row> iterRow = sheet.rowIterator(); iterRow.hasNext(); ) {
//                final Row row = iterRow.next();
//
//                for (Iterator<Cell> iterCell = row.cellIterator(); iterCell.hasNext(); ) {
//                    final Cell cell = iterCell.next();
//                    final String cellValue = cell.getStringCellValue();
//
//                    String newCellValue = cellValue;
//                    Matcher matcher = TemplateExtractor.PATT_V2.matcher(cellValue);
//                    while (matcher.find()) {
//                        String varName = matcher.group(1);
//                        if (varName.contains(".") && !varName.startsWith(".")) {
//                            newCellValue = newCellValue.replace(".", "$");
//                        }
//                    }
//
//                    if (!cellValue.equals(newCellValue)) {
//                        log.info("Replace `{}` to `{}` in : {}", cellValue, newCellValue, template);
//                        cell.setCellValue(newCellValue);
//                    }
//                }
//            }
//
//            try (FileOutputStream fos = new FileOutputStream(fixTemplate)) {
//                wb.write(fos);
//            }
//        } catch (IOException ex) {
//            log.error("Cannot fix template : {}", template, ex);
//
//            FileUtils.deleteQuietly(fixTemplate);
//            return template;
//        }
//
//        return fixTemplate;
    }

    // --

    /**
     * @param reportId
     * @param recordIds
     * @return
     */
    public static EasyExcelGenerator create(ID reportId, List<ID> recordIds) {
        final ID recordId = recordIds.get(0);
        if (recordIds.size() == 1) {
            return create(reportId, recordId);
        } else {
            TemplateFile tt = DataReportManager.instance.getTemplateFile(MetadataHelper.getEntity(recordId.getEntityCode()), reportId);
            return new EasyExcelGenerator33(tt.templateFile, recordIds);
        }
    }

    /**
     * @param reportId
     * @param recordId
     * @return
     */
    public static EasyExcelGenerator create(ID reportId, ID recordId) {
        TemplateFile tt = DataReportManager.instance.getTemplateFile(MetadataHelper.getEntity(recordId.getEntityCode()), reportId);
        return create(tt.templateFile, recordId, tt.isV33);
    }

    /**
     * @param template
     * @param recordId
     * @param isV33
     * @return
     */
    public static EasyExcelGenerator create(File template, ID recordId, boolean isV33) {
        return isV33
                ? new EasyExcelGenerator33(template, recordId)
                : new EasyExcelGenerator(template, recordId);
    }
}
