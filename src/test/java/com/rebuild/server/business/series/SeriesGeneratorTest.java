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

package com.rebuild.server.business.series;

import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author devezhao
 * @since 12/24/2018
 */
public class SeriesGeneratorTest extends TestSupport {

	@Test
	public void testTimeVar() throws Exception {
		String r = new TimeVar("YYMMDD").generate();
		System.out.println(r);
	}
	
	@Test
	public void testIncrementVar() throws Exception {
		IncreasingVar var = new IncreasingVar("0000", getSeriesField(), null);
		System.out.println(var.generate());
		System.out.println(var.generate());
		System.out.println(var.generate());
	}
	
	@Test
	public void testIncrementVarNThreads() throws Exception {
		final IncreasingVar var = new IncreasingVar("0000", getSeriesField(), "Y");
		final Set<String> set = Collections.synchronizedSet(new HashSet<>());
		final int N = 200;
		for (int i = 0; i < N; i++) {
			ThreadPool.exec(() -> {
				String s = var.generate();
				set.add(s);
				System.out.print(s + " ");
			});
		}
		ThreadPool.waitFor(200);
		System.out.println();
		Assert.assertTrue(set.size() == N);
	}
	
	@Test
	public void testGenerate() throws Exception {
		Map<String, String> config = new HashMap<>();
		config.put("seriesFormat", "Y-{YYYYMMDD}-{0000}");
		config.put("seriesZero", "M");
		
		SeriesGenerator generator = new SeriesGenerator(getSeriesField(), (JSONObject) JSON.toJSON(config));
		System.out.println(generator.generate());
		System.out.println(generator.generate());
		System.out.println(generator.generate());
	}
	
	/**
	 * @return
	 * @see DisplayType#SERIES
	 */
	private Field getSeriesField() {
		Field seriesField = MetadataHelper.getField(TEST_ENTITY, "series");
		return seriesField;
	}
}
