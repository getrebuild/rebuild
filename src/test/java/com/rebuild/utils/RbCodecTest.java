/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserService;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 2020/2/4
 */
public class RbCodecTest extends TestSupport {

    @Test
    public void testRbRecordCodec() throws IOException {
        Record record = EntityHelper.forNew(EntityHelper.User, UserService.SYSTEM_USER);
        RbRecordCodec.instance.write(new JSONSerializer(), record, null, null, 0);

        Map<String, Object> map = new HashMap<>();
        map.put("fieldName2", record);
        System.out.println(JSON.toJSONString(map));
    }

    @Test
    public void testRbDateCodec() throws IOException {
        RbDateCodec.instance.write(new JSONSerializer(), CalendarUtils.now(), null, null, 0);
    }
}

