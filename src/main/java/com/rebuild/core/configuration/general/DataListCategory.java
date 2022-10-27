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
import com.rebuild.core.cache.CacheTemplate;
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

        final String ckey = String.format("DLC1.%s.%s", entity.getName(), categoryField.getName());
        JSON c = (JSON) Application.getCommonsCache().getx(ckey);
        if (c != null) return c;

        DisplayType dt = EasyMetaFactory.getDisplayType(categoryField);

        List<Object[]> list = new ArrayList<>();

        if (dt == DisplayType.MULTISELECT || dt == DisplayType.PICKLIST) {
            ConfigBean[] entries = MultiSelectManager.instance.getPickListRaw(categoryField, true);
            for (ConfigBean e : entries) {
                Object id = e.getID("id");
                if (dt == DisplayType.MULTISELECT) id = e.getLong("mask");

                list.add(new Object[] { e.getString("text"), id });
            }

        } else {
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
                list.add(new Object[] { label, id });
            }

            // TODO 分类数据 code 排序
            list.sort(Comparator.comparing(o -> o[0].toString()));
        }

        JSONArray res = new JSONArray();
        for (Object[] o : list) {
            res.add(JSONUtils.toJSONObject(
                    new String[] { "label", "id", "count" },
                    new Object[] { o[0], o[1], 0 } ));
        }

        // TODO 分类缓存 1min
        Application.getCommonsCache().putx(ckey, res, CacheTemplate.TS_HOUR / 60);  // 1min

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
