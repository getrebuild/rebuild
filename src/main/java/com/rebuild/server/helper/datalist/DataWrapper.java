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

package com.rebuild.server.helper.datalist;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.query.compiler.SelectItem;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.FieldValueWrapper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * 数据包装
 * 
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-20
 */
public class DataWrapper {
	
	private static final Log LOG = LogFactory.getLog(DataWrapper.class);
	
	private static final String NO_READ_PRIVILEGES = "$NOPRIVILEGES$";

	private int total;
	private Object[][] data;
	
	private SelectItem[] selectFields;
	private Entity entity;
	
	// for 权限验证
	private ID user;
	private Map<String, Integer> queryJoinFields;
	
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
	 * @param user
	 * @param joinFields
	 */
	protected void setPrivilegesFilter(ID user, Map<String, Integer> joinFields) {
		if (user != null && joinFields != null && !joinFields.isEmpty()) {
			this.user = user;
			this.queryJoinFields = joinFields;
		}
	}
	
	/**
	 * @return
	 */
	public JSON toJson() {
		final Field namedFiled = MetadataHelper.getNameField(entity);
		final int joinFieldsLen = queryJoinFields == null ? 0 : queryJoinFields.size();
		final int selectFieldsLen = selectFields.length - joinFieldsLen;
		
		for (int i = 0; i < data.length; i++) {
			final Object[] original = data[i];

			Object[] row = original;
			if (joinFieldsLen > 0) {
				row = new Object[selectFieldsLen];
				System.arraycopy(original, 0, row, 0, selectFieldsLen);
				data[i] = row;
			}
			
			Object namedVal = null;
			for (int j = 0; j < selectFieldsLen; j++) {
				if (!checkHasJoinFieldPrivileges(selectFields[j], original)) {
					row[j] = NO_READ_PRIVILEGES;
					continue;
				}
				
				if (row[j] == null) {
					row[j] = StringUtils.EMPTY;
					continue;
				}
				
				Field field = selectFields[j].getField();
				if (field.equals(namedFiled)) {
					namedVal = row[j];
				}
				
				if (field.getType() == FieldType.REFERENCE) {
					int rec = field.getReferenceEntity().getEntityCode();
					if (rec == EntityHelper.ClassificationData || rec == EntityHelper.PickList) {
						row[j] = FieldValueWrapper.instance.wrapFieldValue(row[j], EasyMeta.valueOf(field));
					} else {
						row[j] = readReferenceRich((ID) row[j], null);
					}
				} else if (field.getType() == FieldType.PRIMARY) {  // Last index always
					row[j] = readReferenceRich((ID) row[j], namedVal);
				} else {
					row[j] = FieldValueWrapper.instance.wrapFieldValue(row[j], new EasyMeta(field));
				}
			}
		}
		
		return JSONUtils.toJSONObject(
				new String[] { "total", "data" },
				new Object[] { total, data });
	}
	
	/**
	 * 读取引用型字段
	 * 
	 * @param idVal
	 * @param nameVal
	 * @return Returns [ID, Name(Field), EntityMeta[Name, Icon]]
	 */
	private Object[] readReferenceRich(ID idVal, Object nameVal) {
		Entity entity = MetadataHelper.getEntity(idVal.getEntityCode());
		Field nameField = MetadataHelper.getNameField(entity);
		
		if (nameVal == null) {
			String sql = String.format("select %s from %s where %s = ?",
					nameField.getName(), entity.getName(), entity.getPrimaryField().getName());
			Object[] named = Application.createQueryNoFilter(sql).setParameter(1, idVal).unique();
			if (named == null) {
				LOG.debug("Reference is deleted : " + idVal);
				return null;
			}
			nameVal = named[0];
		}
		
		nameVal = FieldValueWrapper.instance.wrapFieldValue(nameVal, new EasyMeta(nameField));
		String[] metadata = new String[] { entity.getName(), new EasyMeta(entity).getIcon() };
		return new Object[] { idVal, nameVal, metadata };
	}
	
	/**
	 * 验证字段权限
	 * 
	 * @param field
	 * @param original
	 * @return
	 */
	private boolean checkHasJoinFieldPrivileges(SelectItem field, Object[] original) {
		if (this.queryJoinFields == null) {
			return true;
		}
		
		String fieldPath[] = field.getFieldPath().split("\\.");
		if (fieldPath.length == 1) {
			return true;
		}
		
		int fieldIndex = queryJoinFields.get(fieldPath[0]);
		Object check = original[fieldIndex];
		return check == null || Application.getSecurityManager().allowedR(user, (ID) check);
	}
}
