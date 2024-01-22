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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * V33
 *
 * @author devezhao
 * @since 2023/4/5
 */
public class TemplateExtractor33 extends TemplateExtractor {

    // 明细字段
    protected static final String DETAIL_PREFIX = NROW_PREFIX + "detail";

    // 排序
    private static final String SORT_ASC = ":asc";
    private static final String SORT_DESC = ":desc";
    private Map<String, String> sortFields = new HashMap<>();

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
            if (varName.startsWith(NROW_PREFIX)) {
                final String listField = varName.substring(1).replace("$", ".");

                if (isPlaceholder(listField)) {
                    map.put(varName, null);
                }
                // 审批流程
                else if (varName.startsWith(APPROVAL_PREFIX)) {
                    String stepNodeField = listField.substring(APPROVAL_PREFIX.length());
                    if (approvalEntity != null && MetadataHelper.getLastJoinField(approvalEntity, stepNodeField) != null) {
                        map.put(varName, stepNodeField);
                    } else {
                        map.put(varName, null);
                    }
                }
                // 明细实体
                else if (varName.startsWith(DETAIL_PREFIX)) {
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
        return varName.startsWith(PLACEHOLDER) || varName.contains(NROW_PREFIX + PLACEHOLDER);
    }
}
