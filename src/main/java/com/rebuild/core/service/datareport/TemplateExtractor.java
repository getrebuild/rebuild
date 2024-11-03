/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import cn.devezhao.commons.excel.Cell;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.utils.ExcelUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板处理
 *
 * @author devezhao
 * @since 2019/8/16
 */
public class TemplateExtractor {

    // 列表（即多条记录）
    public static final String NROW_PREFIX = ".";
    // 审批节点字段
    public static final String APPROVAL_PREFIX = NROW_PREFIX + "approval";

    // 占位
    public static final String PLACEHOLDER = "__";
    // 空
    protected static final String PH__KEEP = PLACEHOLDER + "KEEP";
    // 序号
    protected static final String PH__NUMBER = PLACEHOLDER + "NUMBER";
    // 当前用户
    protected static final String PH__CURRENTUSER = PLACEHOLDER + "CURRENTUSER";
    // 当前部门
    protected static final String PH__CURRENTBIZUNIT = PLACEHOLDER + "CURRENTBIZUNIT";
    // 当前日期
    protected static final String PH__CURRENTDATE = PLACEHOLDER + "CURRENTDATE";
    // 当前日期时间
    protected static final String PH__CURRENTDATETIME = PLACEHOLDER + "CURRENTDATETIME";

    // 变量匹配 v2:{xxx} v1:${xxx}
    protected static final Pattern PATT_V2 = Pattern.compile("\\{(.*?)}");

    final protected File templateFile;
    final private boolean isListType;

    private Set<String> listTypeSortFields = new LinkedHashSet<>();

    /**
     * @param template
     */
    public TemplateExtractor(File template) {
        this(template, Boolean.FALSE);
    }

    /**
     * @param templateFile
     * @param isList 列表模板
     */
    public TemplateExtractor(File templateFile, boolean isList) {
        this.templateFile = templateFile;
        this.isListType = isList;
    }

    /**
     * 转换模板中的变量
     *
     * @param entity
     * @return
     */
    public Map<String, String> transformVars(Entity entity) {
        final Set<String> vars = extractVars();

        Entity detailEntity = this.isListType ? null : entity.getDetailEntity();
        Entity approvalEntity = MetadataHelper.hasApprovalField(entity)
                ? MetadataHelper.getEntity(EntityHelper.RobotApprovalStep) : null;

        Map<String, String> map = new HashMap<>();
        for (final String varName : vars) {
            // v3.8
            String thatName = ValueConvertFunc.splitName(varName);

            // 列表型字段
            if (thatName.startsWith(NROW_PREFIX)) {
                String listField = thatName.substring(1);

                // v3.8.4
                if (isListType) {
                    if (listField.endsWith(TemplateExtractor33.SORT_ASC)) {
                        listTypeSortFields.add(listField);
                        listField = listField.substring(0, listField.length() - 4);
                    } else if (listField.endsWith(TemplateExtractor33.SORT_DESC)) {
                        listTypeSortFields.add(listField);
                        listField = listField.substring(0, listField.length() - 5);
                    }
                }

                // 审批流程
                if (!this.isListType && thatName.startsWith(APPROVAL_PREFIX)) {
                    String stepNodeField = listField.substring(APPROVAL_PREFIX.length());
                    if (approvalEntity != null && MetadataHelper.getLastJoinField(approvalEntity, stepNodeField) != null) {
                        map.put(varName, stepNodeField);
                    } else {
                        map.put(varName, null);
                    }
                }
                // 主+明细实体
                else if (detailEntity != null) {
                    if (MetadataHelper.getLastJoinField(detailEntity, listField) != null) {
                        map.put(varName, listField);
                    } else {
                        map.put(varName, transformRealField(detailEntity, listField));
                    }
                }
                // 普通字段
                else if (MetadataHelper.getLastJoinField(entity, listField) != null) {
                    map.put(varName, listField);
                } else {
                    map.put(varName, transformRealField(entity, listField));
                }

            } else if (MetadataHelper.getLastJoinField(entity, thatName) != null) {
                map.put(varName, thatName);
            } else {
                map.put(varName, transformRealField(entity, thatName));
            }
        }
        return map;
    }

    /**
     * 提取变量
     *
     * @return
     */
    protected Set<String> extractVars() {
        List<Cell[]> rows = ExcelUtils.readExcel(templateFile);

        Set<String> vars = new LinkedHashSet<>();
        for (Cell[] row : rows) {
            for (Cell cell : row) {
                if (cell.isEmpty()) continue;

                // 变量套变量无法支持
                // {.__KEEP:(=IF(ISBLANK({.LimitedCredit}), "", "{.LimitedCredit}天付款"))}

                String cellText = cell.asString();
                Matcher matcher = PATT_V2.matcher(cellText);
                while (matcher.find()) {
                    String varName = matcher.group(1);
                    if (StringUtils.isNotBlank(varName)) vars.add(varName);
                }
            }
        }
        return vars;
    }

    /**
     * 转换模板中的变量字段
     *
     * @param entity
     * @param fieldPath
     * @return
     */
    protected String transformRealField(Entity entity, String fieldPath) {
        if (TemplateExtractor33.isPlaceholder(fieldPath)) return null;

        if (fieldPath.contains("$")) {
            fieldPath = fieldPath.replace("$", ".");
            if (MetadataHelper.getLastJoinField(entity, fieldPath) != null) {
                return fieldPath;
            }
        }

        String[] paths = fieldPath.split("\\.");
        List<String> realPaths = new ArrayList<>();

        Field lastField;
        Entity father = entity;
        for (String field : paths) {
            if (father == null) return null;

            lastField = findFieldByLabel(father, field);
            if (lastField == null) return null;

            realPaths.add(lastField.getName());

            if (lastField.getType() == FieldType.REFERENCE) {
                father = lastField.getReferenceEntity();
            } else {
                father = null;
            }
        }
        return StringUtils.join(realPaths, ".");
    }

    private Field findFieldByLabel(Entity entity, String fieldLabel) {
        for (Field field : entity.getFields()) {
            if (EasyMetaFactory.getLabel(field).equalsIgnoreCase(fieldLabel)) {
                return field;
            }
        }
        return null;
    }

    /**
     * 列表模版指定排序
     *
     * @return
     */
    protected String getListTypeSortFields() {
        return listTypeSortFields.isEmpty() ? null : StringUtils.join(listTypeSortFields, ";");
    }
}
