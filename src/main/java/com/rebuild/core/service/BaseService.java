/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.*;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
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
        Callable2<Integer, Record> call = processN2NReference(record, true);

        record = super.create(record);
        if (call != null) call.call(record);
        return record;
    }

    @Override
    public Record update(Record record) {
        setQuickCodeValue(record);
        Callable2<Integer, Record> call = processN2NReference(record, false);

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
    private Callable2<Integer, Record> processN2NReference(final Record record, boolean isNew) {
        Entity entity = record.getEntity();
        Field[] n2nFields = MetadataSorter.sortFields(entity, DisplayType.N2NREFERENCE);
        if (n2nFields.length == 0) return null;

        final Record n2nRecord = EntityHelper.forNew(EntityHelper.NreferenceItem, UserService.SYSTEM_USER);
        n2nRecord.setString("belongEntity", entity.getName());

        final List<Record> addItems = new ArrayList<>();
        final List<ID> delItems = new ArrayList<>();

        final Map<String, ID[]> holdN2NValues = new HashMap<>();

        for (Field n2nField : n2nFields) {
            ID[] idRefs;
            if (isNew) {
                idRefs = record.getIDArray(n2nField.getName());
                if (idRefs == null || idRefs.length == 0) continue;
            } else {
                if (record.hasValue(n2nField.getName())) {
                    Object maybeNull = record.getObjectValue(n2nField.getName());
                    idRefs = NullValue.is(maybeNull) ? ID.EMPTY_ID_ARRAY : (ID[]) maybeNull;
                } else {
                    continue;
                }
            }

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

                Set<ID> afterIds = new LinkedHashSet<>();
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
}
