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

package com.rebuild.server.metadata;

import java.util.Date;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.bizz.UserService;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.record.JSONRecordCreator;

/**
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-26
 */
public class ExtRecordCreator extends JSONRecordCreator {
	
	/**
	 * @param entity
	 * @param source
	 * @param editor
	 */
	public ExtRecordCreator(Entity entity, JSONObject source, ID editor) {
		super(entity, source, editor);
	}
	
	@Override
	protected void afterCreate(Record record, boolean isNew) {
		super.afterCreate(record, isNew);
		bindCommonsFieldsValue(record, isNew);
	}
	
	/**
	 * 绑定公用/权限字段值
	 * 
	 * @param r
	 * @param isNew
	 */
	protected static void bindCommonsFieldsValue(Record r, boolean isNew) {
		final Date now = CalendarUtils.now();
		final Entity entity = r.getEntity();
		
		if (entity.containsField(EntityHelper.modifiedOn)) {
			r.setDate(EntityHelper.modifiedOn, now);
		}
		if (entity.containsField(EntityHelper.modifiedBy)) {
			r.setID(EntityHelper.modifiedBy, r.getEditor());
		}
		
		if (isNew) {
			if (entity.containsField(EntityHelper.createdOn)) {
				r.setDate(EntityHelper.createdOn, now);
			}
			if (entity.containsField(EntityHelper.createdBy)) {
				r.setID(EntityHelper.createdBy, r.getEditor());
			}
			if (entity.containsField(EntityHelper.owningUser)) {
				r.setID(EntityHelper.owningUser, r.getEditor());
			}
			if (entity.containsField(EntityHelper.owningDept)) {
				r.setID(EntityHelper.owningDept, Application.getBean(UserService.class).getDeptOfUser(r.getEditor()));
			}
		}
	}
}