/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.InternalPersistService;
import com.rebuild.core.support.CommonsLock;
import com.rebuild.core.support.i18n.Language;

/**
 * 配置类的 Service。在增/删/改时调用清理缓存方法
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/27
 */
public abstract class BaseConfigurationService extends InternalPersistService {

    protected BaseConfigurationService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public Record create(Record record) {
        record = super.create(record);
        cleanCache(record.getPrimary());
        return record;
    }

    @Override
    public Record update(Record record) {
        ID locked = hasLock() ? CommonsLock.getLockedUser(record.getPrimary()) : null;
        if (locked != null && !locked.equals(UserContextHolder.getUser())) {
            throw new DataSpecificationException(Language.L("操作失败 (已被锁定)"));
        }

        throwIfNotSelf(record.getPrimary());
        cleanCache(record.getPrimary());
        return super.update(record);
    }

    @Override
    public int delete(ID recordId) {
        ID locked = hasLock() ? CommonsLock.getLockedUser(recordId) : null;
        if (locked != null && !locked.equals(UserContextHolder.getUser())) {
            throw new DataSpecificationException(Language.L("操作失败 (已被锁定)"));
        }

        throwIfNotSelf(recordId);
        cleanCache(recordId);
        return super.delete(recordId);
    }

    /**
     * @param cfgid
     * @throws DataSpecificationException
     */
    protected void throwIfNotSelf(ID cfgid) throws DataSpecificationException {
        final ID user = UserContextHolder.getUser();
        if (UserHelper.isAdmin(user)) return;

        if (!UserHelper.isSelf(user, cfgid)) {
            throw new DataSpecificationException(Language.L("无权操作他人配置"));
        }
    }

    /**
     * @param record
     * @return
     */
    protected Record createOnly(Record record) {
        return super.create(record);
    }

    /**
     * @param record
     * @return
     */
    protected Record updateOnly(Record record) {
        return super.update(record);
    }

    /**
     * 增删改后清理缓存
     *
     * @param cfgid
     */
    abstract protected void cleanCache(ID cfgid);

    protected boolean hasLock() {
        return false;
    }
}
