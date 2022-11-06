/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 列表字段分类数据
 *
 * @author ZHAO
 * @since 07/23/2022
 */
public class DataListCategory {

    /**
     * @param entity
     * @param user
     * @return
     */
    public static JSON datas(Entity entity, ID user) {
        final Field categoryField = getFieldOfCategory(entity);
        if (categoryField == null) return null;

        DisplayType dt = EasyMetaFactory.getDisplayType(categoryField);

        List<Object[]> clist = new ArrayList<>();

        if (dt == DisplayType.MULTISELECT || dt == DisplayType.PICKLIST) {
            ConfigBean[] entries = MultiSelectManager.instance.getPickListRaw(categoryField, true);
            for (ConfigBean e : entries) {
                Object id = e.getID("id");
                if (dt == DisplayType.MULTISELECT) id = e.getLong("mask");

                clist.add(new Object[] { e.getString("text"), id });
            }

        } else {

            // TODO 考虑支持更多分组字段类型，例如日期（但要考虑日期格式）

            String sql;
            if (dt == DisplayType.N2NREFERENCE) {
                sql = MessageFormat.format(
                        "select distinct referenceId from NreferenceItem where belongEntity = ''{0}'' and belongField = ''{1}''",
                        entity.getName(), categoryField.getName());
            } else {
                sql = MessageFormat.format(
                        "select distinct {0} from {1} where {0} is not null", categoryField.getName(), entity.getName());
            }

            Query query = user == null
                    ? Application.createQueryNoFilter(sql)
                    : Application.getQueryFactory().createQuery(sql, user);
            Object[][] array = query.array();

            for (Object[] o : array) {
                Object id = o[0];
                Object label = FieldValueHelper.getLabelNotry((ID) id);
                clist.add(new Object[] { label, id });
            }

            // TODO 分类数据 code 排序
            clist.sort(Comparator.comparing(o -> o[0].toString()));
        }

        JSONArray res = new JSONArray();
        for (Object[] o : clist) {
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
    public static Field getFieldOfCategory(Entity entity) {
        String categoryField = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADV_LIST_SHOWCATEGORY);
        if (StringUtils.isBlank(categoryField) || !entity.containsField(categoryField)) return null;
        return entity.getField(categoryField);
    }
}
