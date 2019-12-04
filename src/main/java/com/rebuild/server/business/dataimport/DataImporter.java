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

package com.rebuild.server.business.dataimport;

import cn.devezhao.commons.excel.Cell;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.helper.task.HeavyTask;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.ExtRecordCreator;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.utils.JSONUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据导入
 * 
 * @author devezhao
 * @since 01/09/2019
 * 
 * @see DisplayType
 */
public class DataImporter extends HeavyTask<Integer> {
	
	private static final ThreadLocal<ID> IN_IMPORTING = new ThreadLocal<>();
	
	final private ImportRule rule;
	final private ID owningUser;

	// 记录每一行的错误日志
	private Map<Integer, Object> eachLogs = new LinkedHashMap<>();
	
	/**
	 * @param rule
	 */
	public DataImporter(ImportRule rule) {
		this(rule, Application.getCurrentUser());
	}
	
	/**
	 * @param rule
	 * @param owningUser
	 */
	public DataImporter(ImportRule rule, ID owningUser) {
		this.rule = rule;
		this.owningUser = rule.getDefaultOwningUser() == null ? owningUser : rule.getDefaultOwningUser();
	}
	
	@Override
	protected Integer exec() throws Exception {
		final List<Cell[]> rows = new DataFileParser(rule.getSourceFile()).parse();
		this.setTotal(rows.size() - 1);

		// 指定的所属用户
		setUser(this.owningUser);
		IN_IMPORTING.set(owningUser);

		for (final Cell[] row : rows) {
			if (isInterrupt()) {
				this.setInterrupted();
				break;
			}

			Cell firstCell = row == null || row.length == 0 ? null : row[0];
			if (firstCell == null || firstCell.getRowNo() == 0) {
				continue;
			}

			try {
				Record record = checkoutRecord(row);
				if (record != null) {
					record = Application.getEntityService(rule.getToEntity().getEntityCode()).createOrUpdate(record);
					this.addSucceeded();
					eachLogs.put(firstCell.getRowNo(), record.getPrimary());
				}
			} catch (Exception ex) {
				eachLogs.put(firstCell.getRowNo(), ex.getLocalizedMessage());
				LOG.error(firstCell.getRowNo() + " > " + ex);
			}
			this.addCompleted();
		}

		return this.getSucceeded();
	}

	@Override
	protected void completedAfter() {
		super.completedAfter();
		IN_IMPORTING.remove();
	}

	/**
	 * 获取错误日志（按错误行）
	 *
	 * @return
	 */
	public Map<Integer, Object> getEachLogs() {
		return eachLogs;
	}
	
	/**
	 * @param row
	 * @return
	 */
	protected Record checkoutRecord(Cell[] row) {
		Record recordNew = EntityHelper.forNew(rule.getToEntity().getEntityCode(), this.owningUser);

		// 新建
		Record record = new RecordCheckout(rule.getFiledsMapping()).checkout(recordNew, row);
		
		// 检查重复
		if (rule.getRepeatOpt() < ImportRule.REPEAT_OPT_IGNORE) {
			final ID repeat = getRepeatedRecordId(rule.getRepeatFields(), recordNew);
			
			if (repeat != null && rule.getRepeatOpt() == ImportRule.REPEAT_OPT_SKIP) {
				return null;
			}
			
			if (repeat != null && rule.getRepeatOpt() == ImportRule.REPEAT_OPT_UPDATE) {
				// 更新
				record = EntityHelper.forUpdate(repeat, this.owningUser);
				for (Iterator<String> iter = recordNew.getAvailableFieldIterator(); iter.hasNext(); ) {
					String field = iter.next();
					if (EntityHelper.OwningUser.equals(field) || EntityHelper.OwningDept.equals(field)
							|| EntityHelper.CreatedBy.equals(field) || EntityHelper.CreatedOn.equals(field)
							|| EntityHelper.ModifiedBy.equals(field) || EntityHelper.ModifiedOn.equals(field)) {
						continue;
					}
					record.setObjectValue(field, recordNew.getObjectValue(field));
				}
			}
		}
		
		// Verify new record
		if (record.getPrimary() == null) {
			ExtRecordCreator verifier = new ExtRecordCreator(rule.getToEntity(), JSONUtils.EMPTY_OBJECT, null);
			verifier.verify(record, true);
		}
		return record;
	}
	
	/**
	 * @param repeatFields
	 * @param data
	 * @return
	 */
	protected ID getRepeatedRecordId(Field[] repeatFields, Record data) {
		Map<String, Object> wheres = new HashMap<>();
		for (Field c : repeatFields) {
			String cName = c.getName();
			if (data.hasValue(cName)) {
				wheres.put(cName, data.getObjectValue(cName));
			}
		}
		
		LOG.info("Checking repeated : " + wheres);
		if (wheres.isEmpty()) {
			return null;
		}
		
		Entity entity = data.getEntity();
		StringBuilder sql = new StringBuilder(String.format("select %s from %s where (1=1)",
				entity.getPrimaryField().getName(), entity.getName()));
		for (String c : wheres.keySet()) {
			sql.append(" and ").append(c).append(" = :").append(c);
		}
		
		Query query = Application.createQueryNoFilter(sql.toString());
		for (Map.Entry<String, Object> e : wheres.entrySet()) {
			query.setParameter(e.getKey(), e.getValue());
		}
		
		Object[] exists = query.unique();
		return exists == null ? null : (ID) exists[0];
	}
	
	// --
	
	/**
	 * 是否为导入状态
	 * 
	 * @return
	 */
	public static boolean inImportingState() {
		return IN_IMPORTING.get() != null;
	}
}
