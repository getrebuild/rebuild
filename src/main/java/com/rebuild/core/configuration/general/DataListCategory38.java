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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.DataListCategory.Item;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
        final JSONArray categoryFields = getSettings(entity);
        if (CollectionUtils.isEmpty(categoryFields)) return null;

        // 查第几个字段
        int fieldIndex = parentValues == null ? 0 : parentValues.length;
        // 特殊处理:引用字段父级
        if (fieldIndex > 0 && categoryFields.size() == 1) {
            JSONObject ff = (JSONObject) categoryFields.get(0);
            String field = ff.getString("field");
            String format = ff.getString("format");
            Field fieldMeta = entity.getField(field);
            if (format != null && fieldMeta.getReferenceEntity().containsField(format)) {
                fieldIndex = 0;
            }
        }
        if (fieldIndex + 1 > categoryFields.size()) return null;

        // 使用配置
        final JSONObject ff = (JSONObject) categoryFields.get(fieldIndex);
        final String useField = ff.getString("field");
        String useFormat = ff.getString("format");
        final Field useFieldMeta = entity.getField(useField);
        final DisplayType dt = EasyMetaFactory.getDisplayType(useFieldMeta);

        // 单个字段
        final boolean isSingleLevel = categoryFields.size() == 1;
        // 分类,引用（父级）支持树状
        if (!isSingleLevel && useFormat != null) {
            if (dt == DisplayType.CLASSIFICATION || dt == DisplayType.REFERENCE) {
                log.warn("When multiple category fields, the format is disabled : {}", categoryFields);
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
                    parentValues == null ? null : parentValues[parentValues.length - 1], ff.getJSONObject("filter"));
            hasChild = true;
            sortMode = 1;
            if (ff.getIntValue("sort") > 0) sortMode = ff.getIntValue("sort");
            if (ff.getIntValue("sort") == -1) sortMode = 0;  // 无需排序
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
                if (ParseHelper.validAdvFilter(ff.getJSONObject("filter"))) {
                    String where = new AdvFilterParser(ff.getJSONObject("filter")).toSqlWhere();
                    if (where != null) sql += " and " + where;
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
            if (ff.getIntValue("sort") > 0) sortMode = ff.getIntValue("sort");
        }

        JSONArray res = new JSONArray();
        for (Item i : dataList) res.add(i.toJSON(0));
        // 排序
        if (sortMode == 2 || sortMode == 1) {
            final boolean isDesc = sortMode == 2;
            final Collator zhc = Collator.getInstance(Locale.CHINESE);
            res.sort((o1, o2) -> {
                String text1 = ((JSONObject) o1).getString("text");
                String text2 = ((JSONObject) o2).getString("text");
                return isDesc ? zhc.compare(text2, text1) : zhc.compare(text1, text2);
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
     * @param appendFilter
     * @return
     */
    protected Collection<Item> datasReference(Field field, String parentField, Object parentValue, JSONObject appendFilter) {
        Field parentFieldMeta = field.getReferenceEntity().getField(parentField);
        String sql = MessageFormat.format(
                "select {0} from {1} where {2}",
                parentFieldMeta.getOwnEntity().getPrimaryField().getName(),
                parentFieldMeta.getOwnEntity().getName(),
                parentFieldMeta.getName());
        if (parentValue == null) sql += " is null";
        else sql += " = '" + parentValue + "'";

        // FIXME 应该使用父级字段的时候作为查询条件
        if (ParseHelper.validAdvFilter(appendFilter)) {
            String where = new AdvFilterParser(appendFilter).toSqlWhere();
            if (where != null) {
                sql += String.format(" and %s in (select %s from %s where %s)",
                        parentFieldMeta.getOwnEntity().getPrimaryField().getName(),
                        field.getName(), field.getOwnEntity().getName(), where);
            }
        }

        // be:4.2.3 用户部门树
        if ("deptId".equals(field.getName()) && "parentDept".equals(parentField)) {
            sql += " order by seq asc, name asc";
        }

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
     * @param entity
     * @param categoryFields
     * @param parentValues
     * @return
     */
    protected String buildParentFilters(Entity entity, JSONArray categoryFields, Object[] parentValues) {
        List<String> and = new ArrayList<>();
        for (int i = 0; i < parentValues.length; i++) {
            JSONObject ff = (JSONObject) categoryFields.get(i);
            String fieldName = ff.getString("field");
            String fieldValue = parentValues[i].toString();
            Field fieldMeta = entity.getField(fieldName);
            DisplayType dt = EasyMetaFactory.getDisplayType(fieldMeta);

            // 一级树:引用
            if (categoryFields.size() == 1 && dt == DisplayType.REFERENCE) {
                fieldValue = parentValues[parentValues.length - 1].toString();
                // v3.8-b4 包括子级
                Set<ID> thisAndChild = new HashSet<>();
                Collection<Item> parent = Collections.singletonList(new Item(ID.valueOf(fieldValue), null));
                boolean hasParentField = ff.getString("format") != null;
                while (true) {
                    Collection<Item> parentNew = new HashSet<>();
                    for (Item item : parent) {
                        thisAndChild.add((ID) item.id);
                        if (hasParentField) {
                            parentNew.addAll(datasReference(fieldMeta, ff.getString("format"), item.id, ff.getJSONObject("filter")));
                        }
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
                // fix:月的做大日
                int maxDayOfMonth = CalendarUtils.getInstance(CalendarUtils.parse(s)).getActualMaximum(Calendar.DAY_OF_MONTH);
                if (maxDayOfMonth != 31) e = e.replace("-31", "-" + maxDayOfMonth);

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
        final JSONArray categoryFields = getSettings(entity);
        if (categoryFields == null || categoryFields.isEmpty()) return null;
        return buildParentFilters(entity, categoryFields, parentValues);
    }

    private JSONArray getSettings(Entity entity) {
        // be:4.2.3 用户部门树
        if ("User".equals(entity.getName())) {
            return JSON.parseArray("[{field:'deptId', format:'parentDept', sort:-1}]");
        }

        int listMode = ObjectUtils.toInt(EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADVLIST_MODE), 1);
        String categoryField;
        if (listMode == 3) {
            categoryField = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADVLIST_MODE3_SHOWCATEGORY);
        } else {
            categoryField = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADVLIST_SHOWCATEGORY);
        }
        if (StringUtils.isBlank(categoryField)) return null;

        if (JSONUtils.wellFormat(categoryField)) return JSON.parseArray(categoryField);

        // comp:v3.9
        JSONArray items = new JSONArray();
        for (String ff : categoryField.split(";")) {
            String[] ff2 = ff.split(":");
            JSONObject item = JSONUtils.toJSONObject("field", ff2[0]);
            if (ff2.length > 1) item.put("format", ff2[1]);
            items.add(item);
        }
        return items;
    }
}
