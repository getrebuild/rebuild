/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.recyclebin;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/08/21
 */
public class RecycleRestoreTest extends TestSupport {

    @Test
    public void restore() {
        ID testId = addRecordOfTestAllFields(SIMPLE_USER);
        Application.getGeneralEntityService().delete(testId);

        // Is in?
        Object[] recycle = Application.createQueryNoFilter(
                "select recycleId from RecycleBin where recordId = ?")
                .setParameter(1, testId)
                .unique();
        Assertions.assertNotNull(recycle);

        int a = new RecycleRestore((ID) recycle[0]).restore();
        Assertions.assertEquals(1, a);
    }
}