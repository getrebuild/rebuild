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

package com.rebuild.server.metadata;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MetadataException;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.entity.EasyMeta;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体元数据
 *
 * @author zhaofang123@gmail.com
 * @since 08/13/2018
 */
public class MetadataHelper {

	private static final Log LOG = LogFactory.getLog(MetadataHelper.class);

	/**
	 * 元数据
	 *
	 * @return
	 */
	public static DynamicMetadataFactory getMetadataFactory() {
		return (DynamicMetadataFactory) Application.getPersistManagerFactory().getMetadataFactory();
	}

	/**
	 * @return
	 */
	public static Entity[] getEntities() {
		return getMetadataFactory().getEntities();
	}

	/**
	 * @param entityName
	 * @return
	 */
	public static boolean containsEntity(String entityName) {
		if (StringUtils.isBlank(entityName)) {
			return false;
		}
		try {
			getEntity(entityName);
			return true;
		} catch (MetadataException ex) {
			return false;
		}
	}

	/**
	 * @param entityCode
	 * @return
	 */
	public static boolean containsEntity(int entityCode) {
		try {
			getEntity(entityCode);
			return true;
		} catch (MetadataException ex) {
			return false;
		}
	}

	/**
	 * @param entityName
	 * @param fieldName
	 * @return
	 */
	public static boolean containsField(String entityName, String fieldName) {
		try {
			return getEntity(entityName).containsField(fieldName);
		} catch (MetadataException ex) {
			return false;
		}
	}

	/**
	 * @param entityName
	 * @return
	 */
	public static Entity getEntity(String entityName) {
		return getMetadataFactory().getEntity(entityName);
	}

	/**
	 * @param entityCode
	 * @return
	 */
	public static Entity getEntity(int entityCode) {
		return getMetadataFactory().getEntity(entityCode);
	}

	/**
	 * @param record
	 * @return
	 */
	public static String getEntityName(ID record) {
		return getEntity(record.getEntityCode()).getName();
	}

    /**
     * @param record
     * @return
     */
    public static String getEntityLabel(ID record) {
	    return EasyMeta.getLabel(getEntity(record.getEntityCode()));
    }

	/**
	 * @param entityName
	 * @param fieldName
	 * @return
	 */
	public static Field getField(String entityName, String fieldName) {
		Entity entity = getEntity(entityName);
		return entity.getField(fieldName);
	}

	/**
	 * {@link Entity#getNameField()} 有可能返回空，应优先使用此方法
	 *
	 * @param entity
	 * @return
	 */
	public static Field getNameField(Entity entity) {
		Field hasName = entity.getNameField();
		if (hasName != null) {
			return hasName;
		}
		if (entity.containsField(EntityHelper.CreatedOn)) {
			return entity.getField(EntityHelper.CreatedOn);
		}
		return entity.getPrimaryField();
	}

	/**
	 * {@link Entity#getNameField()} 有可能返回空，应优先使用此方法
	 *
	 * @param entity
	 * @return
	 */
	public static Field getNameField(String entity) {
		return getNameField(getEntity(entity));
	}

	/**
	 * <tt>reference</tt> 中的哪些字段引用了 <tt>source</tt>
	 *
	 * @param source
	 * @param reference
	 * @return
	 */
	public static Field[] getReferenceToFields(Entity source, Entity reference) {
		List<Field> fields = new ArrayList<>();
		for (Field field : reference.getFields()) {
			if (field.getType() != FieldType.REFERENCE) {
				continue;
			}

			Entity ref = field.getReferenceEntities()[0];
			if (ref.getEntityCode().equals(source.getEntityCode())) {
				fields.add(field);
			}
		}
		return fields.toArray(new Field[0]);
	}

	/**
	 * 仅供系统使用的字段，用户不可见/不可用
	 *
	 * @param field
	 * @return
	 */
	public static boolean isSystemField(Field field) {
		return isSystemField(field.getName()) || field.getType() == FieldType.PRIMARY;
	}

	/**
	 * 仅供系统使用的字段，用户不可见/不可用
	 *
	 * @param fieldName
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static boolean isSystemField(String fieldName) {
		return EntityHelper.AutoId.equalsIgnoreCase(fieldName)
				|| EntityHelper.QuickCode.equalsIgnoreCase(fieldName)
				|| EntityHelper.IsDeleted.equalsIgnoreCase(fieldName)
				|| EntityHelper.ApprovalStepNode.equalsIgnoreCase(fieldName);
	}

	/**
	 * 是否公共字段
	 *
	 * @param field
	 * @return
	 * @see #isSystemField(Field)
	 * @see EntityHelper
	 */
	public static boolean isCommonsField(Field field) {
		if (isSystemField(field)) {
			return true;
		}
		return isCommonsField(field.getName());
	}

