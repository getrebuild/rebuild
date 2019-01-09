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

import com.rebuild.server.helper.task.BulkTask;

/**
 * 数据导入
 * 
 * @author devezhao
 * @since 01/09/2019
 */
public class DataImports extends BulkTask {
	
	final private DataFileParser fileParser;
	
	public DataImports(File sourceFile) {
		this.fileParser = new DataFileParser(sourceFile);
	}
	
	/**
	 * @return
	 */
	public DataFileParser getFileParser() {
		return fileParser;
	}

	@Override
	public void run() {
	}
}
