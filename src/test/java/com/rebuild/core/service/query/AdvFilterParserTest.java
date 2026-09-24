/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import cn.devezhao.commons.CalendarUtils;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author devezhao
 * @since 01/04/2019
 */
public class AdvFilterParserTest extends TestSupport {

    @Test
    void testBaseParse() {
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
    void testBadJoinsParse() {
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
    void testDateAndDatetime() {
        JSONObject filterExp = new JSONObject();
        filterExp.put("entity", TestAllFields);
        JSONArray items = new JSONArray();
        filterExp.put("items", items);

        // Use `=`
        items.add(JSON.parseObject("{ op:'EQ', field:'date1', value:'2019-09-09' }"));
        // Use `between`
        items.add(JSON.parseObject("{ op:'EQ', field:'datetime', value:'2019-09-09' }"));
        System.out.println(new AdvFilterParser(filterExp).toSqlWhere());

        items.clear();
        // Use `=`
        items.add(JSON.parseObject("{ op:'TDA', field:'date1' }"));
        // Use `between`
        items.add(JSON.parseObject("{ op:'TDA', field:'datetime' }"));
        System.out.println(new AdvFilterParser(filterExp).toSqlWhere());

        items.clear();
        // No padding
        items.add(JSON.parseObject("{ op:'GT', field:'date1', value:'2019-09-09' }"));
        // Padding time 23
        items.add(JSON.parseObject("{ op:'GT', field:'datetime', value:'2019-09-09' }"));
        // Padding time 00
        items.add(JSON.parseObject("{ op:'LT', field:'datetime', value:'2019-09-09' }"));
        // No padding
        items.add(JSON.parseObject("{ op:'GT', field:'datetime', value:'2019-09-09 12:12:54' }"));
        System.out.println(new AdvFilterParser(filterExp).toSqlWhere());
    }

    @Test
    void testDateFunc() {
        JSONObject filterExp = new JSONObject();
        filterExp.put("entity", TestAllFields);
        JSONArray items = new JSONArray();
        filterExp.put("items", items);

        // 1 天前
        items.add(JSON.parseObject("{ op:'BFD', field:'date1', value:'1' }"));
        items.add(JSON.parseObject("{ op:'BFD', field:'datetime', value:'1' }"));
        // 1 天后
        items.add(JSON.parseObject("{ op:'AFD', field:'date1', value:'1' }"));
        items.add(JSON.parseObject("{ op:'AFD', field:'datetime', value:'1' }"));
        System.out.println(new AdvFilterParser(filterExp).toSqlWhere());

        items.clear();
        // 最近 1 天
        items.add(JSON.parseObject("{ op:'RED', field:'date1', value:'1' }"));
        // 未来 1 天
        items.add(JSON.parseObject("{ op:'FUD', field:'datetime', value:'1' }"));
        System.out.println(new AdvFilterParser(filterExp).toSqlWhere());
    }

    @Test
    void testRep() {
        JSONObject filterExp = new JSONObject();
        filterExp.put("entity", TestAllFields);
        JSONArray items = new JSONArray();
        filterExp.put("items", items);

        items.add(JSON.parseObject("{ op:'REP', field:'TestAllFieldsName', value:2 }"));
        System.out.println(new AdvFilterParser(filterExp).toSqlWhere());
    }

    @Test
    void testUnitOperators() {
        Date now = CalendarUtils.now();
        Map<String, Date> begins = new HashMap<>();
        begins.put("CUW", DateUtil.beginOfWeek(now));
        begins.put("CUM", DateUtil.beginOfMonth(now));
        begins.put("CUQ", DateUtil.beginOfQuarter(now));
        begins.put("CUY", DateUtil.beginOfYear(now));
        begins.put("PUQ", DateUtil.offsetMonth(DateUtil.beginOfQuarter(now), -3));
        begins.put("NUY", DateUtil.offsetMonth(DateUtil.beginOfYear(now), 12));

        JSONObject filterExp = new JSONObject();
        filterExp.put("entity", TestAllFields);
        JSONArray items = new JSONArray();
        filterExp.put("items", items);

        for (Map.Entry<String, Date> e : begins.entrySet()) {
            items.clear();
            items.add(JSON.parseObject("{ op:'" + e.getKey() + "', field:'date1' }"));
            String where = new AdvFilterParser(filterExp).toSqlWhere();
            System.out.println(where);

            assertTrue(where.contains(CalendarUtils.getUTCDateFormat().format(e.getValue())));
        }
    }

    @Test
    void testYyyMmm() {
        JSONObject filterExp = new JSONObject();
        filterExp.put("entity", TestAllFields);
        JSONArray items = new JSONArray();
        filterExp.put("items", items);

        // 上一年 / 上一月
        items.add(JSON.parseObject("{ op:'YYY', field:'date1', value:'-1' }"));
        items.add(JSON.parseObject("{ op:'MMM', field:'date1', value:'-1' }"));
        String where = new AdvFilterParser(filterExp).toSqlWhere();
        System.out.println(where);

        Calendar now = CalendarUtils.getInstance();
        now.add(Calendar.YEAR, -1);
        assertTrue(where.contains(now.get(Calendar.YEAR) + "-01-01"));
        assertTrue(where.contains(now.get(Calendar.YEAR) + "-12-31"));

        now = CalendarUtils.getInstance();
        now.set(Calendar.DAY_OF_MONTH, 1);
        now.add(Calendar.MONTH, -1);
        assertTrue(where.contains(CalendarUtils.getUTCDateFormat().format(now.getTime())));
        assertTrue(where.contains(CalendarUtils.getUTCDateFormat().format(DateUtil.endOfMonth(now.getTime()))));
    }
}
