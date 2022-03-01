/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;

/**
 * 持久化服务
 *
 * @author Zixin (RB)
 * @since 05/21/2017
 */
public abstract class InternalPersistService implements ServiceSpec {

    private final PersistManagerFactory aPMFactory;

    protected InternalPersistService(PersistManagerFactory aPMFactory) {
        this.aPMFactory = aPMFactory;
    }

    @Override
    public Record create(Record record) {
        return aPMFactory.createPersistManager().save(record);
    }

    @Override
    public Record update(Record record) {
        return aPMFactory.createPersistManager().update(record);
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
}
