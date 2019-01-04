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

package com.rebuild.server.service.base;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.github.stuxuhai.jpinyin.PinyinHelper;
import com.rebuild.server.Application;
import com.rebuild.server.helper.manager.PickListManager;
import com.rebuild.server.helper.task.BulkTask;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.server.service.bizz.UserService;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * QuickCode 字段值重建
 * 
 * @author devezhao
 * @since 12/28/2018
 */
public class QuickCodeReindexTask extends BulkTask {
	
	final private Entity entity;

	/**
	 * @param entity
	 */
	public QuickCodeReindexTask(Entity entity) {
		super();
		this.entity = entity;
	}
	
	@Override
	public void run() {
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
				if (this.isInterrupted()) {
					break;
				}
				this.setCompleteOne();
				
				String quickCodeNew = generateQuickCode(o);
				if (quickCodeNew == null) {
					continue;
				}
				if (quickCodeNew.equals(o.getString(EntityHelper.QuickCode))) {
					continue;
				}
				
				Record record = EntityHelper.forUpdate(o.getPrimary(), UserService.SYSTEM_USER);
				if (StringUtils.isBlank(quickCodeNew)) {
					record.setNull(EntityHelper.QuickCode);
				} else {
					record.setString(EntityHelper.QuickCode, quickCodeNew);
				}
				record.removeValue(EntityHelper.ModifiedBy);
				record.removeValue(EntityHelper.ModifiedOn);
				Application.getCommonService().update(record);
			}
			
			if (records.size() < 1000 || this.isInterrupted()) {
				break;
			}
		}
		
		this.setTotal(this.getTotal() - 1);
		completedAfter();
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
		if (!record.hasValue(nameField.getName())) {
			return null;
		}
		
		DisplayType dt = EasyMeta.getDisplayType(nameField);
		String nameVal = null;
		if (dt == DisplayType.TEXT) {
			nameVal = record.getString(nameField.getName());
		} else if (dt == DisplayType.PICKLIST) {
			ID plid = record.getID(nameField.getName());
			nameVal = PickListManager.getLabel(plid);
		}
		
		if (nameVal != null) {
			return generateQuickCode(nameVal);
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
		
		// 提取 0-9+a-z+A-Z+中文+空格
		nameVal = nameVal.replaceAll("[^a-zA-Z0-9\\s\u4e00-\u9fa5]", "");
		
		String quickCode = StringUtils.EMPTY;
		if (nameVal.matches("[a-zA-Z0-9\\s]+")) {
			String aplit[] = nameVal.split("(?=[A-Z\\s])");
			StringBuffer sb = new StringBuffer();
			for (String a : aplit) {
				if (a.trim().length() > 0) {
					sb.append(a.trim().substring(0, 1));
				}
			}
			quickCode = sb.toString();
		} else {
			nameVal = nameVal.replaceAll(" ", "");
			try {
				quickCode = PinyinHelper.getShortPinyin(nameVal);
			} catch (Exception e) {
				LOG.error("QuickCode shorting error : " + nameVal, e);
			}
		}
		
		if (NumberUtils.isDigits(quickCode)) {
			quickCode = StringUtils.EMPTY;
		}
		return quickCode.toUpperCase();
	}
}
