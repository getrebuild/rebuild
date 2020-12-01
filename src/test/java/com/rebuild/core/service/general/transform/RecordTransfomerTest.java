/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.transform;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.utils.JSONUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2020/10/30
 */
class RecordTransfomerTest extends TestSupport {

    @Test
    void checkFilter() {
        final ID newId = addRecordOfTestAllFields(SIMPLE_USER);

        JSONArray filterItems = new JSONArray();
        JSONObject likeTest = JSONUtils.toJSONObject(
                new String[] { "field", "op", "value" },
                new Object[] { "TestAllFieldsName", "like", "TEXT" });
        filterItems.add(likeTest);
        JSONObject advFilterExp = JSONUtils.toJSONObject("items", filterItems);

        Assertions.assertTrue(new RecordTransfomer(null, advFilterExp).checkFilter(newId));
    }

    @Test
    void transform() {
    }
}