/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.business.charts;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.TestSupport;
import com.rebuild.server.service.bizz.UserService;
import org.junit.Test;

/**
 * 
 * @author devezhao
 * @since 01/04/2019
 */
public class ChartsTest extends TestSupport {

	@Test
	public void testTable() throws Exception {
		JSONObject config = JSON.parseObject(
				"{'entity':'testallfields','title':'表格','type':'TABLE','axis':{'dimension':[],'numerical':[{'field':'testallfieldsName','sort':'NONE','label':'','calc':'COUNT'}]},'option':{'showLineNumber':'false','showSums':'false'}}");
		ChartData index = ChartsFactory.create(config, UserService.ADMIN_USER);
		System.out.println(index.build());
	}
	
	@Test
	public void testIndex() throws Exception {
		JSONObject config = JSON.parseObject(
				"{ entity:'User', title:'指标卡', type:'INDEX', axis:{dimension:[], numerical:[{ field:'userId', sort:'', calc:'COUNT' }]}}");
		ChartData index = ChartsFactory.create(config, UserService.ADMIN_USER);
		System.out.println(index.build());
	}
	
	@Test
	public void testPie() throws Exception {
		JSONObject config = JSON.parseObject(
				"{'entity':'testallfields','title':'饼图','type':'PIE','axis':{'dimension':[{'field':'testallfieldsName','sort':'NONE','label':''}],'numerical':[{'field':'testallfieldsName','sort':'NONE','label':'','calc':'COUNT'}]},'option':{}}");
		ChartData pie = ChartsFactory.create(config, UserService.ADMIN_USER);
		System.out.println(pie.build());
	}
	
	@Test
	public void testLine() throws Exception {
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
	public void testTreemap() throws Exception {
		JSONObject config = JSON.parseObject(
				"{'entity':'testallfields','title':'矩形树图','type':'TREEMAP','axis':{'dimension':[{'field':'createdOn','sort':'NONE','label':'','calc':'D'}],'numerical':[{'field':'testallfieldsName','sort':'NONE','label':'','calc':'COUNT'}]},'option':{}}");
		ChartData line = ChartsFactory.create(config, UserService.ADMIN_USER);
		System.out.println(line.build());
	}
	
	@Test
	public void testFunnel() throws Exception {
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
}
