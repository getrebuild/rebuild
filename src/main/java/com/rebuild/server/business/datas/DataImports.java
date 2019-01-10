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

import java.util.Map;

import com.rebuild.server.Application;
import com.rebuild.server.helper.task.BulkTask;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.ExtRecordCreator;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.commons.excel.Cell;
import cn.devezhao.commons.excel.ExcelReader;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.record.FieldValueException;

/**
 * 数据导入
 * 
 * @author devezhao
 * @since 01/09/2019
 */
public class DataImports extends BulkTask {
	
	final private ImportsEnter enter;
	final private ID user;
	
	/**
	 * @param enter
	 * @param user
	 */
	public DataImports(ImportsEnter enter, ID user) {
		this.enter = enter;
		this.user = user;
	}
	
	/**
	 * @return
	 */
	public ImportsEnter getImportsEnter() {
		return enter;
	}
	
	@Override
	public void run() {
		DataFileParser fileParser = new DataFileParser(enter.getSourceFile());
		setTotal(fileParser.getRowsCount() - 1);
		
		ExcelReader reader = fileParser.getExcelReader();
		reader.next();  // The head row
		while (reader.hasNext()) {
			try {
				Cell[] cell = reader.next();
				Record record = checkoutRecord(cell);
				if (record != null) {
					Application.getEntityService(enter.getToEntity().getEntityCode()).create(record);
				}
			} catch (DataSpecificationException ex1) {
			} catch (FieldValueException ex2) {
			} catch (Exception ex3) {
			} finally {
				this.setCompleteOne();
			}
		}
	}
	
	/**
	 * @param cells
	 * @return
	 */
	protected Record checkoutRecord(Cell cells[]) {
		Record record = EntityHelper.forNew(
				enter.getToEntity().getEntityCode(), 
				enter.getDefaultOwningUser() != null ? enter.getDefaultOwningUser() : user);
		
		for (Map.Entry<Field, Integer> e : enter.getFiledsMapping().entrySet()) {
			int cellIndex = e.getValue();
			if (cells.length > cellIndex) {
				Field field = e.getKey();
				Object value = checkoutFieldValue(field, cells[cellIndex]);
				if (value != null) {
					record.setObjectValue(field.getName(), value);
				}
			}
		}
		
		ExtRecordCreator creator = new ExtRecordCreator(enter.getToEntity(), JSONUtils.EMPTY_OBJECT, null);
		creator.verify(record, true);
		return record;
	}
	
	/**
	 * @param field
	 * @param cell
	 * @return
	 */
	protected Object checkoutFieldValue(Field field, Cell cell) {
		
		// TODO checkoutFieldValue
		
		return null;
	}
}
