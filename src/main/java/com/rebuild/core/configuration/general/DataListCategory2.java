/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.DataListCategory.Item;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 数据列表分组查询
 *
 * @author ZHAO
 * @since 07/27/2024
 */
@Slf4j
public class DataListCategory2 {

    public static final DataListCategory2 instance = new DataListCategory2();

    private DataListCategory2() {}

    /**
     * @param entity
     * @param parentValues
     * @return
     */
    public JSON datas(Entity entity, Object[] parentValues) {
        String categoryField = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADVLIST_SHOWCATEGORY);
        if (StringUtils.isBlank(categoryField)) return null;

        // 查第几个字段
        int fieldIndex = parentValues == null ? 0 : parentValues.length;

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

        // 分类,引用(父级) 支持树状，但设置了多个分组字段后则不支持
        if (categoryFields.size() > 1 && ff.length > 1 && ff[1] != null) {
            if (dt == DisplayType.CLASSIFICATION || dt == DisplayType.REFERENCE) {
                log.warn("When multiple category fields, the format is disabled : {}", categoryField);
                useFormat = null;
            }
        }

        // 是否还有下级需要加载
        boolean hasChild = false;

        Collection<Item> dataList;

        // 选项类的使用全部
        if (dt == DisplayType.MULTISELECT || dt == DisplayType.PICKLIST) {
            dataList = DataListCategory.instance.datasOptions(useFieldMeta, dt);
        }
        // 分类字段设置了字段格式
        else if (dt == DisplayType.CLASSIFICATION && useFormat != null) {
            dataList = DataListCategory.instance.datasClassification(useFieldMeta, useFormat);
        }
        // 引用字段设置了父级
        else if (dt == DisplayType.REFERENCE
                && useFormat != null && useFieldMeta.getReferenceEntity().containsField(useFormat)) {
            dataList = DataListCategory.instance.datasReference(useFieldMeta, useFormat);
        }
        // 其他情况
        else {

            String sql;
            if (dt == DisplayType.N2NREFERENCE) {
                sql = String.format(
                        "select distinct referenceId from NreferenceItem where belongEntity = '%s' and belongField = '%s'",
                        entity.getName(), useField);
            } else {
                String wrapField = useField;
                if (dt == DisplayType.DATETIME || dt == DisplayType.DATE) {
                    if ("yyyy".equals(useFormat)) wrapField = String.format("DATE_FORMAT(%s, '%%Y')", useField);
                    if ("yyyy-MM".equals(useFormat)) wrapField = String.format("DATE_FORMAT(%s, '%%Y-%%m')", useField);
                    if (dt == DisplayType.DATETIME) {
                        if ("yyyy-MM-dd".equals(useFormat)) wrapField = String.format("DATE_FORMAT(%s, '%%Y-%%m-%%d')", useField);
                    }
                }

                sql = MessageFormat.format(
                        "select {0} from {1} where {2} is not null group by {0}",
                        wrapField, entity.getName(), useField);
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

        return JSONUtils.toJSONObject(
                new String[]{ "hasChild", "data" },
                new Object[]{ hasChild, res });
    }

}
