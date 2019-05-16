/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

import org.junit.Test;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/16
 */
public class MarkdownUtilsTest {

	@Test
	public void testParse() throws Exception {
		String md = "[Click here](/admin/)";
		System.out.println(MarkdownUtils.parse(md));
		
		md = "\n"
				+ "|COL1|COL2|COL3|"
				+ "\n|---|---|---|"
				+ "\n|data|data||"
				+ "\n[Table 1. caption]";
		System.out.println(MarkdownUtils.parse(md));
	}
}
