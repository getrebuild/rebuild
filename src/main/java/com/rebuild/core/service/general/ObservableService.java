/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.BaseServiceImpl;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.ServiceSpec;
import com.rebuild.core.service.files.AttachmentAwareObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

/**
 * 可注入观察者的服务
 *
 * @author devezhao
 * @see OperatingObserver
 * @since 12/28/2018
 */
@Slf4j
public abstract class ObservableService extends Observable implements ServiceSpec {

    /**
     * 删除前触发的动作
     */
    public static final Permission DELETE_BEFORE = new BizzPermission("DELETE_BEFORE", 0, false);

    final protected ServiceSpec delegateService;

    /**
     * @param aPMFactory
     */
    protected ObservableService(PersistManagerFactory aPMFactory) {
        this.delegateService = new BaseServiceImpl(aPMFactory);

        // 默认监听者
        addObserver(new RevisionHistoryObserver());
        addObserver(new AttachmentAwareObserver());
    }

    @Override
    public synchronized void addObserver(Observer o) {
        super.addObserver(o);
        log.info("Add observer : {}", o);
    }

    @Override
    public Record createOrUpdate(Record record) {
        return record.getPrimary() == null ? create(record) : update(record);
    }

    @Override
    public Record create(Record record) {
        record = delegateService.create(record);

        if (countObservers() > 0) {
            setChanged();
            notifyObservers(OperatingContext.create(UserContextHolder.getUser(), BizzPermission.CREATE, null, record));
        }
        return record;
    }

    @Override
    public Record update(Record record) {
        final Record before = countObservers() > 0 ? record(record) : null;

        record = delegateService.update(record);

        if (countObservers() > 0) {
            setChanged();
            notifyObservers(OperatingContext.create(UserContextHolder.getUser(), BizzPermission.UPDATE, before, record));
        }
        return record;
    }

    @Override
    public int delete(ID recordId) {
        Record deleted = null;
        if (countObservers() > 0) {
            deleted = EntityHelper.forUpdate(recordId, UserContextHolder.getUser());
            deleted = record(deleted);

            // 删除前触发，做一些状态保持
            setChanged();
            notifyObservers(OperatingContext.create(UserContextHolder.getUser(), DELETE_BEFORE, deleted, null));
        }

        int affected = delegateService.delete(recordId);

        if (countObservers() > 0) {
            setChanged();
            notifyObservers(OperatingContext.create(UserContextHolder.getUser(), BizzPermission.DELETE, deleted, null));
        }
        return affected;
    }

    /**
     * 用于操作前获取原记录
     *
     * @param base
     * @return
     */
    protected Record record(Record base) {
        final ID primary = base.getPrimary();
        Assert.notNull(primary, "Record primary cannot be null");

        StringBuilder sql = new StringBuilder("select ");
        for (Iterator<String> iter = base.getAvailableFieldIterator(); iter.hasNext(); ) {
            sql.append(iter.next()).append(',');
        }
        sql.deleteCharAt(sql.length() - 1);
        sql.append(" from ").append(base.getEntity().getName());
        sql.append(" where ").append(base.getEntity().getPrimaryField().getName()).append(" = ?");

        Record current = Application.createQueryNoFilter(sql.toString()).setParameter(1, primary).record();
        if (current == null) {
            throw new NoRecordFoundException("ID : " + primary);
        }
        return current;
    }
}
