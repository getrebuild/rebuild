/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.configuration.portals;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MetadataException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.approval.ApprovalState;
import com.rebuild.server.helper.cache.NoRecordFoundException;
import com.rebuild.server.helper.state.StateHelper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.metadata.entity.FieldExtConfigProps;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.text.DecimalFormat;

/**
 * 字段值包装。例如 BOOL 类型的 T/F 将格式化为 是/否。
 * 表单/视图/列表等均调用此类，仅在处理特定情景下的特定字段时才需要特殊处理
 * 
 * @author zhaofang123@gmail.com
 * @since 09/23/2018
 */
@SuppressWarnings("unused")
public class FieldValueWrapper {

	/**
	 * 引用值被删除时的默认显示
	 */
	public static final String MISS_REF_PLACE = "[DELETED]";
	/**
	 * 流程未提交
	 */
	public static final String APPROVAL_UNSUBMITTED = "未提交";
	/**
	 * 名称字段为空时，采用 @+ID 的方式显示
	 */
	public static final String NO_LABEL_PREFIX = "@";

	public static final FieldValueWrapper instance = new FieldValueWrapper();
	private FieldValueWrapper() {}

    /**
     * @param value
     * @param field
     * @param unpackMix
     * @return
     * @see #wrapFieldValue(Object, EasyMeta, boolean)
     */
    public Object wrapFieldValue(Object value, Field field, boolean unpackMix) {
        return wrapFieldValue(value, EasyMeta.valueOf(field), unpackMix);
    }

    /**
     * @param value
     * @param field
     * @param unpackMix
     * @return
     * @see #wrapFieldValue(Object, EasyMeta)
     */
    public Object wrapFieldValue(Object value, EasyMeta field, boolean unpackMix) {
        value = wrapFieldValue(value, field);
        if (unpackMix && value != null) {
            DisplayType dt = field.getDisplayType();
            if (value instanceof JSON && (dt == DisplayType.CLASSIFICATION || dt == DisplayType.REFERENCE)) {
                return ((JSONObject) value).getString("text");
            } else if (dt == DisplayType.FILE || dt == DisplayType.IMAGE) {
                return value.toString();
            }
        }
        return value;
    }
	
	/**
     * `REFERENCE` 和 `CLASSIFICATION` 返回复合值
     * `FILE` 和 `IMAGE` 返回 JSONArray
     * 其他返回格式化后的值
     *
	 * @param value
	 * @param field
	 * @return
	 */
	public Object wrapFieldValue(Object value, EasyMeta field) {
		Object specialVal = wrapSpecialField(value, field);
		if (specialVal != null) {
			return specialVal;
		}

		if (value == null || StringUtils.isBlank(value.toString())) {
			return StringUtils.EMPTY;
		}

		DisplayType dt = field.getDisplayType();
		if (dt == DisplayType.DATE) {
			return wrapDate(value, field);
		} else if (dt == DisplayType.DATETIME) {
			return wrapDatetime(value, field);
		} else if (dt == DisplayType.NUMBER) {
			return wrapNumber(value, field);
		} else if (dt == DisplayType.DECIMAL) {
			return wrapDecimal(value, field);
		} else if (dt == DisplayType.REFERENCE) {
            return wrapReference(value, field);
        } else if (dt == DisplayType.BOOL) {
			return wrapBool(value, field);
		} else if (dt == DisplayType.PICKLIST) {
			return wrapPickList(value, field);
		} else if (dt == DisplayType.STATE) {
            return wrapState(value, field);
        } else if (dt == DisplayType.CLASSIFICATION) {
			return wrapClassification(value, field);
		} else if (dt == DisplayType.MULTISELECT) {
			return wrapMultiSelect(value, field);
		} else if (dt == DisplayType.IMAGE || dt == DisplayType.FILE) {
            return wrapFile(value, field);
        } else if (dt == DisplayType.AVATAR || dt == DisplayType.LOCATION) {
            return value;
        } else {
			return wrapSimple(value, field);
		}
	}
	
	/**
	 * @param value
	 * @param field
	 * @return
	 */
	public String wrapDate(Object value, EasyMeta field) {
		String format = field.getExtraAttr(FieldExtConfigProps.DATE_DATEFORMAT);
		format = StringUtils.defaultIfEmpty(format, field.getDisplayType().getDefaultFormat());
		return CalendarUtils.getDateFormat(format).format(value);
	}

	/**
	 * @param value
	 * @param field
	 * @return
	 */
	public String wrapDatetime(Object value, EasyMeta field) {
		String format = field.getExtraAttr(FieldExtConfigProps.DATETIME_DATEFORMAT);
		format = StringUtils.defaultIfEmpty(format, field.getDisplayType().getDefaultFormat());
		return CalendarUtils.getDateFormat(format).format(value);
	}
	
	/**
	 * @param value
	 * @param field
	 * @return
	 */
	public String wrapNumber(Object value, EasyMeta field) {
		String format = field.getExtraAttr(FieldExtConfigProps.NUMBER_FORMAT);
		format = StringUtils.defaultIfEmpty(format, field.getDisplayType().getDefaultFormat());
		return new DecimalFormat(format).format(value);
	}

	/**
	 * @param value
	 * @param field
	 * @return
	 */
	public String wrapDecimal(Object value, EasyMeta field) {
		String format = field.getExtraAttr(FieldExtConfigProps.DECIMAL_FORMAT);
		format = StringUtils.defaultIfEmpty(format, field.getDisplayType().getDefaultFormat());
		return new DecimalFormat(format).format(value);
	}

