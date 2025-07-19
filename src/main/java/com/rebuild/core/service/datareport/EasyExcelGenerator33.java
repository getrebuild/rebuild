/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.approval.ApprovalHelper;
import com.rebuild.core.support.general.ProtocolFilterParser;
import com.rebuild.core.support.general.RecordBuilder;
import com.rebuild.core.support.i18n.Language;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFSimpleShape;
import org.apache.poi.xssf.usermodel.XSSFTextRun;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import static com.rebuild.core.service.datareport.TemplateExtractor.APPROVAL_PREFIX;
import static com.rebuild.core.service.datareport.TemplateExtractor.NROW_PREFIX;
import static com.rebuild.core.service.datareport.TemplateExtractor33.APPROVAL_PREFIX2;
import static com.rebuild.core.service.datareport.TemplateExtractor33.DETAIL_PREFIX;
import static com.rebuild.core.service.datareport.TemplateExtractor33.DETAIL_PREFIX2;
import static com.rebuild.core.service.datareport.TemplateExtractor33.NROW_PREFIX2;

/**
 * V33
 *
 * @author devezhao
 * @since 2023/4/5
 */
@Slf4j
public class EasyExcelGenerator33 extends EasyExcelGenerator {

    // 支持多记录导出，会合并到一个 Excel 文件
    final private List<ID> recordIdMulti;
    // 默认合并到一个文件，也可以打包成一个 zip
    @Setter
    private boolean recordIdMultiMerge2Sheets = true;

    private Set<String> inShapeVars;
    private Map<String, Object> recordMainHolder;

    @Setter
    protected Map<String, Object> tempVars;

    protected EasyExcelGenerator33(File templateFile, ID recordId) {
        super(templateFile, recordId);
        this.recordIdMulti = null;
    }

    protected EasyExcelGenerator33(File template, List<ID> recordIds) {
        super(template, recordIds.get(0));
        this.recordIdMulti = recordIds;
    }

    @Override
    protected Map<String, List<Map<String, Object>>> buildData() {
        final Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());

        final TemplateExtractor33 templateExtractor33 = this.buildTemplateExtractor33();
        final Map<String, String> varsMap = templateExtractor33.transformVars(entity);

        // 变量
        Map<String, String> varsMapOfMain = new HashMap<>();
        Map<String, Map<String, String>> varsMapOfRefs = new HashMap<>();
        // 字段
        List<String> fieldsOfMain = new ArrayList<>();
        Map<String, List<String>> fieldsOfRefs = new HashMap<>();

        for (Map.Entry<String, String> e : varsMap.entrySet()) {
            final String varName = e.getKey();
            final String varNameNoAt = varName.replace(TemplateExtractor33.IMG_PREFIX, "");
            final String fieldName = e.getValue();

            String refKey = null;
            if (varName.startsWith(NROW_PREFIX) || varName.startsWith(NROW_PREFIX2)) {
                if (varName.startsWith(APPROVAL_PREFIX) || varName.startsWith(APPROVAL_PREFIX2)) {
                    refKey = varName.startsWith(NROW_PREFIX) ? APPROVAL_PREFIX : APPROVAL_PREFIX2;
                } else if (varNameNoAt.startsWith(DETAIL_PREFIX) || varNameNoAt.startsWith(DETAIL_PREFIX2)) {
                    refKey = varNameNoAt.startsWith(NROW_PREFIX) ? DETAIL_PREFIX : DETAIL_PREFIX2;
                } else {
                    // 在客户中导出订单（下列 AccountId 为订单中引用客户的引用字段）
                    // .AccountId.SalesOrder.SalesOrderName or $AccountId$SalesOrder$SalesOrderName
                    String[] split = varNameNoAt.substring(1).split("[.$]");
                    if (split.length < 2) throw new ReportsException("Bad REF (Miss .detail prefix?) : " + varName);

                    String refName2 = split[0] + split[1];
                    refKey = varNameNoAt.substring(0, refName2.length() + 2 /* dots */);
                }

                Map<String, String> varsMapOfRef = varsMapOfRefs.getOrDefault(refKey, new HashMap<>());
                varsMapOfRef.put(varName, fieldName);
                varsMapOfRefs.put(refKey, varsMapOfRef);

            } else {
                varsMapOfMain.put(varName, fieldName);
            }

            // 占位字段
            if (TemplateExtractor33.isPlaceholder(varName)) {
                continue;
            }
            // 无效字段
            if (fieldName == null) {
                log.warn("Invalid field `{}` in template : {}", e.getKey(), templateFile);
                continue;
            }

            if (varName.startsWith(NROW_PREFIX) || varName.startsWith(NROW_PREFIX2)) {
                List<String> fieldsOfRef = fieldsOfRefs.getOrDefault(refKey, new ArrayList<>());
                fieldsOfRef.add(fieldName);
                fieldsOfRefs.put(refKey, fieldsOfRef);
            } else {
                fieldsOfMain.add(fieldName);
            }
        }

