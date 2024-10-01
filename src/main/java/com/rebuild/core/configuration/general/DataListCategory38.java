/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.DataListCategory.Item;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据列表分组查询
 *
 * @author ZHAO
 * @since 07/27/2024
 */
@Slf4j
public class DataListCategory38 {

    public static final DataListCategory38 instance = new DataListCategory38();

    private DataListCategory38() {}

    /**
     * @param entity
     * @param parentValues
     * @return
     */
    public JSON datas(Entity entity, Object[] parentValues) {
        final String categoryField = getSettings(entity);
        if (categoryField == null) return null;

        // 查第几个字段
        int fieldIndex = parentValues == null ? 0 : parentValues.length;
        // 特殊处理:引用字段父级
        if (fieldIndex > 0 && categoryField.split(";").length == 1) {
            String[] ff = categoryField.split(":");
            Field fieldMeta = entity.getField(ff[0]);
            if (ff.length == 2
                    && entity.containsField(ff[0]) && fieldMeta.getReferenceEntity().containsField(ff[1])) {
                fieldIndex = 0;
            }
        }

        final List<String[]> categoryFields = new ArrayList<>();
        for (String ff : categoryField.split(";")) {
            String[] fieldAndFormat = ff.split(":");
            if (!entity.containsField(fieldAndFormat[0])) {
                throw new MissingMetaExcetion(entity.getName(), fieldAndFormat[0]);
            }
            categoryFields.add(fieldAndFormat);
        }
        if (fieldIndex + 1 > categoryFields.size()) return null;

        // 使用配置
        String[] ff = categoryFields.get(fieldIndex);
        final String useField = ff[0];
        String useFormat = ff.length > 1 ? ff[1] : null;
        final Field useFieldMeta = entity.getField(useField);
        final DisplayType dt = EasyMetaFactory.getDisplayType(useFieldMeta);

        // 单个字段
        final boolean isSingleLevel = categoryFields.size() == 1;
        // 分类,引用（父级）支持树状
        if (!isSingleLevel && useFormat != null) {
            if (dt == DisplayType.CLASSIFICATION || dt == DisplayType.REFERENCE) {
                log.warn("When multiple category fields, the format is disabled : {}", categoryField);
                useFormat = null;
            }
        }

        Collection<Item> dataList;
        // 是否还有下级需要加载
        boolean hasChild = false;
        // 是否排序
        int sortMode = 0;

        // 单字段适用:选项类的
        if ((dt == DisplayType.MULTISELECT || dt == DisplayType.PICKLIST)) {
            dataList = DataListCategory.instance.datasOptions(useFieldMeta, dt);
            hasChild = categoryFields.size() > fieldIndex + 1;
        }
        // 单字段适用:分类字段
        else if (dt == DisplayType.CLASSIFICATION && isSingleLevel) {
            dataList = DataListCategory.instance.datasClassification(useFieldMeta, useFormat);
            // 开放了几级
            int level = ClassificationManager.instance.getOpenLevel(useFieldMeta);
            int levelSpec = useFormat == null ? level : ObjectUtils.toInt(useFormat);
            if (levelSpec < level) level = levelSpec;
            if (level > 0) hasChild = true;
        }
        // 单字段适用:引用字段父级
        else if (dt == DisplayType.REFERENCE && useFormat != null) {
            dataList = datasReference(useFieldMeta, useFormat,
                    parentValues == null ? null : parentValues[parentValues.length - 1]);
            hasChild = true;
            sortMode = 1;
        }
        else {
            sortMode = 1;
            String sql;
            if (dt == DisplayType.N2NREFERENCE) {
                sql = String.format(
                        "select distinct referenceId from NreferenceItem where belongEntity = '%s' and belongField = '%s'",
                        entity.getName(), useField);
                // N级
                if (parentValues != null) {
                    String nestSql = String.format("select %s from %s where %s",
                            entity.getPrimaryField().getName(), entity.getName(),
                            buildParentFilters(entity, categoryFields, parentValues));
                    sql += String.format(" and recordId in ( %s )", nestSql);
                }

            } else {
                String wrapField = useField;
                // 日期格式
                if (dt == DisplayType.DATETIME || dt == DisplayType.DATE) {
                    // DATE 使用字段设置的
                    if (useFormat == null) {
                        useFormat = EasyMetaFactory.valueOf(useFieldMeta).getExtraAttr(EasyFieldConfigProps.DATE_FORMAT);
                    }
                    if ("yyyy".equalsIgnoreCase(useFormat)) wrapField = String.format("DATE_FORMAT(%s, '%%Y')", useField);
                    else if ("yyyy-MM".equalsIgnoreCase(useFormat)) wrapField = String.format("DATE_FORMAT(%s, '%%Y-%%m')", useField);
                    else wrapField = String.format("DATE_FORMAT(%s, '%%Y-%%m-%%d')", useField);

                    sortMode = 2;
                }

                sql = MessageFormat.format(
                        "select distinct {0} from {1} where {2} is not null",
                        wrapField, entity.getName(), useField);

                if (parentValues != null) {
                    sql += " and " + buildParentFilters(entity, categoryFields, parentValues);
                }
            }

            Object[][] array = Application.createQuery(sql).array();

            dataList = new LinkedHashSet<>();
            for (Object[] o : array) {
                Object id = o[0];
                String label;
                if (id instanceof Date) {
                    String dateFormat = StringUtils.defaultIfBlank(useFormat, CalendarUtils.UTC_DATE_FORMAT);
                    label = CalendarUtils.format(dateFormat, (Date) id);
                } else if (id instanceof ID) {
                    label = FieldValueHelper.getLabelNotry((ID) id);
                } else {
                    label = id.toString();
                }

                dataList.add(new Item(id, label));
            }

            hasChild = categoryFields.size() > fieldIndex + 1;
        }

        JSONArray res = new JSONArray();
        for (Item i : dataList) res.add(i.toJSON(0));
        // 排序
        if (sortMode == 2 || sortMode == 1) {
            final boolean isDesc = sortMode == 2;
            res.sort((o1, o2) -> {
                String text1 = ((JSONObject) o1).getString("text");
                String text2 = ((JSONObject) o2).getString("text");
                return isDesc ? text2.compareTo(text1) : text1.compareTo(text2);
            });
        }

        return JSONUtils.toJSONObject(
                new String[]{ "hasChild", "data" },
                new Object[]{ hasChild, res });
    }