	/**
	 * @param value
	 * @param field
	 * @return
     * @see #wrapMixValue(ID, String)
	 */
	public JSON wrapReference(Object value, EasyMeta field) {
	    Object text = ((ID) value).getLabelRaw();
	    if (text == null) {
            text = getLabelNotry((ID) value);

        } else {
	        Field nameField = ((Field) field.getBaseMeta()).getReferenceEntity().getNameField();
	        text = instance.wrapFieldValue(text, nameField, true);
        }

	    return wrapMixValue((ID) value, text == null ? null : text.toString());
	}
	
	/**
	 * @param value
	 * @param field
	 * @return
	 */
	public String wrapBool(Object value, EasyMeta field) {
		return (Boolean) value ? "是" : "否";
	}
	
	/**
	 * @param value
	 * @param field
	 * @return
	 * @see PickListManager
	 */
	public String wrapPickList(Object value, EasyMeta field) {
		return StringUtils.defaultIfBlank(PickListManager.instance.getLabel((ID) value), MISS_REF_PLACE);
	}

    /**
     * @param value
     * @param field
     * @return
     */
    public String wrapState(Object value, EasyMeta field) {
        String stateClass = field.getExtraAttr(FieldExtConfigProps.STATE_STATECLASS);
        return StateHelper.valueOf(stateClass, (Integer) value).getName();
    }
	
	/**
	 * @param value
	 * @param field
	 * @return
	 * @see ClassificationManager
	 */
	public JSON wrapClassification(Object value, EasyMeta field) {
	    ID id = (ID) value;
	    String text = StringUtils.defaultIfBlank(ClassificationManager.instance.getFullName(id), MISS_REF_PLACE);
		return wrapMixValue(id, text);
	}

	/**
	 * @param value
	 * @param field
	 * @return
	 * @see MultiSelectManager
	 */
	public String wrapMultiSelect(Object value, EasyMeta field) {
		if ((Long) value <= 0) {
			return StringUtils.EMPTY;
		}
		String[] multiLabel = MultiSelectManager.instance.getLabel((Long) value, (Field) field.getBaseMeta());
		return StringUtils.join(multiLabel, " / ");
	}

    /**
     * @param value
     * @param field
     * @return
     */
	public JSON wrapFile(Object value, EasyMeta field) {
	    return JSON.parseArray(value.toString());
    }
	
	/**
	 * @param value
	 * @param field
	 * @return
	 */
	public String wrapSimple(Object value, EasyMeta field) {
		String text = value.toString().trim();
		if (StringUtils.isBlank(text)) {
			return StringUtils.EMPTY;
		} else {
			return text;
		}
	}

	/**
     * 特殊字段处理
     *
	 * @param value
	 * @param field
	 * @return
	 */
	protected Object wrapSpecialField(Object value, EasyMeta field) {
		if (!field.isQueryable()) {
			return "******";
		}

		// 审批
		if (field.getName().equalsIgnoreCase(EntityHelper.ApprovalState)) {
			if (value == null) {
				return ApprovalState.DRAFT.getName();
			} else {
    			return ApprovalState.valueOf((Integer) value).getName();
            }

		} else if (field.getName().equalsIgnoreCase(EntityHelper.ApprovalId) && value == null) {
		    return wrapMixValue(null, APPROVAL_UNSUBMITTED);
        }
		
		return null;
	}
	
	// --
	
	/**
	 * 获取记录的 NAME/LABEL 字段值
	 * 
	 * @param id
	 * @param defaultValue
	 * @return
	 * @throws NoRecordFoundException If no record found
	 */
	public static String getLabel(ID id, String defaultValue) throws NoRecordFoundException {
		if (id == null) {
			throw new NoRecordFoundException("[id] must not be null");
		}
		
		Entity entity = MetadataHelper.getEntity(id.getEntityCode());

		if (id.getEntityCode() == EntityHelper.ClassificationData) {
			String hasValue = ClassificationManager.instance.getFullName(id);
			if (hasValue == null) {
				throw new NoRecordFoundException("No ClassificationData found by ID : " + id);
			}
			return hasValue;
		} else if (id.getEntityCode() == EntityHelper.PickList) {
			String hasValue = PickListManager.instance.getLabel(id);
			if (hasValue == null) {
				throw new NoRecordFoundException("No PickList found by ID : " + id);
			}
			return hasValue;
		}

		Field nameField = MetadataHelper.getNameField(entity);
		Object[] nameValue = Application.getQueryFactory().uniqueNoFilter(id, nameField.getName());
		if (nameValue == null) {
            throw new NoRecordFoundException("No record found by ID : " + id);
        }

		Object nameLabel = instance.wrapFieldValue(nameValue[0], nameField, true);
		if (nameLabel == null || StringUtils.isBlank(nameLabel.toString())) {
		    if (defaultValue == null) {
                defaultValue = NO_LABEL_PREFIX + id.toLiteral().toUpperCase();
            }
			return defaultValue;
		}
		return nameLabel.toString();
	}

    /**
     * @param id
     * @return
     * @throws NoRecordFoundException
     */
	public static String getLabel(ID id) throws NoRecordFoundException {
        return getLabel(id, null);
    }

	/**
	 * @param id
	 * @return
     * @see #getLabel(ID)
	 */
	public static String getLabelNotry(ID id) {
		try {
			return getLabel(id);
		} catch (MetadataException | NoRecordFoundException ex) {
			return MISS_REF_PLACE;
		}
	}

    /**
     * @param id
     * @param text
     * @return Returns `{ id:xxx, text:xxx, entity:xxx }`
     */
	public static JSONObject wrapMixValue(ID id, String text) {
        if (id != null && StringUtils.isBlank(text)) {
            text = id.getLabel();
        }

        JSONObject o = JSONUtils.toJSONObject(new String[] { "id", "text" }, new Object[] { id, text } );
        if (id != null) {
            o.put("entity", MetadataHelper.getEntityName(id));
        }
        return o;
    }
}
