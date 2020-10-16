/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.rbstore;

import com.alibaba.fastjson.JSONArray;
import com.rebuild.TestSupport;
import com.rebuild.core.rbstore.RBStore;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * @author devezhao
 * @since 2020/10/05
 */
public class MetaschemaControllerTest extends TestSupport {

    @Test
    public void findRefs() {
        final JSONArray index = (JSONArray) RBStore.fetchMetaschema("index-2.0.json");

        Set<String> into = new HashSet<>();
        new MetaschemaController().findRefs(index, "Quotation", into);
        System.out.println(into);
    }
}