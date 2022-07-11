/*!
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
import com.rebuild.core.service.BaseService;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.ServiceSpec;
import com.rebuild.core.service.files.AttachmentAwareObserver;
import com.rebuild.core.service.general.recyclebin.RecycleBinCleanerJob;
import com.rebuild.core.service.general.recyclebin.RecycleStore;
import com.rebuild.core.support.general.N2NReferenceSupport;
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
        this.delegateService = new BaseService(aPMFactory);

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
        final Record before = countObservers() > 0 ? recordSnap(record) : null;

        record = delegateService.update(record);

        if (countObservers() > 0) {
            setChanged();
            notifyObservers(OperatingContext.create(UserContextHolder.getUser(), BizzPermission.UPDATE, before, record));
        }
        return record;
    }

    @Override
    public int delete(ID recordId) {
        final ID currentUser = UserContextHolder.getUser();

        Record deleted = null;
        if (countObservers() > 0) {
            deleted = EntityHelper.forUpdate(recordId, currentUser);
            deleted = recordSnap(deleted);

            // 删除前触发，做一些状态保持
            setChanged();
            notifyObservers(OperatingContext.create(currentUser, DELETE_BEFORE, deleted, null));
        }

        int affected = delegateService.delete(recordId);

        if (countObservers() > 0) {
            setChanged();
            notifyObservers(OperatingContext.create(currentUser, BizzPermission.DELETE, deleted, null));
        }
        return affected;
    }

    /**
     * 用于操作前获取原记录
     *
     * @param base
     * @return
     */
    protected Record recordSnap(Record base) {
        final ID primaryId = base.getPrimary();
        Assert.notNull(primaryId, "Record primary cannot be null");

        StringBuilder sql = new StringBuilder("select ");
        for (Iterator<String> iter = base.getAvailableFieldIterator(); iter.hasNext(); ) {
            sql.append(iter.next()).append(',');
        }
        sql.deleteCharAt(sql.length() - 1);
        sql.append(" from ")
                .append(base.getEntity().getName())
                .append(" where ")
                .append(base.getEntity().getPrimaryField().getName())
                .append(" = ?");

        Record snap = Application.createQueryNoFilter(sql.toString()).setParameter(1, primaryId).record();
        if (snap == null) {
            throw new NoRecordFoundException(primaryId);
        }

        N2NReferenceSupport.fillN2NValues(snap);
        return snap;
    }

    /**
     * 使用回收站
     *
     * @param recordId
     * @return 返回 null 表示没开启
     */
    protected RecycleStore useRecycleStore(ID recordId) {
        final ID currentUser = UserContextHolder.getUser();

        RecycleStore recycleBin = null;
        if (RecycleBinCleanerJob.isEnableRecycleBin()) {
            recycleBin = new RecycleStore(currentUser);
        } else {
            log.warn("RecycleBin inactivated! DELETE {} by {}", recordId, currentUser);
        }

        if (recycleBin != null && recycleBin.add(recordId)) {
            return recycleBin;
        } else {
            return null;
        }
    }
}
