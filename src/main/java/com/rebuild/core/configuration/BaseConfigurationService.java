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
import com.rebuild.core.configuration.general.ShareToManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.InternalPersistService;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.i18n.Language;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 配置类的 Service。在增/删/改时调用清理缓存方法
 *
 * @author devezhao-mbp
 * @since 2019/05/27
 */
public abstract class BaseConfigurationService extends InternalPersistService {

    protected BaseConfigurationService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public Record create(Record record) {
        record = super.create(putCreateBy4ShareTo(record));
        cleanCache(record.getPrimary());
        return record;
    }

    @Override
    public Record update(Record record) {
        throwIfNotSelf(record.getPrimary());
        cleanCache(record.getPrimary());
        return super.update(putCreateBy4ShareTo(record));
    }

    @Override
    public int delete(ID recordId) {
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

    /**
     * 配置是否被锁定
     *
     * @return
     * @see com.rebuild.core.support.CommonsLock
     */
    @Deprecated
    protected boolean hasLock() {
        return false;
    }

    /**
     * v34 非 admin 共享则设为系统用户，否则后续查询会有问题（修改了角色）
     *
     * @param cfgRecord
     */
    protected Record putCreateBy4ShareTo(Record cfgRecord) {
        final ID user = ObjectUtils.defaultIfNull(cfgRecord.getEditor(), UserContextHolder.getUser());
        if (UserService.ADMIN_USER.equals(user)) return cfgRecord;
        if (!cfgRecord.hasValue("shareTo")) return cfgRecord;

        if (cfgRecord.getPrimary() != null) {
            Object createBy = QueryHelper.queryFieldValue(cfgRecord.getPrimary(), EntityHelper.CreatedBy);
            if (UserService.ADMIN_USER.equals(createBy)) return cfgRecord;
        }

        String shareTo = cfgRecord.getString("shareTo");
        if (ShareToManager.SHARE_SELF.equalsIgnoreCase(shareTo)) {
            cfgRecord.setID(EntityHelper.CreatedBy, user);
        } else {
            cfgRecord.setID(EntityHelper.CreatedBy, UserService.SYSTEM_USER);
        }
        return cfgRecord;
    }
}
