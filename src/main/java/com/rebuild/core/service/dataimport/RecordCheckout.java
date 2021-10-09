/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dataimport;

import cn.devezhao.commons.excel.Cell;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.*;
import com.rebuild.core.metadata.impl.MetadataModificationException;
import com.rebuild.core.support.state.StateManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 从 Cell[] 中解析结果 Record
 *
 * @author devezhao
 * @since 2019/12/4
 */
@Slf4j
public class RecordCheckout {

    final private Map<Field, Integer> fieldsMapping;

    /**
     * @param fieldsMapping
     */
    protected RecordCheckout(Map<Field, Integer> fieldsMapping) {
        this.fieldsMapping = fieldsMapping;
    }

    /**
     * @param row
     * @return
     */
    public Record checkout(Record record, Cell[] row) {
        for (Map.Entry<Field, Integer> e : this.fieldsMapping.entrySet()) {
            int cellIndex = e.getValue();
            if (cellIndex > row.length) continue;

            Field field = e.getKey();
            Cell cellValue = row[cellIndex];
            Object value = checkoutFieldValue(field, cellValue, true);

            if (value != null) {
                record.setObjectValue(field.getName(), value);
            } else if (cellValue != Cell.NULL && !cellValue.isEmpty()) {
                log.warn("Invalid value of cell : " + cellValue + " > " + field.getName());
            }
        }
        return record;
    }

    /**
     * @param field
     * @param cell
     * @param validate 验证格式，如邮箱/URL等
     * @return
     */
    protected Object checkoutFieldValue(Field field, Cell cell, boolean validate) {
        final DisplayType dt = EasyMetaFactory.getDisplayType(field);
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
        } else if (dt == DisplayType.MULTISELECT) {
            return checkoutMultiSelectValue(field, cell);
        }

        String string = cell.asString();
        if (string != null) string = string.trim();

        // 格式验证
        if (validate) {
            if (dt == DisplayType.EMAIL) {
                string = cell.asString();
                return EasyEmail.isEmail(string) ? string : null;
            } else if (dt == DisplayType.URL) {
                string = cell.asString();
                return EasyUrl.isUrl(string) ? string : null;
            } else if (dt == DisplayType.PHONE) {
                string = cell.asString();
                return EasyPhone.isPhone(string) ? string : null;
            }
        }

        return string;
    }

    /**
     * @param field
     * @param cell
     * @return
     * @see PickListManager
     */
    protected ID checkoutPickListValue(Field field, Cell cell) {
        String val = cell.asString();
        if (StringUtils.isBlank(val)) return null;

        // 支持ID
        if (ID.isId(val) && ID.valueOf(val).getEntityCode() == EntityHelper.PickList) {
            ID iid = ID.valueOf(val);
            if (PickListManager.instance.getLabel(iid) != null) {
                return iid;
            } else {
                log.warn("No item of PickList found by ID : " + iid);
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
     * @see StateManager
     */
    protected Integer checkoutStateValue(Field field, Cell cell) {
        final String val = cell.asString();
        if (StringUtils.isBlank(val)) {
            return null;
        }

        try {
            return StateManager.instance.findState(field, val).getState();
        } catch (MetadataModificationException ignored) {
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
     * @see ClassificationManager
     */
    protected ID checkoutClassificationValue(Field field, Cell cell) {
        String val = cell.asString();
        if (StringUtils.isBlank(val)) return null;

        // 支持ID
        if (ID.isId(val) && ID.valueOf(val).getEntityCode() == EntityHelper.ClassificationData) {
            ID iid = ID.valueOf(val);
            if (ClassificationManager.instance.getName(iid) != null) {
                return iid;
            } else {
                log.warn("No item of Classification found by ID : " + iid);
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
    protected ID checkoutReferenceValue(Field field, Cell cell) {
        String val = cell.asString();
        if (StringUtils.isBlank(val)) return null;

        final Entity refEntity = field.getReferenceEntity();

        // 支持ID导入
        if (ID.isId(val) && ID.valueOf(val).getEntityCode().intValue() == refEntity.getEntityCode()) {
            return ID.valueOf(val);
        }

        Object val2Text = checkoutFieldValue(refEntity.getNameField(), cell, false);
        if (val2Text == null) {
            return null;
        }

        Query query;
        // 用户特殊处理
        if (refEntity.getEntityCode() == EntityHelper.User) {
            String sql = MessageFormat.format(
                    "select userId from User where loginName = ''{0}'' or email = ''{0}'' or fullName = ''{0}''",
                    StringEscapeUtils.escapeSql(val2Text.toString()));
            query = Application.createQueryNoFilter(sql);
        } else {
            // 查找引用实体的名称字段和自动编号字段
            Set<String> queryFields = new HashSet<>();
            queryFields.add(refEntity.getNameField().getName());
            for (Field s : MetadataSorter.sortFields(refEntity, DisplayType.SERIES)) {
                queryFields.add(s.getName());
            }

            String sql = String.format("select %s from %s where ",
                    refEntity.getPrimaryField().getName(), refEntity.getName());
            for (String qf : queryFields) {
                sql += String.format("%s = '%s' or ", qf, StringEscapeUtils.escapeSql((String) val2Text));
            }
            sql = sql.substring(0, sql.length() - 4);

            query = Application.createQueryNoFilter(sql);
        }

        Object[] found = query.unique();
        return found != null ? (ID) found[0] : null;
    }

    /**
     * @param field
     * @param cell
     * @return
     */
    protected Date checkoutDateValue(Field field, Cell cell) {
        Date date = cell.asDate();
        if (date != null) return date;

        if (cell.isEmpty()) return null;

        String date2str = cell.asString();
        // 2017/11/19 11:07
        if (date2str.contains("/")) {
            return cell.asDate(new String[]{"yyyy/M/d H:m:s", "yyyy/M/d H:m", "yyyy/M/d"});
        }
        return null;
    }

    /**
     * @param field
     * @param cell
     * @return
     */
    protected Long checkoutMultiSelectValue(Field field, Cell cell) {
        String val = cell.asString();
        if (StringUtils.isBlank(val)) return null;

        String[] vals = val.split("[,，;；]");

        long mVal = 0;
        for (String s : vals) {
            mVal += MultiSelectManager.instance.findMultiItemByLabel(s.trim(), field);
        }

        return mVal == 0 ? null : mVal;
    }
}
