/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import com.rebuild.TestSupport;
import com.rebuild.core.service.aibot.vector.EntitiesData;
import com.rebuild.core.service.aibot.vector.FileData;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author Zixin
 * @since 2025/5/10
 */
public class VectorDataTest extends TestSupport {

    @Test
    void testEntitiesData() {
        String c = new EntitiesData().toVector();
        System.out.println(c);
    }

    @Test
    void testFileData() throws FileNotFoundException {
        File file = ResourceUtils.getFile("classpath:classification-demo.xlsx");
        String c = new FileData(file.getAbsolutePath()).toVector();
        System.out.println(c);
    }
}
