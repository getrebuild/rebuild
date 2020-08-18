/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * @author devezhao
 * @since 2020/7/15
 */
public class HttpUtilsTest {

    @Test
    public void testGet() throws Exception {
        String ret = HttpUtils.get("https://ipapi.co/58.39.87.252/json/");
        System.out.println(JSONUtils.prettyPrint(JSON.parse(ret)));
    }

    @Test
    public void testPost() throws Exception {
        String ret = HttpUtils.post("http://ip-api.com/json/58.39.87.252", null);
        System.out.println(JSONUtils.prettyPrint(JSON.parse(ret)));
    }

    @Test
    public void readBinary() throws Exception {
        File tmp = HttpUtils.readBinary("http://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png");
        Assert.assertNotNull(tmp);
        System.out.println("Read binary size : " + FileUtils.sizeOf(tmp));
    }
}
