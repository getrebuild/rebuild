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
 * 分类数据
 *
 * @author ZHAO
 * @since 07/23/2022
 */
public class DataListClass {

    /**
     *
     *
     * @param entity
     * @param user
     * @return
     */
    public static JSON datas(Entity entity, ID user) {
        final Field classField = getFieldOfClass(entity);
        if (classField == null) return null;

        final String ckey = String.format("DLC1.%s.%s", entity.getName(), classField.getName());
        JSON c = (JSON) Application.getCommonsCache().getx(ckey);
        if (c != null) return c;

        DisplayType dt = EasyMetaFactory.getDisplayType(classField);

        List<Object[]> list = new ArrayList<>();

        if (dt == DisplayType.MULTISELECT || dt == DisplayType.PICKLIST) {
            ConfigBean[] entries = MultiSelectManager.instance.getPickListRaw(classField, true);
            for (ConfigBean e : entries) {
                Object id = e.getID("id");
                if (dt == DisplayType.MULTISELECT) id = e.getLong("mask");

                list.add(new Object[] { e.getString("text"), id });
            }

        } else {
            String sql;
            if (dt == DisplayType.N2NREFERENCE) {
                sql = MessageFormat.format(
                        "select referenceId from NreferenceItem where belongEntity = ''{0}'' and belongField = ''{1}'' group by referenceId",
                        entity.getName(), classField.getName());
            } else {
                sql = MessageFormat.format(
                        "select {0} from {1} where {0} is not null group by {0}", classField.getName(), entity.getName());
            }
            
            Query query = user == null
                        ? Application.createQueryNoFilter(sql) : Application.getQueryFactory().createQuery(sql, user);
            Object[][] array = query.array();

            for (Object[] o : array) {
                Object id = o[0];
                Object label = FieldValueHelper.getLabelNotry((ID) id);
                list.add(new Object[] { label, id });
            }

            list.sort(Comparator.comparing(o -> o[0].toString()));
        }

        JSONArray res = new JSONArray();
        for (Object[] o : list) {
            res.add(JSONUtils.toJSONObject(
                    new String[] { "label", "id", "count" },
                    new Object[] { o[0], o[1], 0 } ));
        }

        // FIXME 1min 缓存
        Application.getCommonsCache().putx(ckey, res, CacheTemplate.TS_HOUR / 60);  // 1min

        return res;
    }

    /**
     * 获取分类字段（若有）
     *
     * @param entity
     * @return
     */
    public static Field getFieldOfClass(Entity entity) {
        String classField = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADV_LIST_SHOWCLASS);
        if (StringUtils.isBlank(classField) || !entity.containsField(classField)) return null;
        return entity.getField(classField);
    }
}
