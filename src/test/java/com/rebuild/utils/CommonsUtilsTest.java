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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 
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
}
