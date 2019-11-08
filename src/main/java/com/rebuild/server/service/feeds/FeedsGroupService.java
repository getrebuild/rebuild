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

package com.rebuild.server.service.feeds;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.business.feeds.FeedsHelper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.BaseService;
import com.rebuild.server.service.bizz.UserService;

/**
 * 群组
 *
 * @author devezhao
 * @since 2019/11/8
 */
public class FeedsGroupService extends BaseService {

    protected FeedsGroupService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.FeedsGroup;
    }

    @Override
    public Record create(Record record) {
        record = super.create(record);
        FeedsHelper.findGroups(UserService.SYSTEM_USER, true, true);
        return record;
    }

    @Override
    public Record update(Record record) {
        record = super.update(record);
        FeedsHelper.findGroups(UserService.SYSTEM_USER, true, true);
        return record;
    }

    @Override
    public int delete(ID recordId) {
        int del = super.delete(recordId);
        FeedsHelper.findGroups(UserService.SYSTEM_USER, true,true);
        return del;
    }
}
