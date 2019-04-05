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

package com.rebuild.server.portals.datalist;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.server.portals.value.FieldValueWrapper;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.query.compiler.SelectItem;

/**
 * 数据包装
 * 
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-20
 */
public class DataWrapper extends FieldValueWrapper {
	
	private static final Log LOG = LogFactory.getLog(DataWrapper.class);

	private int total;
	private Object[][] data;
	
	private SelectItem[] selectFields;
	private Entity entity;
	
	/**
	 * @param total
	 * @param data
	 * @param selectFields
	 * @param entity
	 */
	public DataWrapper(int total, Object[][] data, SelectItem[] selectFields, Entity entity) {
		this.total = total;
		this.data = data;
		this.selectFields = selectFields;
		this.entity = entity;
	}
	
	/**
	 * @return
	 */
	public JSON toJson() {
		final Field namedFiled = MetadataHelper.getNameField(entity);
		for (Object[] row : data) {
			Object namedVal = null;
			for (int i = 0; i < selectFields.length; i++) {
				if (row[i] == null) {
					row[i] = StringUtils.EMPTY;
					continue;
				}
				
				Field field = selectFields[i].getField();
				if (field.equals(namedFiled)) {
					namedVal = row[i];
				}
				
				if (field.getType() == FieldType.REFERENCE) {
					int ec = field.getReferenceEntity().getEntityCode();
					if (ec == EntityHelper.ClassificationData || ec == EntityHelper.PickList) {
						row[i] = wrapFieldValue(row[i], EasyMeta.valueOf(field));
					} else {
						row[i] = readReferenceValue((ID) row[i], null);
					}
				} else if (field.getType() == FieldType.PRIMARY) {  // Last index always
					row[i] = readReferenceValue((ID) row[i], namedVal);
				} else {
					row[i] = wrapFieldValue(row[i], new EasyMeta(field));
				}
			}
		}
		
		return JSONUtils.toJSONObject(
				new String[] { "total", "data" },
				new Object[] { total, data });
	}
	
	/**
	 * 读取 ID 型字段
	 * 
	 * @param idVal
	 * @param nameVal
	 * @return
	 */
	protected Object[] readReferenceValue(ID idVal, Object nameVal) {
		Entity entity = MetadataHelper.getEntity(idVal.getEntityCode());
		Field nameField = MetadataHelper.getNameField(entity);
		
		if (nameVal == null) {
			String sql = String.format("select %s from %s where %s = ?",
					(nameField.getType() == FieldType.REFERENCE ? "&" : "") + nameField.getName(),
					entity.getName(), entity.getPrimaryField().getName());
			Object[] named = Application.createQueryNoFilter(sql).setParameter(1, idVal).unique();
			if (named == null) {
				LOG.debug("Reference is deleted : " + idVal);
				return null;
			}
			nameVal = named[0];
		}
		
		nameVal = wrapFieldValue(nameVal, new EasyMeta(nameField));
		String[] metadata = new String[] { entity.getName(), new EasyMeta(entity).getIcon() };
		return new Object[] { idVal, nameVal, metadata };
	}
}
