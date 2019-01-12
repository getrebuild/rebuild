/*
rebuild - Building your system freely.
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

package com.rebuild.server.business.datas;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import cn.devezhao.commons.excel.Cell;

/**
 * 
 * @author devezhao
 * @since 01/09/2019
 */
public class DataFileParserTest {

	@Test
	public void testExcel() throws Exception {
		URL testFile = DataFileParserTest.class.getResource("dataimports-test.xls");
		DataFileParser fileParser = new DataFileParser(new File(testFile.toURI()));
		
		System.out.println(fileParser.getRowsCount());
		List<Cell[]> rows = fileParser.parse(10);
		for (Cell[] r : rows) {
			System.out.println(StringUtils.join(r, " | "));
		}
		fileParser.close();
	}
	
	@Test
	public void testCSV() throws Exception {
		URL testFile = DataFileParserTest.class.getResource("dataimports-test.csv");
		DataFileParser fileParser = new DataFileParser(new File(testFile.toURI()));
		
		System.out.println(fileParser.getRowsCount());
		List<Cell[]> rows = fileParser.parse(10);
		for (Cell[] r : rows) {
			System.out.println(StringUtils.join(r, " | "));
		}
		fileParser.close();
	}
	
	@Test
	public void testXExcel() throws Exception {
		URL testFile = DataFileParserTest.class.getResource("dataimports-test.xlsx");
		DataFileParser fileParser = new DataFileParser(new File(testFile.toURI()));
		
		System.out.println(fileParser.getRowsCount());
		List<Cell[]> rows = fileParser.parse(50);
		for (Cell[] r : rows) {
			System.out.println(StringUtils.join(r, " | "));
		}
		fileParser.close();
	}
}