        if (fieldsOfMain.isEmpty() && fieldsOfRefs.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<String, List<Map<String, Object>>> datas = new HashMap<>();
        final String baseSql = "select %s,%s from %s where %s = ?";

        // 主记录
        if (!fieldsOfMain.isEmpty()) {
            String sql = String.format(baseSql,
                    StringUtils.join(fieldsOfMain, ","),
                    entity.getPrimaryField().getName(), entity.getName(), entity.getPrimaryField().getName());

            Record record = Application.createQuery(sql, this.getUser())
                    .setParameter(1, recordId)
                    .record();
            Assert.notNull(record, "No record found : " + recordId);

            Map<String, Object> d = buildData(record, varsMapOfMain);
            datas.put(REFKEY_RECORD_MAIN, Collections.singletonList(d));
            recordMainHolder = d;
        }

        // 相关记录（含明细、审批）
        for (Map.Entry<String, List<String>> e : fieldsOfRefs.entrySet()) {
            final String refKey = e.getKey();
            final boolean isApproval = refKey.startsWith(APPROVAL_PREFIX) || refKey.startsWith(APPROVAL_PREFIX2);
            final boolean isDetail = refKey.startsWith(DETAIL_PREFIX) || refKey.startsWith(DETAIL_PREFIX2);

            String querySql = baseSql;
            if (isApproval) {
                querySql += " and isWaiting = 'F' and isCanceled = 'F' order by createdOn";
                querySql = String.format(querySql, StringUtils.join(e.getValue(), ","),
                        "createdOn,recordId,state,stepId,approvalId", "RobotApprovalStep", "recordId");

            } else if (isDetail) {
                String sortField = templateExtractor33.getSortField(DETAIL_PREFIX);
                querySql += " order by " + StringUtils.defaultIfBlank(sortField, "createdOn asc");

                Entity de = entity.getDetailEntity();
                querySql = String.format(querySql, StringUtils.join(e.getValue(), ","),
                        de.getPrimaryField().getName(), de.getName(), MetadataHelper.getDetailToMainField(de).getName());

            } else {
                String[] split = refKey.substring(1).split("[.$]");
                Field ref2Field = MetadataHelper.getField(split[1], split[0]);
                Entity ref2Entity = ref2Field.getOwnEntity();

                String sortField = templateExtractor33.getSortField(refKey);
                querySql += " order by " + StringUtils.defaultIfBlank(sortField, "createdOn asc");

                // v4.1 多引用字段
                if (ref2Field.getType() == FieldType.REFERENCE_LIST) {
                    Entity ref2Entity4N = ref2Field.getReferenceEntity();
                    Object[] o = Application.getQueryFactory().uniqueNoFilter(recordId, ref2Field.getName());
                    String where = "1=2";
                    if (o != null && o[0] != null && ((Object[]) o[0]).length > 0) {
                        where = String.format("%s in ('%s')",
                                ref2Entity4N.getPrimaryField().getName(), StringUtils.join((Object[]) o[0], "','"));
                    }
                    querySql = querySql.replace("%s = ?", where);

                    querySql = String.format(querySql, StringUtils.join(e.getValue(), ","),
                            ref2Entity4N.getPrimaryField().getName(), ref2Entity4N.getName());
                } else {
                    String relatedExpr = split[1] + "." + split[0];
                    String where = new ProtocolFilterParser().parseRelated(relatedExpr, recordId);
                    querySql = querySql.replace("%s = ?", where);

                    querySql = String.format(querySql, StringUtils.join(e.getValue(), ","),
                            ref2Entity.getPrimaryField().getName(), ref2Entity.getName());
                }
            }

            log.info("SQL of template : {}", querySql);
            List<Record> list = Application.createQuery(querySql, isApproval ? UserService.SYSTEM_USER : getUser())
                    .setParameter(1, recordId)
                    .list();

            // 审批数据处理
            if (isApproval && !list.isEmpty()) {
                final Record firstNode = list.get(0);

                // v4.0.6 解析步骤名称
                ID approvalId = firstNode.getID("approvalId");
                for (Record c : list) {
                    if (c.hasValue("node")) {
                        String nodeName = ApprovalHelper.getNodeNameById(c.getString("node"), approvalId, true);
                        c.setString("node", nodeName);
                    }
                }

                // 补充提交节点
                Record submit = RecordBuilder.builder(EntityHelper.RobotApprovalStep)
                        .add("approver", ApprovalHelper.getSubmitter(firstNode.getID("recordId")))
                        .add("approvedTime", CalendarUtils.getUTCDateTimeFormat().format(firstNode.getDate("createdOn")))
                        .add("state", 0)
                        .add("node", Language.L("提交"))
                        .build(UserService.SYSTEM_USER);

                List<Record> listReplace = new ArrayList<>();
                listReplace.add(submit);
                listReplace.addAll(list);
                list = listReplace;
            }

            phNumber = 1;
            List<Map<String, Object>> refDatas = new ArrayList<>();
            for (Record c : list) {
                // 特殊处理
                if (isApproval) {
                    int state = c.getInt("state");
                    Date approvedTime = c.getDate("approvedTime");
                    if (approvedTime == null && state > 1) c.setDate("approvedTime", c.getDate("createdOn"));
                }
                refDatas.add(buildData(c, varsMapOfRefs.get(refKey)));
                phNumber++;
            }
            datas.put(refKey, refDatas);
        }

        inShapeVars = templateExtractor33.getInShapeVars();
        return datas;
    }

