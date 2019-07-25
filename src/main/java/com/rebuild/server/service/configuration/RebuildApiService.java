/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.service.configuration;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.RebuildApiManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.privileges.AdminGuard;

/**
 * API 鉴权
 *
 * @author devezhao
 * @since 2019/7/23
 */
public class RebuildApiService extends ConfigurationService implements AdminGuard {

    protected RebuildApiService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.RebuildApi;
    }

    @Override
    protected void cleanCache(ID configId) {
        Object[] cfg = Application.createQueryNoFilter(
                "select appId from RebuildApi where uniqueId = ?")
                .setParameter(1, configId)
                .unique();
        RebuildApiManager.instance.clean((String) cfg[0]);
    }
}