    /**
     * @param field
     * @param parentField
     * @param parentValue
     * @return
     */
    protected Collection<Item> datasReference(Field field, String parentField, Object parentValue) {
        Field parentFieldMeta = field.getReferenceEntity().getField(parentField);
        String sql = MessageFormat.format(
                "select {0} from {1} where {2}",
                parentFieldMeta.getOwnEntity().getPrimaryField().getName(),
                parentFieldMeta.getOwnEntity().getName(),
                parentFieldMeta.getName());
        if (parentValue == null) sql += " is null";
        else sql += " = '" + parentValue + "'";

        Object[][] array = Application.createQuery(sql).array();

        Collection<Item> dataList = new LinkedHashSet<>();
        for (Object[] o : array) {
            Object id = o[0];
            String label = FieldValueHelper.getLabelNotry((ID) id);
            dataList.add(new Item(id, label));
        }
        return dataList;
    }

    /**
     * @param categoryFields
     * @param parentValues
     * @return
     */
    protected String buildParentFilters(Entity entity, List<String[]> categoryFields, Object[] parentValues) {
        List<String> and = new ArrayList<>();
        for (int i = 0; i < parentValues.length; i++) {
            String[] ff = categoryFields.get(i);
            String fieldName = ff[0];
            String fieldValue = parentValues[i].toString();
            Field fieldMeta = entity.getField(fieldName);
            DisplayType dt = EasyMetaFactory.getDisplayType(fieldMeta);

            // 一级树:引用
            if (categoryFields.size() == 1 && dt == DisplayType.REFERENCE) {
                fieldValue = parentValues[parentValues.length - 1].toString();
                // v3.8-b4 包括子级
                Set<ID> thisAndChild = new HashSet<>();
                Collection<Item> parent = Collections.singletonList(new Item(ID.valueOf(fieldValue), null));
                while (true) {
                    Collection<Item> parentNew = new HashSet<>();
                    for (Item item : parent) {
                        thisAndChild.add((ID) item.id);
                        parentNew.addAll(datasReference(fieldMeta, ff[1], item.id));
                    }

                    if (parentNew.isEmpty()) break;
                    parent = parentNew;
                }

                return String.format("%s in ('%s')", fieldName, StringUtils.join(thisAndChild, "','"));
            }
            // 一级树:分类
            if (categoryFields.size() == 1 && dt == DisplayType.CLASSIFICATION) {
                fieldValue = parentValues[parentValues.length - 1].toString();
                int level = ClassificationManager.instance.getOpenLevel(fieldMeta);
                // 用 or 提高数据兼容性
                List<String> parentSql = new ArrayList<>();
                parentSql.add(String.format("%s = '%s'", fieldName, fieldValue));
                if (level > 0) parentSql.add(String.format("%s.parent = '%s'", fieldName, fieldValue));
                if (level > 1) parentSql.add(String.format("%s.parent.parent = '%s'", fieldName, fieldValue));
                if (level > 2) parentSql.add(String.format("%s.parent.parent.parent = '%s'", fieldName, fieldValue));
                return "( " + StringUtils.join(parentSql, " or ") + " )";
            }

            if (dt == DisplayType.DATETIME || dt == DisplayType.DATE) {
                String s = fieldValue + "0000-01-01 00:00:00".substring(fieldValue.length());
                String e = fieldValue + "0000-12-31 23:59:59".substring(fieldValue.length());
                if (dt == DisplayType.DATE) {
                    s = s.substring(0, 10);
                    e = e.substring(0, 10);
                }

                String filter = MessageFormat.format("({0} >= ''{1}'' and {0} <= ''{2}'')", fieldName, s, e);
                and.add(filter);
                continue;
            }

            if (dt == DisplayType.MULTISELECT) {
                String filter = String.format("%s && %d", fieldName, ObjectUtils.toInt(fieldValue));
                and.add(filter);
                continue;
            }

            if (dt == DisplayType.N2NREFERENCE) {
                String filter = String.format(
                        "exists (select recordId from NreferenceItem where ^%s = recordId and belongField = '%s' and referenceId = '%s')",
                        entity.getPrimaryField().getName(), fieldMeta.getName(), fieldValue);
                and.add(filter);
                continue;
            }

            String simple = String.format("%s = '%s'", fieldName, fieldValue);
            and.add(simple);
        }
        return "( " + StringUtils.join(and, " and ") + " )";
    }

    /**
     * @param entity
     * @param parentValues
     * @return
     */
    public String buildParentFilters(Entity entity, Object[] parentValues) {
        final String categoryField = getSettings(entity);
        if (categoryField == null) return null;

        List<String[]> categoryFields = new ArrayList<>();
        for (String ff : categoryField.split(";")) {
            categoryFields.add(ff.split(":"));
        }

        return buildParentFilters(entity, categoryFields, parentValues);
    }

    private String getSettings(Entity entity) {
        int listMode = ObjectUtils.toInt(EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADVLIST_MODE), 1);
        String categoryField;
        if (listMode == 3) {
            categoryField = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADVLIST_MODE3_SHOWCATEGORY);
        } else {
            categoryField = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADVLIST_SHOWCATEGORY);
        }
        return StringUtils.isBlank(categoryField) ? null : categoryField;
    }
}
