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

package com.rebuild.utils;

import cn.devezhao.commons.excel.Cell;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author devezhao
 * @since 01/31/2019
 */
public class CommonsUtilsTest {

	@Test
	public void testIsPlainText() {
		assertTrue(CommonsUtils.isPlainText("123abc你好_-"));
		assertTrue(CommonsUtils.isPlainText("123abc-_"));
		assertFalse(CommonsUtils.isPlainText("123abc-_&)"));
		assertFalse(CommonsUtils.isPlainText("123 abc"));
	}
	
	@Test
	public void testStars() {
		String ts[] = new String[] {
				"ab",
				"abc",
				"abcd",
				"abc5943j958923574353524325",
				"abc5943j95892357fdsaFAFDS4353524325",
		};
		for (String t : ts) {
			System.out.println(t + " > " + CommonsUtils.stars(t));
		}
	}

	@Test
	public void testGet() throws Exception {
		String ret = CommonsUtils.get("https://ipapi.co/58.39.87.252/json/");
		System.out.println(JSONUtils.prettyPrint(JSON.parse(ret)));
	}

	@Test
	public void testPost() throws Exception {
		String ret = CommonsUtils.post("http://ip-api.com/json/58.39.87.252", null);
		System.out.println(JSONUtils.prettyPrint(JSON.parse(ret)));
	}
	
	@Ignore
	@Test
	public void testFormatArea() throws Exception {
		String text = FileUtils.readFileToString(new File("e:/hm.txt"), "gbk");
		JSONObject aJson = (JSONObject) JSON.parse(text, Feature.OrderedField);
		
		JSONArray root = new JSONArray();
		for (Entry<String, Object> E1 : aJson.entrySet()) {
			JSONObject L1 = new JSONObject(true);
			root.add(L1);
			String L1Code = "HK";
			if (E1.getKey().contains("澳门")) L1Code = "MO";
			else if (E1.getKey().contains("台湾")) L1Code = "TW";
			intoJson(E1.getKey(), L1Code, L1);
			
			JSONArray L1Child = new JSONArray();
			L1.put("children", L1Child);
			int L2Index = 1;
			for (Map.Entry<String, Object> E2 : ((JSONObject) E1.getValue()).entrySet()) {
				JSONObject L2 = new JSONObject(true);
				L1Child.add(L2);
				String L2Code = L1Code + StringUtils.leftPad((L2Index++) + "", 2, "0");
				intoJson(E2.getKey(), L2Code, L2);
				
				JSONArray L2Child = new JSONArray();
				L2.put("children", L2Child);
				int L3Index = 1;
				for (Object E3 : (JSONArray) E2.getValue()) {
					JSONObject L3 = new JSONObject(true);
					L2Child.add(L3);
					String L3Code = L2Code + StringUtils.leftPad((L3Index++) + "", 2, "0");
					intoJson(E3.toString(), L3Code, L3);
				}
			}
		}
		
		System.out.println(root);
	}
	
	private void intoJson(String name, String code, JSONObject dest) {
		System.out.println(name);
		dest.put("code", code);
		dest.put("name", name);
	}

	@Test
	public void readExcel() throws Exception {
		List<Cell[]> rows = CommonsUtils.readExcel(
				ResourceUtils.getFile("classpath:dataimports-test.xlsx"));
		for (Cell[] row : rows) {
			System.out.println(StringUtils.join(row, " | "));
		}

		rows = CommonsUtils.readExcel(
				ResourceUtils.getFile("classpath:dataimports-test.xls"));
		for (Cell[] row : rows) {
			System.out.println(StringUtils.join(row, " | "));
		}
	}
}
