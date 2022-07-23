/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.support.general.FieldValueHelper;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author ZHAO
 * @since 07/23/2022
 */
public class DataListClass {

    /**
     * @param entity
     * @param user
     * @return
     */
    public static JSON datas(Entity entity, ID user) {
        String classField = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADV_LIST_SHOWCLASS);
        if (StringUtils.isBlank(classField) || !entity.containsField(classField)) return null;

        String sql = MessageFormat.format(
                "select {0} from {1} where {0} is not null group by {0}", classField, entity.getName());
        Object[][] array = Application.getQueryFactory().createQueryNoFilter(sql).array();

        Field fieldMeta = entity.getField(classField);
        EasyField fieldEasy = EasyMetaFactory.valueOf(fieldMeta);
        DisplayType dt = fieldEasy.getDisplayType();

        List<Object[]> list = new ArrayList<>();
        for (Object[] o : array) {
            Object v = o[0];

            if (dt == DisplayType.MULTISELECT) {
            } else if (dt == DisplayType.N2NREFERENCE) {
            } else {
                Object label = FieldValueHelper.wrapFieldValue(v, fieldEasy, true);
                list.add(new Object[] { label, v });
            }
        }

        list.sort(Comparator.comparing(o -> o[0].toString()));
        return (JSON) JSON.toJSON(list);
    }
}
