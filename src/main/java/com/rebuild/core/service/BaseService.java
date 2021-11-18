/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import cn.devezhao.persist4j.*;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.privileges.UserService;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

/**
 * 基础服务类
 *
 * @author zhaofang123@gmail.com
 * @since 05/21/2017
 */
public abstract class BaseService implements ServiceSpec {

    private final PersistManagerFactory aPMFactory;

    protected BaseService(PersistManagerFactory aPMFactory) {
        this.aPMFactory = aPMFactory;
    }

    @Override
    public Record create(Record record) {
        Callable2 call = processN2NReference(record, true);

        record = aPMFactory.createPersistManager().save(record);
        if (call != null) call.call(record.getPrimary());
        return record;
    }

    @Override
    public Record update(Record record) {
        Callable2 call = processN2NReference(record, false);

        record = aPMFactory.createPersistManager().update(record);
        if (call != null) call.call(null);
        return record;
    }

    @Override
    public int delete(ID recordId) {
        int affected = aPMFactory.createPersistManager().delete(recordId);
        Application.getRecordOwningCache().cleanOwningUser(recordId);
        return affected;
    }

    @Override
    public String toString() {
        if (getEntityCode() > 0) {
            return "service." + aPMFactory.getMetadataFactory().getEntity(getEntityCode()).getName() + "@" + Integer.toHexString(hashCode());
        } else {
            return super.toString();
        }
    }

    /**
     * @return
     */
    public PersistManagerFactory getPersistManagerFactory() {
        return aPMFactory;
    }

    /**
     * 多引用存在独立的表
     *
     * @param record
     */
    private Callable2 processN2NReference(Record record, boolean isNew) {
        Entity entity = record.getEntity();
        Field[] n2nFields = MetadataSorter.sortFields(entity, DisplayType.N2NREFERENCE);
        if (n2nFields.length == 0) return null;

        final Record n2nRecord = EntityHelper.forNew(EntityHelper.NreferenceItem, UserService.SYSTEM_USER);
        n2nRecord.setString("belongEntity", entity.getName());

        final Set<Record> addItems = new HashSet<>();
        final Set<ID> delItems = new HashSet<>();

        for (Field n2nField : n2nFields) {
            ID[] idRefs = record.getIDArray(n2nField.getName());
            if (idRefs.length == 0 && isNew) continue;

            // 置空自身
            record.setNull(n2nField.getName());
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
                Object[][] before = aPMFactory.createQuery(
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
            // argv = 主键
            return argv -> {
                PersistManager pm = aPMFactory.createPersistManager();
                for (Record item : addItems) {
                    item.setID("recordId", (ID) argv);
                    pm.save(item);
                }
                return addItems.size();
            };

        }
        // 更新
        else {
            return argv -> {
                PersistManager pm = aPMFactory.createPersistManager();
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
