/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.core.privileges.UserService;
import org.junit.Test;

/**
 * @author devezhao
 * @since 01/04/2019
 */
public class ChartsTest extends TestSupport {

    @Test
    public void testTable() {
        JSONObject config = JSON.parseObject(
                "{'entity':'testallfields','title':'表格','type':'TABLE','axis':{'dimension':[],'numerical':[{'field':'testallfieldsName','sort':'NONE','label':'','calc':'COUNT'}]},'option':{'showLineNumber':'false','showSums':'false'}}");
        ChartData index = ChartsFactory.create(config, UserService.ADMIN_USER);
        System.out.println(index.build());
    }

    @Test
    public void testIndex() {
        JSONObject config = JSON.parseObject(
                "{ entity:'User', title:'指标卡', type:'INDEX', axis:{dimension:[], numerical:[{ field:'userId', sort:'', calc:'COUNT' }]}}");
        ChartData index = ChartsFactory.create(config, UserService.ADMIN_USER);
        System.out.println(index.build());
    }

    @Test
    public void testPie() {
        JSONObject config = JSON.parseObject(
                "{'entity':'testallfields','title':'饼图','type':'PIE','axis':{'dimension':[{'field':'testallfieldsName','sort':'NONE','label':''}],'numerical':[{'field':'testallfieldsName','sort':'NONE','label':'','calc':'COUNT'}]},'option':{}}");
        ChartData pie = ChartsFactory.create(config, UserService.ADMIN_USER);
        System.out.println(pie.build());
    }

    @Test
    public void testLine() {
        JSONObject config = JSON.parseObject(
                "{'entity':'testallfields','title':'折线图','type':'LINE','axis':{'dimension':[{'field':'createdOn','sort':'NONE','label':'','calc':'H'}],'numerical':[{'field':'testallfieldsName','sort':'NONE','label':'','calc':'COUNT'}]},'option':{}}");
        ChartData line = ChartsFactory.create(config, UserService.ADMIN_USER);
        System.out.println(line.build());

        // BAR
        config.put("type", "BAR");
        ChartData bar = ChartsFactory.create(config, UserService.ADMIN_USER);
        System.out.println(bar.build());
    }

    @Test
    public void testTreemap() {
        JSONObject config = JSON.parseObject(
                "{'entity':'testallfields','title':'矩形树图','type':'TREEMAP','axis':{'dimension':[{'field':'createdOn','sort':'NONE','label':'','calc':'D'}],'numerical':[{'field':'testallfieldsName','sort':'NONE','label':'','calc':'COUNT'}]},'option':{}}");
        ChartData line = ChartsFactory.create(config, UserService.ADMIN_USER);
        System.out.println(line.build());
    }

    @Test
    public void testFunnel() {
        JSONObject config = JSON.parseObject(
                "{'entity':'testallfields','title':'漏斗图','type':'FUNNEL','axis':{'dimension':[{'field':'picklist','sort':'NONE','label':''}],'numerical':[{'field':'testallfieldsName','sort':'NONE','label':'','calc':'COUNT'}]},'option':{}}");
        ChartData line = ChartsFactory.create(config, UserService.ADMIN_USER);
        System.out.println(line.build());
    }

    @Test(expected = ChartsException.class)
    public void testBadChart() {
        JSONObject config = JSON.parseObject(
                "{'entity':'testallfields','title':'无效图表','type':'abc'}");
        ChartsFactory.create(config, UserService.ADMIN_USER);
    }

    @Test
    public void testRadarChart() {
        JSONObject config = JSON.parseObject(
                "{'entity':'testallfields','title':'雷达图','type':'RADAR','axis':{'dimension':[{'field':'createdOn','sort':'NONE','label':''}],'numerical':[{'field':'createdOn','sort':'NONE','label':'','calc':'COUNT'}]},'option':{}}");
        ChartData radar = ChartsFactory.create(config, UserService.ADMIN_USER);
        System.out.println(radar.build());
    }

    @Test
    public void testScatterChart() {
        JSONObject config = JSON.parseObject(
                "{'entity':'testallfields','title':'散点图','type':'SCATTER','axis':{'dimension':[],'numerical':[{'field':'createdOn','sort':'NONE','label':'','calc':'COUNT'},{'field':'createdOn','sort':'NONE','label':'','calc':'COUNT'}]},'option':{}}");
        ChartData scatter = ChartsFactory.create(config, UserService.ADMIN_USER);
        System.out.println(scatter.build());

        JSONObject dim = JSON.parseObject("{'field':'createdOn','sort':'NONE','label':''}");
        config.getJSONObject("axis").getJSONArray("dimension").add(dim);
        scatter = ChartsFactory.create(config, UserService.ADMIN_USER);
        System.out.println(scatter.build());
    }
}
