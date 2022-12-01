/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.*;

/**
 * 列表字段分类（分组）数据
 *
 * @author ZHAO
 * @since 07/23/2022
 */
public class DataListCategory {

    public static final DataListCategory instance = new DataListCategory();

    private DataListCategory() {
    }

    /**
     * @param entity
     * @param user
     * @return
     */
    public JSON datas(Entity entity, ID user) {
        final Field categoryField = getFieldOfCategory(entity);
        if (categoryField == null) return null;

        EasyField easyField = EasyMetaFactory.valueOf(categoryField);
        DisplayType dt = easyField.getDisplayType();

        List<Object[]> dataList = new ArrayList<>();

        if (dt == DisplayType.MULTISELECT || dt == DisplayType.PICKLIST) {
            ConfigBean[] entries = MultiSelectManager.instance.getPickListRaw(categoryField, true);
            for (ConfigBean e : entries) {
                Object id = e.getID("id");
                if (dt == DisplayType.MULTISELECT) id = e.getLong("mask");
                dataList.add(new Object[] { e.getString("text"), id });
            }

        } else {

            String cf = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADV_LIST_SHOWCATEGORY);
            String[] ff = cf.split(":");
            String ffField = ff[0];
            String ffFormat = ff.length > 1 ? ff[1] : null;

            String sql;
            if (dt == DisplayType.N2NREFERENCE) {
                sql = MessageFormat.format(
                        "select distinct referenceId from NreferenceItem where belongEntity = ''{0}'' and belongField = ''{1}''",
                        entity.getName(), ffField);
            } else {
                String wrapField = ffField;
                if (dt == DisplayType.DATETIME) {
                    wrapField = String.format("DATE_FORMAT(%s, '%%Y-%%m-%%d')", wrapField);
                }

                sql = MessageFormat.format(
                        "select {0} from {1} where {2} is not null group by {0}",
                        wrapField, entity.getName(), categoryField.getName());
            }

            Query query = user == null
                    ? Application.createQueryNoFilter(sql)
                    : Application.getQueryFactory().createQuery(sql, user);
            Object[][] array = query.array();

            Set<Object> unique = new HashSet<>();

            for (Object[] o : array) {
                Object id = o[0];
                Object label;

                if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
                    ffFormat = StringUtils.defaultIfBlank(ffFormat, CalendarUtils.UTC_DATE_FORMAT);
                    if (id instanceof Date) {
                        label = CalendarUtils.format(ffFormat, (Date) id);
                    } else {
                        label = id.toString().substring(0, ffFormat.length());
                    }
                    id = label;

                } else {
                    label = FieldValueHelper.getLabelNotry((ID) id);
                }

                if (unique.contains(id)) continue;
                unique.add(id);

                dataList.add(new Object[] { label, id });
            }
        }

        JSONArray res = new JSONArray();
        for (Object[] o : dataList) {
            res.add(JSONUtils.toJSONObject(
                    new String[] { "label", "id", "count" },
                    new Object[] { o[0], o[1], 0 } ));
        }

        return res;
    }

    /**
     * 获取分类字段（若有）
     *
     * @param entity
     * @return
     */
    public Field getFieldOfCategory(Entity entity) {
        String categoryField = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADV_LIST_SHOWCATEGORY);
        if (categoryField != null) categoryField = categoryField.split(":")[0];
        if (categoryField != null && entity.containsField(categoryField)) return entity.getField(categoryField);
        return null;
    }
}
