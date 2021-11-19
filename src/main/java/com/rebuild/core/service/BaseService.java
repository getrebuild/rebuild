/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import cn.devezhao.persist4j.*;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.QuickCodeReindexTask;
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
        Callable2 call = processN2NReference(record, true);

        record = super.create(record);
        if (call != null) call.call(record);
        return record;
    }

    @Override
    public Record update(Record record) {
        setQuickCodeValue(record);
        Callable2 call = processN2NReference(record, false);

        record = super.update(record);
        if (call != null) call.call(record);
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
     */
    private Callable2 processN2NReference(final Record record, boolean isNew) {
        Entity entity = record.getEntity();
        Field[] n2nFields = MetadataSorter.sortFields(entity, DisplayType.N2NREFERENCE);
        if (n2nFields.length == 0) return null;

        final Record n2nRecord = EntityHelper.forNew(EntityHelper.NreferenceItem, UserService.SYSTEM_USER);
        n2nRecord.setString("belongEntity", entity.getName());

        final Set<Record> addItems = new HashSet<>();
        final Set<ID> delItems = new HashSet<>();

        final Map<String, ID[]> holdN2NValues = new HashMap<>();

        for (Field n2nField : n2nFields) {
            ID[] idRefs = record.getIDArray(n2nField.getName());
            if (idRefs.length == 0 && isNew) continue;

            // 保持值
            holdN2NValues.put(n2nField.getName(), idRefs);

            // 仅保留第一个用于标识是否为空
            if (idRefs.length == 0) record.setNull(n2nField.getName());
            else record.setIDArray(n2nField.getName(), new ID[] { idRefs[0] });

            // 哪个字段
            n2nRecord.setString("belongField", n2nField.getName());

            // 新建
            if (isNew) {
                for (ID idRef : idRefs) {
                    Record clone = n2nRecord.clone();
                    clone.setID("referenceId", idRef);
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

                Map<ID, ID> beforeIds = new HashMap<>();
                for (Object[] o : before) {
                    beforeIds.put((ID) o[0], (ID) o[1]);
                }

                Set<ID> afterIds = new HashSet<>();
                CollectionUtils.addAll(afterIds, idRefs);

                for (Iterator<ID> iter = afterIds.iterator(); iter.hasNext(); ) {
                    ID a = iter.next();
                    if (beforeIds.containsKey(a)) {
                        beforeIds.remove(a);
                        iter.remove();
                    }
                }

                for (Map.Entry<ID, ID> e : beforeIds.entrySet()) {
                    if (!afterIds.contains(e.getKey())) {
                        delItems.add(e.getValue());
                    }
                }

                n2nRecord.setID("recordId", record.getPrimary());
                for (ID idRef : afterIds) {
                    Record clone = n2nRecord.clone();
                    clone.setID("referenceId", idRef);
                    addItems.add(clone);
                }
            }
        }

        if (addItems.isEmpty() && delItems.isEmpty()) return null;

        // 新建
        if (isNew) {
            return argv -> {
                // 还原
                Record record2 = (Record) argv;
                for (Map.Entry<String, ID[]> e : holdN2NValues.entrySet()) {
                    record2.setIDArray(e.getKey(), e.getValue());
                }

                PersistManager pm = getPersistManagerFactory().createPersistManager();
                for (Record item : addItems) {
                    item.setID("recordId", record2.getPrimary());
                    pm.save(item);
                }
                return addItems.size();
            };
        }
        // 更新
        else {
            return argv -> {
                // 还原
                Record record2 = (Record) argv;
                for (Map.Entry<String, ID[]> e : holdN2NValues.entrySet()) {
                    record2.setIDArray(e.getKey(), e.getValue());
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
     * @see java.util.concurrent.Callable
     */
    interface Callable2 {
        int call(Object argv);
    }
}
