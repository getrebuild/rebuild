/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 */
class N2NReferenceSupportTest extends TestSupport {

    @Test
    void getN2NValueByAnyPath() {
        Object[] simple = N2NReferenceSupport.getN2NValueByAnyPath("kehunn", ID.valueOf("985-018316f0c6b60068"));
        System.out.println(Arrays.toString(simple));

        Object[] N2N_F = N2NReferenceSupport.getN2NValueByAnyPath("kehunn.AccountName", ID.valueOf("985-018316f0c6b60068"));
        System.out.println(Arrays.toString(N2N_F));
    }
}