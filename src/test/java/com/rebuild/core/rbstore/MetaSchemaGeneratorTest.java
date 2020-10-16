/*
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
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/28
 */
public class MetaSchemaGeneratorTest extends TestSupport {

    @Test
    public void testGenerate() {
        if (MetadataHelper.containsEntity(Account)) {
            Entity test = MetadataHelper.getEntity(Account);
            MetaSchemaGenerator generator = new MetaSchemaGenerator(test);
            JSON schema = generator.generate();
            System.out.println(JSON.toJSONString(schema, true));
        }
    }

    @Test
    public void testGenerateHaveDetail() {
        if (MetadataHelper.containsEntity(SalesOrder)) {
            Entity test = MetadataHelper.getEntity(SalesOrder);
            MetaSchemaGenerator generator = new MetaSchemaGenerator(test);
            JSON schema = generator.generate();
            System.out.println(JSON.toJSONString(schema, true));
        }
    }
}
