/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.bizz.InternalPermission;
import com.rebuild.core.service.BaseService;
import com.rebuild.core.service.ServiceSpec;
import com.rebuild.core.service.files.AttachmentAwareObserver;
import com.rebuild.core.service.general.recyclebin.RecycleBinCleanerJob;
import com.rebuild.core.service.general.recyclebin.RecycleStore;
import com.rebuild.core.service.query.QueryHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Observable;
import java.util.Observer;

/**
 * 可注入观察者的服务
 * !!! 此类非线程安全，应避免使用默认的单例默认 !!!
 *
 * @author devezhao
 * @see OperatingObserver
 * @since 12/28/2018
 */
@Slf4j
public abstract class ObservableService extends Observable implements ServiceSpec {

    final protected ServiceSpec delegateService;

    /**
     * @param aPMFactory
     */
    protected ObservableService(PersistManagerFactory aPMFactory) {
        this.delegateService = new BaseService(aPMFactory);

        for (Observer o : getOrderObservers()) {
            log.info("Add observer : {} for [ {} ] ", o, getEntityCode());
            addObserver(o);
        }
    }

    /**
     * @return
     */
    protected Observer[] getOrderObservers() {
        // 默认监听者
        return new Observer[] {
            new RevisionHistoryObserver(),  // 2
            new AttachmentAwareObserver(),  // 1
        };
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
        final Record before = countObservers() > 0 ? recordSnap(record, false) : null;

        record = delegateService.update(record);

        if (countObservers() > 0) {
            setChanged();
            notifyObservers(OperatingContext.create(UserContextHolder.getUser(), BizzPermission.UPDATE, before, record));
        }
        return record;
    }

    @Override
    public int delete(ID recordId) {
        ID currentUser = UserContextHolder.getRestoreUser();
        if (currentUser == null) currentUser = UserContextHolder.getUser();

        Record deleted = null;
        if (countObservers() > 0) {
            deleted = EntityHelper.forUpdate(recordId, currentUser, Boolean.FALSE);
            deleted = recordSnap(deleted, true);

            // 删除前触发，做一些状态保持
            setChanged();
            notifyObservers(OperatingContext.create(currentUser, InternalPermission.DELETE_BEFORE, deleted, deleted));
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
     * @param allFields 使用全部字段
     * @return
     */
    protected Record recordSnap(Record base, boolean allFields) {
        Assert.notNull(base.getPrimary(), "[primary] cannot be null");
        if (allFields) {
            return Application.getQueryFactory().recordNoFilter(base.getPrimary());
        }
        return QueryHelper.querySnap(base);
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
