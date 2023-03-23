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
    void getN2NValueByMixPath() {
        Object[] F_N2N = N2NReferenceSupport.getN2NValueByMixPath("zuijindingdan.kehunn", ID.valueOf("982-01870e95356e0036"));
        System.out.println(Arrays.toString(F_N2N));

        Object[] F_N2N_F = N2NReferenceSupport.getN2NValueByMixPath("zuijindingdan.kehunn.AccountName", ID.valueOf("982-01870e95356e0036"));
        System.out.println(Arrays.toString(F_N2N_F));

        Object[] simple = N2NReferenceSupport.getN2NValueByMixPath("kehunn", ID.valueOf("985-018316f0c6b60068"));
        System.out.println(Arrays.toString(simple));

        Object[] N2N_F = N2NReferenceSupport.getN2NValueByMixPath("kehunn.AccountName", ID.valueOf("985-018316f0c6b60068"));
        System.out.println(Arrays.toString(N2N_F));
    }
}