/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.feeds;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.metadata.EntityHelper;
import org.junit.Test;

/**
 * @author ZHAO
 * @since 2019/11/28
 */
public class FeedsConstantTest {

    @Test
    public void testFeedsScope() {
        System.out.println(FeedsScope.parse("SELF"));
        System.out.println(FeedsScope.parse(ID.newId(EntityHelper.Team).toLiteral()));
    }

    @Test
    public void testFeedsType() {
        System.out.println(FeedsType.parse(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFeedsType2() {
        System.out.println(FeedsType.parse(111));
    }
}
