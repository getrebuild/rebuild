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

package com.rebuild.server;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;

/**
 * 
 * @author devezhao
 * @since 01/09/2019
 */
public class MiscTest {

	@Ignore
	@Test
	public void testJson2String() throws Exception {
		System.out.println(JSON.toJSONString(ID.newId(0)));
		System.out.println(JSON.toJSONString(new Date()));

		System.out.println(System.getenv());
		System.out.println(System.getProperties());
	}
	
	@Ignore
	@Test
	public void testMask() throws Exception {
		long mask = 1;
		for (int i = 0; i < 64; i++) {
			System.out.println(mask);
			mask *= 2;
		}
	}
	
	@Test
	public void testEquation() throws Exception {
		final String equation = "((1 OR 2) AND (2    OR 3)  )";
		
		String clear = equation.toUpperCase();
		clear = clear.replaceAll("[\\(|\\)|AND|OR|1-9| ]", "");
		System.out.println(clear);
	}
}
