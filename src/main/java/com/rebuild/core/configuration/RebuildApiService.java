/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.AdminGuard;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Service;

/**
 * API
 *
 * @author devezhao
 * @since 2019/7/23
 */
@Service
public class RebuildApiService extends BaseConfigurationService implements AdminGuard {

    protected RebuildApiService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.RebuildApi;
    }

    @Override
    public Record create(Record record) {
        record.setString("appId", (100000000 + RandomUtils.nextInt(899999999)) + "");
        record.setString("appSecret", CodecUtils.randomCode(40));
        return super.create(record);
    }

    @Override
    public Record update(Record record) {
        record.removeValue("appId");
        record.removeValue("appSecret");
        return super.update(record);
    }

    @Override
    protected void cleanCache(ID cfgid) {
        Object[] cfg = Application.createQueryNoFilter(
                "select appId from RebuildApi where uniqueId = ?")
                .setParameter(1, cfgid)
                .unique();
        RebuildApiManager.instance.clean(cfg[0]);
    }
}
