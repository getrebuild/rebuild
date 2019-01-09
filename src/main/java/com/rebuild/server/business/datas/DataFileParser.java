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
import java.util.ArrayList;
import java.util.List;

import cn.devezhao.commons.excel.Cell;
import cn.devezhao.commons.excel.ExcelReader;
import cn.devezhao.commons.excel.ExcelReaderFactory;

/**
 * 文件解析
 * 
 * @author devezhao
 * @since 01/09/2019
 */
public class DataFileParser {

	final private File sourceFile;
	private ExcelReader reader = null;
	
	/**
	 * @param sourceFile
	 */
	public DataFileParser(File sourceFile) {
		this.sourceFile = sourceFile;
	}
	
	/**
	 * @return
	 */
	public File getSourceFile() {
		return sourceFile;
	}
	
	/**
	 * @return
	 */
	public List<Cell[]> parse() {
		return parse(Integer.MAX_VALUE);
	}
	
	/**
	 * @param maxRows
	 * @return
	 */
	public List<Cell[]> parse(int maxRows) {
		getExcelReader();  // init
		
		List<Cell[]> rows = new ArrayList<>();
		int rowNo = 1;
		try {
			while (reader.hasNext()) {
				Cell[] row = reader.next();
				rows.add(row);
				if (rowNo++ >= maxRows) {
					break;
				}
			}
		} finally {
			reader.close();
		}
		return rows;
	}
	
	/**
	 * @return
	 */
	public int getRowsCount() {
		return getExcelReader().getRowCount();
	}
	
	/**
	 * @return
	 */
	protected ExcelReader getExcelReader() {
		if (reader == null) {
			reader = ExcelReaderFactory.create(sourceFile, "GBK");
		}
		return reader;	
	}
}
