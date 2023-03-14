/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import cn.devezhao.persist4j.Record;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * @author devezhao
 * @since 02/19/2023
 */
class QueryDecoratorTest extends TestSupport {

    @Test
    void test() {
        Record r = Application.createQueryNoFilter(
                "select guanlibumen,userId from User where userId = '001-9000000000000001'")
                .record();
        System.out.println(Arrays.toString(r.getIDArray("guanlibumen")));
    }
}