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

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import cn.devezhao.persist4j.record.JsonRecordCreator;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.bizz.privileges.User;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 标准 Record 解析
 *
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-26
 */
public class ExtRecordCreator extends JsonRecordCreator {

    private static final Log LOG = LogFactory.getLog(ExtRecordCreator.class);

    /**
     * 更新时。是移除不允许更新的字段还是抛出异常
     */
    private boolean strictMode;

	/**
	 * @param entity
	 * @param source
	 * @param editor
	 */
	public ExtRecordCreator(Entity entity, JSONObject source, ID editor) {
		this(entity, source, editor, false);
	}

    /**
     * @param entity
     * @param source
     * @param editor
     * @param strictMode
     */
    public ExtRecordCreator(Entity entity, JSONObject source, ID editor, boolean strictMode) {
        super(entity, source, editor);
        this.strictMode = strictMode;
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
		final User editor = Application.getUserStore().getUser(r.getEditor());
		
		if (entity.containsField(EntityHelper.ModifiedOn)) {
			r.setDate(EntityHelper.ModifiedOn, now);
		}
		if (entity.containsField(EntityHelper.ModifiedBy)) {
			r.setID(EntityHelper.ModifiedBy, (ID) editor.getIdentity());
		}
		
		if (isNew) {
			if (entity.containsField(EntityHelper.CreatedOn)) {
				r.setDate(EntityHelper.CreatedOn, now);
			}
			if (entity.containsField(EntityHelper.CreatedBy)) {
				r.setID(EntityHelper.CreatedBy, (ID) editor.getIdentity());
			}
			if (entity.containsField(EntityHelper.OwningUser)) {
				r.setID(EntityHelper.OwningUser, (ID) editor.getIdentity());
			}
			if (entity.containsField(EntityHelper.OwningDept)) {
				r.setID(EntityHelper.OwningDept, (ID) editor.getOwningDept().getIdentity());
			}
		}
	}
	
	@Override
	public void verify(Record record, boolean isNew) {
        List<String> notAllowed = new ArrayList<>();
        // 新建
        if (isNew) {
            for (Field field : entity.getFields()) {
                if (MetadataHelper.isSystemField(field)) {
                    continue;
                }
                EasyMeta easy = EasyMeta.valueOf(field);
                if (easy.getDisplayType() == DisplayType.SERIES) {
                    continue;
                }

                Object hasVal = record.getObjectValue(field.getName());
                if ((hasVal == null || NullValue.is(hasVal)) && !field.isNullable()) {
                    notAllowed.add(easy.getLabel());
                }
            }

            if (!notAllowed.isEmpty()) {
                throw new DataSpecificationException(StringUtils.join(notAllowed, "/") + " 不允许为空");
            }
		}
        // 更新
        else {
            for (String fieldName : record.getAvailableFields()) {
                Field field = record.getEntity().getField(fieldName);
                if (EntityHelper.ModifiedOn.equalsIgnoreCase(fieldName)
                    || EntityHelper.ModifiedBy.equalsIgnoreCase(fieldName)
                    || field.getType() == FieldType.PRIMARY) {
                    continue;
                }

                EasyMeta easy = EasyMeta.valueOf(field);
                if (!easy.isUpdatable()) {
                    if (strictMode) {
                        notAllowed.add(easy.getLabel());
                    } else {
                        record.removeValue(fieldName);
                        LOG.warn("Remove non-updatable field : " + fieldName);
                    }
                }
            }

            if (!notAllowed.isEmpty()) {
                throw new DataSpecificationException(StringUtils.join(notAllowed, "/") + " 不允许修改");
            }
        }
	}
}