	/**
	 * 是否公共字段
	 *
	 * @param fieldName
	 * @return
	 * @see #isSystemField(Field)
	 * @see EntityHelper
	 */
	public static boolean isCommonsField(String fieldName) {
		if (isSystemField(fieldName) || isApprovalField(fieldName)) {
			return true;
		}
		return EntityHelper.OwningUser.equalsIgnoreCase(fieldName) || EntityHelper.OwningDept.equalsIgnoreCase(fieldName)
				|| EntityHelper.CreatedOn.equalsIgnoreCase(fieldName) || EntityHelper.CreatedBy.equalsIgnoreCase(fieldName)
				|| EntityHelper.ModifiedOn.equalsIgnoreCase(fieldName) || EntityHelper.ModifiedBy.equalsIgnoreCase(fieldName);
	}

	/**
	 * 是否审批流程字段
	 *
	 * @param fieldName
	 * @return
	 */
	public static boolean isApprovalField(String fieldName) {
		return EntityHelper.ApprovalId.equalsIgnoreCase(fieldName)
				|| EntityHelper.ApprovalState.equalsIgnoreCase(fieldName)
				|| EntityHelper.ApprovalStepNode.equalsIgnoreCase(fieldName);
	}

	/**
	 * 是否 Bizz 实体
	 *
	 * @param entityCode
	 * @return
	 */
	public static boolean isBizzEntity(int entityCode) {
		return entityCode == EntityHelper.User || entityCode == EntityHelper.Department || entityCode == EntityHelper.Role;
	}

	/**
	 * 是否 Bizz 实体
	 *
	 * @param entityName
	 * @return
	 */
	public static boolean isBizzEntity(String entityName) {
		return "User".equalsIgnoreCase(entityName) || "Role".equalsIgnoreCase(entityName) || "Department".equalsIgnoreCase(entityName);
	}

	/**
	 * 实体是否具备权限字段（业务实体）
	 *
	 * @param entity
	 * @return
	 */
	public static boolean hasPrivilegesField(Entity entity) {
		return  entity.containsField(EntityHelper.OwningUser) && entity.containsField(EntityHelper.OwningDept);
	}

	/**
	 * 获取明细实体哪个字段引用自主实体
	 *
	 * @param slave
	 * @return
	 */
	public static Field getSlaveToMasterField(Entity slave) {
		Entity master = slave.getMasterEntity();
		Assert.isTrue(master != null, "Non slave entity");

		for (Field field : slave.getFields()) {
			if (field.getType() != FieldType.REFERENCE) {
				continue;
			}
			// 内建的那个才是，因为明细的其他字段也可能引用主实体
			if (master.equals(field.getReferenceEntity()) && EasyMeta.valueOf(field).isBuiltin()) {
				return field;
			}
		}
		return null;
	}

	/**
	 * 是否主实体
	 *
	 * @param entityCode
	 * @return
	 */
	public static boolean isMasterEntity(int entityCode) {
		return getEntity(entityCode).getSlaveEntity() != null;
	}

	/**
	 * 是否明细实体
	 *
	 * @param entityCode
	 * @return
	 */
	public static boolean isSlaveEntity(int entityCode) {
		return getEntity(entityCode).getMasterEntity() != null;
	}

	/**
	 * 点连接字段（如 owningUser.loginName），获取最后一个字段。
	 * 此方法也可以用来判断点连接字段是否是有效的字段
	 *
	 * @param entity
	 * @param fieldPath
	 * @return
	 */
	public static Field getLastJoinField(Entity entity, String fieldPath) {
		String[] paths = fieldPath.split("\\.");
		if (fieldPath.charAt(0) == '&') {
			paths[0] = paths[0].substring(1);
			if (!entity.containsField(paths[0])) {
				return null;
			}
		}

		Field lastField = null;
		Entity father = entity;
		for (String field : paths) {
			if (father != null && father.containsField(field)) {
				lastField = father.getField(field);
				if (lastField.getType() == FieldType.REFERENCE) {
					father = lastField.getReferenceEntity();
				} else {
					father = null;
				}
			} else {
				return null;
			}
		}
		return lastField;
	}

	/**
	 * 检查字段有效性（无效会 LOG）
	 *
	 * @param entity
	 * @param fieldName
	 * @return
	 */
	public static boolean checkAndWarnField(Entity entity, String fieldName) {
		if (entity.containsField(fieldName)) {
			return true;
		}
		LOG.warn("Unknow field '" + fieldName + "' in '" + entity + "'");
		return false;
	}

	/**
	 * 检查字段有效性（无效会 LOG）
	 *
	 * @param entityName
	 * @param fieldName
	 * @return
	 */
	public static boolean checkAndWarnField(String entityName, String fieldName) {
		if (!containsEntity(entityName)) {
			return false;
		}
		return checkAndWarnField(getEntity(entityName), fieldName);
	}
}
