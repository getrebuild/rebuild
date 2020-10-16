/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 01/04/2019
 */
public class AdvFilterParserTest extends TestSupport {

    @Test
    public void testBaseParse() {
        JSONObject filterExp = new JSONObject();
        filterExp.put("entity", "User");
        JSONArray items = new JSONArray();
        filterExp.put("items", items);
        filterExp.put("equation", "(1 AND 2) or (1 OR 2)");

        // Filter items
        items.add(JSON.parseObject("{ op:'LK', field:'loginName', value:'admin' }"));
        items.add(JSON.parseObject("{ op:'EQ', field:'deptId.name', value:'总部' }"));  // Joins

        String where = new AdvFilterParser(filterExp).toSqlWhere();
        System.out.println(where);
    }

    @Test
    public void testBadJoinsParse() {
        JSONObject filterExp = new JSONObject();
        filterExp.put("entity", "User");
        JSONArray items = new JSONArray();
        filterExp.put("items", items);

        // Filter item
        items.add(JSON.parseObject("{ op:'LK', field:'loginName.name', value:'总部' }"));

        String where = new AdvFilterParser(filterExp).toSqlWhere();
        System.out.println(where);  // null
    }

    @Test
    public void testDateAndDatetime() {
        JSONObject filterExp = new JSONObject();
        filterExp.put("entity", TestAllFields);
        JSONArray items = new JSONArray();
        filterExp.put("items", items);

        // Use `=`
        items.add(JSON.parseObject("{ op:'EQ', field:'date', value:'2019-09-09' }"));
        // Use `between`
        items.add(JSON.parseObject("{ op:'EQ', field:'datetime', value:'2019-09-09' }"));
        System.out.println(new AdvFilterParser(filterExp).toSqlWhere());

        items.clear();
        // Use `=`
        items.add(JSON.parseObject("{ op:'TDA', field:'date' }"));
        // Use `between`
        items.add(JSON.parseObject("{ op:'TDA', field:'datetime' }"));
        System.out.println(new AdvFilterParser(filterExp).toSqlWhere());

        items.clear();
        // No padding
        items.add(JSON.parseObject("{ op:'GT', field:'date', value:'2019-09-09' }"));
        // Padding time
        items.add(JSON.parseObject("{ op:'GT', field:'datetime', value:'2019-09-09' }"));
        // No padding
        items.add(JSON.parseObject("{ op:'GT', field:'datetime', value:'2019-09-09 12:12:54' }"));
        System.out.println(new AdvFilterParser(filterExp).toSqlWhere());
    }
}
