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

package com.rebuild.server.service.base;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.RegexUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.hankcs.hanlp.HanLP;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.ClassificationManager;
import com.rebuild.server.configuration.portals.PickListManager;
import com.rebuild.server.helper.state.StateManager;
import com.rebuild.server.helper.task.HeavyTask;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.bizz.UserService;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * QuickCode 字段值重建
 * 
 * @author devezhao
 * @since 12/28/2018
 */
public class QuickCodeReindexTask extends HeavyTask<Integer> {
	
	final private Entity entity;

	/**
	 * @param entity
	 */
	public QuickCodeReindexTask(Entity entity) {
		super();
		this.entity = entity;
	}
	
	@Override
	public Integer exec() throws Exception {
		if (!entity.containsField(EntityHelper.QuickCode)) {
			throw new IllegalArgumentException("No QuickCode field found : " + entity);
		}
		
		Field nameFiled = entity.getNameField();
		String sql = String.format("select %s,%s,quickCode from %s",
				entity.getPrimaryField().getName(), nameFiled.getName(), entity.getName());
		int page = 1;
		while (true) {
			List<Record> records =  Application.createQueryNoFilter(sql)
					.setLimit(1000, page * 1000 - 1000)
					.list();
			
			this.setTotal(records.size() + this.getTotal() + 1);
			for (Record o : records) {
				if (this.isInterrupt()) {
					this.setInterrupted();
					break;
				}
				
				try {
					String quickCodeNew = generateQuickCode(o);
					if (quickCodeNew == null) {
						continue;
					}
					if (quickCodeNew.equals(o.getString(EntityHelper.QuickCode))) {
						continue;
					}
					
					Record record = EntityHelper.forUpdate(o.getPrimary(), UserService.SYSTEM_USER, false);
					if (StringUtils.isBlank(quickCodeNew)) {
						record.setNull(EntityHelper.QuickCode);
					} else {
						record.setString(EntityHelper.QuickCode, quickCodeNew);
					}
					Application.getCommonService().update(record, false);
				} finally {
					this.addCompleted();
				}
			}
			
			if (records.size() < 1000 || this.isInterrupted()) {
				break;
			}
		}
		
		this.setTotal(this.getTotal() - 1);
		return this.getTotal();
	}
	
	// --
	
	/**
	 * @param record
	 * @return
	 */
	public static String generateQuickCode(Record record) {
		Entity entity = record.getEntity();
		if (!entity.containsField(EntityHelper.QuickCode)) {
			return null;
		}
		
		Field nameField = entity.getNameField();
		if (!record.hasValue(nameField.getName(), false)) {
			return null;
		}
		
		Object nameValue = record.getObjectValue(nameField.getName());
		DisplayType dt = EasyMeta.getDisplayType(nameField);
		if (dt == DisplayType.TEXT || dt == DisplayType.SERIES
				|| dt == DisplayType.EMAIL || dt == DisplayType.PHONE || dt == DisplayType.URL
				|| dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
			nameValue = nameValue.toString();
		} else if (dt == DisplayType.PICKLIST) {
			nameValue = PickListManager.instance.getLabel((ID) nameValue);
		} else if (dt == DisplayType.STATE) {
			nameValue = StateManager.instance.getLabel(nameField, (Integer) nameValue);
		} else if (dt == DisplayType.CLASSIFICATION) {
			nameValue = ClassificationManager.instance.getFullName((ID) nameValue);
		} else if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
			nameValue = CalendarUtils.getPlainDateTimeFormat().format(nameValue);
		} else {
			nameValue = null;
		}
		
		if (nameValue != null) {
			return generateQuickCode((String) nameValue);
		} else {
			return null;
		}
	}
	
	/**
	 * 你好世界 - NHSJ
	 * HelloWorld - HW
	 * hello world - HW
	 * 43284327432 - ""
	 * 
	 * @param nameVal
	 * @return
	 */
	public static String generateQuickCode(String nameVal) {
		if (StringUtils.isBlank(nameVal)) {
			return StringUtils.EMPTY;
		}
		if (nameVal.length() > 100) {
			nameVal = nameVal.substring(0, 100);
		}
		if (RegexUtils.isTel(nameVal) || RegexUtils.isEMail(nameVal) || RegexUtils.isUrl(nameVal)) {
			return StringUtils.EMPTY;
		}
		// 提取 0-9+a-z+A-Z+中文+空格，忽略特殊字符
		nameVal = nameVal.replaceAll("[^a-zA-Z0-9\\s\u4e00-\u9fa5]", "");
		// 忽略数字或小字母
		if (nameVal.matches("[a-z0-9]+")) {
			return StringUtils.EMPTY;
		}
		
		String quickCode = StringUtils.EMPTY;
		// 仅包含字母数字或空格
		if (nameVal.matches("[a-zA-Z0-9\\s]+")) {
			// 提取英文单词的首字母
			String asplit[] = nameVal.split("(?=[A-Z\\s])");
			if (asplit.length == 1) {
				quickCode = nameVal;
			} else {
				StringBuilder sb = new StringBuilder();
				for (String a : asplit) {
					if (a.trim().length() > 0) {
						sb.append(a.trim(), 0, 1);
					}
				}
				quickCode = sb.toString();
			}
		} else {
			nameVal = nameVal.replaceAll(" ", "");
			try {
				quickCode = HanLP.convertToPinyinFirstCharString(nameVal, "", false);
			} catch (Exception e) {
				LOG.error("QuickCode shorting error : " + nameVal, e);
			}
		}
		
		return quickCode.toUpperCase();
	}
}
