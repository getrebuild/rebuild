/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tools;

import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.aibot.vector.EntityData;
import org.junit.jupiter.api.Test;

/**
 * @author Zixin
 * @since 2025/5/10
 */
public class VectorDataTest extends TestSupport {

    @Test
    void testRecordUpsert() {
        String c = new EntityData(MetadataHelper.getEntity(TestAllFields)).toVector();
        System.out.println(c);
    }
}
