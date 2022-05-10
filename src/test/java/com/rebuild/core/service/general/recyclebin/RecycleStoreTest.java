/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.recyclebin;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.TestSupport;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/08/21
 */
public class RecycleStoreTest extends TestSupport {

    @Test
    public void serialize() {
        ID test = addRecordOfTestAllFields(SIMPLE_USER);
        JSON s = new RecycleBean(test).serialize();
        System.out.println(s);
    }

    @Test
    public void store() {
        ID test1 = addRecordOfTestAllFields(SIMPLE_USER);
        ID test2 = addRecordOfTestAllFields(SIMPLE_USER);

        RecycleStore recycleStore = new RecycleStore(SIMPLE_USER);
        recycleStore.add(test1);
        recycleStore.add(test2);
        recycleStore.removeLast();

        recycleStore.add(test2, test1);

        int s = recycleStore.store();
        System.out.println(s);
    }
}