    /**
     * @return
     */
    protected TemplateExtractor33 buildTemplateExtractor33() {
        return new TemplateExtractor33(templateFile);
    }

    // -- V34 支持多记录导出

    @Override
    public File generate() {
        if (recordIdMulti == null) return superGenerate();

        File targetFile = super.getTargetFile();

        // v4.1-b5
        if (!recordIdMultiMerge2Sheets) {
            ReportsFile reportsFile = new ReportsFile();

            for (ID recordId : this.recordIdMulti) {
                this.recordId = recordId;
                this.phNumber = 1;
                this.phValues.clear();

                String reportName = DataReportManager.getPrettyReportName(reportId, recordId, templateFile.getName());
                try {
                    reportsFile.addFile(superGenerate(), reportName);
                } catch (IOException e) {
                    throw new ReportsException(e);
                }
            }
            return reportsFile;
        }

        // init
        try {
            FileUtils.copyFile(templateFile, targetFile);
        } catch (IOException e) {
            throw new ReportsException(e);
        }

        PrintSetup copyPrintSetup = null;
        for (ID recordId : recordIdMulti) {
            int newSheetAt;
            try (Workbook wb = WorkbookFactory.create(Files.newInputStream(targetFile.toPath()))) {
                // 1.复制模板
                Sheet newSheet = wb.cloneSheet(0);
                newSheetAt = wb.getSheetIndex(newSheet);
                String newSheetName = "" + newSheetAt;
                try {
                    wb.setSheetName(newSheetAt, newSheetName);
                } catch (IllegalArgumentException ignored) {
                    newSheetName = recordId.toLiteral().toUpperCase();
                    wb.setSheetName(newSheetAt, newSheetName);
                }

                // 2.复制打印设置（POI 不会自己复制???）
                // https://www.bilibili.com/read/cv15053559/
                if (copyPrintSetup == null) {
                    copyPrintSetup = wb.getSheetAt(0).getPrintSetup();
                }
                newSheet.getPrintSetup().setLandscape(copyPrintSetup.getLandscape());
                newSheet.getPrintSetup().setPaperSize(copyPrintSetup.getPaperSize());

                // 3.保存模板
                try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                    wb.write(fos);
                }

            } catch (IOException e) {
                throw new ReportsException(e);
            }

            // 生成报表
            this.templateFile = targetFile;
            this.writeSheetAt = newSheetAt;
            this.recordId = recordId;
            this.phNumber = 1;
            this.phValues.clear();

            targetFile = superGenerate();
        }

        // 删除模板 Sheet
        try (Workbook wb = WorkbookFactory.create(Files.newInputStream(targetFile.toPath()))) {
            wb.removeSheetAt(0);
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                wb.write(fos);
            }
        } catch (IOException e) {
            throw new ReportsException(e);
        }

        return targetFile;
    }

    private File superGenerate() {
        File file = super.generate();
        if (inShapeVars.isEmpty() || recordMainHolder == null) return file;

        // v3.6 提取文本框
        try (Workbook wb = WorkbookFactory.create(file)) {
            Sheet sheet = wb.getSheetAt(0);
            for (Object o : sheet.getDrawingPatriarch()) {
                if (!(o instanceof XSSFSimpleShape)) continue;  // 仅文本
                XSSFSimpleShape shape = (XSSFSimpleShape) o;
                String shapeText = shape.getText();
                Matcher matcher = TemplateExtractor33.PATT_V2.matcher(shapeText);
                while (matcher.find()) {
                    String varName = matcher.group(1);
                    if (StringUtils.isNotBlank(varName)) {
                        shapeText = shapeText.replace("{" + varName +"}", String.valueOf(recordMainHolder.get(varName)));
                    }
                }

                // 样式
                XSSFTextRun s = shape.getTextParagraphs().get(0).getTextRuns().get(0);
                XSSFFont font = (XSSFFont) wb.createFont();
                font.setFontName(s.getFontFamily());
                font.setFontHeightInPoints((short) s.getFontSize());
                font.setBold(s.isBold());
                font.setItalic(s.isItalic());
                font.setStrikeout(s.isStrikethrough());
                font.setUnderline(s.isUnderline() ? Font.U_SINGLE : Font.U_NONE);
                // TODO 颜色不生效

                XSSFRichTextString richTextString = new XSSFRichTextString(shapeText);
                richTextString.applyFont(font);
                shape.setText(richTextString);
            }

            // 保存生效
            File file2 = new File(file.getParent(), "2" + file.getName());
            try (FileOutputStream fos = new FileOutputStream(file2)) {
                wb.write(fos);

                FileUtils.deleteQuietly(file);
                return file2;
            }

        } catch (Exception ex) {
            log.error("DEBUG:Cannot fill vars to shape", ex);
        }

        return file;
    }
}
