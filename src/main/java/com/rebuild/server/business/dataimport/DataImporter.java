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

import cn.devezhao.commons.RegexUtils;
import cn.devezhao.commons.excel.Cell;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.ClassificationManager;
import com.rebuild.server.configuration.portals.PickListManager;
import com.rebuild.server.helper.state.StateManager;
import com.rebuild.server.helper.task.HeavyTask;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.ExtRecordCreator;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
	
	private int successed = 0;
	private Map<Integer, Object> iLogging = new LinkedHashMap<>();
	
	/**
	 * @param rule
	 */
	public DataImporter(ImportRule rule) {
		this(rule, Application.getCurrentUser());
	}
	
	/**
	 * @param rule
	 * @param user
	 */
	public DataImporter(ImportRule rule, ID user) {
		this.rule = rule;
		this.owningUser = rule.getDefaultOwningUser() == null ? user : rule.getDefaultOwningUser();
	}
	
	@Override
	public Integer exec() throws Exception {
		try {
			DataFileParser fileParser = new DataFileParser(rule.getSourceFile());
			this.setTotal(fileParser.getRowsCount() - 1);
			
			setThreadUser(this.owningUser);
			IN_IMPORTING.set(owningUser);

			int rowLine = 0;
			for (final Cell[] row : fileParser.parse()) {
				if (isInterrupt()) {
					this.setInterrupted();
					break;
				}
				if (rowLine++ == 0 || row == null) {
					continue;
				}

				try {
					Record record = checkoutRecord(row);
					if (record != null) {
						record = Application.getEntityService(rule.getToEntity().getEntityCode()).createOrUpdate(record);
						this.successed++;
						iLogging.put(rowLine, record.getPrimary());
					}
				} catch (Exception ex) {
					iLogging.put(rowLine, ex.getLocalizedMessage());
					LOG.warn(rowLine + " > " + ex);
				} finally {
					this.addCompleted();
				}
			}
		} finally {
			IN_IMPORTING.remove();
		}
		return this.successed;
	}
	
	/**
	 * @return
	 */
	public int getSuccessed() {
		return successed;
	}

	/**
	 * @return
	 */
	public Map<Integer, Object> getLogging() {
		return iLogging;
	}
	
	/**
	 * @param cells
	 * @return
	 */
	protected Record checkoutRecord(Cell[] cells) {
		Record recordNew = EntityHelper.forNew(rule.getToEntity().getEntityCode(), this.owningUser);
		
		for (Map.Entry<Field, Integer> e : rule.getFiledsMapping().entrySet()) {
			int cellIndex = e.getValue();
			if (cells.length > cellIndex) {
				Field field = e.getKey();
				Object value = checkoutFieldValue(field, cells[cellIndex], true);
				if (value != null) {
					recordNew.setObjectValue(field.getName(), value);
				}
			}
		}

		// 新建
		Record record = recordNew;
		
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
	 * @param field
	 * @param cell
	 * @param validate
	 * @return
	 */
	protected Object checkoutFieldValue(Field field, Cell cell, boolean validate) {
		final DisplayType dt = EasyMeta.getDisplayType(field);
		if (dt == DisplayType.NUMBER) {
			return cell.asLong();
		} else if (dt == DisplayType.DECIMAL) {
			return cell.asDouble();
		} else if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
			return checkoutDateValue(field, cell);
		} else if (dt == DisplayType.PICKLIST) {
			return checkoutPickListValue(field, cell);
		} else if (dt == DisplayType.CLASSIFICATION) {
			return checkoutClassificationValue(field, cell);
		} else if (dt == DisplayType.REFERENCE) {
			return checkoutReferenceValue(field, cell);
		} else if (dt == DisplayType.BOOL) {
			return cell.asBool();
		} else if (dt == DisplayType.STATE) {
			return checkoutStateValue(field, cell);
		}
		
		// 格式验证
		if (validate) {
			if (dt == DisplayType.EMAIL) {
				String email = cell.asString();
				return RegexUtils.isEMail(email) ? email : null;
			} else if (dt == DisplayType.URL) {
				String url = cell.asString();
				return RegexUtils.isUrl(url) ? url : null;
			} else if (dt == DisplayType.PHONE) {
				String tel = cell.asString();
				return RegexUtils.isCNMobile(tel) || RegexUtils.isTel(tel)? tel : null;
			}
		}
		return cell.asString();
	}
	
	/**
	 * @param field
	 * @param cell
	 * @return
	 */
	private ID checkoutPickListValue(Field field, Cell cell) {
		String val = cell.asString();
		if (StringUtils.isBlank(val)) {
			return null;
		}
		
		// 支持ID
		if (ID.isId(val) && ID.valueOf(val).getEntityCode() == EntityHelper.PickList) {
			ID iid = ID.valueOf(val);
			if (PickListManager.instance.getLabel(iid) != null) {
				return iid;
			} else {
				LOG.warn("No item of PickList found by ID : " + iid);
				return null;
			}
		} else {
			return PickListManager.instance.findItemByLabel(val, field);
		}
	}

	/**
	 * @param field
	 * @param cell
	 * @return
	 */
	private Integer checkoutStateValue(Field field, Cell cell) {
		final String val = cell.asString();
		if (StringUtils.isBlank(val)) {
			return null;
		}

		Integer state = StateManager.instance.getState(field, val);
		if (state != null) {
			return state;
		}

		// 兼容状态值
		if (NumberUtils.isNumber(val)) {
			return NumberUtils.toInt(val);
		}
		return null;
	}

	/**
	 * @param field
	 * @param cell
	 * @return
	 */
	private ID checkoutClassificationValue(Field field, Cell cell) {
		String val = cell.asString();
		if (StringUtils.isBlank(val)) {
			return null;
		}
		
		// 支持ID
		if (ID.isId(val) && ID.valueOf(val).getEntityCode() == EntityHelper.ClassificationData) {
			ID iid = ID.valueOf(val);
			if (ClassificationManager.instance.getName(iid) != null) {
				return iid;
			} else {
				LOG.warn("No item of Classification found by ID : " + iid);
				return null;
			}
		} else {
			return ClassificationManager.instance.findItemByName(val, field);
		}
	}
	
	/**
	 * @param field
	 * @param cell
	 * @return
	 */
	private ID checkoutReferenceValue(Field field, Cell cell) {
		String val = cell.asString();
		if (StringUtils.isBlank(val)) {
			return null;
		}
		
		Entity oEntity = field.getReferenceEntity();
		
		// 支持ID导入
		if (ID.isId(val) && ID.valueOf(val).getEntityCode().intValue() == oEntity.getEntityCode()) {
			return ID.valueOf(val);
		}
		
		Object textVal = checkoutFieldValue(oEntity.getNameField(), cell, false);
		if (textVal == null) {
			return null;
		}
		
		Query query = null;
		if (oEntity.getEntityCode() == EntityHelper.User) {
			String sql = MessageFormat.format(
					"select userId from User where loginName = ''{0}'' or email = ''{0}'' or fullName = ''{0}''",
					StringEscapeUtils.escapeSql(textVal.toString()));
			query = Application.createQueryNoFilter(sql);
		} else {
			String sql = String.format("select %s from %s where %s = ?",
					oEntity.getPrimaryField().getName(), oEntity.getName(), oEntity.getNameField().getName());
			query = Application.createQueryNoFilter(sql).setParameter(1, textVal);
		}

		Object[] found = query.unique();
		return found != null ? (ID) found[0] : null;
	}
	
	/**
	 * @param field
	 * @param cell
	 * @return
	 */
	private Date checkoutDateValue(Field field, Cell cell) {
		Date date = cell.asDate();
		if (date != null) {
			return date;
		}
		if (cell.isEmpty()) {
			return null;
		}
		
		String date2str = cell.asString();
		// 2017/11/19 11:07
		if (date2str.contains("/")) {
			return cell.asDate(new String[] { "yyyy/M/d H:m:s", "yyyy/M/d H:m", "yyyy/M/d" });
		}
		return null;
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
	 * 是否导入模式
	 * 
	 * @return
	 */
	public static boolean isInImporting() {
		return IN_IMPORTING.get() != null;
	}
}
