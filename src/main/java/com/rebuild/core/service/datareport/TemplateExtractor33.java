/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSimpleShape;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * V33
 *
 * @author devezhao
 * @since 2023/4/5
 */
@Slf4j
public class TemplateExtractor33 extends TemplateExtractor {

    // 明细字段
    public static final String DETAIL_PREFIX = NROW_PREFIX + "detail";
    // $
    public static final String NROW_PREFIX2 = "$";
    public static final String DETAIL_PREFIX2 = NROW_PREFIX2 + "detail";
    public static final String APPROVAL_PREFIX2 = NROW_PREFIX2 + "approval";

    // 排序
    private static final String SORT_ASC = ":asc";
    private static final String SORT_DESC = ":desc";
    private Map<String, String> sortFields = new HashMap<>();

    private Set<String> inShapeVars = new HashSet<>();

    /**
     * @param templateFile
     */
    public TemplateExtractor33(File templateFile) {
        super(templateFile, Boolean.FALSE);
    }

    /**
     * 转换模板中的变量
     *
     * @param entity
     * @return
     */
    public Map<String, String> transformVars(Entity entity) {
        final Set<String> vars = extractVars();

        final Entity detailEntity = entity.getDetailEntity();
        final Entity approvalEntity = MetadataHelper.hasApprovalField(entity)
                ? MetadataHelper.getEntity(EntityHelper.RobotApprovalStep) : null;

        Map<String, String> map = new HashMap<>();
        for (final String varName : vars) {
            // 列表型字段
            if (varName.startsWith(NROW_PREFIX) || varName.startsWith(NROW_PREFIX2)) {
                final String listField = varName.substring(1).replace("$", ".");

                // 占位
                if (isPlaceholder(listField)) {
                    map.put(varName, null);
                }
                // 审批流程
                else if (varName.startsWith(APPROVAL_PREFIX) || varName.startsWith(APPROVAL_PREFIX2)) {
                    String stepNodeField = listField.substring(APPROVAL_PREFIX.length());
                    if (approvalEntity != null && MetadataHelper.getLastJoinField(approvalEntity, stepNodeField) != null) {
                        map.put(varName, stepNodeField);
                    } else {
                        map.put(varName, null);
                    }
                }
                // 明细实体
                else if (varName.startsWith(DETAIL_PREFIX) || varName.startsWith(DETAIL_PREFIX2)) {
                    String detailField = listField.substring(DETAIL_PREFIX.length());
                    detailField = getFieldNameWithSort(DETAIL_PREFIX, detailField);

                    if (detailEntity != null && MetadataHelper.getLastJoinField(detailEntity, detailField) != null) {
                        map.put(varName, detailField);
                    } else {
                        map.put(varName, null);
                    }
                }
                // REF
                else {
                    String[] split = listField.split("\\.");
                    String ref2Field = split[0];
                    String ref2Entity = split.length > 1 ? split[1] : null;
                    Field ref2FieldMeta = ref2Entity != null && MetadataHelper.containsField(ref2Entity, ref2Field)
                            ? MetadataHelper.getField(ref2Entity, ref2Field) : null;

                    if (ref2FieldMeta != null && entity.equals(ref2FieldMeta.getReferenceEntity())) {
                        String refName = NROW_PREFIX + ref2Field + "." + ref2Entity;
                        String subField = listField.substring(refName.length());
                        subField = getFieldNameWithSort(refName, subField);

                        if (MetadataHelper.getLastJoinField(MetadataHelper.getEntity(ref2Entity), subField) != null) {
                            map.put(varName, subField);
                        } else {
                            map.put(varName, null);
                        }
                    } else {
                        map.put(varName, null);
                    }
                }

            } else if (MetadataHelper.getLastJoinField(entity, varName) != null) {
                map.put(varName, varName);
            } else {
                map.put(varName, transformRealField(entity, varName));
            }
        }
        return map;
    }

    private String getFieldNameWithSort(String refName, String varFieldName) {
        String hasSort = null;
        if (varFieldName.endsWith(SORT_ASC)) {
            varFieldName = varFieldName.substring(0, varFieldName.length() - 4);
            hasSort = varFieldName + " asc";
        } else if (varFieldName.endsWith(SORT_DESC)) {
            varFieldName = varFieldName.substring(0, varFieldName.length() - 5);
            hasSort = varFieldName + " desc";
        }

        // 有多个字段排序的，其排序顺序取决于字段出现在模板中的位置
        if (hasSort != null) {
            String useSorts = sortFields.get(refName);
            if (useSorts != null) useSorts += "," + hasSort;
            else useSorts = hasSort;
            sortFields.put(refName, useSorts);
        }

        return varFieldName;
    }

    /**
     * 获取排序字段
     *
     * @param refName
     * @return
     */
    protected String getSortField(String refName) {
        return sortFields.get(refName);
    }

    /**
     * 是否占位符
     *
     * @param varName
     * @return
     */
    public static boolean isPlaceholder(String varName) {
        return varName.startsWith(PLACEHOLDER)
                || varName.contains(NROW_PREFIX + PLACEHOLDER)
                || varName.contains(NROW_PREFIX2 + PLACEHOLDER);
    }

    @Override
    protected Set<String> extractVars() {
        Set<String> vars = super.extractVars();

        // v3.6 LAB 提取文本框
        if (templateFile.getName().endsWith(".xlsx")) {
            try (Workbook wb = WorkbookFactory.create(templateFile)) {
                Sheet sheet = wb.getSheetAt(0);
                Drawing<?> drawing = sheet.getDrawingPatriarch();
                if (drawing != null) {
                    for (Object o : sheet.getDrawingPatriarch()) {
                        XSSFSimpleShape shape = (XSSFSimpleShape) o;
                        String shapeText = shape.getText();
                        Matcher matcher = PATT_V2.matcher(shapeText);
                        while (matcher.find()) {
                            String varName = matcher.group(1);
                            if (StringUtils.isNotBlank(varName)) {
                                vars.add(varName);
                                inShapeVars.add(varName);
                            }
                        }
                    }
                }

            } catch (Exception ex) {
                log.error("Cannot extract vars in shape", ex);
            }
        }

        return vars;
    }

    /**
     * @return
     */
    public Set<String> getInShapeVars() {
        return inShapeVars;
    }
}
