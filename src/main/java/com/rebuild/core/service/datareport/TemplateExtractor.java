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
import java.util.*;
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
    protected static final String APPROVAL_PREFIX = NROW_PREFIX + "approval";

    // 占位
    public static final String PLACEHOLDER = "__";
    // 空
    protected static final String PH__EMPTY = PLACEHOLDER + "EMPTY";
    // 当前用户
    protected static final String PH__CURRENTUSER = PLACEHOLDER + "CURRENTUSER";
    // 序号
    protected static final String PH__NUMBER = PLACEHOLDER + "NUMBER";
    
    // ${xxx}
    private static final Pattern PATT_V1 = Pattern.compile("\\$\\{(.*?)}");
    // {xxx}
    private static final Pattern PATT_V2 = Pattern.compile("\\{(.*?)}");

    private File template;
    // Use easyexcel
    private boolean isV2;

    private boolean useDetails;

    /**
     * @param template
     * @param isV2
     */
    public TemplateExtractor(File template, boolean isV2) {
        this(template, isV2, true);
    }

    /**
     * @param template
     * @param isV2
     * @param useDetails
     */
    public TemplateExtractor(File template, boolean isV2, boolean useDetails) {
        this.template = template;
        this.isV2 = isV2;
        this.useDetails = useDetails;
    }

    /**
     * 转换模板中的变量
     *
     * @param entity
     * @return
     */
    public Map<String, String> transformVars(Entity entity) {
        final Set<String> vars = extractVars();

        Entity detailEntity = this.useDetails ? entity.getDetailEntity() : null;
        Entity approvalEntity = MetadataHelper.hasApprovalField(entity)
                ? MetadataHelper.getEntity(EntityHelper.RobotApprovalStep) : null;

        Map<String, String> map = new HashMap<>();
        for (final String varName : vars) {
            // 列表型字段
            if (varName.startsWith(NROW_PREFIX)) {
                String listField = varName.substring(1);

                // 审批
                if (varName.startsWith(APPROVAL_PREFIX)) {
                    String stepNodeField = listField.substring(APPROVAL_PREFIX.length());
                    if (approvalEntity != null && MetadataHelper.getLastJoinField(approvalEntity, stepNodeField) != null) {
                        map.put(varName, stepNodeField);
                    } else {
                        map.put(varName, null);
                    }
                } else if (detailEntity != null) {
                    if (MetadataHelper.getLastJoinField(detailEntity, listField) != null) {
                        map.put(varName, listField);
                    } else {
                        map.put(varName, transformRealField(detailEntity, listField));
                    }
                } else if (MetadataHelper.getLastJoinField(entity, listField) != null) {
                    map.put(varName, listField);
                } else {
                    map.put(varName, transformRealField(entity, varName));
                }

            } else if (MetadataHelper.getLastJoinField(entity, varName) != null) {
                map.put(varName, varName);
            } else {
                map.put(varName, transformRealField(entity, varName));
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
        List<Cell[]> rows = ExcelUtils.readExcel(this.template);

        Set<String> vars = new HashSet<>();
        for (Cell[] row : rows) {
            for (Cell cell : row) {
                if (cell.isEmpty()) {
                    continue;
                }

                String cellText = cell.asString();
                Matcher matcher = (this.isV2 ? PATT_V2 : PATT_V1).matcher(cellText);
                while (matcher.find()) {
                    String varName = matcher.group(1);
                    vars.add(varName);
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
        if (fieldPath.startsWith(PLACEHOLDER)) return null;

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
}
