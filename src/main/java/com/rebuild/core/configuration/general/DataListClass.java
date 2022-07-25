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
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.*;

/**
 * 分类数据
 *
 * @author ZHAO
 * @since 07/23/2022
 */
public class DataListClass {

    /**
     * FIXME 不要实时查询
     *
     * @param entity
     * @param user
     * @return
     */
    public static JSON datas(Entity entity, ID user) {
        String classField = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADV_LIST_SHOWCLASS);
        if (StringUtils.isBlank(classField) || !entity.containsField(classField)) return null;

        Field fieldMeta = entity.getField(classField);
        EasyField fieldEasy = EasyMetaFactory.valueOf(fieldMeta);
        DisplayType dt = fieldEasy.getDisplayType();

        List<Object[]> list = new ArrayList<>();

        if (dt == DisplayType.MULTISELECT || dt == DisplayType.PICKLIST) {
            ConfigBean[] entries = MultiSelectManager.instance.getPickListRaw(fieldMeta, true);
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
                        entity.getName(), classField);
            } else {
                sql = MessageFormat.format(
                        "select {0} from {1} where {0} is not null group by {0}", classField, entity.getName());
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
        return res;
    }
}
