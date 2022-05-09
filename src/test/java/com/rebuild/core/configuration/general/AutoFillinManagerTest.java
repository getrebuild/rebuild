/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/22
 */
public class AutoFillinManagerTest extends TestSupport {

    @Test
    public void testConversionCompatibleValue() {
        Entity test = MetadataHelper.getEntity(TestAllFields);
        Field textField = test.getField("text");

        System.out.println(AutoFillinManager.instance.
                conversionCompatibleValue(test.getField("reference"), textField, ID.newId(test.getEntityCode())));
        System.out.println(AutoFillinManager.instance.
                conversionCompatibleValue(test.getField("classification"), textField, ID.newId(EntityHelper.ClassificationData)));
        System.out.println(AutoFillinManager.instance.
                conversionCompatibleValue(test.getField("picklist"), textField, ID.newId(EntityHelper.PickList)));
        System.out.println(AutoFillinManager.instance.
                conversionCompatibleValue(test.getField("datetime"), textField, CalendarUtils.now()));
        System.out.println(AutoFillinManager.instance.
                conversionCompatibleValue(test.getField("datetime"), test.getField("date1"), CalendarUtils.now()));
    }

    @Test
    public void testGetFillinValue() {
        final String setField = "REFERENCE";

        Record config = EntityHelper.forNew(EntityHelper.AutoFillinConfig, UserService.SYSTEM_USER);
        config.setString("belongEntity", TestAllFields);
        config.setString("belongField", setField);
        config.setString("sourceField", "TEXT");
        config.setString("targetField", "TEXT");
        config.setString("extConfig", "{'whenCreate':true,'whenUpdate':true,'fillinForce':true}");
        config = Application.getCommonsService().create(config, false);

        try {
            Entity test = MetadataHelper.getEntity(TestAllFields);
            ID recordId = addRecordOfTestAllFields(SIMPLE_USER);

            JSONArray fills = AutoFillinManager.instance.getFillinValue(test.getField(setField), recordId);
            System.out.println("Fills : " + fills);
        } finally {
            Application.getCommonsService().delete(config.getPrimary(), false);
        }

    }
}
