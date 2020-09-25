/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ShareToManager;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.BaseService;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.support.i18n.Language;

/**
 * 配置类的 Service。在增/删/改时调用清理缓存方法
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/27
 */
public abstract class BaseConfigurationService extends BaseService {

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
        throwIfNotSelf(record.getPrimary());
        cleanCache(record.getPrimary());
        return super.update(record);
    }

    @Override
    public int delete(ID recordId) {
        throwIfNotSelf(recordId);
        cleanCache(recordId);
        return super.delete(recordId);
    }

    /**
     * @param cfgid
     */
    protected void throwIfNotSelf(ID cfgid) {
        ID user = Application.getCurrentUser();
        if (UserHelper.isAdmin(user)) {
            return;
        }

        if (!ShareToManager.isSelf(user, cfgid)) {
            throw new DataSpecificationException(Language.getLang("NotOpOtherUserSome", "Conf"));
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
}
