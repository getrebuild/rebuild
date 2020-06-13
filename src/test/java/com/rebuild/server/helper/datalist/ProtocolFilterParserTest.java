/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper.datalist;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.EntityHelper;
import org.junit.Test;

/**
 * @author devezhao
 * @since 2020/6/13
 */
public class ProtocolFilterParserTest extends TestSupport {

    @Test
    public void parseVia() {
        System.out.println(new ProtocolFilterParser(null).parseVia(ID.newId(EntityHelper.ChartConfig).toLiteral()));
    }

    @Test
    public void parseRef() {
        System.out.println(new ProtocolFilterParser(null).parseRef("REFERENCE.TESTALLFIELDS"));
    }
}