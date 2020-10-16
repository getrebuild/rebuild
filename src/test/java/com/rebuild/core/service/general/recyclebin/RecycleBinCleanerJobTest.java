/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.recyclebin;

import com.rebuild.TestSupport;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 2020/2/23
 */
public class RecycleBinCleanerJobTest extends TestSupport {

    @Test
    public void executeInternal() {
        new RecycleBinCleanerJob().executeJob();
    }
}