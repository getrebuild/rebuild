/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSON;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 2019/04/28
 */
public class MetaschemaExporterTest extends TestSupport {

    @Test
    void testExport() {
        if (MetadataHelper.containsEntity(Account)) {
            Entity test = MetadataHelper.getEntity(Account);
            MetaschemaExporter generator = new MetaschemaExporter(test, true);
            JSON schema = generator.export();
            System.out.println(JSON.toJSONString(schema, true));
        }
    }

    @Test
    void testExportHaveDetail() {
        if (MetadataHelper.containsEntity(SalesOrder)) {
            Entity test = MetadataHelper.getEntity(SalesOrder);
            MetaschemaExporter generator = new MetaschemaExporter(test, true);
            JSON schema = generator.export();
            System.out.println(JSON.toJSONString(schema, true));
        }
    }
}
