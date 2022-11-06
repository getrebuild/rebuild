/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.NoRecordFoundException;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

/**
 * @author devezhao-mac zhaofang123@gmail.com
 * @since 2019/03/16
 */
public class FieldValueHelperTest extends TestSupport {

    @Test
    void testWrapFieldValue() {
        Entity useEntity = MetadataHelper.getEntity(TestAllFields);
        for (Field field : useEntity.getFields()) {
            Object value = RandomStringUtils.randomNumeric(10);
            if (field.getType() == FieldType.REFERENCE) {
                value = ID.newId(field.getReferenceEntity().getEntityCode());
            } if (field.getType() == FieldType.ANY_REFERENCE) {
                value = ID.newId(EntityHelper.User);
            } else if (field.getType() == FieldType.DATE || field.getType() == FieldType.TIMESTAMP) {
                value = CalendarUtils.now();
            } else if (field.getType() == FieldType.TIME) {
                value = LocalTime.now();
            } else if (field.getType() == FieldType.LONG
                    || field.getType() == FieldType.DECIMAL) {
                value = null;
            }

            EasyField easyField = EasyMetaFactory.valueOf(field);
            if (easyField.getDisplayType() == DisplayType.IMAGE
                    || easyField.getDisplayType() == DisplayType.FILE
                    || easyField.getDisplayType() == DisplayType.N2NREFERENCE
                    || easyField.getDisplayType() == DisplayType.BOOL
                    || easyField.getDisplayType() == DisplayType.STATE
                    || easyField.getDisplayType() == DisplayType.MULTISELECT) {
                value = easyField.exprDefaultValue();
            } else if (easyField.getDisplayType() == DisplayType.BARCODE
                    || easyField.getDisplayType() == DisplayType.ID) {
                value = ID.newId(useEntity.getEntityCode());
            }

            System.out.println("Wrap ... " + easyField);
            Object wrappedValue = FieldValueHelper.wrapFieldValue(value, easyField, false);
            System.out.println(field.getName() + " > " + wrappedValue + " > " + easyField.isBuiltin());
            if (wrappedValue != null) {
                System.out.println("    TYPE > " + wrappedValue.getClass().getSimpleName());
                System.out.println("  UNPACK > " + FieldValueHelper.wrapFieldValue(value, easyField, true));
            }
        }
    }

    @Test
    void parseDateExpr() {
        System.out.println(FieldValueHelper.parseDateExpr("{NOW}", null));
        System.out.println(FieldValueHelper.parseDateExpr("{NOW - 1H}", null));
        System.out.println(FieldValueHelper.parseDateExpr("{NOW + 1M}", null));
        System.out.println(FieldValueHelper.parseDateExpr("2019-09-01", null));
        System.out.println(FieldValueHelper.parseDateExpr("2019-09-01 01:01", null));
    }

    @Test
    void testGetLabel() {
        System.out.println(FieldValueHelper.getLabel(SIMPLE_USER));
    }

    @Test
    void testGetLabelThrow() {
        Assertions.assertThrows(NoRecordFoundException.class,
                () -> FieldValueHelper.getLabel(ID.newId(1)));
    }
}
