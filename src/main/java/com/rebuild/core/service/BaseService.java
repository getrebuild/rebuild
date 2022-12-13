/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import cn.devezhao.persist4j.*;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyTag;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.QuickCodeReindexTask;
import com.rebuild.utils.Callable2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * 基础服务类
 *
 * @author devezhao
 * @since 01/04/2019
 */
public class BaseService extends InternalPersistService {

    public BaseService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return 0;
    }

    @Override
    public Record create(Record record) {
        setQuickCodeValue(record);
        Callable2<Integer, Record> call1 = processN2NReference(record, Boolean.TRUE);
        Callable2<Integer, Record> call2 = processTag(record, Boolean.TRUE);

        record = super.create(record);
        if (call1 != null) call1.call(record);
        if (call2 != null) call2.call(record);
        return record;
    }

    @Override
    public Record update(Record record) {
        setQuickCodeValue(record);
        Callable2<Integer, Record> call1 = processN2NReference(record, Boolean.FALSE);
        Callable2<Integer, Record> call2 = processTag(record, Boolean.FALSE);

        record = super.update(record);
        if (call1 != null) call1.call(record);
        if (call2 != null) call2.call(record);
        return record;
    }

    /**
     * 设置助记码
     *
     * @param record
     */
    private void setQuickCodeValue(Record record) {
        // 已设置则不再设置
        if (record.hasValue(EntityHelper.QuickCode)) return;
        // 无助记码字段
        if (!record.getEntity().containsField(EntityHelper.QuickCode)) return;

        String quickCode = QuickCodeReindexTask.generateQuickCode(record);
        if (quickCode != null) {
            if (StringUtils.isBlank(quickCode)) record.setNull(EntityHelper.QuickCode);
            else record.setString(EntityHelper.QuickCode, quickCode);
        }
    }

    /**
     * 多引用存在独立的表
     *
     * @param record
     * @param isNew
     * @return
     */
    private Callable2<Integer, Record> processN2NReference(final Record record, boolean isNew) {
        final Entity entity = record.getEntity();
        final Field[] n2nFields = MetadataSorter.sortFields(entity, DisplayType.N2NREFERENCE);
        if (n2nFields.length == 0) return null;

        final Record n2nRecord = EntityHelper.forNew(EntityHelper.NreferenceItem, UserService.SYSTEM_USER);
        n2nRecord.setString("belongEntity", entity.getName());

        final List<Record> addItems = new ArrayList<>();
        final List<ID> delItems = new ArrayList<>();

        final Map<String, ID[]> holdN2NValues = new HashMap<>();

        for (Field n2nField : n2nFields) {
            ID[] newRefs;
            if (isNew) {
                newRefs = record.getIDArray(n2nField.getName());
                if (newRefs == null || newRefs.length == 0) continue;
            } else {
                if (record.hasValue(n2nField.getName())) {
                    Object maybeNull = record.getObjectValue(n2nField.getName());
                    newRefs = NullValue.is(maybeNull) ? ID.EMPTY_ID_ARRAY : (ID[]) maybeNull;
                } else {
                    continue;
                }
            }

            // 保持值
            holdN2NValues.put(n2nField.getName(), newRefs);

            // 仅保留第一个用于标识是否为空
            if (newRefs.length == 0) record.setNull(n2nField.getName());
            else record.setIDArray(n2nField.getName(), new ID[] { newRefs[0] });

            // 哪个字段
            n2nRecord.setString("belongField", n2nField.getName());

            // 新建
            if (isNew) {
                for (ID refId : newRefs) {
                    Record clone = n2nRecord.clone();
                    clone.setID("referenceId", refId);
                    addItems.add(clone);
                }
            }
            // 更新
            else {
                Object[][] before = getPersistManagerFactory().createQuery(
                        "select referenceId,itemId from NreferenceItem where belongField = ? and recordId = ?")
                        .setParameter(1, n2nField.getName())
                        .setParameter(2, record.getPrimary())
                        .array();

                Map<ID, ID> beforeRefs = new HashMap<>();
                for (Object[] o : before) {
                    beforeRefs.put((ID) o[0], (ID) o[1]);
                }

                Set<ID> afterRefs = new LinkedHashSet<>();
                CollectionUtils.addAll(afterRefs, newRefs);

                for (Iterator<ID> iter = afterRefs.iterator(); iter.hasNext(); ) {
                    ID a = iter.next();
                    if (beforeRefs.containsKey(a)) {
                        beforeRefs.remove(a);
                        iter.remove();
                    }
                }

                for (Map.Entry<ID, ID> e : beforeRefs.entrySet()) {
                    if (!afterRefs.contains(e.getKey())) {
                        delItems.add(e.getValue());
                    }
                }

                n2nRecord.setID("recordId", record.getPrimary());
                for (ID refId : afterRefs) {
                    Record clone = n2nRecord.clone();
                    clone.setID("referenceId", refId);
                    addItems.add(clone);
                }
            }
        }

        if (addItems.isEmpty() && delItems.isEmpty()) return null;

        // 新建
        if (isNew) {
            return argv -> {
                // 还原
                for (Map.Entry<String, ID[]> e : holdN2NValues.entrySet()) {
                    argv.setIDArray(e.getKey(), e.getValue());
                }

                PersistManager pm = getPersistManagerFactory().createPersistManager();
                for (Record item : addItems) {
                    item.setID("recordId", argv.getPrimary());
                    pm.save(item);
                }
                return addItems.size();
            };
        }
        // 更新
        else {
            return argv -> {
                // 还原
                for (Map.Entry<String, ID[]> e : holdN2NValues.entrySet()) {
                    argv.setIDArray(e.getKey(), e.getValue());
                }

                PersistManager pm = getPersistManagerFactory().createPersistManager();
                for (Record item : addItems) {
                    pm.save(item);
                }
                if (!delItems.isEmpty()) {
                    pm.delete(delItems.toArray(new ID[0]));
                }
                return addItems.size() + delItems.size();
            };
        }
    }

    /**
     * 标签存在独立的表
     *
     * @param record
     * @param isNew
     * @return
     * @see #processN2NReference(Record, boolean)
     */
    private Callable2<Integer, Record> processTag(final Record record, boolean isNew) {
        final Entity entity = record.getEntity();
        final Field[] tagFields = MetadataSorter.sortFields(entity, DisplayType.TAG);
        if (tagFields.length == 0) return null;

        final Record tagRecord = EntityHelper.forNew(EntityHelper.TagItem, UserService.SYSTEM_USER);
        tagRecord.setString("belongEntity", entity.getName());

        final List<Record> addItems = new ArrayList<>();
        final List<ID> delItems = new ArrayList<>();

        final Map<String, String[]> holdTagValues = new HashMap<>();

        for (Field tagField : tagFields) {
            String[] newTags;
            if (isNew) {
                newTags = cleanNameArray(record.getString(tagField.getName()));
                if (newTags.length == 0) continue;
            } else {
                if (record.hasValue(tagField.getName())) {
                    newTags = cleanNameArray(record.getObjectValue(tagField.getName()));
                } else {
                    continue;
                }
            }

            // 保持值
            holdTagValues.put(tagField.getName(), newTags);

            // 仅保留第一个用于标识是否为空
            if (newTags.length == 0) record.setNull(tagField.getName());
            else record.setString(tagField.getName(), newTags[0]);

            // 哪个字段
            tagRecord.setString("belongField", tagField.getName());

            // 新建
            if (isNew) {
                for (String tagName : newTags) {
                    Record clone = tagRecord.clone();
                    clone.setString("tagName", tagName);
                    addItems.add(clone);
                }
            }
            // 更新
            else {
                Object[][] before = getPersistManagerFactory().createQuery(
                        "select tagName,itemId from TagItem where belongField = ? and recordId = ?")
                        .setParameter(1, tagField.getName())
                        .setParameter(2, record.getPrimary())
                        .array();

                Map<String, ID> beforeTags = new HashMap<>();
                for (Object[] o : before) {
                    beforeTags.put((String) o[0], (ID) o[1]);
                }

                Set<String> afterTags = new LinkedHashSet<>();
                CollectionUtils.addAll(afterTags, newTags);

                for (Iterator<String> iter = afterTags.iterator(); iter.hasNext(); ) {
                    String a = iter.next();
                    if (beforeTags.containsKey(a)) {
                        beforeTags.remove(a);
                        iter.remove();
                    }
                }

                for (Map.Entry<String, ID> e : beforeTags.entrySet()) {
                    if (!afterTags.contains(e.getKey())) {
                        delItems.add(e.getValue());
                    }
                }

                tagRecord.setID("recordId", record.getPrimary());
                for (String tagName : afterTags) {
                    Record clone = tagRecord.clone();
                    clone.setString("tagName", tagName);
                    addItems.add(clone);
                }
            }
        }

        if (addItems.isEmpty() && delItems.isEmpty()) return null;

        // 新建
        if (isNew) {
            return argv -> {
                // 还原
                for (Map.Entry<String, String[]> e : holdTagValues.entrySet()) {
                    argv.setString(e.getKey(), StringUtils.join(e.getValue(), EasyTag.VALUE_SPLIT));
                }

                PersistManager pm = getPersistManagerFactory().createPersistManager();
                for (Record item : addItems) {
                    item.setID("recordId", argv.getPrimary());
                    pm.save(item);
                }
                return addItems.size();
            };
        }
        // 更新
        else {
            return argv -> {
                // 还原
                for (Map.Entry<String, String[]> e : holdTagValues.entrySet()) {
                    argv.setString(e.getKey(), StringUtils.join(e.getValue(), EasyTag.VALUE_SPLIT));
                }

                PersistManager pm = getPersistManagerFactory().createPersistManager();
                for (Record item : addItems) {
                    pm.save(item);
                }
                if (!delItems.isEmpty()) {
                    pm.delete(delItems.toArray(new ID[0]));
                }
                return addItems.size() + delItems.size();
            };
        }
    }

    private String[] cleanNameArray(Object raw) {
        if (NullValue.isNull(raw) || StringUtils.isBlank((String) raw)) return new String[0];

        List<String> list = new ArrayList<>();
        for (String s : ((String) raw).split(EasyTag.VALUE_SPLIT_RE)) {
            if (StringUtils.isNotBlank(s)) list.add(s.trim());
        }
        return list.toArray(new String[0]);
    }
}
