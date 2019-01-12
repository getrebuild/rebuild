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

import java.io.Closeable;
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
public class DataFileParser implements Closeable {

	final private File sourceFile;
	final private String encoding;
	private ExcelReader reader = null;
	
	/**
	 * @param sourceFile
	 */
	public DataFileParser(File sourceFile) {
		this.sourceFile = sourceFile;
		this.encoding = "GBK";
	}
	
	/**
	 * @param sourceFile
	 * @param encoding
	 */
	public DataFileParser(File sourceFile, String encoding) {
		this.sourceFile = sourceFile;
		this.encoding = encoding;
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
		while (reader.hasNext()) {
			Cell[] row = reader.next();
			if (row != null) {
				rows.add(row);
			}
			
			if (rowNo++ >= maxRows) {
				break;
			}
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
			reader = ExcelReaderFactory.create(sourceFile, encoding);
		}
		return reader;	
	}
	
	@Override
	public void close() {
		if (reader != null) {
			reader.close();
		}
	}
}
