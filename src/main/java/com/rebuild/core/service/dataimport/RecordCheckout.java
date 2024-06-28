/*!
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
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyEmail;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.EasyPhone;
import com.rebuild.core.metadata.easymeta.EasyUrl;
import com.rebuild.core.metadata.impl.MetadataModificationException;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.state.StateManager;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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

    public static final String MVAL_SPLIT = "[,，;；]";

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
                if (field.getName().equalsIgnoreCase(EntityHelper.OwningUser)) {
                    User owning = Application.getUserStore().getUser((ID) value);
                    if (owning.getOwningDept() == null) {
                        putTraceLog(cellValue, Language.L(EasyMetaFactory.getDisplayType(field)));
                    } else {
                        // 用户部门联动
                        record.setObjectValue(field.getName(), value);
                        record.setID(EntityHelper.OwningDept, (ID) owning.getOwningDept().getIdentity());
                    }

                } else {
                    record.setObjectValue(field.getName(), value);
                }

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
     * @param verifyFormat
     * @return
     */
    protected Object checkoutFieldValue(Field field, Cell cell, boolean verifyFormat) {
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
            return cell.asBool() || "是".equals(cell.asString()) || "Y".equalsIgnoreCase(cell.asString());
        } else if (dt == DisplayType.STATE) {
            return checkoutStateValue(field, cell);
        } else if (dt == DisplayType.MULTISELECT) {
            return checkoutMultiSelectValue(field, cell);
        } else if (dt == DisplayType.FILE || dt == DisplayType.IMAGE) {
            return checkoutFileOrImage(cell);
        } else if (dt == DisplayType.TAG) {
            return checkoutTagValue(cell);
        }

        String text = cell.asString();
        if (text != null) text = text.trim();

        // 格式验证
        if (verifyFormat) {
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
        ID val2id = MetadataHelper.checkSpecEntityId(val, EntityHelper.PickList);
        if (val2id != null) {
            if (PickListManager.instance.getLabel(val2id) != null) {
                return val2id;
            } else {
                log.warn("No item of PickList found by ID {}", val2id);
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
        ID vla2id = MetadataHelper.checkSpecEntityId(val, EntityHelper.ClassificationData);
        if (vla2id != null) {
            if (ClassificationManager.instance.getName(vla2id) != null) {
                return vla2id;
            } else {
                log.warn("No item of Classification found by ID : {}", vla2id);
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
        ID vla2id = MetadataHelper.checkSpecEntityId(val, refEntity.getEntityCode());
        if (vla2id != null) {
            if (QueryHelper.exists(vla2id)) return vla2id;

            log.warn("Reference ID `{}` not exists", vla2id);
            return null;
        }

        Object val2Text = checkoutFieldValue(refEntity.getNameField(), cell, false);
        if (val2Text == null) return null;

        Query query;
        // 用户特殊处理
        if (refEntity.getEntityCode() == EntityHelper.User) {
            String sql = MessageFormat.format(
                    "select userId from User where loginName = ''{0}'' or email = ''{0}'' or fullName = ''{0}''",
                    CommonsUtils.escapeSql(val2Text));
            query = Application.createQueryNoFilter(sql);
        } else {
            // 查找引用实体的名称字段和自动编号字段
            Set<String> queryFields = new HashSet<>();
            queryFields.add(refEntity.getNameField().getName());
            // 名称字段又是引用字段
            if (!(val2Text instanceof ID)) {
                for (Field s : MetadataSorter.sortFields(refEntity, DisplayType.SERIES)) {
                    queryFields.add(s.getName());
                }
            }

            StringBuilder sql = new StringBuilder(
                    String.format("select %s from %s where ", refEntity.getPrimaryField().getName(), refEntity.getName()));
            for (String qf : queryFields) {
                sql.append(
                        String.format("%s = '%s' or ", qf, CommonsUtils.escapeSql(val2Text)));
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
        for (String s : val.split(MVAL_SPLIT)) {
            ID id = checkoutReferenceValue(field, new Cell(s, cell.getRowNo(), cell.getColumnNo()));
            if (id != null) ids.add(id);
        }
        return ids.toArray(new ID[0]);
    }

    protected Date checkoutDateValue(Cell cell) {
        Date date = cell.asDate();
        if (date != null) return date;
        return CommonsUtils.parseDate(cell.asString());
    }

    protected LocalTime checkoutTimeValue(Cell cell) {
        try {
            return RecordVisitor.tryParseTime(cell.asString());
        } catch (FieldValueException ignored) {
        }
        return null;
    }

    protected Long checkoutMultiSelectValue(Field field, Cell cell) {
        long mVal = 0;
        for (String s : cell.asString().split(MVAL_SPLIT)) {
            mVal += MultiSelectManager.instance.findMultiItemByLabel(s.trim(), field);
        }
        return mVal == 0 ? null : mVal;
    }

    protected String checkoutFileOrImage(Cell cell) {
        List<String> urls = new ArrayList<>();
        for (String s : cell.asString().split(MVAL_SPLIT)) {
            if (EasyUrl.isUrl(s) || s.startsWith("rb/")) urls.add(s);
        }
        return urls.isEmpty() ? null : JSON.toJSON(urls).toString();
    }

    protected String[] checkoutTagValue(Cell cell) {
        Set<String> mVal = new HashSet<>();
        for (String s : cell.asString().split(MVAL_SPLIT)) {
            if (StringUtils.isNotBlank(s)) mVal.add(s.trim());
        }
        return mVal.toArray(new String[0]);
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
            num = (int) (double) (num / 26) - 1;
        }
        name.append(cell.getRowNo() + 1);

        traceLogs.add(name + ":" + log);
    }
}
