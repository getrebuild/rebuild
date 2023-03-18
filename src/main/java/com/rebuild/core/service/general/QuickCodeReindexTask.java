/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.hankcs.hanlp.HanLP;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyEmail;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.EasyPhone;
import com.rebuild.core.metadata.easymeta.EasyUrl;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.state.StateManager;
import com.rebuild.core.support.state.StateSpec;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * QuickCode 字段值重建
 *
 * @author devezhao
 * @since 12/28/2018
 */
@Slf4j
public class QuickCodeReindexTask extends HeavyTask<Integer> {

    private static final int PAGE_SIZE = 1000;

    final private Entity entity;

    /**
     * @param entity
     */
    public QuickCodeReindexTask(Entity entity) {
        super();
        this.entity = entity;
    }

    @Override
    protected Integer exec() {
        if (!entity.containsField(EntityHelper.QuickCode)) {
            throw new IllegalArgumentException("No QuickCode field found : " + entity);
        }

        Field nameFiled = entity.getNameField();
        String sql = String.format("select %s,%s,quickCode from %s order by createdOn",
                entity.getPrimaryField().getName(), nameFiled.getName(), entity.getName());

        int pageNo = 1;
        while (true) {
            List<Record> records = Application.createQueryNoFilter(sql)
                    .setLimit(PAGE_SIZE, pageNo * PAGE_SIZE - PAGE_SIZE)
                    .list();
            pageNo++;

            this.setTotal(records.size() + this.getTotal() + 1);
            for (Record o : records) {
                if (this.isInterrupt()) {
                    this.setInterrupted();
                    break;
                }

                try {
                    String quickCodeNew = generateQuickCode(o);
                    if (quickCodeNew == null) continue;
                    if (quickCodeNew.equals(o.getString(EntityHelper.QuickCode))) continue;

                    Record record = EntityHelper.forUpdate(o.getPrimary(), UserService.SYSTEM_USER, Boolean.FALSE);
                    if (StringUtils.isBlank(quickCodeNew)) {
                        record.setNull(EntityHelper.QuickCode);
                    } else {
                        record.setString(EntityHelper.QuickCode, quickCodeNew);
                    }
                    Application.getCommonsService().update(record, Boolean.FALSE);
                    this.addSucceeded();

                } finally {
                    this.addCompleted();
                }
            }

            if (records.size() < PAGE_SIZE || this.isInterrupted()) break;
        }

        this.setTotal(this.getTotal() - 1);
        return this.getSucceeded();
    }

    // --

    /**
     * 生成助记码
     *
     * @param record
     * @return
     */
    public static String generateQuickCode(Record record) {
        Entity entity = record.getEntity();
        if (!entity.containsField(EntityHelper.QuickCode)) return null;

        Field nameField = entity.getNameField();
        if (!record.hasValue(nameField.getName(), Boolean.FALSE)) return null;

        Object nameValue = record.getObjectValue(nameField.getName());
        DisplayType dt = EasyMetaFactory.getDisplayType(nameField);
        if (dt == DisplayType.TEXT || dt == DisplayType.SERIES
                || dt == DisplayType.EMAIL || dt == DisplayType.PHONE || dt == DisplayType.URL
                || dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
            nameValue = nameValue.toString();
        } else if (dt == DisplayType.PICKLIST) {
            nameValue = PickListManager.instance.getLabel((ID) nameValue);
        } else if (dt == DisplayType.STATE) {
            StateSpec state = StateManager.instance.findState(nameField, nameValue);
            nameValue = Language.L(state);
        } else if (dt == DisplayType.CLASSIFICATION) {
            nameValue = ClassificationManager.instance.getFullName((ID) nameValue);
        } else if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
            nameValue = CalendarUtils.getPlainDateFormat().format(nameValue);
        } else if (dt == DisplayType.LOCATION) {
            nameValue = nameValue.toString().split(CommonsUtils.COMM_SPLITER_RE)[0];
        } else {
            nameValue = null;
        }

        if (nameValue == null) return null;
        return generateQuickCode((String) nameValue);
    }

    /**
     * 生成助记码
     *
     * @param nameVal
     * @return
     */
    public static String generateQuickCode(String nameVal) {
        if (StringUtils.isBlank(nameVal)) return StringUtils.EMPTY;

        if (nameVal.length() > 100) nameVal = nameVal.substring(0, 100);

        if (EasyPhone.isPhone(nameVal) || EasyEmail.isEmail(nameVal) || EasyUrl.isUrl(nameVal)) return StringUtils.EMPTY;

        // 提取 0-9+a-z+A-Z+中文+空格，忽略特殊字符
        nameVal = nameVal.replaceAll("[^a-zA-Z0-9\\s\u4e00-\u9fa5]", "");
        // 忽略数字或小字母
        if (nameVal.matches("[a-z0-9]+")) return StringUtils.EMPTY;

        String quickCode = nameVal;

        if (nameVal.matches("[a-zA-Z0-9\\s]+")) {
            // 仅包含字母数字或空格
        } else {
            // v3.3 拼音全拼
            try {
                quickCode = HanLP.convertToPinyinString(nameVal, "", Boolean.FALSE);
            } catch (Exception e) {
                log.error("QuickCode shorting error : " + nameVal, e);
                quickCode = StringUtils.EMPTY;
            }
        }

        // 去除空格
        quickCode = quickCode.replaceAll(" ", "");
        return CommonsUtils.maxstr(quickCode, 50).toUpperCase();
    }
}
