/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.beust.ah.A;
import com.rebuild.TestSupport;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2020/08/18
 */
public class RBStoreTest extends TestSupport {

    @Test
    void fetchMetaschema() {
        JSON data = RBStore.fetchMetaschema("ACCOUNT-1.0.json");
        Assertions.assertNotNull(data);

        System.out.println(JSONUtils.prettyPrint(data));
    }

    @Disabled
    @Test
    void countriesAndStates() throws IOException {
        String source = FileUtils.readFileToString(
                new File("C:\\Users\\devezhao\\Downloads\\countries+states.json"), "utf-8");
        JSONArray datas = JSON.parseArray(source);

        JSONArray dest = new JSONArray();
        for (Object o : datas) {
            final JSONObject c = (JSONObject) o;
            JSONObject country = new JSONObject(true);
            country.put("name", c.getString("name"));
            country.put("code", c.getString("iso3"));

            JSONArray states = new JSONArray();
            for (Object o2 : c.getJSONArray("states")) {
                final JSONObject c2 = (JSONObject) o2;
                JSONObject state = new JSONObject(true);
                state.put("name", c2.getString("name"));
                state.put("code", c2.getString("id"));
                states.add(state);
            }
            country.put("children", states);

            dest.add(country);
        }

        FileUtils.writeStringToFile(new File("D:\\WORLD-STATES.en.json"), dest.toJSONString(), "utf-8");
    }
}