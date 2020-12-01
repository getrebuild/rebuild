/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import junit.framework.TestCase;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * @author ZHAO
 * @since 2020/11/12
 */
class BusinessModelImporterTest extends TestCase {

    @Test
    void findRefs() {
        Map<String, String> map = new BusinessModelImporter().findRefs("Quotation");
        System.out.println(map);
    }
}