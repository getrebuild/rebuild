/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.files;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.general.OperatingObserver;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 更新媒体字段到附件表
 *
 * @author devezhao
 * @since 12/25/2018
 */
@Slf4j
public class AttachmentAwareObserver extends OperatingObserver {

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void onCreate(OperatingContext context) {
        Record record = context.getAfterRecord();
        Field[] fileFields = MetadataSorter.sortFields(record.getEntity(), DisplayType.FILE, DisplayType.IMAGE);
        if (fileFields.length == 0) return;

        List<Record> creates = new ArrayList<>();
        for (Field field : fileFields) {
            if (record.hasValue(field.getName(), false)) {
                JSONArray files = parseFilesJson(record.getObjectValue(field.getName()));
                for (Object file : files) {
                    Record c = buildAttachment(
                            field, context.getAfterRecord().getPrimary(), (String) file, context.getOperator());
                    creates.add(c);
                }
            }
        }
        if (creates.isEmpty()) return;

        Application.getCommonsService().createOrUpdate(creates.toArray(new Record[0]), false);
    }

    @Override
    public void onUpdate(OperatingContext context) {
        Record record = context.getAfterRecord();
        Field[] fileFields = MetadataSorter.sortFields(record.getEntity(), DisplayType.FILE, DisplayType.IMAGE);
        if (fileFields.length == 0) return;

        Record before = context.getBeforeRecord();

        // 4.2 标记删除
        List<Record> createsAndUpdates = new ArrayList<>();
        for (Field field : fileFields) {
            String fieldName = field.getName();
            if (record.hasValue(fieldName)) {
                JSONArray beforeFiles = parseFilesJson(before.getObjectValue(fieldName));  // 修改前
                JSONArray afterFiles = parseFilesJson(record.getObjectValue(fieldName));   // 修改后

                for (Iterator<Object> iter = afterFiles.iterator(); iter.hasNext(); ) {
                    Object a = iter.next();
                    if (beforeFiles.contains(a)) {
                        beforeFiles.remove(a);
                        iter.remove();
                    }
                }

                if (log.isDebugEnabled()) {
                    log.debug("Remove ... {}", beforeFiles);
                    log.debug("Add ... {}", afterFiles);
                }

                for (Object o : beforeFiles) {
                    Object[] delete = Application.createQueryNoFilter(
                            "select attachmentId from Attachment where belongEntity = ? and belongField = ? and filePath = ?")
                            .setParameter(1, field.getOwnEntity().getEntityCode())
                            .setParameter(2, fieldName)
                            .setParameter(3, o)
                            .unique();
                    if (delete != null) {
                        Record d = EntityHelper.forUpdate((ID) delete[0], UserService.SYSTEM_USER, false);
                        d.setBoolean(EntityHelper.IsDeleted, true);
                        d.setDate(EntityHelper.ModifiedOn, CalendarUtils.now());
                        createsAndUpdates.add(d);
                    }
                }

                for (Object o : afterFiles) {
                    Record c = buildAttachment(
                            field, context.getAfterRecord().getPrimary(), (String) o, context.getOperator());
                    createsAndUpdates.add(c);
                }
            }
        }
        if (createsAndUpdates.isEmpty()) return;

        Application.getCommonsService()
                .createOrUpdate(createsAndUpdates.toArray(new Record[0]), false);
    }

    @Override
    public void onDelete(OperatingContext context) {
        Record record = context.getBeforeRecord();
        Field[] fileFields = MetadataSorter.sortFields(record.getEntity(), DisplayType.FILE, DisplayType.IMAGE);
        if (fileFields.length == 0) return;

        Object[][] array = Application.createQueryNoFilter(
                "select attachmentId from Attachment where relatedRecord = ?")
                .setParameter(1, record.getPrimary())
                .array();
        if (array.length == 0) return;

        // 4.2 标记删除
        List<Record> updates = new ArrayList<>();
        for (Object[] o : array) {
            Record d = EntityHelper.forUpdate((ID) o[0], UserService.SYSTEM_USER, false);
            d.setBoolean(EntityHelper.IsDeleted, true);
            d.setDate(EntityHelper.ModifiedOn, CalendarUtils.now());
            updates.add(d);
        }

        Application.getCommonsService()
                .createOrUpdate(updates.toArray(new Record[0]), false);
    }

    private JSONArray parseFilesJson(Object files) {
        if (files instanceof JSON) return (JSONArray) files;
        else if (files == null || StringUtils.isBlank(files.toString())) return JSONUtils.EMPTY_ARRAY;
        else return JSON.parseArray(files.toString());
    }

    private Record buildAttachment(Field field, ID recordId, String filePath, ID user) {
        Record attach = FilesHelper.createAttachment(filePath, user);
        attach.setInt("belongEntity", field.getOwnEntity().getEntityCode());
        attach.setString("belongField", field.getName());
        attach.setID("relatedRecord", recordId);
        return attach;
    }
}