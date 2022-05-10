/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSON;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.jupiter.api.Test;

class RecordDifferenceTest extends TestSupport {

    @Test
    void diffMerge() {
        Entity entity = MetadataHelper.getEntity(TestAllFields);

        Record record1 = EntityHelper.forNew(entity.getEntityCode(), UserService.ADMIN_USER);
        record1.setString("text", "TEXT-" + RandomUtils.nextLong());
        record1.setDate("date1", CalendarUtils.now());

        Record record2 = EntityHelper.forNew(entity.getEntityCode(), UserService.ADMIN_USER);
        record2.setString("text", "TEXT-" + RandomUtils.nextLong());
        record2.setString("ntext", "NTEXT-" + RandomUtils.nextLong());

        JSON diff = new RecordDifference(record1).diffMerge(record2);
        System.out.println(JSONUtils.prettyPrint(diff));
    }

    @Test
    void isSame() {
        Entity entity = MetadataHelper.getEntity(TestAllFields);

        Record record1 = EntityHelper.forNew(entity.getEntityCode(), UserService.ADMIN_USER);
        record1.setString("text", "TEXT1");

        Record record2 = EntityHelper.forNew(entity.getEntityCode(), UserService.ADMIN_USER);
        record2.setString("text", "TEXT1");

        boolean same = new RecordDifference(record1).isSame(record2, false);
        System.out.println(same);
    }
}