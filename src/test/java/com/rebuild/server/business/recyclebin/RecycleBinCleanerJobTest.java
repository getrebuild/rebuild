/*
rebuild - Building your business-systems freely.
Copyright (C) 2020 devezhao <zhaofang123@gmail.com>

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
For more information, please see <https://getrebuild.com>
*/

package com.rebuild.server.business.recyclebin;

import com.rebuild.server.TestSupport;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author devezhao
 * @since 2020/2/23
 */
public class RecycleBinCleanerJobTest extends TestSupport {

    @Test
    public void executeInternal() throws Exception {
        new RecycleBinCleanerJob().executeInternal(null);
    }
}