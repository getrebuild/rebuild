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
import cn.devezhao.persist4j.record.FieldValueException;
import cn.devezhao.persist4j.record.RecordVisitor;
import cn.hutool.core.date.DateException;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.*;
import com.rebuild.core.metadata.impl.MetadataModificationException;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.state.StateManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.text.MessageFormat;
import java.time.LocalTime;
import java.util.*;

/**
 * 从 Cell[] 中解析结果 Record
 *
 * @author devezhao
 * @since 2019/12/4
 */
@Slf4j
public class RecordCheckout {

    final private List<String> traceLogs = new ArrayList<>();

    final private Map<Field, Integer> fieldsMapping;

    protected RecordCheckout(Map<Field, Integer> fieldsMapping) {
        this.fieldsMapping = fieldsMapping;
    }

    /**
     * @param record
     * @param row
     * @return
     */
    public Record checkout(Record record, Cell[] row) {
        for (Map.Entry<Field, Integer> e : this.fieldsMapping.entrySet()) {
            int cellIndex = e.getValue();
            if (cellIndex >= row.length) continue;

            Cell cellValue = row[cellIndex];
            if (cellValue == Cell.NULL || cellValue.isEmpty()) {
                continue;
            }

            Field field = e.getKey();
            Object value = checkoutFieldValue(field, cellValue, true);

            if (value != null) {
                record.setObjectValue(field.getName(), value);
            } else {
                putTraceLog(cellValue, Language.L(EasyMetaFactory.getDisplayType(field)));
            }
        }

        return record;
    }

    /**
     * 验证格式，如邮箱/URL等
     *
     * @param field
     * @param cell
     * @param validate
     * @return
     */
    protected Object checkoutFieldValue(Field field, Cell cell, boolean validate) {
        final DisplayType dt = EasyMetaFactory.getDisplayType(field);
        if (dt == DisplayType.NUMBER) {
            return cell.asLong();
        } else if (dt == DisplayType.DECIMAL) {
            return cell.asDouble();
        } else if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
            return checkoutDateValue(cell);
        } else if (dt == DisplayType.TIME) {
            return checkoutTimeValue(cell);
        } else if (dt == DisplayType.PICKLIST) {
            return checkoutPickListValue(field, cell);
        } else if (dt == DisplayType.CLASSIFICATION) {
            return checkoutClassificationValue(field, cell);
        } else if (dt == DisplayType.REFERENCE) {
            return checkoutReferenceValue(field, cell);
        } else if (dt == DisplayType.N2NREFERENCE) {
            return checkoutN2NReferenceValue(field, cell);
        } else if (dt == DisplayType.BOOL) {
            return cell.asBool();
        } else if (dt == DisplayType.STATE) {
            return checkoutStateValue(field, cell);
        } else if (dt == DisplayType.MULTISELECT) {
            return checkoutMultiSelectValue(field, cell);
        } else if (dt == DisplayType.FILE || dt == DisplayType.IMAGE) {
            return checkoutFileOrImage(cell);
        }

        String text = cell.asString();
        if (text != null) text = text.trim();

        // 格式验证
        if (validate) {
            if (dt == DisplayType.EMAIL) {
                return EasyEmail.isEmail(text) ? text : null;
            } else if (dt == DisplayType.URL || dt == DisplayType.AVATAR) {
                return EasyUrl.isUrl(text) ? text : null;
            } else if (dt == DisplayType.PHONE) {
                return EasyPhone.isPhone(text) ? text : null;
            }
        }

        return text;
    }

    protected ID checkoutPickListValue(Field field, Cell cell) {
        final String val = cell.asString();

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

    protected Integer checkoutStateValue(Field field, Cell cell) {
        final String val = cell.asString();

        try {
            return StateManager.instance.findState(field, val).getState();
        } catch (MetadataModificationException ignored) {
        }
        return null;
    }

    protected ID checkoutClassificationValue(Field field, Cell cell) {
        final String val = cell.asString();

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

    protected ID checkoutReferenceValue(Field field, Cell cell) {
        final String val = cell.asString();
        final Entity refEntity = field.getReferenceEntity();

        // 支持ID
        if (ID.isId(val) && ID.valueOf(val).getEntityCode().intValue() == refEntity.getEntityCode()) {
            ID checkId = ID.valueOf(val);
            Object exists = Application.getQueryFactory().uniqueNoFilter(checkId, refEntity.getPrimaryField().getName());
            if (exists == null) {
                log.warn("Reference ID `{}` not exists", checkId);
                return null;
            } else {
                return checkId;
            }
        }

        Object val2Text = checkoutFieldValue(refEntity.getNameField(), cell, false);
        if (val2Text == null) return null;

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

            StringBuilder sql = new StringBuilder(
                    String.format("select %s from %s where ",
                            refEntity.getPrimaryField().getName(), refEntity.getName()));
            for (String qf : queryFields) {
                sql.append(String.format("%s = '%s' or ",
                        qf, StringEscapeUtils.escapeSql((String) val2Text)));
            }
            sql = new StringBuilder(sql.substring(0, sql.length() - 4));

            query = Application.createQueryNoFilter(sql.toString());
        }

        Object[] found = query.unique();
        return found != null ? (ID) found[0] : null;
    }

    protected ID[] checkoutN2NReferenceValue(Field field, Cell cell) {
        final String val = cell.asString();

        Set<ID> ids = new LinkedHashSet<>();
        for (String s : val.split("[,，;；]")) {
            ID id = checkoutReferenceValue(field, new Cell(s, cell.getRowNo(), cell.getColumnNo()));
            if (id != null) ids.add(id);
        }
        return ids.toArray(new ID[0]);
    }

    protected Date checkoutDateValue(Cell cell) {
        Date date = cell.asDate();
        if (date != null) return date;

        String date2str = cell.asString();

        try {
            DateTime dt = DateUtil.parse(date2str);
            if (dt != null) date = dt.toJdkDate();
        } catch (DateException ignored) {
        }

        // 2017/11/19 11:07
        if (date == null && date2str.contains("/")) {
            return cell.asDate(new String[]{"yyyy/M/d H:m:s", "yyyy/M/d H:m", "yyyy/M/d"});
        }
        return date;
    }

    protected LocalTime checkoutTimeValue(Cell cell) {
        try {
            return RecordVisitor.tryParseTime(cell.asString());
        } catch (FieldValueException ignored) {
        }
        return null;
    }

    protected Long checkoutMultiSelectValue(Field field, Cell cell) {
        final String val = cell.asString();

        long mVal = 0;
        for (String s : val.split("[,，;；]")) {
            mVal += MultiSelectManager.instance.findMultiItemByLabel(s.trim(), field);
        }
        return mVal == 0 ? null : mVal;
    }

    protected String checkoutFileOrImage(Cell cell) {
        final String val = cell.asString();

        List<String> urls = new ArrayList<>();
        for (String s : val.split("[,，;；]")) {
            if (EasyUrl.isUrl(s)) urls.add(s);
        }
        return urls.isEmpty() ? null : JSON.toJSON(urls).toString();
    }

    /**
     * @return
     */
    public List<String> getTraceLogs() {
        return traceLogs;
    }

    private void putTraceLog(Cell cell, String log) {
        // A1 A2 ...
        int num = cell.getColumnNo();
        StringBuilder name = new StringBuilder();
        while (num >= 0) {
            int remainder = num % 26;
            name.insert(0, (char) (remainder + 65));
            //noinspection IntegerDivisionInFloatingPointContext
            num = (int) Math.floor(num / 26) - 1;
        }
        name.append(cell.getRowNo() + 1);

        traceLogs.add(name + ":" + log);
    }
}
